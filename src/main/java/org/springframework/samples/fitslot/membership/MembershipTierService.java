package org.springframework.samples.fitslot.membership;

import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


/*El repositorio se ocupa de guardar y buscar; el servicio decide si los datos representan
un plan válido. La expresión hasDuration == hasEntryQuota detecta tanto 
“ninguno informado” como “ambos informados”. @Transactional hace que la operación
de escritura se ejecute en una transacción. */

@Service
public class MembershipTierService {

    private final MembershipTierRepository membershipTierRepository;

    public MembershipTierService(
        MembershipTierRepository membershipTierRepository
    ) {
        this.membershipTierRepository = membershipTierRepository;
    }

    @Transactional(readOnly = true)
    public List<MembershipTier> findAll() {
        return membershipTierRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public MembershipTier findById(Integer id) {
        return membershipTierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Membership tier",
                "ID",
                id
            ));
    }

    @Transactional
    public MembershipTier create(MembershipTierRequest request) {
        validatePlanType(request);

        if (membershipTierRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A membership tier with that name already exists"
            );
        }

        MembershipTier tier = new MembershipTier();
        applyRequest(tier, request);
        return membershipTierRepository.save(tier);
    }

    @Transactional
    public MembershipTier update(Integer id, MembershipTierRequest request) {
        validatePlanType(request);

        MembershipTier tier = findById(id);

        if (membershipTierRepository
                .existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A membership tier with that name already exists"
            );
        }

        applyRequest(tier, request);
        return membershipTierRepository.save(tier);
    }

    private void validatePlanType(MembershipTierRequest request) {
        boolean hasDuration = request.durationDays() != null;
        boolean hasEntryQuota = request.entryQuota() != null;

        if (hasDuration == hasEntryQuota) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A tier must define either durationDays or entryQuota, but not both"
            );
        }

        if (request.allowedServices() == null || request.allowedServices().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "At least one allowed service must be selected"
            );
        }
    }

    private void applyRequest(
        MembershipTier tier,
        MembershipTierRequest request
    ) {
        tier.setName(request.name().trim());
        tier.setPrice(request.price());
        tier.setDurationDays(request.durationDays());
        tier.setEntryQuota(request.entryQuota());
        tier.setAllowedServices(new HashSet<>(request.allowedServices()));
        tier.setTimeBand(request.timeBand());
    }
}