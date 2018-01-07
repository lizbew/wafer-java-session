package com.viifly.wafer.weapp.auth;

/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import com.viifly.wafer.WeixinConfig;
import com.viifly.wafer.database.rxjava.SessionDatabaseService;
import com.viifly.wafer.util.AesUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthServiceImpl implements AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);

    private WeixinConfig wxConfig;
    private WebClient webClient;

    private SessionDatabaseService sessionDatabaseService;

    public AuthServiceImpl(WeixinConfig wxConfig, WebClient webClient, SessionDatabaseService sessionDatabaseService, Handler<AsyncResult<AuthService>> readyHandler) {
        this.wxConfig = wxConfig;
        this.webClient = webClient;
        this.sessionDatabaseService = sessionDatabaseService;

        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public AuthService authorization(String code, String iv, String encryptedData, Handler<AsyncResult<JsonObject>> resultHandler) {
        String appid = wxConfig.getAppid();
        String secret = wxConfig.getSecret();

        LOGGER.debug("code={}, iv={}, encryptedData:\n{}", code, iv, encryptedData);

        webClient.get(443, "api.weixin.qq.com", "/sns/jscode2session")
                .addQueryParam("appid", appid)
                .addQueryParam("secret", secret)
                .addQueryParam("js_code", code)
                .addQueryParam("grant_type", "authorization_code")
                .as(BodyCodec.jsonObject())
                .rxSend()
                .subscribe(response -> {
                    //if (response.statusCode() == 200)
                    LOGGER.debug("jscode2session, response.statusCode()={}", response.statusCode());

                    JsonObject json = response.body();
                    LOGGER.debug("jscode2session, json={}", json);

                    if (json.getString("openid") != null) {
                        String sessionKey = json.getString("session_key");
                        String sKey = DigestUtils.sha1Hex(sessionKey);

                        try {
                            String dataStr = AesUtils.aesDecrypt(sessionKey, iv, encryptedData);
                            LOGGER.debug("decryptedData:\n{}", dataStr);

                            JsonObject jsonObject = new JsonObject(dataStr);


                            sessionDatabaseService.rxSaveUserInfo(jsonObject, sKey, sessionKey)
                                    .subscribe(result -> {
                                        resultHandler.handle(Future.succeededFuture(result));
                                    }, t -> {
                                        resultHandler.handle(Future.failedFuture(t));
                                    });

                                /*
                                JsonObject retJson = new JsonObject()
                                        .put("userinfo", jsonObject)
                                        .put("skey", sKey);
                                resultHandler.handle(Future.succeededFuture(retJson));
                                */
                        } catch (Exception e) {
                            LOGGER.error("AesUtils.aesDecrypt Error", e);
                            resultHandler.handle(Future.failedFuture(e.getMessage()));
                        }

                    } else {
                        resultHandler.handle(Future.failedFuture(json.getString("errmsg")));
                    }

                }, t -> {
                    resultHandler.handle(Future.failedFuture(t));
                });


        return this;
    }

    @Override
    public AuthService validation(String skey, Handler<AsyncResult<JsonObject>> resultHandler) {
        sessionDatabaseService.rxGetUserInfoBySKey(skey)
                .subscribe(data -> {
                    if (data.getBoolean("found")) {
                        resultHandler.handle(Future.succeededFuture(new JsonObject(data.getString("USER_INFO"))));
                    } else {
                        resultHandler.handle(Future.failedFuture("Not Found"));
                    }
                }, t -> {
                    resultHandler.handle(Future.failedFuture(t));
                });

        return this;
    }
}
