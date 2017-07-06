package com.cmad.vertx;

import static com.cmad.util.CmadUtils.JWT_TOKEN;
import static com.cmad.util.CmadUtils.TICKET_GET;
import static com.cmad.util.CmadUtils.TICKET_VERIFY;
import static com.cmad.util.CmadUtils.USER;
import static com.cmad.util.CmadUtils.USER_ADD;
import static com.cmad.util.CmadUtils.USER_GET;
import static com.cmad.util.CmadUtils.USER_LOGIN;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class QAForumMainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) throws Exception {

        vertx.deployVerticle("com.cmad.mongo.MongoServiceVerticle",
                new DeploymentOptions().setWorker(true));

        vertx.deployVerticle("com.cmad.jwt.JWTServiceVerticle",
                new DeploymentOptions().setWorker(true));

        Router router = Router.router(vertx);

        router.get("/").handler(rctx -> {

            rctx.response().setStatusCode(200).setStatusMessage("OK")
                    .end("It works!");

        });

        router.get("/user/:username/").handler(rctx -> {

            vertx.eventBus().send(USER_GET, rctx.request().getParam(USER),
                    res -> {
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

        router.route("/user/").handler(BodyHandler.create());
        router.post("/user/").handler(rctx -> {
            vertx.eventBus().send(USER_ADD,
                    rctx.getBodyAsJson().encodePrettily(), res -> {

                        if (res.result().body().toString().equals("-1")) {
                            rctx.response().setStatusCode(500).end(
                                    "User with this username already exist");
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

        router.route("/user/ticket/").handler(BodyHandler.create());
        router.post("/user/ticket/").handler(rctx -> {
            vertx.eventBus().send(USER_LOGIN,
                    rctx.getBodyAsJson().encodePrettily(), res -> {

                        if (res.result().body().toString().contains("Failed")) {
                            rctx.response().setStatusCode(401)
                                    .end("Please enter correct credentials.");
                        } else {

                            vertx.eventBus().send(TICKET_GET,
                                    rctx.getBodyAsJson().getString(USER),
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

        router.route("/user/ticket/").handler(BodyHandler.create());
        router.get("/user/ticket/").handler(rctx -> {

            if (rctx.request().getHeader(JWT_TOKEN) == null) {
                System.out.println("Auth Header not given");
                rctx.response().setStatusCode(401).end();
            } else {
                vertx.eventBus().send(TICKET_VERIFY,
                        rctx.request().getHeader(JWT_TOKEN), res -> {
                            System.out.println(res.result().body());
                            JsonObject result = (JsonObject) res.result()
                                    .body();
                            if (result.getString("status").equals("Success")) {
                                rctx.response().setStatusCode(200)
                                        .end(result.getString("username"));
                            } else {
                                rctx.response().setStatusCode(401).end();
                            }
                        });
            }

        }

        );

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