package org.springframework.samples.fitslot.member;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        return memberRepository.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public Member findById(Integer id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Member", "ID", id));
    }

    @Transactional
    public Member create(MemberRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);

        Member member = new Member();
        applyRequest(member, request);
        return memberRepository.save(member);
    }

    @Transactional
    public Member saveMember(Member member) {
        if (member.getEmail() != null) {
            String email = normalizeEmail(member.getEmail());
            ensureEmailAvailable(email, member.getId());
            member.setEmail(email);
        }
        return memberRepository.save(member);
    }

    @Transactional
    public Member update(Integer id, MemberRequest request) {
        Member member = findById(id);

        ensureEmailAvailable(normalizeEmail(request.email()), id);

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
        member.setEmail(normalizeEmail(request.email()));
        member.setTelephone(request.telephone());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureEmailAvailable(String email, Integer memberId) {
        boolean alreadyRegistered = memberId == null
            ? memberRepository.existsByEmailIgnoreCase(email)
            : memberRepository.existsByEmailIgnoreCaseAndIdNot(email, memberId);
        if (alreadyRegistered) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }
}