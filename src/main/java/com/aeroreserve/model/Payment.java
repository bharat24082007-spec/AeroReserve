package com.aeroreserve.model;

/**
 * Payment records how a booking was paid for.
 *
 * PaymentMethod is a nested enum listing the four accepted payment modes.
 * PaymentStatus is a nested enum showing whether the transaction succeeded.
 *
 * A Payment object is created during the checkout step and stored inside a Booking.
 */
public class Payment {

    // ── Nested enums ─────────────────────────────────────────────────────────

    /**
     * The four payment modes displayed on the checkout screen.
     */
    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        NET_BANKING,
        WALLET
    }

    /**
     * Whether the payment attempt succeeded or failed.
     * PENDING is the initial state before the simulated transaction runs.
     */
    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private PaymentMethod method;   // which mode the user chose
    private PaymentStatus status;   // result of the simulated transaction
    private double        amount;   // total amount charged in INR

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a Payment in PENDING state with the chosen method and amount.
     * The status is updated to SUCCESS or FAILED by PaymentService.
     *
     * @param method the payment mode selected by the user
     * @param amount the total INR amount to be charged
     */
    public Payment(PaymentMethod method, double amount) {
        this.method = method;
        this.amount = amount;
        this.status = PaymentStatus.PENDING; // always starts as PENDING
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the payment method chosen. */
    public PaymentMethod getMethod() { return method; }

    /** Returns the current payment status. */
    public PaymentStatus getStatus() { return status; }

    /** Returns the amount charged. */
    public double getAmount() { return amount; }

    // ── Setter ────────────────────────────────────────────────────────────────

    /**
     * Updates the payment status after the simulated transaction.
     * Called by PaymentService with either SUCCESS or FAILED.
     *
     * @param status the result of the transaction
     */
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Returns a readable summary of this payment.
     * Example: "CREDIT_CARD | INR 6900.0 | SUCCESS"
     */
    @Override
    public String toString() {
        return method + " | INR " + amount + " | " + status;
    }
}

