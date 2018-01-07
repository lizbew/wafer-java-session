/**
 * Created on 2018/1/1.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.viifly.wafer;

import com.viifly.wafer.database.SessionDatabaseVerticle;
import com.viifly.wafer.http.HttpServerVerticle;
import com.viifly.wafer.weapp.auth.AuthServiceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import rx.Single;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        JsonObject mainConfig = config().copy();

        Single<String> dbserviceDeployment = vertx.rxDeployVerticle(SessionDatabaseVerticle.class.getName());
        dbserviceDeployment.flatMap(id -> {
            Single<String> authVerticleDeployment =
                    vertx.rxDeployVerticle(AuthServiceVerticle.class.getName(),
                            new DeploymentOptions().setConfig(mainConfig));
            return authVerticleDeployment;
        }).flatMap(id -> {
            Single<String> httpVerticleDeployment = vertx.rxDeployVerticle(HttpServerVerticle.class.getName(),
                    new DeploymentOptions().setInstances(2));
            return httpVerticleDeployment;
        }).subscribe(id -> startFuture.complete(), startFuture::fail);
    }

}
