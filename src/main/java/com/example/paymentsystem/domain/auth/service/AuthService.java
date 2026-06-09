package com.example.paymentsystem.domain.auth.service;

import com.example.paymentsystem.domain.auth.dto.LoginRequest;
import com.example.paymentsystem.domain.auth.dto.LoginResponse;
import com.example.paymentsystem.domain.auth.dto.SignupRequest;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.global.security.jwt.JwtUtil;
import com.example.paymentsystem.global.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 회원가입 처리
     * @param request 회원가입 요청 DTO (이메일, 비밀번호, 이름, 전화번호)
     * @throws BusinessException 이메일 중복 시 {@link ErrorCode#DUPLICATE_EMAIL}
     * @throws BusinessException 전화번호 중복 시 {@link ErrorCode#DUPLICATE_PHONE}
     */
    @Transactional
    public void signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }

        Member member = new Member(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phoneNumber(),
                0
        );
        memberRepository.save(member);
    }

    /**
     * 로그인을 처리하고 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 DTO (이메일, 비밀번호)
     * @return JWT 액세스 토큰이 담긴 {@link LoginResponse}
     * @throws BusinessException 이메일 미존재 또는 비밀번호 불일치 시 {@link ErrorCode#INVALID_CREDENTIALS}
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 회원 조회
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. JWT 토큰 발급
        String token = jwtUtil.generateToken(member.getId());

        // 4. 토큰 반환
        return LoginResponse.of(token, member.getName(), member.getPhoneNumber());
    }

    @Transactional
    public void logout(String token) {
        tokenBlacklistService.addToBlacklist(token);
    }

}
