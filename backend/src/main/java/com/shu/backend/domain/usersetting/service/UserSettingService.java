package com.shu.backend.domain.usersetting.service;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.usersetting.dto.UserSettingDTO;
import com.shu.backend.domain.usersetting.entity.UserSetting;
import com.shu.backend.domain.usersetting.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;


    public UserSettingDTO.Response getMySetting(Long userId) {
        UserSetting setting = getOrCreate(userId);
        return UserSettingDTO.Response.from(setting);
    }

    public UserSettingDTO.Response updateMySetting(Long userId, UserSettingDTO.UpdateRequest req) {
        UserSetting setting = getOrCreate(userId);

        setting.update(
                req.getAllowPush(),
                req.getAllowCommentNotification(),
                req.getAllowReplyNotification(),
                req.getAllowLikeNotification(),
                req.getAllowChatNotification()
        );

        return UserSettingDTO.Response.from(setting);
    }

    /**
     * 유저 설정이 없으면 자동 생성.
     * - user_setting은 user_id unique이므로 동시 생성 경쟁 시 예외가 날 수 있어 방어 처리 포함.
     */
    @Transactional
    protected UserSetting getOrCreate(Long userId) {

        return userSettingRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            try {
                return userSettingRepository.save(UserSetting.create(user));
            } catch (DataIntegrityViolationException e) {
                // 동시성으로 이미 생성된 경우 재조회로 회복
                return userSettingRepository.findByUserId(userId)
                        .orElseThrow(() -> e);
            }
        });
    }
}
