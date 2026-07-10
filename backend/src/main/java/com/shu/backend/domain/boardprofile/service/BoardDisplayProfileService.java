package com.shu.backend.domain.boardprofile.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.boardprofile.dto.BoardDisplayProfileDTO;
import com.shu.backend.domain.boardprofile.entity.BoardDisplayProfile;
import com.shu.backend.domain.boardprofile.repository.BoardDisplayProfileRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.global.exception.GeneralException;
import com.shu.backend.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardDisplayProfileService {

    private static final int CHANGE_COOLDOWN_DAYS = 90;
    private static final int MAX_GENERATION_ATTEMPTS = 30;
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^(.+)한틴플러#(\\d{4})$");
    private static final List<String> ADJECTIVES = List.of(
            "조용", "엉뚱", "차분", "다정", "솔직", "상냥", "용감", "밝", "성실", "기운찬",
            "빠른", "느긋", "명랑", "친절", "꼼꼼", "새로운", "듬직", "재치", "반짝", "든든"
    );

    private final BoardDisplayProfileRepository profileRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardAccessPolicy boardAccessPolicy;
    private final FileStorageService fileStorageService;
    private final SecureRandom random = new SecureRandom();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BoardDisplayProfile getOrCreate(User user, Board board) {
        Long userId = user.getId();
        Long boardId = board.getId();
        return profileRepository.findByUserIdAndBoardId(userId, boardId)
                .orElseGet(() -> createWithRetry(
                        userRepository.getReferenceById(userId),
                        boardRepository.getReferenceById(boardId)
                ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<Long, BoardDisplayProfile> getOrCreateByUserIds(Long boardId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.BOARD_PROFILE_NOT_FOUND));
        List<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BoardDisplayProfile> profiles = profileRepository.findByBoardIdAndUserIdIn(boardId, distinctUserIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<Long> missingUserIds = distinctUserIds.stream()
                .filter(id -> !profiles.containsKey(id))
                .toList();
        if (!missingUserIds.isEmpty()) {
            Map<Long, User> users = userRepository.findAllById(missingUserIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            for (Long userId : missingUserIds) {
                User user = users.get(userId);
                if (user != null) {
                    profiles.put(userId, createWithRetry(user, board));
                }
            }
        }
        return profiles;
    }

    @Transactional
    public List<BoardDisplayProfileDTO.Response> getMyProfiles(Long userId) {
        User user = boardAccessPolicy.requireVerifiedActiveUserWithSchool(userId);
        List<Board> boards = loadAccessibleBoards(user);
        List<BoardDisplayProfile> profiles = boards.stream()
                .map(board -> getOrCreate(user, board))
                .toList();
        LocalDateTime now = LocalDateTime.now();
        return profiles.stream()
                .map(profile -> BoardDisplayProfileDTO.Response.from(
                        profile,
                        toReadUrl(profile.getProfileImageUrl()),
                        now
                ))
                .toList();
    }

    @Transactional
    public BoardDisplayProfileDTO.Response updateMyProfile(Long userId, Long boardId, String requestedDisplayName, MultipartFile file) {
        User user = boardAccessPolicy.requireVerifiedActiveUserWithSchool(userId);
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus.BOARD_PROFILE_NOT_FOUND));
        boardAccessPolicy.assertCanAccessBoard(user, board);

        BoardDisplayProfile profile = getOrCreate(user, board);
        LocalDateTime now = LocalDateTime.now();
        if (profile.getNextChangeAvailableAt() != null && profile.getNextChangeAvailableAt().isAfter(now)) {
            throw new GeneralException(UserErrorStatus.BOARD_PROFILE_CHANGE_COOLDOWN);
        }

        NameParts nameParts = parseOrGenerateName(boardId, requestedDisplayName, profile.getDisplayName());
        String imageUrl = profile.getProfileImageUrl();
        if (file != null && !file.isEmpty()) {
            imageUrl = fileStorageService.uploadProfileImage(file);
        }

        profile.update(
                nameParts.adjective(),
                nameParts.number(),
                nameParts.displayName(),
                imageUrl,
                now,
                now.plusDays(CHANGE_COOLDOWN_DAYS)
        );
        return BoardDisplayProfileDTO.Response.from(profile, toReadUrl(profile.getProfileImageUrl()), now);
    }

    public String toReadUrl(String storedUrl) {
        return fileStorageService.toPresignedReadUrl(storedUrl);
    }

    private List<Board> loadAccessibleBoards(User user) {
        List<Board> schoolBoards = boardRepository.findBySchoolIdAndScopeAndActiveTrueOrderBySortOrderAscIdAsc(
                user.getSchool().getId(),
                BoardScope.SCHOOL
        );
        if (user.getSchool().getRegion() == null) {
            return schoolBoards;
        }
        List<Board> regionBoards = boardRepository.findByRegionId(user.getSchool().getRegion().getId())
                .stream()
                .filter(Board::isActive)
                .toList();
        List<Board> result = new ArrayList<>(schoolBoards);
        result.addAll(regionBoards);
        return result;
    }

    private BoardDisplayProfile createWithRetry(User user, Board board) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            NameParts name = generateName(board.getId());
            try {
                return profileRepository.saveAndFlush(BoardDisplayProfile.builder()
                        .user(user)
                        .board(board)
                        .adjective(name.adjective())
                        .number(name.number())
                        .displayName(name.displayName())
                        .profileImageUrl(BoardDisplayProfile.DEFAULT_PROFILE_IMAGE_URL)
                        .build());
            } catch (DataIntegrityViolationException e) {
                Optional<BoardDisplayProfile> existing = profileRepository.findByUserIdAndBoardId(user.getId(), board.getId());
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
        }
        throw new GeneralException(UserErrorStatus.BOARD_PROFILE_GENERATION_FAILED);
    }

    private NameParts generateName(Long boardId) {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
            String number = String.format("%04d", random.nextInt(10_000));
            String displayName = adjective + "한틴플러#" + number;
            if (!profileRepository.existsByBoardIdAndDisplayName(boardId, displayName)) {
                return new NameParts(adjective, number, displayName);
            }
        }
        throw new GeneralException(UserErrorStatus.BOARD_PROFILE_GENERATION_FAILED);
    }

    private NameParts parseOrGenerateName(Long boardId, String requestedDisplayName, String currentDisplayName) {
        String displayName = requestedDisplayName == null ? "" : requestedDisplayName.trim();
        if (displayName.isBlank() || displayName.equals(currentDisplayName)) {
            return parseDisplayName(currentDisplayName).orElseGet(() -> generateName(boardId));
        }
        Matcher matcher = DISPLAY_NAME_PATTERN.matcher(displayName);
        if (!matcher.matches()) {
            throw new GeneralException(UserErrorStatus.INVALID_BOARD_PROFILE_DISPLAY_NAME);
        }
        if (profileRepository.existsByBoardIdAndDisplayName(boardId, displayName)) {
            throw new GeneralException(UserErrorStatus.EXIST_BOARD_PROFILE_DISPLAY_NAME);
        }
        return new NameParts(matcher.group(1), matcher.group(2), displayName);
    }

    private Optional<NameParts> parseDisplayName(String displayName) {
        if (displayName == null) return Optional.empty();
        Matcher matcher = DISPLAY_NAME_PATTERN.matcher(displayName);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new NameParts(matcher.group(1), matcher.group(2), displayName));
    }

    private record NameParts(String adjective, String number, String displayName) {
    }
}
