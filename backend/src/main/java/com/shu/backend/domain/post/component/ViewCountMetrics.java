package com.shu.backend.domain.post.component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;



@Component
public class ViewCountMetrics {

    public final Counter flushRuns;
    public final Counter flushedKeys;
    public final Counter flushedTotalDelta;
    public final Timer flushTimer;

    public ViewCountMetrics(MeterRegistry registry) {
        this.flushRuns = Counter.builder("viewcount.flush.runs").register(registry);
        this.flushedKeys = Counter.builder("viewcount.flush.keys").register(registry);
        this.flushedTotalDelta = Counter.builder("viewcount.flush.delta_total").register(registry);
        this.flushTimer = Timer.builder("viewcount.flush.duration").register(registry);
    }
}