package com.cmad.mongo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoServiceVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.user.userid",
                message -> {

                    JsonObject config = new JsonObject();
                    config.put("db_name", "qaforum");
                    config.put("connection_string",
                            "mongodb://mymongo:27017");
                    MongoClient client = MongoClient.createShared(vertx,
                            config);

                    client.find("user", new JsonObject().put("userid",
                            message.body().toString()), res -> {
                                if (res.succeeded()) {
                                    if (res.result().size() != 0) {
                                        System.out.println("User exist " + res.result());
                                        message.reply(res.result().toString());
                                    } else {
                                        message.reply("");
                                    }
                                } else {
                                    res.cause().printStackTrace();
                                    message.reply("");
                                }
                            });
                });

        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.user.add",
                message -> {

                    JsonObject config = new JsonObject();
                    config.put("db_name", "qaforum");
                    config.put("connection_string",
                            "mongodb://mymongo:27017");
                    MongoClient client = MongoClient.createShared(vertx,
                            config);
                    System.out.println("To be Created User " +message.body().toString());
                    client.insert("user",
                            new JsonObject(message.body().toString()), res -> {
                                if (res.succeeded()) {
                                    message.reply(res.result());
                                } else {
                                    res.cause().printStackTrace();
                                    message.reply(-1);
                                }

                            });
                });
    }
}
