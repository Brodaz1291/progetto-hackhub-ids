package it.unicam.cs.hackhub.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated payment system the prizes are moved through, cut down to the single operation
 * the platform needs; the signature is the provider's, not ours. The refusal is the piece of
 * real behaviour that survives the simulation: a transfer can still come back unconfirmed,
 * exactly as with a real bank.
 *
 * Unlike the Calendar stub, nothing is remembered between calls: a bank does not refuse a
 * transfer because it has already made a similar one. Whether a prize has already been paid
 * is the platform's own business, answered by the payment it recorded, not by the system.
 */
@Component
public class PaymentSystem {

    private static final Logger log = LoggerFactory.getLogger(PaymentSystem.class);

    /**
     * Executes a transfer and returns the identifier the transaction is filed under, or null
     * when the account is not one it can credit or there is nothing to credit it with.
     */
    public String executeTransaction(String recipientAccount, long amountCents, String currency) {
        if (recipientAccount == null || recipientAccount.isBlank()) {
            log.info("PaymentSystem: refused a transfer of {} {}, no recipient account given",
                    amountCents, currency);
            return null;
        }
        if (amountCents <= 0) {
            log.info("PaymentSystem: refused the transfer to {}, {} {} is not an amount to move",
                    recipientAccount, amountCents, currency);
            return null;
        }

        String externalId = "PAY-" + UUID.randomUUID();
        log.info("PaymentSystem: transferred {} {} to {} as {}",
                amountCents, currency, recipientAccount, externalId);
        return externalId;
    }
}
