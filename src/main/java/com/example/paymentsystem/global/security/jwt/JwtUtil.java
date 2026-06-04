package com.example.paymentsystem.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret.key}")
    private String secretKeyString;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;
    private JwtParser parser;

    @PostConstruct
    public void init() {
        byte[] bytes = Decoders.BASE64.decode(secretKeyString);
        this.key = Keys.hmacShaKeyFor(bytes);
        this.parser = Jwts.parser()
                .verifyWith(this.key)
                .build();
    }

    /**
     * 토큰 생성 (memberId 기반)
     * @param memberId 토큰에 저장할 회원 ID
     * @return Bearer 접두어가 포함된 JWT 토큰 문자열
     */
    public String generateToken(Long memberId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))  // memberId를 subject에 저장
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 토큰 검증 (만료/위변조 구분)
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            parser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] 만료된 토큰: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("[JWT] 위변조된 토큰: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] 유효하지 않은 토큰: {}", e.getMessage());
            return false;
        }
    }

    // memberId 추출
    public Long getMemberId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    // Claims 파싱
    private Claims extractAllClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
