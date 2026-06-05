package com.example.paymentsystem.global.config;

import com.example.paymentsystem.global.security.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final JwtFilter jwtFilter;  // ← 주석 해제

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, SecurityContextHolderAwareRequestFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/payments/confirm").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/payments/*/cancel").authenticated()
                        .requestMatchers(HttpMethod.GET, "/payment-test.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/config.js").permitAll()
                        .requestMatchers(
                                "/api/auth/**",      // 회원가입, 로그인
                                "/api/products/**",  // 상품 조회 (인증 불필요)
                                "/api/portone/config", // PortOne 결제창 공개 설정
                                "/api/webhooks/**",   // PortOne 웹훅 (JWT 대신 서명 검증)
                                "/api/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
