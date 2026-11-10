package org.springframework.samples.fitslot.member;

import java.util.ArrayList;
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
        List<Member> members = new ArrayList<>();
        memberRepository.findAll().forEach(members::add);
        return members;
    }

    @Transactional(readOnly = true)
    public Member findById(Integer id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", "ID", id));
    }

    @Transactional
    public Member create(MemberRequest request) {
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Member member = new Member();
        applyRequest(member, request);
        return memberRepository.save(member);
    }

    @Transactional
    public Member update(Integer id, MemberRequest request) {
        Member member = findById(id);

        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        applyRequest(member, request);
        return memberRepository.save(member);
    }

    @Transactional
    public void delete(Integer id) {
        Member member = findById(id);
        memberRepository.delete(member);
    }

    private void applyRequest(Member member, MemberRequest request) {
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setTelephone(request.telephone());
    }
}