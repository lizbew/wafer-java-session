package com.viifly.wafer;

/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

import com.viifly.wafer.database.SessionDatabaseVerticle;
import com.viifly.wafer.http.HttpServerVerticle;
import com.viifly.wafer.weapp.auth.AuthServiceVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;


public class MainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Future<String> dbserviceDeployment = Future.future();
        vertx.deployVerticle(SessionDatabaseVerticle.class.getName(), dbserviceDeployment.completer());

        dbserviceDeployment.compose(ret -> {

            DeploymentOptions authDeployOptions = new DeploymentOptions();
            authDeployOptions.setConfig(config().copy());

            Future<String> authVerticleDeployment = Future.future();
            vertx.deployVerticle(AuthServiceVerticle.class.getName(), authDeployOptions, authVerticleDeployment.completer());

            return authVerticleDeployment.compose(id ->{
                Future<String> httpVerticleDeployment = Future.future();
                vertx.deployVerticle(
                        HttpServerVerticle.class.getName(),
                        new DeploymentOptions().setInstances(2),
                        httpVerticleDeployment.completer());
                return httpVerticleDeployment;
            });
        }).setHandler(ar -> {
            if (ar.succeeded()) {
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
    }

}
