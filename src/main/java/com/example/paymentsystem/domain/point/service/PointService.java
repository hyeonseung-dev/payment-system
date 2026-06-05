package com.example.paymentsystem.domain.point.service;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.point.dto.response.PointHistoryPageResponse;
import com.example.paymentsystem.domain.point.dto.response.PointHistoryResponse;
import com.example.paymentsystem.domain.point.repository.PointHistoryRepository;
import com.example.paymentsystem.domain.point.entity.PointHistory;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointHistoryPageResponse getPointHistories(
            int page,
            int size
    ) {
        Long memberId = 1L;

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                );

        Pageable pageable = PageRequest.of(page, size);

        Page<PointHistory> histories =
                pointHistoryRepository
                        .findByMemberOrderByCreatedAtDesc(
                                member,
                                pageable
                        );

        List<PointHistoryResponse> content =
                histories.getContent()
                        .stream()
                        .map(history -> new PointHistoryResponse(
                                history.getId(),
                                history.getPaymentId(),
                                history.getType(),
                                history.getAmount(),
                                history.getBalanceAfter(),
                                history.getCreatedAt()
                        ))
                        .toList();

        return new PointHistoryPageResponse(
                content,
                histories.getNumber(),
                histories.getSize(),
                histories.getTotalElements()
        );
    }
}
