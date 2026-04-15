package com.shu.backend.domain.board.repository;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findBySchoolId(Long schoolId);

    Optional<Board> findBySchoolIdAndTitle(Long schoolId, String boardTitle);

    List<Board> findByRegionId(Long regionId);

    Optional<Board> findBySchoolAndTitle(School school, String title);

    Optional<Board> findByRegionAndTitle(Region region, String title);
}
