package com.shu.backend.global.neis;

import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 학교 엔티티에 NEIS 코드가 없을 때 schoolInfo API로 자동 조회·저장하는 서비스.
 * NeisMealService, NeisTimetableService에서 호출된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NeisSchoolSyncService {

    private final NeisApiClient neisApiClient;
    private final SchoolRepository schoolRepository;

    /**
     * NEIS 코드가 없는 학교를 학교명으로 자동 검색해 코드를 저장한다.
     * 검색 결과가 정확히 1건일 때만 저장하고, 0건이거나 2건 이상이면 원본을 반환한다.
     *
     * @return 코드가 채워진 School (갱신 성공 시) 또는 원본 School
     */
    @Transactional
    public School syncIfMissing(School school) {
        if (school.getNeisOfficeCode() != null && school.getNeisSchoolCode() != null) {
            return school;
        }

        List<Map<String, Object>> results = neisApiClient.getSchoolInfo(school.getName());

        if (results.size() == 1) {
            String officeCode = (String) results.get(0).get("ATPT_OFCDC_SC_CODE");
            String schoolCode  = (String) results.get(0).get("SD_SCHUL_CODE");

            if (officeCode != null && schoolCode != null) {
                school.updateNeisCodes(officeCode, schoolCode);
                School saved = schoolRepository.save(school);
                log.info("[NEIS] 학교 코드 자동 등록 완료: school={}, officeCode={}, schoolCode={}",
                        school.getName(), officeCode, schoolCode);
                return saved;
            }
        } else if (results.isEmpty()) {
            log.warn("[NEIS] 학교 검색 결과 없음: school={}", school.getName());
        } else {
            log.warn("[NEIS] 학교 검색 결과 다수 ({}건) — 수동 등록 필요: school={}", results.size(), school.getName());
        }

        return school;
    }

    /**
     * NEIS 코드가 없는 모든 학교를 일괄 자동 동기화한다. (어드민 전용)
     */
    @Transactional
    public NeisSyncResult syncAllMissing() {
        List<School> targets = schoolRepository.findAllWithoutNeisCodes();
        int synced = 0, ambiguous = 0, notFound = 0;

        for (School school : targets) {
            List<Map<String, Object>> results = neisApiClient.getSchoolInfo(school.getName());
            if (results.size() == 1) {
                String officeCode = (String) results.get(0).get("ATPT_OFCDC_SC_CODE");
                String schoolCode  = (String) results.get(0).get("SD_SCHUL_CODE");
                if (officeCode != null && schoolCode != null) {
                    school.updateNeisCodes(officeCode, schoolCode);
                    schoolRepository.save(school);
                    log.info("[NEIS Bulk] 등록 완료: {} → {}/{}", school.getName(), officeCode, schoolCode);
                    synced++;
                } else {
                    notFound++;
                }
            } else if (results.isEmpty()) {
                log.warn("[NEIS Bulk] 검색 결과 없음: {}", school.getName());
                notFound++;
            } else {
                log.warn("[NEIS Bulk] 검색 결과 다수 ({}건): {}", results.size(), school.getName());
                ambiguous++;
            }
        }

        log.info("[NEIS Bulk] 완료 — 대상: {}, 성공: {}, 모호: {}, 미발견: {}",
                targets.size(), synced, ambiguous, notFound);
        return new NeisSyncResult(targets.size(), synced, ambiguous, notFound);
    }
}
