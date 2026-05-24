package com.shu.backend.domain.board.service;

import com.shu.backend.domain.board.dto.BoardCreateRequest;
import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.exception.RegionException;
import com.shu.backend.domain.region.exception.status.RegionErrorStatus;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final SchoolRepository schoolRepository;
    private final RegionRepository regionRepository;
    private final BoardAccessPolicy boardAccessPolicy;
    private final DefaultSchoolBoardService defaultSchoolBoardService;

    @Transactional
    public Long createBoard(BoardCreateRequest req) {
        String title = req.getTitle();
        if (title == null || title.isBlank()) {
            throw new BoardException(BoardErrorStatus.INVALID_BOARD_TITLE);
        }

        BoardScope scope = req.getScope() != null ? req.getScope() : BoardScope.SCHOOL;
        Board.BoardBuilder builder = Board.builder()
                .scope(scope)
                .title(title.trim())
                .description(req.getDescription());

        if (scope == BoardScope.SCHOOL) {
            if (req.getSchoolId() == null) {
                throw new BoardException(BoardErrorStatus.SCHOOL_ID_REQUIRED);
            }

            School school = schoolRepository.findById(req.getSchoolId())
                    .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

            builder.school(school).region(null);
        } else {
            if (req.getRegionId() == null) {
                throw new BoardException(BoardErrorStatus.REGION_ID_REQUIRED);
            }

            Region region = regionRepository.findById(req.getRegionId())
                    .orElseThrow(() -> new RegionException(RegionErrorStatus.REGION_NOT_FOUND));

            builder.region(region).school(null);
        }

        Board board = builder.build();
        boardRepository.save(board);

        return board.getId();
    }

    public List<BoardResponse> getBoardsBySchool(Long schoolId, Long currentUserId) {
        boardAccessPolicy.assertSchoolMember(currentUserId, schoolId);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        defaultSchoolBoardService.ensureDefaultBoards(school);

        return boardRepository
                .findBySchoolIdAndScopeAndActiveTrueAndDefaultBoardTrueOrderBySortOrderAscIdAsc(schoolId, BoardScope.SCHOOL)
                .stream()
                .map(BoardResponse::toDto)
                .toList();
    }
}
