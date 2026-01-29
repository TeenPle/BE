package com.shu.backend.domain.post.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PostViewCountJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostViewCountJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchIncreaseViewCount(List<ViewDelta> deltas) {
        String sql = "UPDATE post SET view_count = view_count + ? WHERE id = ?";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ViewDelta d = deltas.get(i);
                ps.setLong(1, d.delta());
                ps.setLong(2, d.postId());
            }

            @Override
            public int getBatchSize() {
                return deltas.size();
            }
        });
    }

    public record ViewDelta(Long postId, Long delta) {}
}
