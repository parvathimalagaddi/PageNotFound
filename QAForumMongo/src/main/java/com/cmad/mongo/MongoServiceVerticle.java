package com.cmad.mongo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoServiceVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        
        String mongoHost = System.getProperty("MONGOHOST");
        JsonObject config = new JsonObject();
        config.put("db_name", "qaforum");
        config.put("connection_string", "mongodb://"+mongoHost+":27017");
        MongoClient client = MongoClient.createShared(super.vertx, config);

        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.user.userid",
                message -> {

                    client.find("user", new JsonObject().put("userid",
                            message.body().toString()), res -> {
                                if (res.succeeded()) {
                                    if (res.result().size() != 0) {
                                        System.out.println(
                                                "User exist " + res.result());
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

                    System.out.println(
                            "To be Created User " + message.body().toString());
                    
                    JsonObject userObject = new JsonObject(
                            message.body().toString());

                    client.insert("user", userObject, res -> {
                        if (res.succeeded()) {
                            message.reply(userObject.getString("userId"));
                        } else {
                            res.cause().printStackTrace();
                            message.reply(-1);
                        }

                    });
                });

        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.add",
                message -> {

                    System.out.println("To be Created Question "
                            + message.body().toString());
                    client.insert("question",
                            new JsonObject(message.body().toString()), res -> {
                                if (res.succeeded()) {
                                    message.reply(res.result());
                                } else {
                                    res.cause().printStackTrace();
                                    message.reply(-1);
                                }

                            });
                });
        
        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.all",
                message -> {

                    client.find("question", new JsonObject(), res -> {
                                if (res.succeeded()) {
                                    if (res.result().size() != 0) {
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

        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.answer.add",
                message -> {
                    JsonObject input = new JsonObject(
                            message.body().toString());

                    // Create the query and the update operation.
                    JsonObject query = new JsonObject().put("_id",
                            input.getString("qid"));
                    JsonObject answer = new JsonObject()
                            .put("answer", input.getString("answer"))
                            .put("userid", input.getString("userid"))
                            .put("postTime", System.currentTimeMillis());

                    JsonObject update = new JsonObject().put("$addToSet",
                            new JsonObject().put("answers", answer));

                    client.updateCollection("question", query, update, res -> {
                        if (res.succeeded()) {
                            message.reply(201);
                        } else {
                            res.cause().printStackTrace();
                            message.reply(-1);
                        }

                    });
                });

        vertx.eventBus().consumer("com.cmad.vertx.qaforum.question.get",
                message -> {

                    client.find("question", new JsonObject().put("_id",
                            message.body().toString()), res -> {
                                if (res.succeeded()) {
                                    if (res.result().size() != 0) {
                                        System.out.println(
                                                "User exist " + res.result());
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

        vertx.eventBus().consumer("com.cmad.vertx.qaforum.login", message -> {

            JsonObject userObject = new JsonObject(message.body().toString());

            client.find("user", userObject, res -> {
                if (res.succeeded()) {
                    if (res.result().size() != 0) {
                        System.out.println("User exist " + res.result());
                        message.reply("Authentication Successful");
                    } else {
                        message.reply(
                                "Authentication Failed, Invalid User/Password");
                    }
                } else {
                    res.cause().printStackTrace();
                    message.reply(
                            "Authentication Failed, Invalid User/Password");
                }
            });
        });

    }
}
