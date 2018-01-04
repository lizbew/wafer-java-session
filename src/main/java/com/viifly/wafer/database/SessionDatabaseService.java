package com.viifly.wafer.database;

/**
 * Created on 2018/1/3.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;


@ProxyGen
public interface SessionDatabaseService {
    static SessionDatabaseService create(JDBCClient dbClient, Handler<AsyncResult<SessionDatabaseService>> readyHandler) {
        return new SessionDatabaseServiceImpl(dbClient, readyHandler);
    }

    static SessionDatabaseService createProxy(Vertx vertx, String address) {
        return new SessionDatabaseServiceVertxEBProxy(vertx, address);
    }

    @Fluent
    SessionDatabaseService saveUserInfo(JsonObject decryptedData, String skey, String sessionKey, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    SessionDatabaseService getUserInfoBySKey(String skey, Handler<AsyncResult<JsonObject>> resultHandler);
}
