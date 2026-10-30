package schedulingsaas.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** A empresa do usuário logado vem do token; nenhuma rota do painel aceita o tenant por parâmetro. */
public final class CurrentUser {

    public static final String BUSINESS_CLAIM = "business_id";

    private CurrentUser() {
    }

    public static UUID businessId() {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getClaimAsString(BUSINESS_CLAIM));
    }
}
