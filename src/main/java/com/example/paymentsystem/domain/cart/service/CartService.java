package com.example.paymentsystem.domain.cart.service;

import com.example.paymentsystem.domain.cart.dto.CartItemResponse;
import com.example.paymentsystem.domain.cart.dto.CartResponse;
import com.example.paymentsystem.domain.cart.dto.UpdateCartResponse;
import com.example.paymentsystem.domain.cart.entity.Cart;
import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.cart.repository.CartRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public CartResponse findCartItems(Long memberId) {
        Cart cart = cartRepository.findByMember_Id(memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.CART_NOT_FOUND)
        );

        List<CartItemResponse> list = cartItemRepository.findAllByMemberId(memberId).stream()
                .map(CartItemResponse::from)
                .toList();

        return CartResponse.of(cart.getId(), list);
    }

    @Transactional
    public Long addItem(Long memberId, Long productId, int quantity) {

        Product product = productRepository.findById(productId).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        );

        Cart cart = getOrCreateCart(memberId);

        CartItem cartItem = cartItemRepository.findByCart_Member_IdAndProduct_Id(memberId, productId)
                .orElse(CartItem.create(cart, product, quantity));

        int totalQuantity = cartItem.getId() != null ? cartItem.getQuantity() + quantity : quantity;

        if (totalQuantity > product.getStockQuantity()) {
            throw new BusinessException(ErrorCode.CART_ITEM_STOCK_EXCEEDED);
        }

        if (cartItem.getId() != null) {
            cartItem.addQuantity(quantity);
        }

        CartItem save = cartItemRepository.save(cartItem);

        return save.getId();
    }

    private Cart getOrCreateCart(Long memberId) {
        return cartRepository.findByMember_Id(memberId).orElseGet(
                () -> {
                    Member member = memberRepository.findById(memberId).orElseThrow(
                            () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
                    );

                    Cart cart = Cart.create(member);
                    return cartRepository.save(cart);
                });
    }

    @Transactional
    public UpdateCartResponse updateQuantity(Long memberId, Long itemId, int quantity) {
        CartItem cartItem = cartItemRepository.findByIdAndCart_Member_Id(itemId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (quantity > cartItem.getProduct().getStockQuantity()) {
            throw new BusinessException(ErrorCode.CART_ITEM_STOCK_EXCEEDED);
        }

        cartItem.changeQuantity(quantity);

        return new UpdateCartResponse(cartItem.getId(), cartItem.getQuantity());
    }

    @Transactional
    public void removeItem(Long memberId, Long cartItemId) {
        int deleted = cartItemRepository.deleteByIdAndCart_Member_Id(cartItemId, memberId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    /**
     * 주문에 사용된 장바구니 상품만 삭제한다.
     *
     * <p>주문 생성 시에는 결제 실패 가능성이 있으므로 장바구니를 유지하고,
     * 결제 성공이 확정된 뒤 OrderItem에 저장된 원본 cartItemId 기준으로 선택 항목만 삭제한다.
     * 상품 바로 주문처럼 원본 CartItem이 없는 주문 상품은 삭제 대상에서 제외된다.</p>
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void deleteOrderedCartItemsByOrderId(Long orderId) {
        List<Long> cartItemIds = orderItemRepository.findCartItemIdsByOrderId(orderId);
        if (cartItemIds.isEmpty()) {
            return;
        }

        cartItemRepository.deleteByIdIn(cartItemIds);
    }
}
