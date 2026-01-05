package com.shu.backend.domain.usersetting.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class UserSettingException extends GeneralException {

    public UserSettingException(BaseErrorCode code) {
        super(code);
    }
}
