package com.shu.backend.domain.pushtoken.repository;

import com.shu.backend.domain.pushtoken.entity.PushToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findByUserIdAndIsActiveTrue(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PushToken t set t.isActive = false where t.userId = :userId")
    int deactivateAllByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PushToken t set t.isActive = false where t.token = :token")
    int deactivateByToken(@Param("token") String token);

    /**
     * 토큰이 없으면 INSERT, 있으면 userId/platform/is_active/updated_at을 갱신.
     * DB 레벨 upsert이므로 동시 요청으로 인한 race condition이 발생하지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO push_token (user_id, token, platform, is_active, created_at, updated_at)
            VALUES (:userId, :token, :platform, true, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                user_id    = VALUES(user_id),
                platform   = VALUES(platform),
                is_active  = true,
                updated_at = NOW()
            """, nativeQuery = true)
    int upsert(@Param("userId") Long userId,
               @Param("token") String token,
               @Param("platform") String platform);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PushToken t where t.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
