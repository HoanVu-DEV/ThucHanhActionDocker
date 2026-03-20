package com.example.Buoi3.service;

import com.example.Buoi3.Entity.CartItem;
import com.example.Buoi3.Entity.Order;
import com.example.Buoi3.Entity.OrderDetail;
import com.example.Buoi3.Entity.Product;
import com.example.Buoi3.repository.OrderDetailRepository;
import com.example.Buoi3.repository.OrderRepository;
import com.example.Buoi3.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final PromoReservationService reservationService;
    private final jakarta.servlet.http.HttpSession session;

    @Transactional
    public Order createOrder(String customerName, String phone, String address, String notes, String paymentMethod) {
        List<CartItem> cartItems = cartService.getCartItems();
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        // Tạo Order
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setNotes(notes);
        order.setPaymentMethod(paymentMethod);
        
        order = orderRepository.save(order);

        // Tạo OrderDetails
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(product);
            orderDetail.setQuantity(item.getQuantity());
            
            orderDetailRepository.save(orderDetail);
        }

        // Finalize reservations so they aren't restored by the scheduled task
        reservationService.consumeReservation(session.getId());

        // Xóa giỏ hàng sau khi đặt thành công
        cartService.clearCart();

        return order;
    }
}
