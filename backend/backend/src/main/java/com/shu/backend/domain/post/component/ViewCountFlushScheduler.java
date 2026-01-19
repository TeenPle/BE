package com.shu.backend.domain.post.component;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import com.shu.backend.domain.post.repository.PostViewCountJdbcRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ViewCountFlushScheduler {

    private static final long FLUSH_INTERVAL_MS = 60_000; // 10초 예시

    private final ViewCountAccumulator accumulator;
    private final PostViewCountJdbcRepository jdbcRepository;
    private final ViewCountMetrics metrics;

    public ViewCountFlushScheduler(ViewCountAccumulator accumulator,
                                   PostViewCountJdbcRepository jdbcRepository,
                                   ViewCountMetrics metrics) {
        this.accumulator = accumulator;
        this.jdbcRepository = jdbcRepository;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    public void flush() {
        metrics.flushTimer.record(() -> {
            metrics.flushRuns.increment();

            Map<Long, LongAdder> map = accumulator.snapshot();
            if (map.isEmpty()) return;

            long keys = 0;
            long total = 0;

            List<PostViewCountJdbcRepository.ViewDelta> deltas = new ArrayList<>();
            for (var e : map.entrySet()) {
                long delta = e.getValue().sumThenReset();
                if (delta > 0) {
                    deltas.add(new PostViewCountJdbcRepository.ViewDelta(e.getKey(), delta));
                    keys++;
                    total += delta;
                }
            }
            if (deltas.isEmpty()) return;

            metrics.flushedKeys.increment(keys);
            metrics.flushedTotalDelta.increment(total);

            // 실제 DB 반영 (batch update)
            jdbcRepository.batchIncreaseViewCount(deltas);
        });
    }
}

