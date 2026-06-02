package com.example.paymentsystem.domain.member.repository;

import com.example.paymentsystem.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
