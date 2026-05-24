package com.shu.backend.board.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.enums.BoardType;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.board.service.DefaultSchoolBoardService;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.school.entity.School;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSchoolBoardServiceTest {

    @Mock
    BoardRepository boardRepository;

    @Test
    @SuppressWarnings("unchecked")
    void ensureDefaultBoardsCreatesNineSchoolBoardsInProductOrder() {
        DefaultSchoolBoardService service = new DefaultSchoolBoardService(boardRepository);
        School school = school();

        for (BoardType type : BoardType.values()) {
            when(boardRepository.findBySchoolIdAndType(eq(20L), eq(type))).thenReturn(Optional.empty());
        }

        service.ensureDefaultBoards(school);

        ArgumentCaptor<List<Board>> captor = ArgumentCaptor.forClass(List.class);
        verify(boardRepository).saveAll(captor.capture());

        List<Board> boards = captor.getValue();
        assertThat(boards).hasSize(9);
        assertThat(boards)
                .extracting(Board::getType)
                .containsExactly(BoardType.values());
        assertThat(boards)
                .extracting(Board::getTitle)
                .endsWith("졸업생 게시판");
        assertThat(boards)
                .extracting(Board::getSortOrder)
                .containsExactly(10, 20, 30, 40, 50, 60, 70, 80, 90);
        assertThat(boards)
                .allSatisfy(board -> {
                    assertThat(board.getScope()).isEqualTo(BoardScope.SCHOOL);
                    assertThat(board.getSchool()).isSameAs(school);
                    assertThat(board.getRegion()).isNull();
                    assertThat(board.isActive()).isTrue();
                    assertThat(board.isDefaultBoard()).isTrue();
                });
    }

    @Test
    void ensureDefaultBoardsRefreshesExistingBoardSortOrder() {
        DefaultSchoolBoardService service = new DefaultSchoolBoardService(boardRepository);
        School school = school();
        Board graduateBoard = Board.builder()
                .title("졸업생 게시판")
                .description("old")
                .active(true)
                .school(school)
                .scope(BoardScope.SCHOOL)
                .type(BoardType.GRADUATE)
                .defaultBoard(true)
                .sortOrder(60)
                .build();

        for (BoardType type : BoardType.values()) {
            when(boardRepository.findBySchoolIdAndType(eq(20L), eq(type)))
                    .thenReturn(type == BoardType.GRADUATE
                            ? Optional.of(graduateBoard)
                            : Optional.empty());
            if (type != BoardType.GRADUATE) {
                when(boardRepository.findBySchoolIdAndTitle(eq(20L), eq(type.getTitle())))
                        .thenReturn(Optional.empty());
            }
        }

        service.ensureDefaultBoards(school);

        assertThat(graduateBoard.getSortOrder()).isEqualTo(90);
        assertThat(graduateBoard.getDescription()).isEqualTo("졸업생과 이야기해요");
        verify(boardRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private static School school() {
        Region region = Region.builder().name("Seoul").build();
        ReflectionTestUtils.setField(region, "id", 10L);

        School school = School.builder()
                .name("Teenple High")
                .region(region)
                .build();
        ReflectionTestUtils.setField(school, "id", 20L);
        return school;
    }
}
