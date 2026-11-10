package org.springframework.samples.fitslot.member;

public record MemberResponse(
    Integer id,
    String firstName,
    String lastName,
    String email,
    String telephone
) {
}