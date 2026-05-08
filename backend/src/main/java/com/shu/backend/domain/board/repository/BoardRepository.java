package com.shu.backend.domain.board.repository;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findBySchoolId(Long schoolId);

    Optional<Board> findBySchoolIdAndTitle(Long schoolId, String boardTitle);

    List<Board> findByRegionId(Long regionId);

    Optional<Board> findBySchoolAndTitle(School school, String title);

    Optional<Board> findByRegionAndTitle(Region region, String title);

    @Query("""
        select b
        from Board b
        left join fetch b.school s
        left join fetch b.region r
        where s.id = :schoolId
           or (r.id = (select s2.region.id from School s2 where s2.id = :schoolId))
        order by b.scope asc, b.title asc
    """)
    List<Board> findAdminBoardsBySchoolId(@Param("schoolId") Long schoolId);
}
