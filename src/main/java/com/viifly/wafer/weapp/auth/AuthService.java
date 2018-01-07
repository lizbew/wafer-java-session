package com.viifly.wafer.weapp.auth;

/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import com.viifly.wafer.WeixinConfig;
import com.viifly.wafer.database.rxjava.SessionDatabaseService;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.client.WebClient;

@ProxyGen
@VertxGen
public interface AuthService {

    @GenIgnore
    static AuthService create(WeixinConfig wxConfig, WebClient webClient, SessionDatabaseService sessionDatabaseService, Handler<AsyncResult<AuthService>> readyHandler) {
        return new AuthServiceImpl(wxConfig, webClient, sessionDatabaseService, readyHandler);
    }

    @GenIgnore
    static com.viifly.wafer.weapp.auth.rxjava.AuthService createProxy(Vertx vertx, String address) {
        return new com.viifly.wafer.weapp.auth.rxjava.AuthService(new AuthServiceVertxEBProxy(vertx, address));
    }

    @Fluent
    AuthService authorization(String code, String iv, String encryptedData, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    AuthService validation(String skey, Handler<AsyncResult<JsonObject>> resultHandler);
}
