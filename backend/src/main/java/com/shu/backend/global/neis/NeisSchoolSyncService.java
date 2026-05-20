package com.shu.backend.global.neis;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NeisSchoolSyncService {

    private static final String HIGH_SCHOOL = "\uACE0\uB4F1\uD559\uAD50";
    private static final String DEFAULT_REGION = "\uC9C0\uC5ED \uBBF8\uBD84\uB958";
    private static final String FREE_BOARD_TITLE = "\uC790\uC720\uAC8C\uC2DC\uD310";
    private static final String FIRST_GRADE_BOARD_TITLE = "1\uD559\uB144 \uAC8C\uC2DC\uD310";
    private static final String SECOND_GRADE_BOARD_TITLE = "2\uD559\uB144 \uAC8C\uC2DC\uD310";
    private static final String THIRD_GRADE_BOARD_TITLE = "3\uD559\uB144 \uAC8C\uC2DC\uD310";
    private static final String ALUMNI_BOARD_TITLE = "\uC878\uC5C5\uC0DD \uAC8C\uC2DC\uD310";
    private static final String REGION_BOARD_TITLE = "\uC9C0\uC5ED \uAC8C\uC2DC\uD310";

    private final NeisApiClient neisApiClient;
    private final SchoolRepository schoolRepository;
    private final RegionRepository regionRepository;
    private final BoardRepository boardRepository;
    private final NeisSchoolSyncProperties properties;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public School syncIfMissing(School school) {
        if (school.getNeisOfficeCode() != null && school.getNeisSchoolCode() != null) {
            return school;
        }

        List<Map<String, Object>> results = neisApiClient.getSchoolInfo(school.getName());

        if (results.size() == 1) {
            String officeCode = value(results.get(0), "ATPT_OFCDC_SC_CODE");
            String schoolCode = value(results.get(0), "SD_SCHUL_CODE");

            if (officeCode != null && schoolCode != null) {
                school.updateNeisCodes(officeCode, schoolCode);
                School saved = schoolRepository.save(school);
                log.info("[NEIS] school code synced: school={}, officeCode={}, schoolCode={}",
                        school.getName(), officeCode, schoolCode);
                return saved;
            }
        } else if (results.isEmpty()) {
            log.warn("[NEIS] school not found: school={}", school.getName());
        } else {
            log.warn("[NEIS] ambiguous school result: count={}, school={}", results.size(), school.getName());
        }

        return school;
    }

    @Transactional
    public NeisSyncResult syncAllMissing() {
        List<School> targets = schoolRepository.findAllWithoutNeisCodes();
        int synced = 0;
        int ambiguous = 0;
        int notFound = 0;

        for (School school : targets) {
            List<Map<String, Object>> results = neisApiClient.getSchoolInfo(school.getName());
            if (results.size() == 1) {
                String officeCode = value(results.get(0), "ATPT_OFCDC_SC_CODE");
                String schoolCode = value(results.get(0), "SD_SCHUL_CODE");
                if (officeCode != null && schoolCode != null) {
                    school.updateNeisCodes(officeCode, schoolCode);
                    schoolRepository.save(school);
                    synced++;
                } else {
                    notFound++;
                }
            } else if (results.isEmpty()) {
                notFound++;
            } else {
                ambiguous++;
            }
        }

        log.info("[NEIS Bulk] missing-code sync completed: total={}, synced={}, ambiguous={}, notFound={}",
                targets.size(), synced, ambiguous, notFound);
        return NeisSyncResult.legacy(targets.size(), synced, ambiguous, notFound);
    }

    public NeisSyncResult syncAllHighSchools(boolean dryRun, Boolean createBoardsOverride) {
        boolean createBoards = createBoardsOverride != null ? createBoardsOverride : properties.isCreateBoards();
        List<Map<String, Object>> rows = fetchAllHighSchools();
        return transactionTemplate.execute(status -> upsertHighSchools(rows, dryRun, createBoards));
    }

    private List<Map<String, Object>> fetchAllHighSchools() {
        java.util.ArrayList<Map<String, Object>> rows = new java.util.ArrayList<>();
        int pageSize = Math.max(1, properties.getPageSize());
        int maxPages = Math.max(1, properties.getMaxPages());

        for (int page = 1; page <= maxPages; page++) {
            List<Map<String, Object>> pageRows = neisApiClient.getHighSchoolInfoPage(page, pageSize);
            if (pageRows.isEmpty()) {
                break;
            }
            rows.addAll(pageRows);

            if (pageRows.size() < pageSize) {
                break;
            }
            sleepBetweenRequests();
        }

        log.info("[NEIS School Import] fetched high schools: {}", rows.size());
        return rows;
    }

    private NeisSyncResult upsertHighSchools(List<Map<String, Object>> rows, boolean dryRun, boolean createBoards) {
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        int skipped = 0;

        for (Map<String, Object> row : rows) {
            if (!HIGH_SCHOOL.equals(value(row, "SCHUL_KND_SC_NM"))) {
                skipped++;
                continue;
            }

            String name = value(row, "SCHUL_NM");
            String officeCode = value(row, "ATPT_OFCDC_SC_CODE");
            String schoolCode = value(row, "SD_SCHUL_CODE");
            if (isBlank(name) || isBlank(officeCode) || isBlank(schoolCode)) {
                skipped++;
                continue;
            }

            Region region = findOrCreateRegion(resolveRegionName(row), dryRun);
            School existing = schoolRepository.findByNeisOfficeCodeAndNeisSchoolCode(officeCode, schoolCode)
                    .orElseGet(() -> schoolRepository.findFirstByNameAndNeisOfficeCodeIsNullAndNeisSchoolCodeIsNull(name).orElse(null));

            if (existing == null) {
                created++;
                if (!dryRun) {
                    School school = schoolRepository.save(School.builder()
                            .name(name)
                            .region(region)
                            .neisOfficeCode(officeCode)
                            .neisSchoolCode(schoolCode)
                            .build());
                    if (createBoards) {
                        createDefaultBoards(school);
                    }
                }
                continue;
            }

            boolean changed = hasChanged(existing, name, region, officeCode, schoolCode);
            if (changed) {
                updated++;
                if (!dryRun) {
                    existing.updateFromNeis(name, region, officeCode, schoolCode);
                    if (createBoards) {
                        createDefaultBoards(existing);
                    }
                }
            } else {
                unchanged++;
                if (!dryRun && createBoards) {
                    createDefaultBoards(existing);
                }
            }
        }

        log.info("[NEIS School Import] completed: total={}, created={}, updated={}, unchanged={}, skipped={}, dryRun={}",
                rows.size(), created, updated, unchanged, skipped, dryRun);
        return new NeisSyncResult(rows.size(), created + updated, created, updated, unchanged, skipped, 0, 0, dryRun);
    }

    private Region findOrCreateRegion(String regionName, boolean dryRun) {
        return regionRepository.findByName(regionName)
                .orElseGet(() -> dryRun ? Region.builder().name(regionName).build() : regionRepository.save(Region.builder().name(regionName).build()));
    }

    private void createDefaultBoards(School school) {
        createSchoolBoardIfNotExists(school, FREE_BOARD_TITLE, "\uC790\uC720\uB86D\uAC8C \uC774\uC57C\uAE30\uD574\uC694");
        createSchoolBoardIfNotExists(school, FIRST_GRADE_BOARD_TITLE, "1\uD559\uB144\uC744 \uC704\uD55C \uAC8C\uC2DC\uD310");
        createSchoolBoardIfNotExists(school, SECOND_GRADE_BOARD_TITLE, "2\uD559\uB144\uC744 \uC704\uD55C \uAC8C\uC2DC\uD310");
        createSchoolBoardIfNotExists(school, THIRD_GRADE_BOARD_TITLE, "3\uD559\uB144\uC744 \uC704\uD55C \uAC8C\uC2DC\uD310");
        createSchoolBoardIfNotExists(school, ALUMNI_BOARD_TITLE, "\uC878\uC5C5\uC0DD\uC744 \uC704\uD55C \uAC8C\uC2DC\uD310");

        if (school.getRegion() != null) {
            createRegionBoardIfNotExists(school.getRegion());
        }
    }

    private void createSchoolBoardIfNotExists(School school, String title, String description) {
        boardRepository.findBySchoolAndTitle(school, title)
                .orElseGet(() -> boardRepository.save(Board.builder()
                        .title(title)
                        .description(description)
                        .school(school)
                        .scope(BoardScope.SCHOOL)
                        .build()));
    }

    private void createRegionBoardIfNotExists(Region region) {
        boardRepository.findByRegionAndTitle(region, REGION_BOARD_TITLE)
                .orElseGet(() -> boardRepository.save(Board.builder()
                        .title(REGION_BOARD_TITLE)
                        .description("\uAC19\uC740 \uC9C0\uC5ED \uD559\uC0DD\uB4E4\uACFC \uC774\uC57C\uAE30\uD574\uC694")
                        .region(region)
                        .scope(BoardScope.REGION)
                        .build()));
    }

    private boolean hasChanged(School school, String name, Region region, String officeCode, String schoolCode) {
        Long currentRegionId = school.getRegion() != null ? school.getRegion().getId() : null;
        Long nextRegionId = region != null ? region.getId() : null;
        return !Objects.equals(school.getName(), name)
                || !Objects.equals(currentRegionId, nextRegionId)
                || !Objects.equals(school.getNeisOfficeCode(), officeCode)
                || !Objects.equals(school.getNeisSchoolCode(), schoolCode);
    }

    private String resolveRegionName(Map<String, Object> row) {
        String location = value(row, "LCTN_SC_NM");
        if (!isBlank(location)) {
            return location;
        }

        String office = value(row, "ATPT_OFCDC_SC_NM");
        if (!isBlank(office)) {
            return office;
        }

        return DEFAULT_REGION;
    }

    private void sleepBetweenRequests() {
        long millis = Math.max(0, properties.getRequestIntervalMillis());
        if (millis == 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting between NEIS requests", e);
        }
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
