package com.shu.backend.domain.school.repository;

import com.shu.backend.domain.school.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByName(@Param("name") String name);

    Optional<School> findFirstByNameOrderByIdAsc(String name);

    Optional<School> findByNeisOfficeCodeAndNeisSchoolCode(String neisOfficeCode, String neisSchoolCode);

    Optional<School> findFirstByNameAndNeisOfficeCodeIsNullAndNeisSchoolCodeIsNull(String name);

    public List<School> findByRegionId(Long regionId);

    List<School> findByRegionIdOrderByNameAsc(Long regionId);

    // 지역 ID와 학교명(keyword)을 기준으로 학교 검색
    @Query("SELECT s FROM School s WHERE s.region.id = :regionId " +
            "AND (:keyword IS NULL OR :keyword = '' OR s.name LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.name ASC")
    List<School> findSchoolsByRegionAndName(@Param("regionId") Long regionId, @Param("keyword") String keyword);

    @Query("""
        select s
        from School s
        left join s.region r
        where (:keyword is null or :keyword = '' or s.name like concat('%', :keyword, '%'))
        order by s.name asc
    """)
    Page<School> searchAdminSchools(@Param("keyword") String keyword, Pageable pageable);

    // 학교명(keyword)으로 전체 학교 검색
    @Query("SELECT s FROM School s " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR s.name LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.name ASC")
    List<School> findSchoolsByName(@Param("keyword") String keyword);


    boolean existsByRegionIdAndName(Long id, String name);

    // NEIS 코드가 미등록된 학교 전체 조회
    @Query("SELECT s FROM School s WHERE s.neisOfficeCode IS NULL OR s.neisSchoolCode IS NULL")
    List<School> findAllWithoutNeisCodes();
}
