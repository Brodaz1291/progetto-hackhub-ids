package it.unicam.cs.hackhub.designPatterns;

import java.math.BigDecimal;

/**
 * How the platform asks for a prize to be transferred, said in the words of the domain: an
 * iban, an amount in euro, and the identifier of the transaction back. PaymentService depends
 * on this interface alone: which payment system answers, and what its API looks like, stays
 * outside the domain.
 */
public interface PaymentProcessor {

    String transfer(String iban, BigDecimal amount);
}
