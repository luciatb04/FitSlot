package org.springframework.samples.fitslot.member;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.samples.fitslot.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Member findById(Integer id) {
        Member member = memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", "ID", id));

        if (!member.isActive()) {
            throw new ResourceNotFoundException("Member", "ID", id);
        }

        return member;
    }

    @Transactional
    public Member create(MemberRequest request) {
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already registered"
            );
        }

        Member member = new Member();
        applyRequest(member, request);
        member.setActive(true);

        return memberRepository.save(member);
    }

    @Transactional
    public Member update(Integer id, MemberRequest request) {
        Member member = findById(id);

        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email already registered"
            );
        }

        applyRequest(member, request);
        return memberRepository.save(member);
    }

    @Transactional
    public void deactivate(Integer id) {
        Member member = findById(id);
        member.setActive(false);
        memberRepository.save(member);
    }

    private void applyRequest(Member member, MemberRequest request) {
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setTelephone(request.telephone());
    }
}