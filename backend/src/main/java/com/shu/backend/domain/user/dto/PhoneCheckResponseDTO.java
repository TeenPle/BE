package com.shu.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneCheckResponseDTO {
    private boolean exists;
}