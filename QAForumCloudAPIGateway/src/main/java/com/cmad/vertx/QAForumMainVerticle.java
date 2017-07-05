package com.cmad.vertx;

import com.cmad.util.CmadUtils;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
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
                System.out.println(
                        "This is a restricted resource and needs authentication");

                /*
                 * Check if jwt-auth-token header is there. If not there, throw
                 * unauthorized.
                 */

                if (rctx.request().getHeader(CmadUtils.JWT_TOKEN) == null) {
                    System.out.println("Status Unauthorized");
                    rctx.response().setStatusCode(401)
                            .setStatusMessage("Unauthorized")
                            .end("Unauthorized Access, jwt-auth-token required in header.");

                } else {

                    vertx.createHttpClient()
                            .request(HttpMethod.GET, "metadata",
                                    "/computeMetadata/v1/project/attributes/v1-user")
                            .putHeader("Metadata-Flavor", "Google")
                            .handler(res -> {

                                res.bodyHandler(endPoint -> {
                                    String ipAddress = endPoint.toString();
                                    System.out.println(
                                            "Auth: User Service located at "
                                                    + ipAddress);

                                    vertx.createHttpClient()
                                            .request(HttpMethod.GET, ipAddress,
                                                    "/user/ticket")
                                            .putHeader(CmadUtils.JWT_TOKEN,
                                                    rctx.request().getHeader(
                                                            CmadUtils.JWT_TOKEN))
                                            .handler(authResp -> {

                                                switch (authResp.statusCode()) {

                                                case 200:
                                                    System.out.println(
                                                            "Status OK");
                                                    sendAndReceiveFromEndpoint(
                                                            rctx, apiVersion,
                                                            resource, routeTo);
                                                    break;
                                                case 401:
                                                    System.out.println(
                                                            "Status Unauthorized");
                                                    rctx.response()
                                                            .setStatusCode(401)
                                                            .setStatusMessage(
                                                                    "Unauthorized")
                                                            .end("Unauthorized Access");
                                                    break;
                                                default:
                                                    rctx.response()
                                                            .setStatusCode(
                                                                    authResp.statusCode())
                                                            .setStatusMessage(
                                                                    authResp.statusMessage())
                                                            .end("Failure");
                                                }
                                            }).end();

                                });

                            }).end();
                }
            } else {
                sendAndReceiveFromEndpoint(rctx, apiVersion, resource, routeTo);
            }

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

    /**
     * @param rctx
     * @param apiVersion
     * @param resource
     * @param routeTo
     */
    private void sendAndReceiveFromEndpoint(RoutingContext rctx,
            String apiVersion, String resource, String routeTo) {
        HttpClient httpClient = vertx.createHttpClient();

        httpClient
                .request(HttpMethod.GET, "metadata",
                        "/computeMetadata/v1/project/attributes/" + apiVersion
                                + "-" + resource)
                .putHeader("Metadata-Flavor", "Google").handler(res -> {

                    res.bodyHandler(buff -> {
                        String ipAddress = buff.toString();
                        System.out.println("POST version " + apiVersion
                                + " of resource " + resource + " with route to "
                                + routeTo + " end point is " + ipAddress);
                        HttpClientRequest postRequest = vertx.createHttpClient()
                                .post(ipAddress, routeTo, resp -> {
                                    resp.bodyHandler(data -> {
                                        HttpServerResponse responseToBeSent = rctx
                                                .response()
                                                .setStatusCode(
                                                        resp.statusCode())
                                                .setStatusMessage(
                                                        resp.statusMessage());
                                        responseToBeSent.headers()
                                                .addAll(resp.headers());
                                        responseToBeSent.end(data);
                                    });
                                });
                        postRequest.headers().addAll(rctx.request().headers());
                        postRequest.end(rctx.getBodyAsString());
                    });

                }).end();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}