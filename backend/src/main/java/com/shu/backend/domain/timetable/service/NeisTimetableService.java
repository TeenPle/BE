package com.shu.backend.domain.timetable.service;

import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.timetable.dto.TimetableDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.global.neis.NeisApiClient;
import com.shu.backend.global.neis.NeisSchoolSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NeisTimetableService {

    private final NeisApiClient neisApiClient;
    private final NeisSchoolSyncService neisSchoolSyncService;

    @Cacheable(
            value = "timetable",
            key = "#user.school.id + '_' + #user.grade.name() + '_' + #classRoom + '_' + #from + '_' + #to",
            unless = "#result.periods.isEmpty()"
    )
    public TimetableDTO.WeekResponse getTimetable(
            User user, String classRoom, String from, String to) {

        String gradeNum = toNeisGrade(user.getGrade());
        if (gradeNum == null) {
            return TimetableDTO.WeekResponse.builder()
                    .grade("0").classRoom(classRoom).periods(List.of()).neisAvailable(false).build();
        }

        // NEIS 코드 없으면 학교명으로 자동 조회·저장 시도
        School school = user.getSchool();
        if (school.getNeisOfficeCode() == null || school.getNeisSchoolCode() == null) {
            school = neisSchoolSyncService.syncIfMissing(school);
        }

        if (school.getNeisOfficeCode() == null || school.getNeisSchoolCode() == null) {
            return TimetableDTO.WeekResponse.builder()
                    .grade(gradeNum).classRoom(classRoom).periods(List.of()).neisAvailable(false).build();
        }

        LocalDate date = LocalDate.parse(from, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String ay = String.valueOf(date.getYear());
        String sem = date.getMonthValue() >= 3 && date.getMonthValue() <= 8 ? "1" : "2";

        List<Map<String, Object>> rows = neisApiClient.getTimetable(
                school.getNeisOfficeCode(),
                school.getNeisSchoolCode(),
                ay, sem, gradeNum, classRoom, from, to
        );

        List<TimetableDTO.Period> periods = rows.stream()
                .map(row -> TimetableDTO.Period.builder()
                        .date((String) row.getOrDefault("ALL_TI_YMD", ""))
                        .dayOfWeek(parseDayOfWeek((String) row.getOrDefault("ALL_TI_YMD", "")))
                        .period(parseIntSafe((String) row.getOrDefault("PERIO", "0")))
                        .subject((String) row.getOrDefault("ITRT_CNTNT", ""))
                        .build())
                .collect(Collectors.toList());

        return TimetableDTO.WeekResponse.builder()
                .grade(gradeNum)
                .classRoom(classRoom)
                .periods(periods)
                .neisAvailable(true)
                .build();
    }

    private String toNeisGrade(Grade grade) {
        return switch (grade) {
            case FIRST -> "1";
            case SECOND -> "2";
            case THIRD -> "3";
            default -> null; // GRADUATED
        };
    }

    private int parseDayOfWeek(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return 0;
        LocalDate d = LocalDate.parse(yyyymmdd, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return d.getDayOfWeek().getValue(); // 1=월 ~ 7=일
    }

    private int parseIntSafe(String value) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
