package com.viifly.wafer.database;

import com.viifly.wafer.database.rxjava.SessionDatabaseService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * Created on 2018/1/5.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
@RunWith(VertxUnitRunner.class)
public class SessionDatabaseVerticleTest {
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(SessionDatabaseVerticle.class.getName(), context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testDbServiceSaveUserInfo(TestContext context) {
        final Async async = context.async();

        SessionDatabaseService sessionDatabaseService = com.viifly.wafer.database.SessionDatabaseService.createProxy(vertx, SessionDatabaseVerticle.CONFIG_DB_QUEUE);

        JsonObject decryptedData = new JsonObject()
                .put("openId", "aaa");
        sessionDatabaseService.saveUserInfo(decryptedData, "skey", "sessionKey", ar -> {
            if (ar.succeeded()) {
                JsonObject resultObj = ar.result();
                context.assertTrue(resultObj.containsKey("userInfo"));
            } else {
                System.out.printf(ar.cause().toString());
            }
            async.complete();
        });

    }
}
