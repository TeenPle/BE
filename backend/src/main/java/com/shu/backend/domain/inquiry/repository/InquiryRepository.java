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

    @EntityGraph(attributePaths = {"user", "user.school"})
    @Query("""
            select i from Inquiry i
            join i.user u
            join u.school s
            where i.status = :status
              and (
                :keyword is null
                or :keyword = ''
                or lower(i.title) like lower(concat('%', :keyword, '%'))
                or lower(i.content) like lower(concat('%', :keyword, '%'))
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(u.nickname) like lower(concat('%', :keyword, '%'))
                or lower(s.name) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Inquiry> searchByStatus(
            @Param("status") InquiryStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("select i from Inquiry i join fetch i.user u join fetch u.school where i.id = :id")
    Optional<Inquiry> findDetailById(@Param("id") Long id);

    @Query("select i from Inquiry i join fetch i.user u join fetch u.school where i.id = :id and i.user.id = :userId")
    Optional<Inquiry> findDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
