package com.shu.backend.global.security;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/// local 프로필에서 userId=2를 강제로 인증 사용자로 넣는 개발용 필터
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevAuthenticationFilter extends OncePerRequestFilter {

    private static final Long DEV_USER_ID = 2L;

    private final UserRepository userRepository;

    /// 모든 요청마다 개발용 인증 사용자를 SecurityContext에 주입
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 이미 인증 정보가 있으면 덮어쓰지 않음
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            User devUser = userRepository.findById(DEV_USER_ID)
                    .orElseThrow(() -> new IllegalStateException("개발용 유저(userId=2)를 찾을 수 없습니다."));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            devUser,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + devUser.getRole().name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}