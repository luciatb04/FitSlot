package org.springframework.samples.fitslot.membership.TimeBandConfiguration;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.samples.fitslot.membership.TimeBandConfiguration.TimeBand;

public interface TimeBandConfigurationRepository
        extends CrudRepository<TimeBandConfiguration, Integer> {

    Optional<TimeBandConfiguration> findByTimeBand(TimeBand timeBand);
    
     
}
