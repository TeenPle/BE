package com.shu.backend.domain.admin.service;

import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminPushService {

    private final UserRepository userRepository;
    private final PushService pushService;

    public void notifyActiveAdmins(String title, String body, Map<String, String> data) {
        userRepository.findByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)
                .forEach(admin -> pushService.sendToUserAfterCommit(admin.getId(), title, body, data));
    }
}
