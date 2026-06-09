package com.example.paymentsystem.domain.point.service;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.point.dto.response.PointBalanceResponse;
import com.example.paymentsystem.domain.point.dto.response.PointHistoryPageResponse;
import com.example.paymentsystem.domain.point.dto.response.PointHistoryResponse;
import com.example.paymentsystem.domain.point.entity.PointHistory;
import com.example.paymentsystem.domain.point.repository.PointHistoryRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PointService {

    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(Long memberId) {

                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() ->
                                new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return new PointBalanceResponse(
                member.getId(),
                member.getPointBalance());
    }

    @Transactional
    public PointHistoryPageResponse getPointHistories(
            Long memberId,
            int page,
            int size
    ) {
        if (page < 0 || size < 1) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT
            );
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBER_NOT_FOUND
                ));

        Pageable pageable = PageRequest.of(page, size);

        Page<PointHistory> histories =
                pointHistoryRepository.findByMemberOrderByCreatedAtDesc(
                        member,pageable
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

    public void usePoint(
            Long memberId,
            Long paymentId,
            int amount
    ) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND
                        )
                );

        member.usePoint(amount);

        PointHistory history =
                PointHistory.createUseHistory(
                        paymentId,
                        member,
                        amount,
                        member.getPointBalance()
                );

        pointHistoryRepository.save(history);
    }

    public void earnPoint(
            Long memberId,
            Long paymentId,
            int amount
    ) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND
                        )
                );

        member.earnPoint(amount);

        PointHistory history =
                PointHistory.createEarnHistory(
                        paymentId,
                        member,
                        amount,
                        member.getPointBalance()
                );

        pointHistoryRepository.save(history);
    }

    public void cancelUsePoint(
            Long memberId,
            Long paymentId,
            int amount
    ) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND
                        )
                );

        member.restoreUsePoint(amount);

        PointHistory history =
                PointHistory.createUseCancelHistory(
                        paymentId,
                        member,
                        amount,
                        member.getPointBalance()
                );

        pointHistoryRepository.save(history);
    }

    public void cancelEarnPoint(
            Long memberId,
            Long paymentId,
            int amount
    ) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.MEMBER_NOT_FOUND
                        )
                );

        member.cancelEarnPoint(amount);

        PointHistory history =
                PointHistory.createEarnCancelHistory(
                        paymentId,
                        member,
                        amount,
                        member.getPointBalance()
                );

        pointHistoryRepository.save(history);
    }
}
