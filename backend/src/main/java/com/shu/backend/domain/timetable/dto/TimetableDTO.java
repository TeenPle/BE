package com.shu.backend.domain.timetable.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class TimetableDTO {

    @Getter
    @Builder
    public static class Period {
        private String date;       // "20260425"
        private int dayOfWeek;     // 1=월 ~ 5=금
        private int period;        // 교시 (1~7)
        private String subject;    // 과목명
    }

    @Getter
    @Builder
    public static class WeekResponse {
        private String grade;      // "1", "2", "3"
        private String classRoom;  // "3"
        private List<Period> periods;
        private boolean neisAvailable; // false = 학교 NEIS 코드 미등록
    }
}
