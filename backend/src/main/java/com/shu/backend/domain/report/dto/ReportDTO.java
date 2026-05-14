package com.shu.backend.domain.report.dto;

import com.shu.backend.domain.report.enums.ReportReason;
import com.shu.backend.domain.report.enums.TargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ReportDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest{
        @NotNull
        private TargetType targetType;

        @NotNull
        private Long targetId;

        @NotNull
        private ReportReason reportReason;

        @Size(max = 500)
        private String reportDetail;
    }

    @Getter
    @Builder
    public static class CreateResponse{
        private Long reportId;
    }
}
