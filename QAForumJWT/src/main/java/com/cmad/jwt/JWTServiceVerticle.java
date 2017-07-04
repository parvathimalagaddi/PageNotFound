package com.cmad.jwt;

import static com.cmad.util.CmadUtils.TICKET_GET;
import static com.cmad.util.CmadUtils.TICKET_VERIFY;

import java.security.Key;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class JWTServiceVerticle extends AbstractVerticle {

    @Override
    public void start() {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
        Key signingKey = new SecretKeySpec("cmad@cisco.com".getBytes(),
                signatureAlgorithm.getJcaName());
        vertx.eventBus().consumer(TICKET_GET, message -> {

            String compactJws = Jwts.builder()
                    .setSubject(message.body().toString())
                    .setIssuer("cmad@cisco.com")
                    .signWith(SignatureAlgorithm.HS512, signingKey).compact();
            message.reply(compactJws);
        });

        vertx.eventBus().consumer(TICKET_VERIFY, message -> {

            try {

                Claims claims = Jwts.parser().setSigningKey(signingKey)
                        .parseClaimsJws(message.body().toString()).getBody();
                // Verify the issuer claim.

                if (claims.getIssuer().equals("cmad@cisco.com")) {
                    message.reply(new JsonObject().put("status", "Success")
                            .put("username", claims.getSubject()));

                } else {
                    message.reply(new JsonObject().put("status", "Failure")
                            .put("username", claims.getSubject()));
                }
            } catch (Exception e) {
                System.out.println("An Exception while parsing the token.");
                e.printStackTrace();
                message.reply(new JsonObject().put("status", "Failure"));
            }

        });
    }

}
