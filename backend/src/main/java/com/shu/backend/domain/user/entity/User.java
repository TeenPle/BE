package com.shu.backend.domain.user.entity;

import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="name", nullable = false)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl = "default_profile.png";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    //학교 인증 o,x 여부
    @Column(name="verified",nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;


    @Column(nullable = false, length = 20,unique = true)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;

    // 학교 인증 완료 처리
    public void verifySchool() {
        this.verified = true;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.nicknameChangedAt = LocalDateTime.now();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate() {
        this.status = UserStatus.DELETED;
    }

    /** 탈퇴 시 PII 즉시 파기. User 행은 게시글/댓글 FK 보존을 위해 유지. */
    public void anonymize() {
        this.username = "탈퇴한 사용자";
        this.email = "deleted_" + this.id + "@deleted.invalid";
        this.password = UUID.randomUUID().toString();
        this.nickname = "탈퇴한사용자_" + this.id;
        this.phoneNumber = "D" + this.id;
        this.profileImageUrl = "default_profile.png";
        this.role = UserRole.USER;
        this.verified = false;
        this.phoneVerified = false;
        this.nicknameChangedAt = null;
        this.status = UserStatus.DELETED;
    }
}
