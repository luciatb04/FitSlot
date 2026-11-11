package org.springframework.samples.fitslot.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MemberServiceTests {

    @Mock
    private MemberRepository memberRepository;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository);
    }

    @Test
    void createsActiveMemberAndNormalizesEmail() {
        MemberRequest request = new MemberRequest("Ada", "Lovelace", "  ADA@example.com  ", "123456789");
        when(memberRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setId(42);
            return member;
        });

        Member created = memberService.create(request);

        assertThat(created.getId()).isEqualTo(42);
        assertThat(created.getEmail()).isEqualTo("ada@example.com");
        assertThat(created.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberRepository).existsByEmailIgnoreCase("ada@example.com");
        verify(memberRepository).save(created);
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void rejectsEmailAlreadyRegisteredWithoutSaving() {
        MemberRequest request = new MemberRequest("Ada", "Lovelace", " ADA@example.com ", null);
        when(memberRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> memberService.create(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("Email already registered");
        verify(memberRepository).existsByEmailIgnoreCase("ada@example.com");
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void preventsDuplicateProfilesThroughTheExistingSavePath() {
        Member member = new Member();
        member.setEmail(" ADA@example.com ");
        when(memberRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> memberService.saveMember(member)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("Email already registered");
        verify(memberRepository).existsByEmailIgnoreCase("ada@example.com");
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    void updatesMemberEmailWithoutChangingItsStatus() {
        Member existing = new Member();
        existing.setId(7);
        existing.setStatus(MemberStatus.INACTIVE);
        when(memberRepository.findById(7)).thenReturn(Optional.of(existing));
        when(memberRepository.existsByEmailIgnoreCaseAndIdNot("ada@example.com", 7)).thenReturn(false);
        when(memberRepository.save(existing)).thenReturn(existing);

        Member updated = memberService.update(
            7,
            new MemberRequest("Ada", "Lovelace", " ADA@example.com ", null)
        );

        assertThat(updated.getEmail()).isEqualTo("ada@example.com");
        assertThat(updated.getStatus()).isEqualTo(MemberStatus.INACTIVE);
        verify(memberRepository).findById(7);
        verify(memberRepository).existsByEmailIgnoreCaseAndIdNot("ada@example.com", 7);
        verify(memberRepository).save(existing);
        verifyNoMoreInteractions(memberRepository);
    }
}
