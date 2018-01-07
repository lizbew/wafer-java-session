/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.viifly.wafer.weapp.auth;

import com.viifly.wafer.WeixinConfig;
import com.viifly.wafer.database.SessionDatabaseVerticle;
import com.viifly.wafer.database.rxjava.SessionDatabaseService;
import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.serviceproxy.ProxyHelper;


public class AuthServiceVerticle extends AbstractVerticle {

    public static final String CONFIG_AUTH_QUEUE = "auth.queue";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        WebClient webClient = WebClient.create(vertx, new WebClientOptions().setSsl(true));

        WeixinConfig wxConfig = new WeixinConfig();
        wxConfig.setAppid(config().getString("wx.appid"));
        wxConfig.setSecret(config().getString("wx.secret"));

        SessionDatabaseService sessionDatabaseService = com.viifly.wafer.database.SessionDatabaseService.createProxy(vertx.getDelegate(), SessionDatabaseVerticle.CONFIG_DB_QUEUE);

        AuthService.create(wxConfig, webClient, sessionDatabaseService, ready -> {
            if (ready.succeeded()) {
                ProxyHelper.registerService(AuthService.class, vertx.getDelegate(), ready.result(), CONFIG_AUTH_QUEUE);
                startFuture.complete();
            } else {
                startFuture.fail(ready.cause());
            }
        });
    }
}
