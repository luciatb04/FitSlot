package org.springframework.samples.fitslot.member;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface MemberRepository extends CrudRepository<Member, Integer> {

    List<Member> findByActiveTrue();

    Optional<Member> findByEmailIgnoreCase(String email);

    Optional<Member> findByUser_Id(Integer userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}