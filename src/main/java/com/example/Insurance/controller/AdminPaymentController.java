package com.example.Insurance.controller;

import com.example.Insurance.entity.Payment;
import com.example.Insurance.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@CrossOrigin(origins = "*")
public class AdminPaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/pending")
    public ResponseEntity<List<Payment>> getPendingPayments() {
        try {
            List<Payment> payments = paymentService.getPendingPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<Payment> approvePayment(@PathVariable Long paymentId, @RequestBody String comments) {
        try {
            Payment payment = paymentService.approvePayment(paymentId, comments);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{paymentId}/reject")
    public ResponseEntity<Payment> rejectPayment(@PathVariable Long paymentId, @RequestBody String comments) {
        try {
            Payment payment = paymentService.rejectPayment(paymentId, comments);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
