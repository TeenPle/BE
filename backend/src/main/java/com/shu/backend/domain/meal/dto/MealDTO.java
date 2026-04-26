package com.shu.backend.domain.meal.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class MealDTO {

    @Getter
    @Builder
    public static class MealItem {
        private String date;           // "20260425"
        private List<String> dishes;   // 파싱된 메뉴 목록
        private String calories;       // "850 Kcal"
    }

    @Getter
    @Builder
    public static class MealListResponse {
        private List<MealItem> meals;
        private boolean neisAvailable; // false = 학교 NEIS 코드 미등록
    }
}
