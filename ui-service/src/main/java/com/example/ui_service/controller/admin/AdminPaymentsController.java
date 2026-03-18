package com.example.ui_service.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller cho trang Theo dõi thanh toán Admin
 */
@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentsController {

    @GetMapping
    public String payments(Model model) {
        model.addAttribute("pageTitle", "Theo dõi thanh toán");
        model.addAttribute("pageSubtitle", "Theo dõi và quản lý các khoản thanh toán của thành viên");
        model.addAttribute("activePage", "payments");
        return "admin-payments";
    }

    @GetMapping("/edit/{paymentId}")
    public String editPayment(@PathVariable Integer paymentId, Model model) {
        model.addAttribute("pageTitle", "Chỉnh sửa thanh toán");
        model.addAttribute("pageSubtitle", "Cập nhật thông tin thanh toán");
        model.addAttribute("activePage", "payments");
        model.addAttribute("paymentId", paymentId);
        return "admin-payments-edit";
    }
}

