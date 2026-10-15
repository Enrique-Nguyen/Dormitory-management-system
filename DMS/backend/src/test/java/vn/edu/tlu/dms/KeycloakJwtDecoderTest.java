package vn.edu.tlu.dms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import vn.edu.tlu.dms.account.config.SecurityConfig;
import vn.edu.tlu.dms.account.config.SecurityConfig.KeycloakProperties;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Token ký thật và JWK qua HTTP: kiểm tra chữ ký, issuer, audience và thời hạn. */
class KeycloakJwtDecoderTest {

    private static HttpServer server;

    private static RSAKey signingKey;

    private static JwtDecoder decoder;

    private static String issuer;

    @BeforeAll
    static void startJwkEndpoint() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("dms-test-key").generate();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body =
                new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        server.createContext(
                "/realms/dms/protocol/openid-connect/certs",
                exchange -> {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        issuer = url + "/realms/dms";
        decoder =
                new SecurityConfig()
                        .jwtDecoder(
                                new KeycloakProperties(
                                        url, "dms", "dms-api", "dms-admin", "unused"));
    }

    @AfterAll
    static void stopJwkEndpoint() {
        server.stop(0);
    }

    @Test
    void acceptsCorrectlySignedToken() throws Exception {
        assertThat(
                        decoder.decode(
                                        token(
                                                signingKey,
                                                issuer,
                                                "dms-api",
                                                Instant.now().plusSeconds(300)))
                                .getSubject())
                .isEqualTo("linked-identity");
    }

    @Test
    void rejectsAnotherRealm() throws Exception {
        String value =
                token(signingKey, issuer + "-other", "dms-api", Instant.now().plusSeconds(300));
        assertThatThrownBy(() -> decoder.decode(value)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenForAnotherApi() throws Exception {
        String value = token(signingKey, issuer, "other-api", Instant.now().plusSeconds(300));
        assertThatThrownBy(() -> decoder.decode(value)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String value = token(signingKey, issuer, "dms-api", Instant.now().minusSeconds(120));
        assertThatThrownBy(() -> decoder.decode(value)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsForgedSignatureEvenWithMatchingKeyId() throws Exception {
        RSAKey forgedKey = new RSAKeyGenerator(2048).keyID(signingKey.getKeyID()).generate();
        String value = token(forgedKey, issuer, "dms-api", Instant.now().plusSeconds(300));
        assertThatThrownBy(() -> decoder.decode(value)).isInstanceOf(JwtException.class);
    }

    private static String token(RSAKey key, String tokenIssuer, String audience, Instant expiration)
            throws Exception {
        var claims =
                new JWTClaimsSet.Builder()
                        .subject("linked-identity")
                        .issuer(tokenIssuer)
                        .audience(audience)
                        .issueTime(Date.from(Instant.now().minusSeconds(300)))
                        .expirationTime(Date.from(expiration))
                        .build();
        var jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                        claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
