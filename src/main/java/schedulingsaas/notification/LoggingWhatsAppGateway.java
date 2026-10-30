package schedulingsaas.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Padrão para demo e testes: registra as mensagens em vez de enviá-las. */
@Component
@ConditionalOnExpression("'${whatsapp.token:}'.isEmpty()")
public class LoggingWhatsAppGateway implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingWhatsAppGateway.class);

    public record Message(String phone, String text) {
    }

    private final List<Message> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String phone, String message) {
        log.info("[WhatsApp simulado] para {}: {}", phone, message);
        sent.add(new Message(phone, message));
    }

    public List<Message> sent() {
        return List.copyOf(sent);
    }
}
