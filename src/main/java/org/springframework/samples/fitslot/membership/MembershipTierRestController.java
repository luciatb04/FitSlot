package org.springframework.samples.fitslot.membership;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/membership-tiers")

/*El controlador publica las operaciones HTTP y convierte la entidad a una respuesta segura. 
Por ahora incluye listar, consultar, crear y editar; la issue no exige eliminar planes, y borrarlos 
podría ser problemático si alguna suscripción ya los utiliza. */

public class MembershipTierRestController {

    private final MembershipTierService membershipTierService;

    public MembershipTierRestController(
        MembershipTierService membershipTierService
    ) {
        this.membershipTierService = membershipTierService;
    }

    @GetMapping
    public List<MembershipTierResponse> findAll() {
        return membershipTierService.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{id}")
    public MembershipTierResponse findById(@PathVariable Integer id) {
        return toResponse(membershipTierService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MembershipTierResponse> create(
        @Valid @RequestBody MembershipTierRequest request
    ) {
        MembershipTier tier = membershipTierService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toResponse(tier));
    }

    @PutMapping("/{id}")
    public MembershipTierResponse update(
        @PathVariable Integer id,
        @Valid @RequestBody MembershipTierRequest request
    ) {
        return toResponse(membershipTierService.update(id, request));
    }

    private MembershipTierResponse toResponse(MembershipTier tier) {
        return new MembershipTierResponse(
            tier.getId(),
            tier.getName(),
            tier.getPrice(),
            tier.getDurationDays(),
            tier.getEntryQuota(),
            Set.copyOf(tier.getAllowedServices()),
            tier.getTimeBand()
        );
    }
}