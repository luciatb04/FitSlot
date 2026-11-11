package org.springframework.samples.fitslot.membership;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.MembershipConfigurationDTO;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfiguration;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBandConfigurationRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MembershipTierService {

    private final MembershipTierRepository repository;
    private final TimeBandConfigurationRepository timeBandRepository;

    public MembershipTierService(
            MembershipTierRepository repository, TimeBandConfigurationRepository timeBandRepository) {
        this.repository = repository;
        this.timeBandRepository = timeBandRepository;
    }

    @Transactional(readOnly = true)
    public Set<ServiceType> getServices(Integer tierId) {

        MembershipTier tier = repository.findById(tierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership tier not found"));

        return new HashSet<>(tier.getAllowedServices());
    }

    @Transactional(readOnly = true)
    public Iterable<MembershipTier> getAllTiers() {
        return repository.findAll();
    }

    @Transactional
    public Set<ServiceType> updateServices(
            Integer tierId, Set<ServiceType> services) {

        if (services == null || services.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one service is required");
        }

        MembershipTier tier = repository.findById(tierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership tier not found"));

        tier.getAllowedServices().clear();
        tier.getAllowedServices().addAll(services);

        repository.save(tier);

        return new HashSet<>(tier.getAllowedServices());
    }

    @Transactional(readOnly = true)
    public TimeBand getTimeBand(Integer tierId) {

        MembershipTier tier = repository.findById(tierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership tier not found"));

        return tier.getTimeBand();
    }

    @Transactional
    public TimeBand updateTimeBand(
            Integer tierId, TimeBand timeBand) {

        if (timeBand == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Time band cannot be null");
        }

        MembershipTier tier = repository.findById(tierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership tier not found"));

        tier.setTimeBand(timeBand);
        repository.save(tier);

        return tier.getTimeBand();
    }

    @Transactional
    public MembershipTier createTier(MembershipTierRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String name = request.name().trim();
        if (repository.findByNameIgnoreCase(name).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A membership tier with this name already exists");
        }

        MembershipTier tier = new MembershipTier();
        tier.setName(name);
        tier.setPrice(request.price());
        tier.setDurationDays(request.durationDays());
        tier.setEntryQuota(request.entryQuota());
        tier.setAllowedServices(new HashSet<>(request.allowedServices()));
        tier.setTimeBand(request.timeBand());

        return repository.save(tier);
    }

    @Transactional
    public MembershipTier updateConfiguration(
            Integer tierId,
            MembershipConfigurationDTO request) {

        if (request == null ||
                request.allowedServices() == null ||
                request.allowedServices().isEmpty() ||
                request.allowedServices().stream().anyMatch(Objects::isNull) ||
                request.timeBand() == null ||
                request.startTime() == null ||
                request.endTime() == null ||
                !request.startTime().isBefore(request.endTime())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid membership configuration");
        }

        MembershipTier tier = repository.findById(tierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Membership tier not found"));

        TimeBandConfiguration band = timeBandRepository
                .findByTimeBand(request.timeBand())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Time band configuration not found"));

        tier.getAllowedServices().clear();
        tier.getAllowedServices().addAll(request.allowedServices());
        tier.setTimeBand(request.timeBand());

        band.setStartTime(request.startTime());
        band.setEndTime(request.endTime());

        timeBandRepository.save(band);

        return repository.save(tier);
    }

}
