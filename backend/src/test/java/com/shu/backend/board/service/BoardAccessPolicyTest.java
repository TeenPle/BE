package com.shu.backend.board.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardAccessPolicyTest {

    @Mock
    UserRepository userRepository;

    @Test
    void requireVerifiedActiveUserWithSchoolRejectsPendingUser() {
        BoardAccessPolicy policy = new BoardAccessPolicy(userRepository);
        User user = user(false);

        when(userRepository.findByIdWithSchoolAndRegion(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> policy.requireVerifiedActiveUserWithSchool(1L))
                .isInstanceOf(UserException.class);
    }

    @Test
    void requireVerifiedActiveUserWithSchoolReturnsApprovedUser() {
        BoardAccessPolicy policy = new BoardAccessPolicy(userRepository);
        User user = user(true);

        when(userRepository.findByIdWithSchoolAndRegion(1L)).thenReturn(Optional.of(user));

        assertThat(policy.requireVerifiedActiveUserWithSchool(1L)).isSameAs(user);
    }

    @Test
    void assertCanAccessBoardRequiresApprovedSchoolVerification() {
        BoardAccessPolicy policy = new BoardAccessPolicy(userRepository);
        User user = user(false);
        Board board = Board.builder()
                .title("free")
                .scope(BoardScope.SCHOOL)
                .school(user.getSchool())
                .active(true)
                .build();

        when(userRepository.findByIdWithSchoolAndRegion(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> policy.assertCanAccessBoard(1L, board))
                .isInstanceOf(UserException.class);
    }

    @Test
    void assertCanAccessBoardRejectsRegionBoardForUserAccess() {
        BoardAccessPolicy policy = new BoardAccessPolicy(userRepository);
        User user = user(true);
        Board board = Board.builder()
                .title("region")
                .scope(BoardScope.REGION)
                .region(user.getSchool().getRegion())
                .active(true)
                .build();

        when(userRepository.findByIdWithSchoolAndRegion(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> policy.assertCanAccessBoard(1L, board))
                .isInstanceOf(PostException.class);
    }

    private static User user(boolean verified) {
        Region region = Region.builder().name("Seoul").build();
        ReflectionTestUtils.setField(region, "id", 10L);

        School school = School.builder()
                .name("Teenple High")
                .region(region)
                .build();
        ReflectionTestUtils.setField(school, "id", 20L);

        User user = User.builder()
                .username("student")
                .email("student@example.com")
                .password("password")
                .nickname("student1")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .school(school)
                .verified(verified)
                .gender(Gender.MALE)
                .grade(Grade.FIRST)
                .phoneNumber("01012345678")
                .phoneVerified(true)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
