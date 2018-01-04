package com.viifly.wafer.weapp.auth;

/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import com.viifly.wafer.WeixinConfig;
import com.viifly.wafer.database.SessionDatabaseService;
import com.viifly.wafer.util.AesUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;


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

        webClient.get("api.weixin.qq.com", "/sns/jscode2session")
                .addQueryParam("appid", appid)
                .addQueryParam("secret", secret)
                .addQueryParam("js_code", code)
                .addQueryParam("grant_type", "authorization_code")
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<JsonObject> response = ar.result();
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

                                JsonObject jsonObject = Json.decodeValue(dataStr, JsonObject.class);

                                sessionDatabaseService.saveUserInfo(jsonObject, sKey, sessionKey, saved -> {
                                    if (saved.succeeded()) {
                                        resultHandler.handle(Future.succeededFuture(saved.result()));
                                    } else {
                                        resultHandler.handle(Future.failedFuture(saved.cause()));
                                    }
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

                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });

        return this;
    }

    @Override
    public AuthService validation(String skey, Handler<AsyncResult<JsonObject>> resultHandler) {
        sessionDatabaseService.getUserInfoBySKey(skey, fetch -> {
            if (fetch.succeeded()) {
                JsonObject jsonObject = fetch.result();
                if (jsonObject.getBoolean("found")) {
                    resultHandler.handle(Future.succeededFuture(Json.decodeValue(jsonObject.getString("user_info"), JsonObject.class)));
                } else {
                    resultHandler.handle(Future.failedFuture(fetch.cause()));
                }
            } else {
                resultHandler.handle(Future.failedFuture(fetch.cause()));
            }
        });
        return this;
    }
}
