package com.cmad.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.impl.RabbitMQClientImpl;

public class QAForumMainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) throws Exception {
        Router router = Router.router(vertx);

        vertx.deployVerticle("com.cmad.mongo.MongoServiceVerticle",
                new DeploymentOptions().setWorker(true));

        vertx.deployVerticle("com.cmad.jwt.JWTServiceVerticle",
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

        router.route("/qaforum/ticket").handler(BodyHandler.create());
        router.post("/qaforum/ticket").handler(rctx -> {
            vertx.eventBus().send("com.cmad.vertx.qaforum.login",
                    rctx.getBodyAsJson().encodePrettily(), res -> {

                        if (res.result().body().toString().contains("Failed")) {
                            rctx.response().setStatusCode(401).end();
                        } else {

                            vertx.eventBus().send(
                                    "com.cmad.vertx.qaforum.ticket",
                                    rctx.getBodyAsJson().getString("userid"),
                                    response -> {
                                        rctx.response().setStatusCode(201)
                                                .putHeader("Content-Type",
                                                        "application/json")
                                                .end(response.result().body()
                                                        .toString());
                                    });
                        }
                    });

        });

        router.route("/qaforum/question").handler(BodyHandler.create());
        router.post("/qaforum/question").handler(rctx -> {
            vertx.eventBus().send("com.cmad.vertx.qaforum.question.add",
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

        router.route("/qaforum/question/:qid/answer")
                .handler(BodyHandler.create());
        router.post("/qaforum/question/:qid/answer").handler(rctx -> {
            vertx.eventBus().send("com.cmad.vertx.qaforum.question.answer.add",
                    rctx.getBodyAsJson().put("qid",
                            rctx.request().getParam("qid")),
                    res -> {

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

        router.get("/qaforum/question/:qid").handler(rctx -> {

            vertx.eventBus().send("com.cmad.vertx.qaforum.question.get",
                    rctx.request().getParam("qid"), res -> {
                        System.out.println("Result for Query by Question ID "
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

        router.get("/qaforum/question").handler(rctx -> {

            vertx.eventBus().send("com.cmad.vertx.qaforum.question.all", "",
                    res -> {

                        rctx.response().setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(res.result().body().toString());
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