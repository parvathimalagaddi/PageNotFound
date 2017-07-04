package com.cmad.mongo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
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

        vertx.eventBus().consumer(QUESTION_ADD, message -> {

            System.out.println(
                    "To be Created Question " + message.body().toString());
            client.insert(QUESTION_COLLECTION,
                    new JsonObject(message.body().toString()), res -> {
                        if (res.succeeded()) {
                            message.reply(res.result());
                        } else {
                            res.cause().printStackTrace();
                            message.reply(-1);
                        }

                    });
        });

        vertx.eventBus().consumer(QUESTION_ALL, message -> {

            FindOptions findOptions = new FindOptions();
            findOptions.setFields(new JsonObject().put("question", "1"));

            client.findWithOptions(QUESTION_COLLECTION, new JsonObject(),
                    findOptions, res -> {
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

        vertx.eventBus().consumer(QUESTION_ANSWER_ADD, message -> {
            JsonObject input = new JsonObject(message.body().toString());

            // Create the query and the update operation.
            JsonObject query = new JsonObject().put("_id",
                    input.getString("qid"));
            JsonObject answer = new JsonObject()
                    .put("answer", input.getString("answer"))
                    .put("username", input.getString("username"))
                    .put("postTime", System.currentTimeMillis());

            JsonObject update = new JsonObject().put("$addToSet",
                    new JsonObject().put("answers", answer));

            client.updateCollection(QUESTION_COLLECTION, query, update, res -> {
                if (res.succeeded()) {
                    message.reply(res.result().toString());
                } else {
                    res.cause().printStackTrace();
                    message.reply("-1");
                }

            });
        });

        vertx.eventBus().consumer(QUESTION_GET, message -> {

            client.find(QUESTION_COLLECTION,
                    new JsonObject().put("_id", message.body().toString()),
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

    }
}
