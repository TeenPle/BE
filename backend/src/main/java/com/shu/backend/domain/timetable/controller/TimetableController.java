package com.shu.backend.domain.timetable.controller;

import com.shu.backend.domain.timetable.dto.TimetableDTO;
import com.shu.backend.domain.timetable.service.NeisTimetableService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Timetable", description = "시간표 관련 API")
@RestController
@RequiredArgsConstructor
public class TimetableController {

    private final NeisTimetableService neisTimetableService;

    @Operation(
            summary = "내 시간표 조회",
            description = "인증된 유저의 학년을 자동 적용해 시간표를 조회합니다. " +
                    "classRoom(반)은 직접 입력, from/to 형식: YYYYMMDD"
    )
    @GetMapping("/api/timetable/me")
    public ApiResponse<TimetableDTO.WeekResponse> getMyTimetable(
            @AuthenticationPrincipal User user,
            @RequestParam String classRoom,
            @RequestParam String from,
            @RequestParam String to
    ) {
        TimetableDTO.WeekResponse response =
                neisTimetableService.getTimetable(user, classRoom, from, to);
        return ApiResponse.onSuccess(response);
    }
}
