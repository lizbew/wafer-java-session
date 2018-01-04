package com.viifly.wafer.weapp.auth;

import com.viifly.wafer.WeixinConfig;
import com.viifly.wafer.database.SessionDatabaseService;
import com.viifly.wafer.database.SessionDatabaseVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
public class AuthServiceVerticle extends AbstractVerticle {

    public static final String CONFIG_AUTH_QUEUE = "auth.queue";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setSsl(true));

        WeixinConfig wxConfig = new WeixinConfig();
        wxConfig.setAppid(config().getString("wx.appid"));
        wxConfig.setSecret(config().getString("wx.secret"));

        SessionDatabaseService sessionDatabaseService = SessionDatabaseService.createProxy(vertx, SessionDatabaseVerticle.CONFIG_DB_QUEUE);

        AuthService.create(wxConfig, webClient, sessionDatabaseService, ready ->{
           if (ready.succeeded()) {
               ProxyHelper.registerService(AuthService.class, vertx, ready.result(), CONFIG_AUTH_QUEUE);
               startFuture.complete();
           } else {
               startFuture.fail(ready.cause());
           }
        });
    }
}
