package org.springframework.samples.fitslot.member;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.fitslot.auth.payload.response.MessageResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberRestController {

    private final MemberService memberService;

    public MemberRestController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<MemberResponse> findAll() {
        return memberService.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Integer id) {
        return toResponse(memberService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> create(
        @Valid @RequestBody MemberRequest request
    ) {
        Member member = memberService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(member));
    }

    @PutMapping("/{id}")
    public MemberResponse update(
        @PathVariable Integer id,
        @Valid @RequestBody MemberRequest request
    ) {
        return toResponse(memberService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deactivate(@PathVariable Integer id) {
        memberService.deactivate(id);
        return ResponseEntity.ok(new MessageResponse("Member deactivated"));
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getFirstName(),
            member.getLastName(),
            member.getEmail(),
            member.getTelephone(),
            member.isActive()
        );
    }
}