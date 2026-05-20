package com.shu.backend.domain.school.dto;

import com.shu.backend.domain.school.entity.School;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolResponse {

    private Long id;

    private String name;

    private Long regionId;

    private String regionName;

    private String neisOfficeCode;

    private String neisSchoolCode;

    public static SchoolResponse toDto(School school) {
        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .regionId(school.getRegion() != null ? school.getRegion().getId() : null)
                .regionName(school.getRegion() != null ? school.getRegion().getName() : null)
                .neisOfficeCode(school.getNeisOfficeCode())
                .neisSchoolCode(school.getNeisSchoolCode())
                .build();
    }
}
