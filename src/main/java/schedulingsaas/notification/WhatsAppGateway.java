package schedulingsaas.notification;

/** Envio de mensagens ao cliente. O domínio não conhece o provedor (Cloud API, Twilio, Z-API...). */
public interface WhatsAppGateway {

    void send(String phone, String message);
}
