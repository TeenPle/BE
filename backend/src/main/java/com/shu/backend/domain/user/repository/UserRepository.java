package com.shu.backend.domain.user.repository;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * User 엔티티에 대한 조회/저장을 담당하는 Repository.
 *
 * 이메일, 닉네임 중복 여부 확인과 이메일 기반 사용자 조회를 처리하며,
 * 필요한 경우 학교/지역 정보까지 함께 조회하는 사용자 전용 데이터 접근 계층이다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndPhoneNumber(String username, String phoneNumber);

    boolean existsByRole(UserRole role);

    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("""
    select u from User u
    join fetch u.school s
    join fetch s.region r
    where u.id = :userId
    """)
    Optional<User> findByIdWithSchoolAndRegion(@Param("userId") Long userId);
}
