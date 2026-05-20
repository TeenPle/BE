package com.shu.backend.global.neis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "neis.school-sync")
public class NeisSchoolSyncProperties {

    private boolean enabled = false;
    private boolean runOnStartup = false;
    private boolean createBoards = true;
    private int pageSize = 1000;
    private int maxPages = 30;
    private long requestIntervalMillis = 250;
}
