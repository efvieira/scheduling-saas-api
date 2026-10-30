package schedulingsaas.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import schedulingsaas.booking.BookingService;
import schedulingsaas.shared.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@Tag(name = "Webhooks")
public class PixWebhookController {

    public record PixPaid(@NotBlank String txid) {
    }

    private final BookingService booking;
    private final byte[] token;

    public PixWebhookController(BookingService booking, @Value("${pix.webhook-token}") String token) {
        this.booking = booking;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/api/webhooks/pix")
    @Operation(summary = "Confirmação de pagamento Pix (ex.: consumidor do evento pix.charge.paid)")
    public void paid(@RequestHeader("X-Webhook-Token") String header, @RequestBody PixPaid body) {
        if (!MessageDigest.isEqual(token, header.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
        booking.confirmPayment(body.txid());
    }
}
