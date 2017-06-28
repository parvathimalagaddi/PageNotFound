package com.cmad.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

public class QAForumMainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) throws Exception {

        vertx.deployVerticle("com.cmad.jwt.JWTServiceVerticle",
                new DeploymentOptions().setWorker(true));

    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}