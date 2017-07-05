package com.cmad.jwt.test;

import static com.cmad.util.CmadUtils.TICKET_GET;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cmad.jwt.JWTServiceVerticle;
import com.cmad.util.CmadUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext tc) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(JWTServiceVerticle.class.getName(),
                tc.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext tc) {
        vertx.close(tc.asyncAssertSuccess());
    }

    @Test
    public void testThatTheServerIsStarted(TestContext tc) {
        Async async = tc.async();

        vertx.eventBus().send(TICKET_GET, "babandi", response -> {
            System.out.println(response.result().body().toString());

            vertx.eventBus().send(CmadUtils.TICKET_VERIFY,
                    response.result().body().toString(), res -> {

                        JsonObject jsonObject = (JsonObject) res.result()
                                .body();
                        tc.assertEquals(jsonObject.getString("status"),
                                "Success");
                        tc.assertEquals(jsonObject.getString("username"),
                                "babandi");
                        async.complete();

                    });
        });
    }

}