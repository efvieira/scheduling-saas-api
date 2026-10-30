package schedulingsaas.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import schedulingsaas.shared.BusinessException;
import schedulingsaas.tenant.BusinessRepository;
import schedulingsaas.tenant.StaffUserRepository;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, Instant expiresAt, String name, String businessName, String businessSlug) {
    }

    private final StaffUserRepository users;
    private final BusinessRepository businesses;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final Clock clock;

    public AuthController(StaffUserRepository users, BusinessRepository businesses, PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder, JwtProperties props, Clock clock) {
        this.users = users;
        this.businesses = businesses;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.clock = clock;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var user = users.findByEmailIgnoreCase(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));
        var business = businesses.findById(user.getBusinessId()).orElseThrow();
        var now = clock.instant();
        var expiresAt = now.plus(props.ttl());
        var claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CurrentUser.BUSINESS_CLAIM, user.getBusinessId().toString())
                .claim("role", user.getRole().name())
                .build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return new LoginResponse(token.getTokenValue(), expiresAt, user.getName(), business.getName(), business.getSlug());
    }
}
