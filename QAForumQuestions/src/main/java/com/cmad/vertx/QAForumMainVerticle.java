package com.cmad.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import static com.cmad.util.CmadUtils.*;

public class QAForumMainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) throws Exception {

        vertx.deployVerticle("com.cmad.mongo.MongoServiceVerticle",
                new DeploymentOptions().setWorker(true));

        Router router = Router.router(vertx);

        /*
         * The below route is for health check.
         */

        router.get("/").handler(rctx -> {

            rctx.response().setStatusCode(200).setStatusMessage("OK")
                    .end("It works!");

        });

        router.route("/question/").handler(BodyHandler.create());
        router.post("/question/").handler(rctx -> {

            vertx.eventBus().send(QUESTION_ADD,
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

        router.route("/question/:qid/answer/").handler(BodyHandler.create());
        router.post("/question/:qid/answer/").handler(rctx -> {
            vertx.eventBus().send(QUESTION_ANSWER_ADD, rctx.getBodyAsJson()
                    .put("qid", rctx.request().getParam("qid")), res -> {

                        if (res.result().body().toString().equals("-1")) {
                            rctx.response().setStatusCode(500).end();
                        } else {
                            rctx.response().setStatusCode(201)
                                    .putHeader("Content-Type",
                                            "application/json")
                                    .putHeader("Location", "/question/"
                                            + rctx.request().getParam("qid"))
                                    .end();
                        }
                    });

        });

        router.get("/question/:qid/").handler(rctx -> {

            vertx.eventBus().send(QUESTION_GET, rctx.request().getParam("qid"),
                    res -> {
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
        
        
        router.get("/question/filter/by/").handler(rctx -> {

            vertx.eventBus().send(QUESTION_SEARCH, rctx.request().getParam("question"),
                    res -> {
                        System.out.println("Result for search question "
                                + res.result().body());

                        if (res.result().body().toString().isEmpty()) {
                            rctx.response().setStatusCode(200).end();
                        } else {
                            rctx.response().setStatusCode(200)
                                    .putHeader("Content-Type",
                                            "application/json")
                                    .end(res.result().body().toString());
                        }
                    });
        });

        router.get("/question/").handler(rctx -> {

            vertx.eventBus().send(QUESTION_ALL, "", res -> {

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