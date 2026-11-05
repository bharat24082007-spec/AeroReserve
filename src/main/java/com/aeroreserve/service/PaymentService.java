package com.aeroreserve.service;

import com.aeroreserve.exception.PaymentFailedException;
import com.aeroreserve.model.Payment;
import com.aeroreserve.model.Payment.PaymentMethod;
import com.aeroreserve.model.Payment.PaymentStatus;

import java.util.Random;

/**
 * PaymentService simulates a payment gateway transaction.
 *
 * In a real app this would call a bank API.
 * Here we simulate it with a 15% random failure rate — the same failure
 * rate you'd see with a real gateway under poor network conditions.
 *
 * Usage:
 *   PaymentService ps = new PaymentService();
 *   Payment p = ps.processPayment(PaymentMethod.CREDIT_CARD, 7200.0);
 *   // If it throws PaymentFailedException, offer "Retry" to the user.
 */
public class PaymentService {

    // 15% chance that a transaction fails (matches the spec)
    private static final double FAILURE_RATE = 0.15;

    // Random number generator — reused across calls
    private Random random;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Creates a PaymentService with a fresh Random instance.
     */
    public PaymentService() {
        this.random = new Random();
    }

    // ── Process Payment ───────────────────────────────────────────────────────

    /**
     * Simulates a payment transaction and returns a Payment object.
     *
     * Logic:
     *  1. Create a Payment object in PENDING state.
     *  2. Generate a random number between 0.0 and 1.0.
     *  3. If the number is less than FAILURE_RATE (15%), the transaction fails.
     *  4. Otherwise, mark the payment as SUCCESS and return it.
     *
     * The caller (UI) must catch PaymentFailedException and offer a retry.
     *
     * @param method the payment mode chosen by the user
     * @param amount the total INR amount to charge
     * @return a Payment object with status SUCCESS
     * @throws PaymentFailedException if the simulated transaction fails (~15% of the time)
     */
    public Payment processPayment(PaymentMethod method, double amount)
            throws PaymentFailedException {

        // Create a payment object — starts in PENDING state
        Payment payment = new Payment(method, amount);

        // Simulate a short processing delay (500 ms) for realism
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
        }

        // Roll the dice: random number in [0.0, 1.0)
        double roll = random.nextDouble();

        if (roll < FAILURE_RATE) {
            // ── Transaction failed ──
            payment.setStatus(PaymentStatus.FAILED);
            System.out.println("[PaymentService] Transaction FAILED (roll=" +
                               String.format("%.2f", roll) + ", limit=" + FAILURE_RATE + ")");
            throw new PaymentFailedException(
                "Payment declined by the bank. Please retry or use a different method.");
        }

        // ── Transaction succeeded ──
        payment.setStatus(PaymentStatus.SUCCESS);
        System.out.println("[PaymentService] Transaction SUCCESS — INR " + amount
                           + " via " + method);
        return payment;
    }
}

