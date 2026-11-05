package com.aeroreserve.exception;

/**
 * Thrown when the simulated payment transaction fails.
 *
 * PaymentService randomly fails about 15 % of transactions to simulate
 * real-world payment gateway failures.  When this exception is caught
 * the UI offers the user a "Retry" button or a way to go back.
 */
public class PaymentFailedException extends Exception {

    /**
     * Creates the exception with a failure reason message.
     *
     * @param message e.g. "Transaction declined. Please try again or use a different method."
     */
    public PaymentFailedException(String message) {
        super(message);
    }
}

