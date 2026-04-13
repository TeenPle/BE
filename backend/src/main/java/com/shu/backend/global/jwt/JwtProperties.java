package com.shu.backend.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml 또는 application.properties 의 jwt 설정값을 바인딩하는 설정 클래스.
 *
 * JWT 서명에 사용할 secret 값과
 * access token / refresh token 만료시간을 외부 설정에서 주입받아
 * 토큰 생성 및 검증 로직에서 공통으로 사용한다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}