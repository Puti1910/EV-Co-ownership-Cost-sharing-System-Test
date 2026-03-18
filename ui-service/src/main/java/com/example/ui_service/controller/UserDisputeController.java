package com.example.ui_service.controller;

import com.example.ui_service.external.service.DisputeRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/disputes")
public class UserDisputeController {

    @Autowired
    private DisputeRestClient disputeRestClient;

    private String resolveReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            return "/user/disputes";
        }
        if (returnUrl.startsWith("http")) {
            return "/user/disputes";
        }
        return returnUrl.startsWith("/") ? returnUrl : "/" + returnUrl;
    }

    @PostMapping("/costs/{costId}")
    public String submitCostDispute(@PathVariable Integer costId,
                                    @RequestParam Integer groupId,
                                    @RequestParam Integer createdBy,
                                    @RequestParam(required = false) Integer reportedUserId,
                                    @RequestParam String title,
                                    @RequestParam String description,
                                    @RequestParam(required = false, defaultValue = "MEDIUM") String priority,
                                    @RequestParam(required = false) Integer vehicleId,
                                    RedirectAttributes redirectAttributes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", groupId);
        payload.put("createdBy", createdBy);
        if (reportedUserId != null) {
            payload.put("reportedUserId", reportedUserId);
        }
        payload.put("title", title);
        payload.put("description", description);
        payload.put("priority", priority);
        payload.put("category", "COST_SHARING");
        payload.put("costId", costId);
        if (vehicleId != null) {
            payload.put("vehicleId", vehicleId);
        }
        payload.put("status", "PENDING");

        try {
            Map<String, Object> response = disputeRestClient.createDispute(payload);
            if (response != null) {
                redirectAttributes.addAttribute("disputeSuccess", "Đã gửi yêu cầu tranh chấp cho Admin.");
            } else {
                redirectAttributes.addAttribute("disputeError", "Không thể gửi tranh chấp. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("disputeError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/costs/" + costId;
    }

    @PostMapping("/payments/{paymentId}")
    public String submitPaymentDispute(@PathVariable Integer paymentId,
                                       @RequestParam Integer groupId,
                                       @RequestParam Integer createdBy,
                                       @RequestParam(required = false) Integer reportedUserId,
                                       @RequestParam String title,
                                       @RequestParam String description,
                                       @RequestParam(required = false, defaultValue = "MEDIUM") String priority,
                                       @RequestParam(required = false) Integer costId,
                                       RedirectAttributes redirectAttributes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", groupId);
        payload.put("createdBy", createdBy);
        if (reportedUserId != null) {
            payload.put("reportedUserId", reportedUserId);
        }
        payload.put("title", title);
        payload.put("description", description);
        payload.put("priority", priority);
        payload.put("category", "PAYMENT");
        payload.put("paymentId", paymentId);
        if (costId != null) {
            payload.put("costId", costId);
        }
        payload.put("status", "PENDING");

        try {
            Map<String, Object> response = disputeRestClient.createDispute(payload);
            if (response != null) {
                redirectAttributes.addAttribute("disputeSuccess", "Đã gửi tranh chấp thanh toán.");
            } else {
                redirectAttributes.addAttribute("disputeError", "Không thể gửi tranh chấp thanh toán.");
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("disputeError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/costs/payments";
    }

    @PostMapping("/report")
    public String submitGeneralDispute(@RequestParam Integer groupId,
                                       @RequestParam Integer createdBy,
                                       @RequestParam String title,
                                       @RequestParam String description,
                                       @RequestParam(required = false) Integer reportedUserId,
                                       @RequestParam(required = false, defaultValue = "GENERAL") String category,
                                       @RequestParam(required = false, defaultValue = "MEDIUM") String priority,
                                       @RequestParam(required = false) Integer vehicleId,
                                       @RequestParam(required = false) Integer reservationId,
                                       @RequestParam(required = false) Integer costId,
                                       @RequestParam(required = false) Integer paymentId,
                                       @RequestParam(required = false) String returnUrl,
                                       RedirectAttributes redirectAttributes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("groupId", groupId);
        payload.put("createdBy", createdBy);
        if (reportedUserId != null) {
            payload.put("reportedUserId", reportedUserId);
        }
        payload.put("title", title);
        payload.put("description", description);
        payload.put("priority", priority);
        payload.put("category", category);
        if (vehicleId != null) {
            payload.put("vehicleId", vehicleId);
        }
        if (reservationId != null) {
            payload.put("reservationId", reservationId);
        }
        if (costId != null) {
            payload.put("costId", costId);
        }
        if (paymentId != null) {
            payload.put("paymentId", paymentId);
        }
        payload.put("status", "PENDING");

        try {
            Map<String, Object> response = disputeRestClient.createDispute(payload);
            if (response != null) {
                redirectAttributes.addAttribute("disputeSuccess", "Đã gửi báo cáo tranh chấp đến Admin.");
            } else {
                redirectAttributes.addAttribute("disputeError", "Không thể gửi báo cáo tranh chấp. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("disputeError", "Lỗi: " + e.getMessage());
        }
        return "redirect:" + resolveReturnUrl(returnUrl);
    }
}




