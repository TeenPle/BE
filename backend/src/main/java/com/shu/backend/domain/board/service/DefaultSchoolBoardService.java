package com.shu.backend.domain.board.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.enums.BoardType;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.school.entity.School;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultSchoolBoardService {

    private final BoardRepository boardRepository;

    @Transactional
    public void ensureDefaultBoards(School school) {
        if (school == null || school.getId() == null) {
            return;
        }

        List<Board> missingBoards = new ArrayList<>();
        for (BoardType type : BoardType.values()) {
            Board boardByType = boardRepository.findBySchoolIdAndType(school.getId(), type)
                    .orElse(null);
            if (boardByType != null) {
                boardByType.markAsDefault(type);
                continue;
            }

            Board existingBoard = boardRepository.findBySchoolIdAndTitle(school.getId(), type.getTitle())
                    .orElse(null);
            if (existingBoard != null) {
                existingBoard.markAsDefault(type);
                continue;
            }

            missingBoards.add(
                    Board.builder()
                            .title(type.getTitle())
                            .description(type.getDescription())
                            .active(true)
                            .school(school)
                            .region(null)
                            .scope(BoardScope.SCHOOL)
                            .type(type)
                            .defaultBoard(true)
                            .sortOrder(type.getSortOrder())
                            .build()
            );
        }

        if (!missingBoards.isEmpty()) {
            boardRepository.saveAll(missingBoards);
        }
    }
}
