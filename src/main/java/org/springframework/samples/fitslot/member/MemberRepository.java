// member/MemberRepository.java
package org.springframework.samples.fitslot.member;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface MemberRepository extends CrudRepository<Member, Integer> {
    java.util.List<Member> findAllByOrderByLastNameAscFirstNameAsc();
    Optional<Member> findByEmailIgnoreCase(String email);
    Optional<Member> findByUser_Id(Integer userId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}