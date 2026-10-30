package schedulingsaas.payment;

import java.math.BigDecimal;

/** Criação de cobrança Pix. Pode ser o pix-payment-gateway ou qualquer PSP. */
public interface PixGateway {

    record PixCharge(String txid, String copiaECola) {
    }

    /** @param idempotencyKey garante que um retry não gere duas cobranças para o mesmo agendamento */
    PixCharge createCharge(String idempotencyKey, BigDecimal amount, String description);
}
