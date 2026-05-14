package com.shu.backend.domain.inquiry.repository;

import com.shu.backend.domain.inquiry.entity.Inquiry;
import com.shu.backend.domain.inquiry.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = {"user", "user.school"})
    Page<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.school"})
    Page<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status, Pageable pageable);

    @Query("select i from Inquiry i join fetch i.user u join fetch u.school where i.id = :id")
    Optional<Inquiry> findDetailById(@Param("id") Long id);

    @Query("select i from Inquiry i join fetch i.user u join fetch u.school where i.id = :id and i.user.id = :userId")
    Optional<Inquiry> findDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
