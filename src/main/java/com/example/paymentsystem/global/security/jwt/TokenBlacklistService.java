package com.example.paymentsystem.global.security.jwt;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TokenBlacklistService {

    // 메모리에 블랙리스트 저장
    // 앱 재시작 시 초기화
    private final Set<String> blacklist = new HashSet<>();

    /**
     * 토큰을 블랙리스트에 추가합니다.
     *
     * @param token 블랙리스트에 추가할 토큰
     */
    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     *
     * @param token 확인할 토큰
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }
}