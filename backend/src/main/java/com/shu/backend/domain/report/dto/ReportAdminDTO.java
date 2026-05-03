package com.shu.backend.domain.report.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ReportAdminDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproveRequest{
        @NotNull
        @Min(1)
        private Integer penaltyDays;
    }
}
