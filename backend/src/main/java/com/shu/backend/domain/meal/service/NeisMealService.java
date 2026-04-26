package com.shu.backend.domain.meal.service;

import com.shu.backend.domain.meal.dto.MealDTO;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.global.neis.NeisApiClient;
import com.shu.backend.global.neis.NeisSchoolSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NeisMealService {

    private final NeisApiClient neisApiClient;
    private final SchoolRepository schoolRepository;
    private final NeisSchoolSyncService neisSchoolSyncService;

    @Cacheable(value = "meal", key = "#schoolId + '_' + #from + '_' + #to", unless = "#result.meals.isEmpty()")
    public MealDTO.MealListResponse getMeals(Long schoolId, String from, String to) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        // NEIS 코드 없으면 학교명으로 자동 조회·저장 시도
        if (school.getNeisOfficeCode() == null || school.getNeisSchoolCode() == null) {
            school = neisSchoolSyncService.syncIfMissing(school);
        }

        if (school.getNeisOfficeCode() == null || school.getNeisSchoolCode() == null) {
            return MealDTO.MealListResponse.builder().meals(List.of()).neisAvailable(false).build();
        }

        List<Map<String, Object>> rows = neisApiClient.getMealInfo(
                school.getNeisOfficeCode(),
                school.getNeisSchoolCode(),
                from, to
        );

        List<MealDTO.MealItem> meals = rows.stream()
                .map(this::parseRow)
                .collect(Collectors.toList());

        return MealDTO.MealListResponse.builder().meals(meals).neisAvailable(true).build();
    }

    private MealDTO.MealItem parseRow(Map<String, Object> row) {
        String date = (String) row.getOrDefault("MLSV_YMD", "");
        String rawDishes = (String) row.getOrDefault("DDISH_NM", "");
        String calories = (String) row.getOrDefault("CAL_INFO", "");

        List<String> dishes = Arrays.stream(rawDishes.split("<br/>|\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceAll("\\([\\d\\.]+\\)", "").trim())
                .collect(Collectors.toList());

        return MealDTO.MealItem.builder()
                .date(date)
                .dishes(dishes)
                .calories(calories)
                .build();
    }
}
