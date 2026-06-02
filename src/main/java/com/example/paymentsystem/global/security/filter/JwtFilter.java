package com.example.paymentsystem.global.security.filter;

import com.example.paymentsystem.global.security.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 검증 후 SecurityContext에 저장
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Long memberId = jwtUtil.getMemberId(token);
            setAuthentication(memberId);
        }

        // 3. 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 토큰 추출
     *
     * if문에서 ("Bearer eyJ..." 형태인지 확인 후 "Bearer " 제거)
     * @param request HTTP 요청 객체
     * @return 추출된 토큰 문자열, 없으면 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");


        if (StringUtils.hasText(bearerToken) &&
                bearerToken.startsWith(JwtUtil.BEARER_PREFIX)) {
            return bearerToken.substring(JwtUtil.BEARER_PREFIX.length());
        }
        return null;
    }

    // SecurityContext에 인증 정보 저장

    /**
     * SecurityContext에 인증 정보를 저장
     *
     * <p>JWT 검증이 완료된 memberId를 기반으로 인증 객체를 생성하여
     * SecurityContext에 저장합니다.</p>
     *
     * @param memberId
     */
    private void setAuthentication(Long memberId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        memberId,   // principal → 이후 @AuthenticationPrincipal로 꺼냄
                        null,       // credentials → 비밀번호 (JWT 방식에서 불필요)
                        List.of()   // authorities → 권한 목록 (지금은 빈 목록)
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[JWT] 인증 성공 memberId={}", memberId);
    }

}