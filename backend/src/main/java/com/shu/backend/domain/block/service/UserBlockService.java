package com.shu.backend.domain.block.service;

import com.shu.backend.domain.block.entity.UserBlock;
import com.shu.backend.domain.block.repository.UserBlockRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return;
        }
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
        if (blocked.getStatus() == UserStatus.DELETED) {
            throw new UserException(UserErrorStatus.USER_NOT_FOUND);
        }

        userBlockRepository.save(UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build());
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public long getBlockedCount(Long blockerId) {
        return userBlockRepository.countByBlockerId(blockerId);
    }

    @Transactional
    public void unblockAll(Long blockerId) {
        userBlockRepository.deleteByBlockerId(blockerId);
    }
}
