package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.entity.CrmBookingTicket;
import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.CrmBookingRepository;
import com.nexlora.nexbusiness.repository.PendingApprovalRepository;
import com.nexlora.nexbusiness.repository.PosOrderRepository;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import com.nexlora.nexbusiness.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Numbers for the dashboard cards. Admins see business-wide totals; regular users
 * see only their own orders and appointments. Stock is shared by everyone.
 */
@RestController
@RequestMapping("/api/omniserve/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    @Autowired
    private PosOrderRepository orderRepository;

    @Autowired
    private ProductInventoryRepository productRepository;

    @Autowired
    private CrmBookingRepository bookingRepository;

    @Autowired
    private PendingApprovalRepository approvalRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> stats(Authentication auth) {
        boolean admin = SecureDocController.isAdmin(auth);
        Long userId = CurrentUser.id(auth);

        List<ProductInventory> products = productRepository.findAll();
        int stockUnits = 0;
        int lowStock = 0;
        for (ProductInventory p : products) {
            stockUnits += p.getStockQty();
            if (p.getStockQty() <= p.getReorderLevel()) {
                lowStock++;
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", admin ? "ALL" : "MINE");
        out.put("totalOrders", admin ? orderRepository.count() : orderRepository.countByUserId(userId));
        out.put("appointments", admin
                ? bookingRepository.countByType(CrmBookingTicket.BookingType.APPOINTMENT)
                : bookingRepository.countByTypeAndUserId(CrmBookingTicket.BookingType.APPOINTMENT, userId));
        out.put("products", products.size());
        out.put("stockUnits", stockUnits);
        out.put("lowStock", lowStock);
        if (admin) {
            out.put("pendingApprovals", approvalRepository.countByStatus(PendingApproval.ApprovalStatus.PENDING));
        }
        return ResponseEntity.ok(out);
    }
}
