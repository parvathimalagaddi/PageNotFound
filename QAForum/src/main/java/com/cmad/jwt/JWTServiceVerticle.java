package com.cmad.jwt;

import java.security.Key;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import io.vertx.core.AbstractVerticle;

public class JWTServiceVerticle extends AbstractVerticle {

    @Override
    public void start() {
        vertx.eventBus().consumer("com.cmad.vertx.qaforum.ticket", message -> {
            Key key = MacProvider.generateKey();
            String compactJws = Jwts.builder()
                    .setSubject(message.body().toString())
                    .setIssuer("cmad@cisco.com")
                    .signWith(SignatureAlgorithm.HS512, key).compact();
            message.reply(compactJws);
        });
    }

}
