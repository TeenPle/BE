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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
            key = "'v6_' + #user.school.id + '_' + #user.grade.name() + '_' + #classRoom + '_' + #from + '_' + #to",
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

        LocalDate fromDate = LocalDate.parse(from, DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate toDate = LocalDate.parse(to, DateTimeFormatter.BASIC_ISO_DATE);
        String ay = String.valueOf(fromDate.getYear());
        String sem = fromDate.getMonthValue() >= 3 && fromDate.getMonthValue() <= 8 ? "1" : "2";

        List<Map<String, Object>> rows = fetchWeekByDateAndPeriod(
                school, ay, sem, gradeNum, classRoom, fromDate, toDate);

        List<TimetableDTO.Period> periods = rows.stream()
                .map(row -> TimetableDTO.Period.builder()
                        .date(stringValue(row.get("ALL_TI_YMD")))
                        .dayOfWeek(parseDayOfWeek(stringValue(row.get("ALL_TI_YMD"))))
                        .period(parseIntSafe(row.get("PERIO")))
                        .subject(stringValue(row.get("ITRT_CNTNT")))
                        .build())
                .filter(period -> period.getDayOfWeek() >= 1 && period.getDayOfWeek() <= 5)
                .filter(period -> period.getPeriod() > 0)
                .sorted(Comparator
                        .comparing(TimetableDTO.Period::getDate)
                        .thenComparingInt(TimetableDTO.Period::getPeriod))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                period -> period.getDate() + "_" + period.getPeriod(),
                                period -> period,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));

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

    private int parseIntSafe(Object value) {
        try { return Integer.parseInt(stringValue(value).trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private List<Map<String, Object>> fetchWeekByDateAndPeriod(
            School school,
            String ay,
            String sem,
            String gradeNum,
            String classRoom,
            LocalDate fromDate,
            LocalDate toDate) {

        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        LocalDate date = fromDate;

        while (!date.isAfter(toDate)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek >= 1 && dayOfWeek <= 5) {
                String dateParam = date.format(DateTimeFormatter.BASIC_ISO_DATE);
                rows.addAll(neisApiClient.getTimetableDate(
                        school.getNeisOfficeCode(),
                        school.getNeisSchoolCode(),
                        ay, sem, gradeNum, classRoom, dateParam
                ));
                for (int period = 1; period <= 8; period++) {
                    rows.addAll(neisApiClient.getTimetablePeriod(
                            school.getNeisOfficeCode(),
                            school.getNeisSchoolCode(),
                            ay, sem, gradeNum, classRoom, dateParam, period
                    ));
                }
            }
            date = date.plusDays(1);
        }

        return rows;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
