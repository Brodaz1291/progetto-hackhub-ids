package it.unicam.cs.hackhub.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The third party payment system the prizes are transferred through. It is not our code: the
 * signature is the one the external system offers, and only the operation the platform
 * consumes is modelled.
 *
 * This is a stub: the transfer is simulated, where a real system would answer over the
 * network. What is kept from the real behaviour is the refusal, so that an answer which is not
 * a confirmation stays a case the platform has to cope with.
 *
 * Unlike the Calendar stub, nothing is remembered between calls: a bank does not refuse a
 * transfer because it has already executed a similar one, and a register of the transactions
 * done would refuse the second prize of the same amount to the same team as a duplicate.
 * Whether a prize has already been paid is the platform's own business, not the system's.
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
