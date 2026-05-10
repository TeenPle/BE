package com.shu.backend.domain.user.support;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserStatus;

public final class UserDisplay {

    public static final String DELETED_USER_NAME = "탈퇴한 사용자";

    private UserDisplay() {
    }

    public static boolean isDeleted(User user) {
        return user == null || user.getStatus() == UserStatus.DELETED;
    }

    public static String nicknameOrDeleted(User user) {
        return isDeleted(user) ? DELETED_USER_NAME : user.getNickname();
    }

    public static String usernameOrDeleted(User user) {
        return isDeleted(user) ? DELETED_USER_NAME : user.getUsername();
    }

    public static String anonymousOrDeleted(User user, String anonymousName) {
        return isDeleted(user) ? DELETED_USER_NAME : anonymousName;
    }
}
