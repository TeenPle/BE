package com.shu.backend.domain.poll.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class PollException extends GeneralException {
    public PollException(BaseErrorCode code) {
        super(code);
    }
}
