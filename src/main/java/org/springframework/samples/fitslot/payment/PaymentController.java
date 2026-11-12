package org.springframework.samples.fitslot.payment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentRecordResponse> findAll(
            @RequestParam(required = false) Integer memberId) {
        return paymentService.findAll(memberId);
    }

    @PostMapping
    public ResponseEntity<PaymentRecordResponse> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @PutMapping("/{paymentId}/status")
    public PaymentRecordResponse updateStatus(
            @PathVariable Integer paymentId,
            @Valid @RequestBody PaymentStatusRequest request) {
        return paymentService.updateStatus(paymentId, request.status());
    }
}
