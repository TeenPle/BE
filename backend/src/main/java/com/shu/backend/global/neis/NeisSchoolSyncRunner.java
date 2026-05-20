package com.shu.backend.global.neis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NeisSchoolSyncRunner implements ApplicationRunner {

    private final NeisSchoolSyncProperties properties;
    private final NeisSchoolSyncService neisSchoolSyncService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.isRunOnStartup()) {
            log.info("[NEIS School Import] startup sync skipped: enabled={}, runOnStartup={}",
                    properties.isEnabled(), properties.isRunOnStartup());
            return;
        }

        NeisSyncResult result = neisSchoolSyncService.syncAllHighSchools(false, properties.isCreateBoards());
        log.info("[NEIS School Import] startup sync result: {}", result);
    }
}
