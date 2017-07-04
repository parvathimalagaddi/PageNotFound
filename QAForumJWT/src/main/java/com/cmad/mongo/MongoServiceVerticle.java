package com.cmad.mongo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import static com.cmad.util.CmadUtils.*;

public class MongoServiceVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        String mongoHost = System.getProperty("MONGOHOST", "localhost");
        JsonObject config = new JsonObject();
        config.put("db_name", "qaforum");
        config.put("connection_string", "mongodb://" + mongoHost + ":27017");
        MongoClient client = MongoClient.createShared(super.vertx, config);

        vertx.eventBus().consumer(USER_GET, message -> {

            client.find(USER_COLLECTION,
                    new JsonObject().put(USER, message.body().toString()),
                    res -> {
                        if (res.succeeded()) {
                            if (res.result().size() != 0) {
                                System.out
                                        .println("User exist " + res.result());
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

        vertx.eventBus().consumer(USER_ADD, message -> {

            System.out
                    .println("To be Created User " + message.body().toString());

            JsonObject userObject = new JsonObject(message.body().toString());

            client.insert(USER_COLLECTION, userObject, res -> {
                if (res.succeeded()) {
                    message.reply(userObject.getString(USER));
                } else {
                    res.cause().printStackTrace();
                    message.reply(-1);
                }

            });
        });

        vertx.eventBus().consumer(USER_LOGIN, message -> {

            JsonObject userObject = new JsonObject(message.body().toString());

            client.find(USER_COLLECTION, userObject, res -> {
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
