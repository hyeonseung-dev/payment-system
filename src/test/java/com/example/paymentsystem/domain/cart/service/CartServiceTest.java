package com.example.paymentsystem.domain.cart.service;

import com.example.paymentsystem.domain.cart.entity.Cart;
import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.cart.repository.CartRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.paymentsystem.domain.product.enumtype.ProductCategory.FOOD;
import static com.example.paymentsystem.domain.product.enumtype.ProductStatus.FOR_SALE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void 동시에_같은_장바구니_상품을_수정하면_낙관적_락이_발생한다() throws InterruptedException {
        // given
        // 테스트에 사용할 회원을 저장한다.
        Member member = memberRepository.save(
                Member.create("test@test.com", "password", "테스트회원", "010-1234-5678")
        );

        // 테스트에 사용할 상품을 저장한다.
        Product product = productRepository.save(
                Product.create("아이폰 케이스", 15000, 30, FOOD, FOR_SALE)
        );

        // 회원의 장바구니를 생성한다.
        Cart cart = cartRepository.save(Cart.create(member));

        // 낙관적 락 테스트를 위해 같은 상품을 장바구니에 미리 담아둔다.
        cartItemRepository.save(CartItem.create(cart, product, 1));

        Long memberId = member.getId();
        Long productId = product.getId();
        int quantity = 1;

        int threadCount = 2;

        // 두 요청을 동시에 시작시키기 위한 도구다.
        CountDownLatch startLatch = new CountDownLatch(1);

        // 두 요청이 모두 끝날 때까지 기다리기 위한 도구다.
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // 동시에 실행할 스레드 풀을 만든다.
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 각 스레드에서 발생한 예외를 저장한다.
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // startLatch.countDown()이 호출될 때까지 대기한다.
                    startLatch.await();

                    // 같은 회원이 같은 상품을 동시에 장바구니에 추가한다.
                    cartService.addItem(memberId, productId, quantity);
                } catch (Exception e) {
                    // 낙관적 락 충돌 등 발생한 예외를 저장한다.
                    exceptions.add(e);
                } finally {
                    // 현재 스레드 작업이 끝났음을 알린다.
                    endLatch.countDown();
                }
            });
        }

        // 대기 중이던 두 스레드를 동시에 출발시킨다.
        startLatch.countDown();

        // 두 스레드가 모두 끝날 때까지 기다린다.
        endLatch.await();

        // 스레드 풀을 종료한다.
        executorService.shutdown();

        // then
        // 동시에 같은 CartItem을 수정하면 둘 중 하나는 낙관적 락 예외가 발생해야 한다.
        assertThat(exceptions)
                .anyMatch(e ->
                        e instanceof ObjectOptimisticLockingFailureException
                                || e.getCause() instanceof OptimisticLockException
                );
    }
}