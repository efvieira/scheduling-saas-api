package schedulingsaas.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("auth.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {
}
