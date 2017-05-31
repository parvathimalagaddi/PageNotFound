package com.cmad.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class QAForumMainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx);

        vertx.deployVerticle("com.cmad.mongo.MongoServiceVerticle",
                new DeploymentOptions().setWorker(true));

        router.route("/static/*").handler(StaticHandler.create("web"));

        router.get("/qaforum/user/:userid").handler(rctx -> {
            vertx.eventBus().send("com.cmad.vertx.qaforum.question.user.userid",
                    rctx.request().getParam("userid"), res -> {
                        System.out.println("Result for Query by User ID "
                                + res.result().body());

                        if (res.result().body().toString().isEmpty()) {
                            rctx.response().setStatusCode(404).end();
                        } else {
                            rctx.response().setStatusCode(200)
                                    .putHeader("Content-Type",
                                            "application/json")
                                    .end(res.result().body().toString());
                        }
                    });
        });

        router.route("/qaforum/user").handler(BodyHandler.create());
        router.post("/qaforum/user").handler(rctx -> {
            vertx.eventBus().send("com.cmad.vertx.qaforum.question.user.add",
                    rctx.getBodyAsJson().encodePrettily(), res -> {

                        if (res.result().body().toString().equals("-1")) {
                            rctx.response().setStatusCode(500).end();
                        } else {

                            rctx.response().setStatusCode(201)
                                    .putHeader("Content-Type",
                                            "application/json")
                                    .putHeader("Location", "qaforum/user/"
                                            + res.result().body().toString())
                                    .end();
                        }
                    });

        });

        router.route("/qaforum/question").handler(BodyHandler.create());
        router.post("/qaforum/question").handler(rctx -> {
            vertx.eventBus().send("com.cmad.vertx.qaforum.question.question.add",
                    rctx.getBodyAsJson().encodePrettily(), res -> {

                        if (res.result().body().toString().equals("-1")) {
                            rctx.response().setStatusCode(500).end();
                        } else {

                            rctx.response().setStatusCode(201)
                                    .putHeader("Content-Type",
                                            "application/json")
                                    .putHeader("Location", "qaforum/question/"
                                            + res.result().body().toString())
                                    .end();
                        }
                    });

        });

        vertx.createHttpServer().requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080), result -> {
                    if (result.succeeded()) {
                        future.complete();
                    } else {
                        future.fail(result.cause());
                    }
                });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}