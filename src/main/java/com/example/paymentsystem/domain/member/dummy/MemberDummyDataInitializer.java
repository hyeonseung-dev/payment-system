package com.example.paymentsystem.domain.member.dummy;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class MemberDummyDataInitializer {

    private final MemberRepository memberRepository;

    @PostConstruct
    public void init() {

        if (memberRepository.count() > 0) {
            return;
        }

        memberRepository.save(
                new Member(
                        "test@test.com",
                        "1234",
                        "홍길동",
                        "01012345678",
                        5000
                )
        );
    }
}