package org.springframework.samples.fitslot.membership;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/*El primer metodo lista los planes ordenador, los otros rechazan nombres repetidos
al crear y editar un plan de membresia
*/

public interface MembershipTierRepository
        extends CrudRepository<MembershipTier, Integer> {

    List<MembershipTier> findAllByOrderByNameAsc();

    Optional<MembershipTier> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}