package schedulingsaas.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Envio pela WhatsApp Business Cloud API (Meta). Ativado quando whatsapp.token está definido.
 * Mensagens fora da janela de 24h exigem template aprovado; aqui usamos texto simples para
 * manter o exemplo curto.
 */
@Component("cloudApiWhatsAppGateway")
@ConditionalOnProperty(name = "whatsapp.token")
public class CloudApiWhatsAppGateway implements WhatsAppGateway {

    private final RestClient client;
    private final String phoneNumberId;

    public CloudApiWhatsAppGateway(RestClient.Builder builder,
                                   @Value("${whatsapp.token}") String token,
                                   @Value("${whatsapp.phone-number-id}") String phoneNumberId,
                                   @Value("${whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl) {
        this.client = builder.baseUrl(apiUrl).defaultHeader("Authorization", "Bearer " + token).build();
        this.phoneNumberId = phoneNumberId;
    }

    @Override
    public void send(String phone, String message) {
        client.post()
                .uri("/{id}/messages", phoneNumberId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messaging_product", "whatsapp", "to", phone, "type", "text",
                        "text", Map.of("body", message)))
                .retrieve()
                .toBodilessEntity();
    }
}
