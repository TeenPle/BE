package com.shu.backend.domain.verification.repository;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSchoolVerificationRequestRepository extends JpaRepository<UserSchoolVerificationRequest, Long> {
    boolean existsByUser(User user);

    boolean existsByUserAndStatus(User user, VerificationStatus status);

    Optional<UserSchoolVerificationRequest> findTopByUserOrderByRequestedAtDesc(User user);

    @EntityGraph(attributePaths = {"user", "school"})
    List<UserSchoolVerificationRequest> findByStatusOrderByRequestedAtDesc(VerificationStatus status);

    @EntityGraph(attributePaths = {"user", "school"})
    Page<UserSchoolVerificationRequest> findByStatus(VerificationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "school"})
    @Query("""
            select r from UserSchoolVerificationRequest r
            join r.user u
            join r.school s
            where r.status = :status
              and (
                :keyword is null
                or :keyword = ''
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(s.name) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<UserSchoolVerificationRequest> searchByStatus(
            @Param("status") VerificationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "school"})
    Optional<UserSchoolVerificationRequest> findWithUserAndSchoolById(Long id);

    List<UserSchoolVerificationRequest> findByUser(User user);

    void deleteByUser(User user);
}
