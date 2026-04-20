package com.shu.backend.domain.user.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerificationReapplyInfoResponseDTO {
    private Long schoolId;
    private String schoolName;
    private String adminComment;
}