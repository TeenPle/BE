package com.shu.backend.domain.school.service;

import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.enums.BoardType;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.board.service.DefaultSchoolBoardService;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.post.service.PostService;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.exception.RegionException;
import com.shu.backend.domain.region.exception.status.RegionErrorStatus;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.dto.SchoolCreateRequest;
import com.shu.backend.domain.school.dto.SchoolDetailResponse;
import com.shu.backend.domain.school.dto.SchoolNeisUpdateRequest;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchoolService {

    private final RegionRepository regionRepository;
    private final SchoolRepository schoolRepository;
    private final BoardRepository boardRepository;
    private final PostService postService;
    @SuppressWarnings("unused")
    private final CommentRepository commentRepository;
    @SuppressWarnings("unused")
    private final PostRepository postRepository;
    private final BoardAccessPolicy boardAccessPolicy;
    private final DefaultSchoolBoardService defaultSchoolBoardService;

    @Transactional
    public Long createSchool(SchoolCreateRequest schoolCreateRequest) {
        Region region = regionRepository.findById(schoolCreateRequest.getRegionId())
                .orElseThrow(() -> new RegionException(RegionErrorStatus.REGION_NOT_FOUND));

        String name = schoolCreateRequest.getName();
        if (name == null || name.isEmpty()) {
            throw new SchoolException(SchoolErrorStatus.INVALID_SCHOOL_NAME);
        }

        if (schoolRepository.existsByRegionIdAndName(region.getId(), name.trim())) {
            throw new SchoolException(SchoolErrorStatus.SCHOOL_ALREADY_EXISTS);
        }

        School school = School.builder()
                .name(name.trim())
                .region(region)
                .neisOfficeCode(schoolCreateRequest.getNeisOfficeCode())
                .neisSchoolCode(schoolCreateRequest.getNeisSchoolCode())
                .build();

        schoolRepository.save(school);
        defaultSchoolBoardService.ensureDefaultBoards(school);

        return school.getId();
    }

    public List<School> getSchoolsByRegion(Long regionId, String keyword) {
        regionRepository.findById(regionId)
                .orElseThrow(() -> new RegionException(RegionErrorStatus.REGION_NOT_FOUND));

        if (keyword == null || keyword.isBlank()) {
            return schoolRepository.findByRegionIdOrderByNameAsc(regionId);
        }
        return schoolRepository.findSchoolsByRegionAndName(regionId, keyword);
    }

    @Transactional
    public void updateNeisCodes(Long schoolId, SchoolNeisUpdateRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
        school.updateNeisCodes(request.getNeisOfficeCode(), request.getNeisSchoolCode());
    }

    public List<School> searchSchools(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new SchoolException(SchoolErrorStatus.INVALID_SCHOOL_NAME);
        }

        return schoolRepository.findSchoolsByName(keyword.trim());
    }

    public Slice<PostResponse> getAllPostsBySchool(Long schoolId, Pageable pageable, Long currentUserId) {
        boardAccessPolicy.assertSchoolMember(currentUserId, schoolId);

        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        return postService.getPostsBySchool(schoolId, pageable, currentUserId);
    }

    public SchoolDetailResponse getSchoolDetail(Long schoolId, String boardTitle, Pageable pageable, Long currentUserId) {
        boardAccessPolicy.assertSchoolMember(currentUserId, schoolId);

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        defaultSchoolBoardService.ensureDefaultBoards(school);

        Board freeBoard = boardRepository.findBySchoolIdAndType(schoolId, BoardType.FREE)
                .orElseGet(() -> boardRepository.findBySchoolIdAndTitle(schoolId, "자유게시판")
                        .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND)));

        List<BoardResponse> boardResponses = boardRepository
                .findBySchoolIdAndScopeAndActiveTrueAndDefaultBoardTrueOrderBySortOrderAscIdAsc(schoolId, BoardScope.SCHOOL)
                .stream()
                .map(BoardResponse::toDto)
                .toList();

        Slice<PostResponse> postSlice = postService.getPostsByBoardId(freeBoard.getId(), pageable, currentUserId);

        return new SchoolDetailResponse(
                school.getId(),
                school.getName(),
                boardResponses,
                postSlice.getContent(),
                postSlice.hasNext()
        );
    }
}
