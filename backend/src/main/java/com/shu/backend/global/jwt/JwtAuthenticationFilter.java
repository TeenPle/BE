package com.shu.backend.global.jwt;

import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);

                    userRepository.findByIdWithSchoolAndRegion(userId).ifPresent(user -> {
                        if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
                            SecurityContextHolder.clearContext();
                            return;
                        }
                        List<SimpleGrantedAuthority> authorities =
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, authorities);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
                }
            } catch (Exception e) {
                // 만료·서명 오류 등 모든 JWT 예외는 인증 실패로 처리.
                // 예외를 전파하지 않고 SecurityContext를 비워둔 채 다음 필터로 넘김.
                // 이후 Spring Security의 AuthenticationEntryPoint가 401을 반환.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring("Bearer ".length());
        }
        return null;
    }
}
