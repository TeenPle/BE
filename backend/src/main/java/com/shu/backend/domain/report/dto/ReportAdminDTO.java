package com.shu.backend.domain.report.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

        @NotBlank
        @Size(max = 500)
        private String adminComment;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        @NotBlank
        @Size(max = 500)
        private String adminComment;
    }
}
