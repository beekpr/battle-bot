package io.beekeeper.meetly.survey;

import com.auth0.jwt.algorithms.Algorithm;

public class JWTSecrets {

    public static Algorithm algorithmHS;

    public static final void setSecret(String secret) {
        algorithmHS = Algorithm.HMAC256(secret);
    }

}
