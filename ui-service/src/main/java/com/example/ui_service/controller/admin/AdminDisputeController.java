package com.example.ui_service.controller.admin;

import com.example.ui_service.external.service.DisputeRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/disputes")
public class AdminDisputeController {

    @Autowired
    private DisputeRestClient disputeRestClient;

    @GetMapping
    public String disputesList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            Model model) {
        try {
            List<Map<String, Object>> disputes;
            if (status != null && !status.isEmpty()) {
                disputes = disputeRestClient.getDisputesByStatus(status);
            } else {
                disputes = disputeRestClient.getAllDisputes();
            }

            Map<String, Object> statistics = disputeRestClient.getStatistics();
            
            model.addAttribute("disputes", disputes);
            model.addAttribute("statistics", statistics);
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentPriority", priority);
            model.addAttribute("pageTitle", "Quản lý Tranh chấp");
            return "admin/admin-disputes";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách tranh chấp: " + e.getMessage());
            return "admin/admin-disputes";
        }
    }

    @GetMapping("/unassigned")
    public String unassignedDisputes(Model model) {
        try {
            List<Map<String, Object>> disputes = disputeRestClient.getUnassignedDisputes();
            model.addAttribute("disputes", disputes);
            model.addAttribute("pageTitle", "Tranh chấp chưa được giao");
            return "admin/admin-disputes";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách: " + e.getMessage());
            return "admin/admin-disputes";
        }
    }

    @GetMapping("/pending")
    public String pendingDisputes(Model model) {
        try {
            List<Map<String, Object>> disputes = disputeRestClient.getPendingDisputes();
            model.addAttribute("disputes", disputes);
            model.addAttribute("pageTitle", "Tranh chấp đang chờ xử lý");
            return "admin/admin-disputes";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách: " + e.getMessage());
            return "admin/admin-disputes";
        }
    }

    @GetMapping("/{disputeId}")
    public String disputeDetail(@PathVariable Integer disputeId, Model model) {
        try {
            Map<String, Object> dispute = disputeRestClient.getDisputeById(disputeId);
            if (dispute == null) {
                model.addAttribute("error", "Không tìm thấy tranh chấp");
                return "admin/admin-dispute-detail";
            }

            List<Map<String, Object>> comments = disputeRestClient.getComments(disputeId, true);
            List<Map<String, Object>> history = disputeRestClient.getHistory(disputeId);
            Map<String, Object> resolution = disputeRestClient.getResolution(disputeId);

            model.addAttribute("dispute", dispute);
            model.addAttribute("comments", comments);
            model.addAttribute("history", history);
            model.addAttribute("resolution", resolution);
            model.addAttribute("pageTitle", "Chi tiết Tranh chấp");
            return "admin/admin-dispute-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải chi tiết: " + e.getMessage());
            return "admin/admin-dispute-detail";
        }
    }

    @PostMapping("/{disputeId}/assign")
    public String assignDispute(@PathVariable Integer disputeId,
                                @RequestParam Integer staffId,
                                RedirectAttributes redirectAttributes) {
        try {
            boolean success = disputeRestClient.assignDispute(disputeId, staffId);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "Đã giao tranh chấp thành công");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không thể giao tranh chấp");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/disputes/" + disputeId;
    }

    @PostMapping("/{disputeId}/update-status")
    public String updateStatus(@PathVariable Integer disputeId,
                              @RequestParam String status,
                              @RequestParam(required = false) String note,
                              RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", status);
            if (note != null && !note.isEmpty()) {
                updateData.put("resolutionNote", note);
            }
            Map<String, Object> updated = disputeRestClient.updateDispute(disputeId, updateData);
            if (updated != null) {
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái thành công");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không thể cập nhật trạng thái");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/disputes/" + disputeId;
    }

    @PostMapping("/{disputeId}/comments")
    public String addComment(@PathVariable Integer disputeId,
                            @RequestParam String content,
                            @RequestParam(required = false, defaultValue = "false") Boolean isInternal,
                            @RequestParam(required = false, defaultValue = "ADMIN") String userRole,
                            RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("userId", 1); // TODO: Get from session
            commentData.put("userRole", userRole);
            commentData.put("content", content);
            commentData.put("isInternal", isInternal);
            
            Map<String, Object> created = disputeRestClient.addComment(disputeId, commentData);
            if (created != null) {
                redirectAttributes.addFlashAttribute("success", "Đã thêm bình luận");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không thể thêm bình luận");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/disputes/" + disputeId;
    }

    @PostMapping("/{disputeId}/resolve")
    public String resolveDispute(@PathVariable Integer disputeId,
                                 @RequestParam String resolutionType,
                                 @RequestParam String resolutionDetails,
                                 @RequestParam(required = false) String actionTaken,
                                 @RequestParam(required = false, defaultValue = "0") Double compensationAmount,
                                 RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> resolutionData = new HashMap<>();
            resolutionData.put("resolvedBy", 1); // TODO: Get from session
            resolutionData.put("resolutionType", resolutionType);
            resolutionData.put("resolutionDetails", resolutionDetails);
            if (actionTaken != null && !actionTaken.isEmpty()) {
                resolutionData.put("actionTaken", actionTaken);
            }
            resolutionData.put("compensationAmount", compensationAmount);
            
            Map<String, Object> created = disputeRestClient.createResolution(disputeId, resolutionData);
            if (created != null) {
                redirectAttributes.addFlashAttribute("success", "Đã giải quyết tranh chấp thành công");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không thể tạo giải pháp");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/disputes/" + disputeId;
    }
}

