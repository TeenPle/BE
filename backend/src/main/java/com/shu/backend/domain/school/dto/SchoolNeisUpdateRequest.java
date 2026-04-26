package com.shu.backend.domain.school.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SchoolNeisUpdateRequest {

    @NotBlank
    private String neisOfficeCode;

    @NotBlank
    private String neisSchoolCode;
}
