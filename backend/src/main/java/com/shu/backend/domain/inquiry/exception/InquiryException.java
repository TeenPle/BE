package com.shu.backend.domain.inquiry.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class InquiryException extends GeneralException {
    public InquiryException(BaseErrorCode code) {
        super(code);
    }
}
