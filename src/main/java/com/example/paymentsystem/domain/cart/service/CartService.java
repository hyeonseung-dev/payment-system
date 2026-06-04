package com.example.paymentsystem.domain.cart.service;

import com.example.paymentsystem.domain.cart.entity.Cart;
import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.cart.repository.CartRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Long addItem(Long memberId, Long productId, int quantity) {

        Product product = productRepository.findById(productId).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        );

        Cart cart = getOrCreateCart(memberId);

        Optional<CartItem> byCartMemberIdAndProductId = cartItemRepository.findByCart_Member_IdAndProduct_Id(memberId, productId);

        int totalQuantity = quantity;

        if (byCartMemberIdAndProductId.isPresent()) {
            CartItem cartItem = byCartMemberIdAndProductId.get();

            totalQuantity = cartItem.getQuantity() + quantity;
        }

        if (totalQuantity > product.getStockQuantity()) {
            throw new BusinessException(ErrorCode.CART_ITEM_STOCK_EXCEEDED);
        }

        if (byCartMemberIdAndProductId.isPresent()) {
            CartItem cartItem = byCartMemberIdAndProductId.get();
            cartItem.addQuantity(quantity);
            return cartItem.getId();
        }

        CartItem cartItem = CartItem.create(cart, product, quantity);

        cartItemRepository.save(cartItem);

        return cartItem.getId();
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
}