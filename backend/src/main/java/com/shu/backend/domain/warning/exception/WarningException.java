package com.shu.backend.domain.warning.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class WarningException extends GeneralException {
    public WarningException(BaseErrorCode code) {
        super(code);
    }
}
