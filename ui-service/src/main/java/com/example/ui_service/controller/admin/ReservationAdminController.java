package com.example.ui_service.controller.admin;

import com.example.ui_service.service.AdminReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ReservationAdminController {

    private final AdminReservationService adminReservationService;

    @GetMapping("/reservations")
    public String manageReservations(Model model) {
        model.addAttribute("pageTitle", "Quản lý đặt lịch");
        model.addAttribute("pageSubtitle", "Quản lý và theo dõi các lịch đặt xe");
        
        try {
            // Gọi API để lấy danh sách reservations
            List<Map<String, Object>> reservations = adminReservationService.getAllReservations();
            model.addAttribute("reservations", reservations);
            System.out.println("✓ Đã load " + reservations.size() + " reservations vào model");
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi load reservations: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("reservations", List.of());
            model.addAttribute("errorMessage", "Không thể tải danh sách đặt lịch. Vui lòng thử lại sau.");
        }
        
        return "admin-reservations";
    }

    @GetMapping("/ai-recommendations")
    public String aiRecommendations(Model model) {
        model.addAttribute("pageTitle", "AI Recommendations");
        model.addAttribute("pageSubtitle", "Gợi ý thông minh từ AI để tối ưu hóa việc sử dụng xe và chia sẻ chi phí");
        model.addAttribute("message", "AI Recommendations - Gợi ý thông minh từ AI");
        return "admin-ai-recommendations";
    }

    @GetMapping("/schedule")
    public String adminSchedule(Model model) {
        model.addAttribute("pageTitle", "Quản lý lịch xe");
        model.addAttribute("pageSubtitle", "Theo dõi và quản lý lịch sử dụng xe đồng sở hữu");
        return "admin-schedule";
    }

    @GetMapping("/fair-schedule")
    public String fairSchedule(Model model) {
        model.addAttribute("pageTitle", "Lịch đặt xe công bằng");
        model.addAttribute("pageSubtitle", "Ưu tiên theo tỷ lệ sở hữu & lịch sử sử dụng");
        return "admin-fair-schedule";
    }

    /**
     * Xử lý cập nhật reservation
     */
    @PostMapping("/reservations/{id}/update")
    public String updateReservation(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam Long vehicleId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String note,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔧 Updating reservation ID: " + id);
            
            // Convert datetime-local format (YYYY-MM-DDTHH:mm) to ISO format (YYYY-MM-DDTHH:mm:ss)
            String startDatetime = startDate.contains(":") && !startDate.contains(":00:00") 
                ? startDate + ":00" 
                : startDate;
            String endDatetime = endDate.contains(":") && !endDate.contains(":00:00") 
                ? endDate + ":00" 
                : endDate;
            
            boolean success = adminReservationService.updateReservation(id, userId, vehicleId, startDatetime, endDatetime, note, status);
            
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch đặt thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật lịch đặt. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi cập nhật reservation: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/reservations";
    }
    
    /**
     * Xử lý xóa reservation
     */
    @PostMapping("/reservations/{id}/delete")
    public String deleteReservation(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🗑️ Deleting reservation ID: " + id);
            
            boolean success = adminReservationService.deleteReservation(id);
            
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "Xóa lịch đặt thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa lịch đặt. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi xóa reservation: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/admin/reservations";
    }

    /**
     * Cập nhật trạng thái nhanh (Check-in / Check-out)
     */
    @PostMapping("/reservations/{id}/status")
    public String quickUpdateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔄 Updating status for reservation ID: " + id + " to: " + status);
            boolean success = adminReservationService.updateReservationStatus(id, status);
            if (success) {
                String actionLabel = switch (status.toUpperCase()) {
                    case "IN_USE" -> "Check-in";
                    case "COMPLETED" -> "Check-out";
                    default -> "Cập nhật";
                };
                redirectAttributes.addFlashAttribute("successMessage", actionLabel + " thành công cho lịch #" + id);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái cho lịch #" + id);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi cập nhật trạng thái nhanh: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/reservations";
    }
}

