package com.example.paymentsystem.integration;

import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.payment.repository.PaymentRepository;
import com.example.paymentsystem.domain.point.enumtype.PointHistoryType;
import com.example.paymentsystem.domain.point.repository.PointHistoryRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import com.example.paymentsystem.infra.portone.dto.PortOnePaymentResponse;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @MockitoBean
    private PortOneClient portOneClient;

    @Test
    void 인증_없이_보호된_API를_호출하면_차단된다() throws Exception {
        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 상품주문_결제확정_흐름에서_결제상태와_포인트이력이_반영된다() throws Exception {
        String token = signupAndLogin("flow1@test.com", "010-1111-1111");
        Product product = firstProduct();
        int beforeStock = product.getStockQuantity();

        OrderResponse order = createProductOrder(token, product.getId(), 1, 0);
        Long orderId = order.orderId();
        String portonePaymentId = order.portonePaymentId();
        Long pgAmount = order.pgAmount();

        when(portOneClient.getPayment(portonePaymentId))
                .thenReturn(new PortOnePaymentResponse(portonePaymentId, "PAID", pgAmount));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "portonePaymentId": "%s"
                                }
                                """.formatted(orderId, portonePaymentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.earnedPointAmount").value(pgAmount / 100));

        mockMvc.perform(get("/api/points/balance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointBalance").value(pgAmount / 100));

        assertThat(paymentRepository.findByOrder_Id(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(beforeStock - 1);
        assertThat(pointHistoryRepository.findAll())
                .extracting("type")
                .contains(PointHistoryType.EARN);
    }

    @Test
    void 결제전체취소_시_주문결제상태_재고_적립포인트가_복구된다() throws Exception {
        String token = signupAndLogin("cancel@test.com", "010-2222-2222");
        Product product = firstProduct();
        int beforeStock = product.getStockQuantity();

        OrderResponse order = createProductOrder(token, product.getId(), 1, 0);
        Long orderId = order.orderId();
        String portonePaymentId = order.portonePaymentId();
        Long pgAmount = order.pgAmount();

        when(portOneClient.getPayment(portonePaymentId))
                .thenReturn(new PortOnePaymentResponse(portonePaymentId, "PAID", pgAmount));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "portonePaymentId": "%s"
                                }
                                """.formatted(orderId, portonePaymentId)))
                .andExpect(status().isOk());

        Long paymentId = paymentRepository.findByOrder_Id(orderId).orElseThrow().getId();
        when(portOneClient.cancelPayment(eq(portonePaymentId), eq(pgAmount), anyString(), anyString()))
                .thenReturn(new PortOneCancelResponse(portonePaymentId, pgAmount, "SUCCEEDED"));

        mockMvc.perform(post("/api/payments/{paymentId}/cancel", paymentId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "통합테스트 결제취소"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELED"));

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(beforeStock);

        mockMvc.perform(get("/api/points/balance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pointBalance").value(0));

        assertThat(pointHistoryRepository.findAll())
                .extracting("type")
                .contains(PointHistoryType.EARN, PointHistoryType.EARN_CANCEL);
    }

    @Test
    void 부분환불_시_환불수량만큼_재고와_포인트상태가_반영된다() throws Exception {
        String token = signupAndLogin("refund@test.com", "010-3333-3333");
        Product product = firstProduct();
        int beforeStock = product.getStockQuantity();

        OrderResponse order = createProductOrder(token, product.getId(), 2, 0);
        Long orderId = order.orderId();
        String portonePaymentId = order.portonePaymentId();
        Long pgAmount = order.pgAmount();

        when(portOneClient.getPayment(portonePaymentId))
                .thenReturn(new PortOnePaymentResponse(portonePaymentId, "PAID", pgAmount));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "portonePaymentId": "%s"
                                }
                                """.formatted(orderId, portonePaymentId)))
                .andExpect(status().isOk());

        Long paymentId = paymentRepository.findByOrder_Id(orderId).orElseThrow().getId();
        Long orderItemId = orderItemRepository.findAllByOrder_Id(orderId).get(0).getId();
        Long halfPgAmount = pgAmount / 2;

        when(portOneClient.cancelPayment(eq(portonePaymentId), eq(halfPgAmount), anyString(), anyString()))
                .thenReturn(new PortOneCancelResponse(portonePaymentId, halfPgAmount, "SUCCEEDED"));

        mockMvc.perform(post("/api/payments/{paymentId}/refunds", paymentId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "부분 환불",
                                  "items": [
                                    {
                                      "orderItemId": %d,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(orderItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIAL_REFUNDED"));

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PARTIAL_REFUNDED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity())
                .isEqualTo(beforeStock - 1);

        List<PointHistoryType> historyTypes = pointHistoryRepository.findAll()
                .stream()
                .map(history -> history.getType())
                .toList();
        assertThat(historyTypes).contains(PointHistoryType.EARN, PointHistoryType.EARN_CANCEL);
    }

    private String signupAndLogin(String email, String phoneNumber) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "test1234",
                                  "name": "통합테스트",
                                  "phoneNumber": "%s"
                                }
                                """.formatted(email, phoneNumber)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "test1234"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private OrderResponse createProductOrder(String token, Long productId, int quantity, int usePointAmount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": %d,
                                  "usePointAmount": %d
                                }
                                """.formatted(productId, quantity, usePointAmount)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Integer orderId = JsonPath.read(response, "$.data.orderId");
        String portonePaymentId = JsonPath.read(response, "$.data.portonePaymentId");
        Integer pgAmount = JsonPath.read(response, "$.data.pgAmount");

        return new OrderResponse(orderId.longValue(), portonePaymentId, pgAmount.longValue());
    }

    private Product firstProduct() {
        return productRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record OrderResponse(
            Long orderId,
            String portonePaymentId,
            Long pgAmount
    ) {
    }
}
