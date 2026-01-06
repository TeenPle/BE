package com.shu.backend.global.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbCheckRunner implements CommandLineRunner {
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (var c = dataSource.getConnection();
             var ps = c.prepareStatement("select database()");
             var rs = ps.executeQuery()) {
            rs.next();
            log.info("Connected DB(schema) = {}", rs.getString(1));
        }
    }
}
