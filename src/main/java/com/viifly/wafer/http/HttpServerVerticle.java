/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.viifly.wafer.http;

import com.viifly.wafer.CommonConstants;
import com.viifly.wafer.weapp.auth.AuthServiceVerticle;
import com.viifly.wafer.weapp.auth.rxjava.AuthService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {

    public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    private AuthService authService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        authService = com.viifly.wafer.weapp.auth.AuthService.createProxy(vertx.getDelegate(), AuthServiceVerticle.CONFIG_AUTH_QUEUE);

        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create());

        router.get("/").handler(this::indexHandler);
        router.get("/hello").handler(this::testHandler);
        router.get("/login").handler(this::weappLoginHandler);
        router.get("/me").handler(this::weappMeHandler);


        int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .rxListen(portNumber)
                .subscribe(s -> {
                    LOGGER.info("HTTP server running on port " + portNumber);
                    startFuture.complete();
                }, t -> {
                    LOGGER.error("Could not start a HTTP server", t);
                    startFuture.fail(t);
                });
    }

    private void indexHandler(RoutingContext context) {
        context.response().putHeader("Content-Type", "text/plain");
        context.response().end("hah");
    }

    private void weappLoginHandler(RoutingContext context) {
        String code = context.request().getHeader(CommonConstants.WX_HEADER_CODE);
        String encryptedData = context.request().getHeader(CommonConstants.WX_HEADER_ENCRYPTED_DATA);
        String iv = context.request().getHeader(CommonConstants.WX_HEADER_IV);

        LOGGER.info("code={}, iv={}, encryptedData={}", code, iv, encryptedData);

        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(encryptedData) || StringUtils.isEmpty(iv)) {
            context.response().setStatusCode(400);
            context.response().end("Miss Header");
            return;
        }

        authService.rxAuthorization(code, iv, encryptedData)
                .subscribe(session -> {
                    JsonObject retJson = new JsonObject()
                            .put("F2C224D4-2BCE-4C64-AF9F-A6D872000D1A", 1)
                            .put("session", session);

                    context.response().putHeader("Content-Type", "application/json");
                    context.response().end(retJson.encode());
                }, t -> {
                    context.response().setStatusCode(400);
                    context.response().end("getSessionKey Failed: " + t.getMessage());
                });

    }

    private void weappMeHandler(RoutingContext context) {
        String skey = context.request().getHeader(CommonConstants.WX_HEADER_SKEY);
        if (StringUtils.isEmpty(skey)) {
            context.response().setStatusCode(400);
            context.response().end("Miss Header");
            return;
        }

        authService.rxValidation(skey)
                .subscribe(result -> {
                    context.response().setStatusCode(200);
                    context.response().putHeader("Content-Type", "application/json");
                    context.response().end(result.encode());
                }, t -> {
                    JsonObject data = new JsonObject()
                            .put("noBody", true);
                    context.response().setStatusCode(401);
                    context.response().putHeader("Content-Type", "application/json");
                    context.response().end(data.encode());
                });

        /*
        JsonObject data = new JsonObject()
                .put("openId", "openId")
                .put("nickName", "nickName")
                .put("avatarUrl", "avatarUrl");
        context.response().setStatusCode(200);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(data.encode());
        */
    }

    private void testHandler(RoutingContext context) {
        context.response().setStatusCode(200);
        context.response().putHeader("Content-Type", "text/plain");
        context.response().end("OK");
    }

}
