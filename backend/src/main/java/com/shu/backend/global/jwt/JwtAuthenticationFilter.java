package com.shu.backend.global.jwt;

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

/**
 * JWT 기반 인증 필터.
 *
 * 클라이언트 요청의 Authorization 헤더에서 Bearer 토큰을 추출한 뒤
 * 토큰이 유효하면 userId를 꺼내 DB에서 사용자를 조회하고,
 * 해당 사용자를 Spring Security의 인증 객체로 등록한다.
 *
 * 즉, 매 요청마다 JWT를 통해 로그인 상태를 복원해
 * 이후 인가 처리에서 현재 로그인 사용자를 사용할 수 있게 해준다.
 */
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

        // 토큰이 있고 유효하면 사용자 인증 정보를 SecurityContext에 저장
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            userRepository.findById(userId).ifPresent(user -> {
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring("Bearer ".length());
        }
        return null;
    }
}