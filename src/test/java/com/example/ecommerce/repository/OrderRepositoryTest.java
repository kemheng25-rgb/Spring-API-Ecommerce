package com.example.ecommerce.repository;

import com.example.ecommerce.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// dev profile forces H2 + ddl-auto:update, so @DataJpaTest's embedded database actually has a
// schema to write to - the default (now prod) profile's ddl-auto:validate would fail startup
// against an empty embedded DB.
@DataJpaTest
@ActiveProfiles("dev")
@DisplayName("OrderRepository")
class OrderRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private OrderRepository orderRepository;

    @Test
    @DisplayName("[FIX] findBySellerIdOrderByOrderDateDesc matches via OrderItem.seller, " +
        "not the always-unset Order.seller field")
    void findsOrdersByItemLevelSellerOwnership() {
        User buyerUser = em.persist(User.builder()
            .email("buyer@test.com").passwordHash("x").fullName("Buyer").build());
        User sellerUser = em.persist(User.builder()
            .email("seller@test.com").passwordHash("x").fullName("Seller").isSeller(true).build());
        SellerProfile sellerProfile = em.persist(SellerProfile.builder()
            .user(sellerUser).shopName("Shop").build());
        Category category = em.persist(Category.builder().categoryName("Widgets").build());
        Product product = em.persist(Product.builder()
            .seller(sellerProfile).category(category)
            .productName("Widget").sku("SKU-TEST-1").price(new BigDecimal("9.99")).build());
        Address address = em.persist(Address.builder()
            .user(buyerUser).fullName("Buyer").phone("1234567890")
            .streetAddress("1 Main St").city("City").stateProvince("ST")
            .postalCode("00000").country("US").build());

        Order order = em.persist(Order.builder()
            .orderNumber("ORD-TEST-1").buyer(buyerUser)
            .totalAmount(new BigDecimal("9.99")).shippingAddress(address).build());
        em.persist(OrderItem.builder()
            .order(order).product(product).seller(sellerProfile)
            .quantity(1).unitPrice(new BigDecimal("9.99")).subtotal(new BigDecimal("9.99")).build());
        em.flush();

        assertThat(order.getSeller()).isNull(); // the field this bug used to rely on

        Page<Order> result = orderRepository.findBySellerIdOrderByOrderDateDesc(sellerProfile.getId(), Pageable.unpaged());

        assertThat(result.getContent()).extracting(Order::getId).containsExactly(order.getId());
    }
}
