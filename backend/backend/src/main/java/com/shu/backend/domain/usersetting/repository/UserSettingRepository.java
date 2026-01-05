package com.shu.backend.domain.usersetting.repository;

import com.shu.backend.domain.usersetting.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
