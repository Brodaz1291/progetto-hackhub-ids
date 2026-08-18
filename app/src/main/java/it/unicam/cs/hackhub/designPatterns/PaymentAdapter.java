package it.unicam.cs.hackhub.designPatterns;

import it.unicam.cs.hackhub.external.PaymentSystem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Translates a transfer between the two sides: the domain asks to move an amount in euro to an
 * iban, the payment system wants a recipient account, an amount in cents and a currency. The
 * distance between transfer and executeTransaction is what this class exists to cover.
 */
@Component
public class PaymentAdapter implements PaymentProcessor {

    /**
     * The domain holds the prize as a bare amount, while the payment system demands a currency:
     * the adapter is the one place where the missing half of the translation can live.
     */
    private static final String CURRENCY = "EUR";

    private final PaymentSystem paymentSystem;

    public PaymentAdapter(PaymentSystem paymentSystem) {
        this.paymentSystem = paymentSystem;
    }

    @Override
    public String transfer(String iban, BigDecimal amount) {
        String externalId = paymentSystem.executeTransaction(iban, toCents(amount), CURRENCY);
        if (externalId == null) {
            throw new IllegalArgumentException("The payment system did not confirm the transfer of " + amount);
        }
        return externalId;
    }

    /**
     * UNNECESSARY makes the conversion fail on an amount that does not fit in cents, instead of
     * rounding it: an adapter that moves money must not guess the figure. Everything down to
     * the cent passes untouched, a third decimal is refused before it reaches the bank, and
     * longValueExact refuses an amount too large to be counted in cents.
     */
    private long toCents(BigDecimal amount) {
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(amount + " is not an amount that can be transferred in cents");
        }
    }
}
