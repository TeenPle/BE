package com.shu.backend.domain.meal.controller;

import com.shu.backend.domain.meal.dto.MealDTO;
import com.shu.backend.domain.meal.service.NeisMealService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Meal", description = "급식 관련 API")
@RestController
@RequiredArgsConstructor
public class MealController {

    private final NeisMealService neisMealService;

    @Operation(
            summary = "급식 조회",
            description = "학교 ID와 날짜 범위로 급식 메뉴를 조회합니다. from/to 형식: YYYYMMDD"
    )
    @GetMapping("/api/schools/{schoolId}/meal")
    public ApiResponse<MealDTO.MealListResponse> getMeals(
            @PathVariable Long schoolId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        MealDTO.MealListResponse response = neisMealService.getMeals(schoolId, from, to);
        return ApiResponse.onSuccess(response);
    }
}
