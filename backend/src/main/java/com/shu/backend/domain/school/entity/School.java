package com.shu.backend.domain.school.entity;

import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class School extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "logo_image_url")
    private String logoImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "neis_office_code", length = 10)
    private String neisOfficeCode;

    @Column(name = "neis_school_code", length = 20)
    private String neisSchoolCode;


    @Builder
    public School(String name) {
        this.name = name;
    }

    public void updateRegion(Region region) {
        this.region = region;
    }

    public void updateNeisCodes(String neisOfficeCode, String neisSchoolCode) {
        this.neisOfficeCode = neisOfficeCode;
        this.neisSchoolCode = neisSchoolCode;
    }
}
