package com.cmad.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class QAForumMainVerticle extends AbstractVerticle {
    @Override
    public void start(Future<Void> future) throws Exception {

        Router router = Router.router(vertx);

        router.get("/api/:version/:resource/*").handler(rctx -> {
            String apiVersion = rctx.request().getParam("version");
            String resource = rctx.request().getParam("resource");
            String routeTo = rctx.request().path().replace("/api/" + apiVersion,
                    "");

            vertx.createHttpClient()
                    .getNow("metadata/computeMetadata/v1/project/attributes/"
                            + apiVersion + ":" + resource, res -> {
                                res.bodyHandler(buff -> {
                                    String ipAddress = buff.toString();

                                    vertx.createHttpClient().getNow(
                                            ipAddress + routeTo, resp -> {
                                                resp.bodyHandler(data -> {
                                                    rctx.response()
                                                            .setStatusCode(
                                                                    resp.statusCode())
                                                            .end(data);
                                                });
                                            });

                                });
                            });
        });

        router.route("/api/:version/:resource/*").handler(BodyHandler.create());
        router.post("/api/:version/:resource/*").handler(rctx -> {
            String apiVersion = rctx.request().getParam("version");
            String resource = rctx.request().getParam("resource");
            String routeTo = rctx.request().path().replace("/api/" + apiVersion,
                    "");
            
            /*
             * All post requests should contain a valid JWT Auth Token.
             */
            
            vertx.createHttpClient()
                    .getNow("metadata/computeMetadata/v1/project/attributes/"
                            + apiVersion + ":" + resource, res -> {
                                res.bodyHandler(buff -> {
                                    String ipAddress = buff.toString();

                                    HttpClientRequest postRequest= vertx.createHttpClient()
                                            .post(ipAddress + routeTo, resp -> {
                                                resp.bodyHandler(data -> {
                                                    rctx.response()
                                                            .setStatusCode(
                                                                    resp.statusCode())
                                                            .end(data);
                                                });
                                            });
                                    postRequest.headers().addAll(rctx.request().headers());
                                    postRequest.end(rctx.getBodyAsString());
                                });
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