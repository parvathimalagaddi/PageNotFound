package com.cmad.vertx;

import com.cmad.util.CmadUtils;

import io.netty.handler.codec.http.HttpStatusClass;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
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

            System.out.println("Processing the request for " + apiVersion + "/"
                    + resource);

            HttpClient httpClient = vertx.createHttpClient();

            httpClient
                    .request(HttpMethod.GET, "metadata",
                            "/computeMetadata/v1/project/attributes/"
                                    + apiVersion + "-" + resource)
                    .putHeader("Metadata-Flavor", "Google").handler(res -> {

                        res.bodyHandler(buff -> {
                            String ipAddress = buff.toString();
                            System.out.println("GET version " + apiVersion
                                    + " of resource " + resource
                                    + " with route to " + routeTo
                                    + " end point is " + ipAddress);

                            HttpClientRequest getRequest = vertx
                                    .createHttpClient()
                                    .get(ipAddress, routeTo, resp -> {
                                        resp.bodyHandler(data -> {
                                            HttpServerResponse responseToBeSent = rctx
                                                    .response()
                                                    .setStatusCode(
                                                            resp.statusCode())
                                                    .setStatusMessage(resp
                                                            .statusMessage());
                                            responseToBeSent.headers()
                                                    .addAll(resp.headers());
                                            responseToBeSent.end(data);
                                        });
                                    });

                            getRequest.headers()
                                    .addAll(rctx.request().headers());
                            getRequest.end();

                        });

                    }).end();
        });

        router.route("/api/:version/:resource/*").handler(BodyHandler.create());
        router.post("/api/:version/:resource/*").handler(rctx -> {
            String apiVersion = rctx.request().getParam("version");
            String resource = rctx.request().getParam("resource");
            String routeTo = rctx.request().path().replace("/api/" + apiVersion,
                    "");

            /*
             * All post requests should contain a valid JWT Auth Token except
             * user creations.
             */

            if (!resource.equals("user")) {
                vertx.createHttpClient()
                        .request(HttpMethod.GET, "metadata",
                                "/computeMetadata/v1/project/attributes/v1-user")
                        .putHeader("Metadata-Flavor", "Google").handler(res -> {

                            res.bodyHandler(endPoint -> {
                                String ipAddress = endPoint.toString();

                                vertx.createHttpClient()
                                        .request(HttpMethod.GET, ipAddress,
                                                "/user/ticket")
                                        .putHeader(CmadUtils.JWT_TOKEN,
                                                rctx.request().getHeader(
                                                        CmadUtils.JWT_TOKEN))
                                        .handler(authResp -> {
                                            if (authResp.statusCode() == 401) {
                                                rctx.response()
                                                        .setStatusCode(401)
                                                        .setStatusMessage(
                                                                "Unauthorized")
                                                        .end("Unauthorized Access");
                                            }
                                        }).end();

                            });

                        }).end();
            }

            HttpClient httpClient = vertx.createHttpClient();

            httpClient
                    .request(HttpMethod.GET, "metadata",
                            "/computeMetadata/v1/project/attributes/"
                                    + apiVersion + "-" + resource)
                    .putHeader("Metadata-Flavor", "Google").handler(res -> {

                        res.bodyHandler(buff -> {
                            String ipAddress = buff.toString();
                            System.out.println("POST version " + apiVersion
                                    + " of resource " + resource
                                    + " with route to " + routeTo
                                    + " end point is " + ipAddress);
                            HttpClientRequest postRequest = vertx
                                    .createHttpClient()
                                    .post(ipAddress, routeTo, resp -> {
                                        resp.bodyHandler(data -> {
                                            HttpServerResponse responseToBeSent = rctx
                                                    .response()
                                                    .setStatusCode(
                                                            resp.statusCode())
                                                    .setStatusMessage(resp
                                                            .statusMessage());
                                            responseToBeSent.headers()
                                                    .addAll(resp.headers());
                                            responseToBeSent.end(data);
                                        });
                                    });
                            postRequest.headers()
                                    .addAll(rctx.request().headers());
                            postRequest.end(rctx.getBodyAsString());
                        });

                    }).end();

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