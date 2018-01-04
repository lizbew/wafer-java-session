package com.viifly.wafer.http;


/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */


import com.viifly.wafer.CommonConstants;
import com.viifly.wafer.weapp.auth.AuthService;
import com.viifly.wafer.weapp.auth.AuthServiceVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.AbstractVerticle;

public class HttpServerVerticle extends AbstractVerticle {

    public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    private AuthService authService;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        authService = AuthService.createProxy(vertx, AuthServiceVerticle.CONFIG_AUTH_QUEUE);

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
                .listen(portNumber, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port " + portNumber);
                        startFuture.complete();
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause());
                        startFuture.fail(ar.cause());
                    }
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

        authService.authorization(code, iv, encryptedData, ar -> {
            if (ar.succeeded()) {
                JsonObject jsonObject = ar.result();
                //String openid = jsonObject.getString("openid");
                //String sessionKey = jsonObject.getString("session_key");

                context.response().putHeader("Content-Type", "application/json");
                context.response().end(jsonObject.encode());
            } else {
                context.response().setStatusCode(400);
                context.response().end("getSessionKey Failed: " + ar.cause());
            }
        });

    }

    private void weappMeHandler(RoutingContext context) {
        String skey = context.request().getHeader(CommonConstants.WX_HEADER_SKEY);
        if (StringUtils.isEmpty(skey)) {
            context.response().setStatusCode(400);
            context.response().end("Miss Header");
            return;
        }

        authService.validation(skey, ar -> {
            if (ar.succeeded()) {
                context.response().setStatusCode(200);
                context.response().putHeader("Content-Type", "application/json");
                context.response().end(ar.result().encode());
            } else {
                JsonObject data = new JsonObject()
                        .put("noBody", true);
                context.response().setStatusCode(401);
                context.response().putHeader("Content-Type", "application/json");
                context.response().end(data.encode());
            }
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
