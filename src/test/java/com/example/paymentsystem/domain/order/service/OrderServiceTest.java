package com.example.paymentsystem.domain.order.service;

import com.example.paymentsystem.domain.cart.entity.Cart;
import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.order.dto.OrderPreviewItemResponse;
import com.example.paymentsystem.domain.order.dto.OrderPreviewRequest;
import com.example.paymentsystem.domain.order.dto.OrderPreviewResponse;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.enumtype.ProductCategory;
import com.example.paymentsystem.domain.product.enumtype.ProductStatus;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static com.example.paymentsystem.domain.product.enumtype.ProductCategory.FOOD;
import static com.example.paymentsystem.domain.product.enumtype.ProductStatus.FOR_SALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private CartItemRepository cartItemRepository;

    @Test
    void 주문서_미리보기를_성공한다() throws Exception {
        // given
        Long memberId = 1L;
        Long cartItemId = 1L;
        Long productId = 1L;

        OrderPreviewRequest request = new OrderPreviewRequest(List.of(cartItemId));

        Member member = createMember(memberId);
        Product product = createProduct(productId, "휴대폰", 500000, 30, FOOD, FOR_SALE);
        Cart cart = createCart(1L, member);
        CartItem cartItem = createCartItem(cartItemId, cart, product, 2);

        when(cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId))
                .thenReturn(List.of(cartItem));

        // when
        OrderPreviewResponse response = orderService.previewOrder(memberId, request);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount()).isEqualTo(1000000);

        OrderPreviewItemResponse item = response.items().get(0);

        assertThat(item.cartItemId()).isEqualTo(cartItemId);
        assertThat(item.productId()).isEqualTo(productId);
        assertThat(item.productName()).isEqualTo("휴대폰");
        assertThat(item.price()).isEqualTo(500000);
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotal()).isEqualTo(1_000_000);
    }

    @Test
    void 조회된_장바구니_상품이_없으면_예외가_발생한다() {
        // given
        Long memberId = 1L;
        OrderPreviewRequest request = new OrderPreviewRequest(List.of(10L));

        when(cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId))
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 요청한_장바구니_상품_중_조회되지_않은_항목이_있으면_예외가_발생한다() throws Exception {
        // given
        Long memberId = 1L;

        Long requestedCartItemId1 = 10L;
        Long requestedCartItemId2 = 20L;

        OrderPreviewRequest request = new OrderPreviewRequest(
                List.of(requestedCartItemId1, requestedCartItemId2)
        );

        Member member = createMember(memberId);
        Product product = createProduct(100L, "휴대폰", 500000, 30, FOOD, FOR_SALE);
        Cart cart = createCart(1L, member);
        CartItem cartItem = createCartItem(requestedCartItemId1, cart, product, 2);

        when(cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId))
                .thenReturn(List.of(cartItem));

        // when & then
        assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    private Member createMember(Long id) throws Exception {
        Member member = new Member(
                "test@test.com",
                "password",
                "테스트회원",
                "01012345678",
                0
        );

        setId(member, id);
        return member;
    }

    private Product createProduct(Long id, String name, int price, int stockQuantity, ProductCategory category, ProductStatus status) throws Exception {
        Product product = new Product(
                "휴대폰",
                500000,
                30,
                FOOD,
                FOR_SALE
        );

        setId(product, id);
        return product;
    }

    private Cart createCart(Long id, Member member) throws Exception {
        Cart cart = Cart.create(member);
        setId(cart, id);
        return cart;
    }

    private CartItem createCartItem(Long id, Cart cart, Product product, int quantity) throws Exception {
        CartItem cartItem = CartItem.create(cart, product, quantity);
        setId(cartItem, id);
        return cartItem;
    }

    private void setId(Object target, Long id) throws Exception {
        Field idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}