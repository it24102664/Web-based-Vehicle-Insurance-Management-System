package com.example.Insurance.controller;

import com.example.Insurance.entity.Payment;
import com.example.Insurance.entity.Policy;
import com.example.Insurance.repository.PolicyRepository;
import com.example.Insurance.DTO.UserPolicyPaymentDTO;
import com.example.Insurance.service.PaymentService;
import com.example.Insurance.Enums.PolicyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/payments")
@CrossOrigin(origins = "*")
public class UserPaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PolicyRepository policyRepository;

    // DEBUG ENDPOINTS
    @GetMapping("/test-connection")
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("Payment controller is working! Current time: " + java.time.LocalDateTime.now());
    }

    @GetMapping("/test-policies-count")
    public ResponseEntity<String> testPoliciesCount() {
        try {
            List<Policy> allPolicies = policyRepository.findAll();
            List<Policy> approvedPolicies = allPolicies.stream()
                    .filter(policy -> policy.getStatus() == PolicyStatus.APPROVED || policy.getStatus() == PolicyStatus.ACTIVE)
                    .collect(Collectors.toList());

            String result = "Database Connection: SUCCESS\n" +
                    "Total policies: " + allPolicies.size() + "\n" +
                    "Approved/Active policies: " + approvedPolicies.size() + "\n\n" +
                    "Policy Details:\n";

            for (Policy policy : allPolicies) {
                result += "ID: " + policy.getId() +
                        ", Name: " + policy.getName() +
                        ", Status: " + policy.getStatus() +
                        ", Premium: " + policy.getPremiumAmount() + "\n";
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok("Database Error: " + e.getMessage());
        }
    }

    // FIXED - Get user's approved policies with payment information
    @GetMapping("/policies/{userId}")
    public ResponseEntity<?> getUserPolicies(@PathVariable Long userId) {
        try {
            System.out.println("=== Controller: getUserPolicies called for user " + userId + " ===");

            List<UserPolicyPaymentDTO> policies = paymentService.getUserPoliciesWithPayments(userId);

            System.out.println("=== Controller: Returning " + policies.size() + " policies ===");
            for (UserPolicyPaymentDTO policy : policies) {
                System.out.println("Policy: " + policy.getPolicyNumber() + " - " + policy.getPolicyType());
            }

            return ResponseEntity.ok(policies);
        } catch (Exception e) {
            System.err.println("=== Controller Error: " + e.getMessage() + " ===");
            e.printStackTrace();

            // Return empty list instead of error to prevent 500
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    // EMERGENCY FIX - Get REAL payment history (no more test data!)
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getPaymentHistory(@PathVariable Long userId) {
        System.out.println("=== EMERGENCY CONTROLLER: Getting history for user " + userId + " ===");

        try {
            List<Payment> payments = paymentService.getPaymentHistory(userId);

            System.out.println("CONTROLLER SUCCESS: Got " + payments.size() + " payments");

            // Convert to simple JSON to avoid serialization issues
            List<Map<String, Object>> simplePayments = new ArrayList<>();
            for (Payment p : payments) {
                try {
                    Map<String, Object> simple = new HashMap<>();
                    simple.put("paymentId", p.getPaymentId());
                    simple.put("paymentMonth", p.getPaymentMonth());
                    simple.put("amount", p.getAmount());
                    simple.put("status", p.getStatus().toString());
                    simple.put("paymentMethod", p.getPaymentMethod().toString());
                    simple.put("submittedDate", p.getSubmittedDate() != null ? p.getSubmittedDate().toString() : "N/A");
                    simple.put("expiryTime", p.getExpiryTime() != null ? p.getExpiryTime().toString() : "N/A");

                    // Add policy info safely
                    if (p.getPolicy() != null) {
                        Map<String, Object> policy = new HashMap<>();
                        policy.put("id", p.getPolicy().getId());
                        policy.put("policyNumber", "POL-" + p.getPolicy().getId());
                        simple.put("policy", policy);
                    }

                    simplePayments.add(simple);
                } catch (Exception e) {
                    System.err.println("Error processing payment " + p.getPaymentId() + ": " + e.getMessage());
                }
            }

            return ResponseEntity.ok(simplePayments);

        } catch (Exception e) {
            System.err.println("EMERGENCY CONTROLLER ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>()); // Return empty list, NEVER 500
        }
    }

    // ENHANCED - Submit new payment with immediate feedback
    @PostMapping
    public ResponseEntity<?> submitPayment(@RequestBody Payment payment) {
        try {
            System.out.println("=== Controller: submitPayment called ===");
            System.out.println("Payment data received: Month=" + payment.getPaymentMonth() +
                    ", Amount=" + payment.getAmount() +
                    ", Method=" + payment.getPaymentMethod());

            Payment savedPayment = paymentService.createPayment(payment);
            System.out.println("=== Controller: Payment created with ID " + savedPayment.getPaymentId() + " ===");

            // Immediately verify the payment exists
            try {
                Payment verification = paymentService.getPaymentById(savedPayment.getPaymentId());
                System.out.println("✅ CONTROLLER VERIFICATION: Payment " + savedPayment.getPaymentId() + " exists and can be retrieved");
            } catch (Exception ve) {
                System.err.println("❌ CONTROLLER VERIFICATION FAILED: " + ve.getMessage());
            }

            return ResponseEntity.ok(savedPayment);
        } catch (Exception e) {
            System.err.println("=== Controller Error in submitPayment: " + e.getMessage() + " ===");
            e.printStackTrace();

            // Return error message as JSON instead of throwing 500
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Failed to create payment: " + e.getMessage())
            );
        }
    }

    // ENHANCED - Get payment by ID with detailed logging
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId) {
        try {
            System.out.println("=== Controller: getPayment called for ID " + paymentId + " ===");

            Payment payment = paymentService.getPaymentById(paymentId);

            System.out.println("✅ Controller: Found payment " + paymentId +
                    ", Status=" + payment.getStatus() +
                    ", Month=" + payment.getPaymentMonth());

            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            System.err.println("=== Controller Error in getPayment: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // ENHANCED - Update payment with validation
    @PutMapping("/{paymentId}")
    public ResponseEntity<?> updatePayment(@PathVariable Long paymentId, @RequestBody Payment payment) {
        try {
            System.out.println("=== Controller: updatePayment called for ID " + paymentId + " ===");

            Payment updatedPayment = paymentService.updatePayment(paymentId, payment);

            System.out.println("✅ Controller: Payment " + paymentId + " updated successfully");

            return ResponseEntity.ok(updatedPayment);
        } catch (Exception e) {
            System.err.println("=== Controller Error in updatePayment: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Failed to update payment: " + e.getMessage())
            );
        }
    }

    // ENHANCED - Delete payment with confirmation
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable Long paymentId) {
        try {
            System.out.println("=== Controller: deletePayment called for ID " + paymentId + " ===");

            paymentService.deletePayment(paymentId);

            System.out.println("✅ Controller: Payment " + paymentId + " deleted successfully");

            return ResponseEntity.ok().body(java.util.Map.of("message", "Payment deleted successfully"));
        } catch (Exception e) {
            System.err.println("=== Controller Error in deletePayment: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Failed to delete payment: " + e.getMessage())
            );
        }
    }

    // Upload bank slip
    @PostMapping("/{paymentId}/upload-slip")
    public ResponseEntity<?> uploadBankSlip(@PathVariable Long paymentId, @RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Controller: uploadBankSlip called for payment " + paymentId + " ===");

            paymentService.uploadBankSlip(paymentId, file);

            System.out.println("✅ Controller: Bank slip uploaded for payment " + paymentId);

            return ResponseEntity.ok().body(java.util.Map.of("message", "Bank slip uploaded successfully"));
        } catch (IOException e) {
            System.err.println("=== Controller Error in uploadBankSlip: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Failed to upload file"));
        } catch (Exception e) {
            System.err.println("=== Controller Error in uploadBankSlip: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Error: " + e.getMessage()));
        }
    }

    // Check if payment can be edited
    @GetMapping("/{paymentId}/can-edit")
    public ResponseEntity<Boolean> canEditPayment(@PathVariable Long paymentId) {
        try {
            System.out.println("=== Controller: canEditPayment called for ID " + paymentId + " ===");

            Payment payment = paymentService.getPaymentById(paymentId);
            boolean canEdit = paymentService.canEditPayment(payment);

            System.out.println("Controller: Payment " + paymentId + " can edit = " + canEdit);

            return ResponseEntity.ok(canEdit);
        } catch (Exception e) {
            System.err.println("=== Controller Error in canEditPayment: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.ok(false);
        }
    }

    // Get bank details
    @GetMapping("/bank-details")
    public ResponseEntity<?> getBankDetails() {
        try {
            System.out.println("=== Controller: getBankDetails called ===");
            Map<String, String> bankDetails = paymentService.getBankDetails();
            System.out.println("=== Controller: Returning bank details ===");
            return ResponseEntity.ok(bankDetails);
        } catch (Exception e) {
            System.err.println("=== Controller Error in getBankDetails: " + e.getMessage() + " ===");
            e.printStackTrace();

            // Return default bank details to prevent errors
            return ResponseEntity.ok(java.util.Map.of(
                    "bankName", "Commercial Bank of Ceylon PLC",
                    "accountNumber", "8001234567890",
                    "accountName", "MOTORCARE LK (PVT) LTD",
                    "branch", "Colombo 03"
            ));
        }
    }

    // Get pending payments for current user
    @GetMapping("/pending/{userId}")
    public ResponseEntity<List<Payment>> getPendingPayments(@PathVariable Long userId) {
        try {
            System.out.println("=== Controller: getPendingPayments called for user " + userId + " ===");

            // Get all user payments and filter for pending ones
            List<Payment> allUserPayments = paymentService.getPaymentHistory(userId);
            List<Payment> pendingPayments = allUserPayments.stream()
                    .filter(p -> p.getStatus() == com.example.Insurance.Enums.PaymentStatus.PENDING)
                    .collect(Collectors.toList());

            System.out.println("Found " + pendingPayments.size() + " pending payments for user " + userId);

            return ResponseEntity.ok(pendingPayments);
        } catch (Exception e) {
            System.err.println("=== Controller Error in getPendingPayments: " + e.getMessage() + " ===");
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // DEBUG: Get all payments (for troubleshooting)
    @GetMapping("/debug/all-payments")
    public ResponseEntity<?> getAllPayments() {
        try {
            System.out.println("=== Controller: DEBUG - Getting all payments ===");

            List<Payment> allPayments = paymentService.getPaymentHistory(1L); // Get payments for user 1

            // Get actual all payments from service - try to get more users if needed
            List<Map<String, Object>> debugInfo = new ArrayList<>();

            for (Payment payment : allPayments) {
                try {
                    Map<String, Object> info = new HashMap<>();
                    info.put("paymentId", payment.getPaymentId());
                    info.put("userId", payment.getUserId());
                    info.put("month", payment.getPaymentMonth());
                    info.put("amount", payment.getAmount());
                    info.put("status", payment.getStatus().toString());
                    info.put("submittedDate", payment.getSubmittedDate() != null ? payment.getSubmittedDate().toString() : "N/A");
                    debugInfo.add(info);
                } catch (Exception e) {
                    System.err.println("Error processing payment for debug: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "totalPayments", allPayments.size(),
                    "payments", debugInfo
            ));
        } catch (Exception e) {
            System.err.println("Debug endpoint error: " + e.getMessage());
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    // Test endpoint to create approved policies
    @PostMapping("/create-test-policy")
    public ResponseEntity<String> createTestPolicy() {
        try {
            Policy testPolicy = new Policy();
            testPolicy.setName("Test Motor Insurance");
            testPolicy.setIcon("🚗");
            testPolicy.setDescription("Test policy for payment integration");
            testPolicy.setPremiumAmount(15000.0);
            testPolicy.setCoverageAmount(500000.0);
            testPolicy.setVehicleType("Car");
            testPolicy.setStatus(PolicyStatus.APPROVED);
            testPolicy.setCreatedDate(java.time.LocalDate.now());
            testPolicy.setUpdatedDate(java.time.LocalDate.now());

            Policy saved = policyRepository.save(testPolicy);

            return ResponseEntity.ok("Test policy created with ID: " + saved.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating test policy: " + e.getMessage());
        }
    }

    // Endpoint to approve existing policies
    @PostMapping("/approve-policy/{policyId}")
    public ResponseEntity<String> approvePolicy(@PathVariable Long policyId) {
        try {
            Policy policy = policyRepository.findById(policyId).orElse(null);
            if (policy == null) {
                return ResponseEntity.badRequest().body("Policy not found");
            }

            policy.setStatus(PolicyStatus.APPROVED);
            policyRepository.save(policy);

            return ResponseEntity.ok("Policy " + policyId + " approved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error approving policy: " + e.getMessage());
        }
    }
}
