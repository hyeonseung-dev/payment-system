package com.example.paymentsystem.domain.point.dummy;

import com.example.paymentsystem.domain.member.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class PointDummyDataInitializer {

    private final MemberRepository memberRepository;

    @PostConstruct
    public void init() {
        memberRepository.findAll().stream()
                .filter(m -> m.getPointBalance() == 0)
                .forEach(m -> {
                    m.earnPoint(1000);
                    memberRepository.save(m);
                });
    }
}
