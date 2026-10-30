package schedulingsaas.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Configuration
public class PixGatewayConfig {

    record ChargeResponse(String txid, String pixCopiaECola) {
    }

    /** Integra com o projeto pix-payment-gateway quando pix.gateway.url está configurado. */
    @Bean
    @ConditionalOnProperty(name = "pix.gateway.url")
    PixGateway pixPaymentGatewayClient(RestClient.Builder builder, @Value("${pix.gateway.url}") String url) {
        var client = builder.baseUrl(url).build();
        return (key, amount, description) -> {
            var response = client.post().uri("/v1/charges")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amount, "description", description))
                    .retrieve()
                    .body(ChargeResponse.class);
            return new PixGateway.PixCharge(response.txid(), response.pixCopiaECola());
        };
    }

    /** Sem gateway configurado, gera uma cobrança fictícia para a demo. */
    @Bean
    @ConditionalOnMissingBean(PixGateway.class)
    PixGateway simulatedPixGateway() {
        return (key, amount, description) -> {
            var txid = UUID.nameUUIDFromBytes(key.getBytes()).toString().replace("-", "");
            return new PixGateway.PixCharge(txid, "00020101021226...SIMULADO..." + txid);
        };
    }
}
