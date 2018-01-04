package com.viifly.wafer.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Created on 2018/1/3.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
public class SessionDatabaseVerticle extends AbstractVerticle {
    public static final String CONFIG_DB_JDBC_URL = "db.jdbc.url";
    public static final String CONFIG_DB_JDBC_DRIVER_CLASS = "db.jdbc.driver_class";
    public static final String CONFIG_DB_JDBC_MAX_POOL_SIZE = "db.jdbc.max_pool_size";
    public static final String CONFIG_DB_QUEUE = "db.queue";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        JDBCClient dbClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getString(CONFIG_DB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
                .put("driver_class", config().getString(CONFIG_DB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbcDriver"))
                .put("max_pool_size", config().getInteger(CONFIG_DB_JDBC_MAX_POOL_SIZE, 30)));

        SessionDatabaseService.create(dbClient, ready -> {
            if (ready.succeeded()) {
                ProxyHelper.registerService(SessionDatabaseService.class, vertx, ready.result(),CONFIG_DB_QUEUE);
                startFuture.complete();
            } else {
                startFuture.fail(ready.cause());
            }
        });
    }
}
