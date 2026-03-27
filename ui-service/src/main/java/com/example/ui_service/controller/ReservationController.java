package com.example.ui_service.controller;

import com.example.ui_service.service.VehicleService;
import com.example.ui_service.service.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final VehicleService vehicleService;
    private final ReservationService reservationService;

    public ReservationController(VehicleService vehicleService, ReservationService reservationService) {
        this.vehicleService = vehicleService;
        this.reservationService = reservationService;
    }

    // ✅ Trang đặt lịch mặc định: hiển thị xe đầu tiên
    @GetMapping("/book")
    public String showBookingForm(
            Model model, 
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "userId", required = false) Long userIdFromParam,
            @CookieValue(value = "userId", required = false) Long userIdFromCookie) {
        
        // Lấy userId từ parameter (từ frontend) hoặc cookie
        Long userId = userIdFromParam != null ? userIdFromParam : userIdFromCookie;
        
        // Chỉ lấy xe của user đăng nhập
        List<Map<String, Object>> vehicles;
        if (userId != null) {
            logger.info("🔍 Lấy danh sách xe cho user ID: {}", userId);
            vehicles = vehicleService.getUserVehicles(userId);
            logger.info("✅ Tìm thấy {} xe cho user", vehicles.size());
        } else {
            logger.warn("⚠️ Không có userId, trả về danh sách xe rỗng");
            // Nếu chưa đăng nhập, trả về danh sách rỗng
            vehicles = List.of();
        }
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("currentUserId", userId);

        if (!vehicles.isEmpty()) {
            Long vehicleId = ((Number) vehicles.get(0).get("vehicleId")).longValue();
            model.addAttribute("selectedVehicleId", vehicleId);

            Map<String, Object> selectedVehicle = vehicles.stream()
                    .filter(v -> ((Number) v.get("vehicleId")).longValue() == vehicleId)
                    .findFirst()
                    .orElse(null);
            model.addAttribute("selectedVehicle", selectedVehicle);

            model.addAttribute("reservations", reservationService.getReservationsByVehicleId(vehicleId.intValue()));
            
            // Lấy thông tin nhóm sở hữu
            Map<String, Object> groupInfo = vehicleService.getVehicleGroupInfo(vehicleId);
            model.addAttribute("groupInfo", groupInfo);
        } else {
            model.addAttribute("reservations", List.of());
            model.addAttribute("selectedVehicleId", null);
            model.addAttribute("selectedVehicle", null);
            model.addAttribute("groupInfo", Map.of());
        }

        return "booking-form";
    }

    // ✅ Khi chọn xe khác
    @GetMapping("/book/{vehicleId}")
    public String showBookingFormForVehicle(
            @PathVariable("vehicleId") Long vehicleId, 
            Model model,
            @RequestParam(value = "userId", required = false) Long userIdFromParam,
            @CookieValue(value = "userId", required = false) Long userIdFromCookie) {
        
        // Lấy userId từ parameter hoặc cookie
        Long userId = userIdFromParam != null ? userIdFromParam : userIdFromCookie;
        
        // Chỉ lấy xe của user đăng nhập
        List<Map<String, Object>> vehicles;
        if (userId != null) {
            logger.info("🔍 Lấy danh sách xe cho user ID: {} (khi chọn xe {})", userId, vehicleId);
            vehicles = vehicleService.getUserVehicles(userId);
            logger.info("✅ Tìm thấy {} xe cho user", vehicles.size());
        } else {
            logger.warn("⚠️ Không có userId, trả về danh sách xe rỗng");
            vehicles = List.of();
        }
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("selectedVehicleId", vehicleId);

        Map<String, Object> selectedVehicle = vehicles.stream()
                .filter(v -> ((Number) v.get("vehicleId")).longValue() == vehicleId)
                .findFirst()
                .orElse(null);
        model.addAttribute("selectedVehicle", selectedVehicle);

        model.addAttribute("reservations", reservationService.getReservationsByVehicleId(vehicleId.intValue()));
        
        // Lấy thông tin nhóm sở hữu
        Map<String, Object> groupInfo = vehicleService.getVehicleGroupInfo(vehicleId);
        model.addAttribute("groupInfo", groupInfo);
        
        // Flash attribute 'success' sẽ tự động được thêm vào model nếu có
        return "booking-form";
    }

    // ✅ API endpoint để refresh reservations (trả về JSON)
    @GetMapping("/book/{vehicleId}/refresh")
    @ResponseBody
    public List<Map<String, Object>> refreshReservations(
            @PathVariable("vehicleId") Long vehicleId) {
        logger.info("🔄 Refreshing reservations for vehicle: {}", vehicleId);
        List<Map<String, Object>> reservations = reservationService.getReservationsByVehicleId(vehicleId.intValue());
        logger.info("✅ Returning {} reservations", reservations != null ? reservations.size() : 0);
        return reservations != null ? reservations : List.of();
    }

    // ✅ Khi người dùng submit form đặt lịch
    @PostMapping("/book")
    public String createReservation(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "note", required = false) String note,
            @CookieValue(value = "userId", required = false) Long userIdFromCookie,
            @RequestParam(value = "userId", required = false) Long userIdFromForm,
            Model model
    ) {
        logger.info("🔥 POST /reservations/book - vehicleId={}, startDate={}, endDate={}", vehicleId, startDate, endDate);
        
        try {
            // Lấy userId theo thứ tự ưu tiên: form > cookie
            Long userId = userIdFromForm != null ? userIdFromForm : userIdFromCookie;
            logger.info("🔥 userId from form={}, from cookie={}, final={}", userIdFromForm, userIdFromCookie, userId);
            
            if (userId == null) {
                logger.warn("⚠️ No userId found, returning error");
                model.addAttribute("error", "❌ Vui lòng đăng nhập để đặt lịch");
                model.addAttribute("vehicles", List.of());
                model.addAttribute("selectedVehicleId", vehicleId);
                model.addAttribute("reservations", reservationService.getReservationsByVehicleId(vehicleId.intValue()));
                model.addAttribute("groupInfo", Map.of());
                return "booking-form";
            }

            // 🔹 Gửi body tới ReservationService (8081)
            Map<String, Object> newReservation = Map.of(
                    "vehicleId", vehicleId,
                    "userId", userId,
                    "startDate", startDate,
                    "endDate", endDate,
                    "note", note != null ? note : ""
            );

            reservationService.createReservation(newReservation);
            logger.info("✅ Reservation created successfully");

            // ✅ Thêm thông báo thành công (không redirect, hiện modal)
            model.addAttribute("showSuccessModal", true);
            model.addAttribute("successMessage", "Đặt lịch thành công!");
            logger.info("🔥 Added showSuccessModal=true to model");

            // ✅ Tải lại form với xe đã chọn
            List<Map<String, Object>> vehicles;
            if (userId != null) {
                vehicles = vehicleService.getUserVehicles(userId);
            } else {
                vehicles = List.of();
            }
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("selectedVehicleId", vehicleId);

            Map<String, Object> selectedVehicle = vehicles.stream()
                    .filter(v -> ((Number) v.get("vehicleId")).longValue() == vehicleId)
                    .findFirst()
                    .orElse(null);
            model.addAttribute("selectedVehicle", selectedVehicle);

            model.addAttribute("reservations", reservationService.getReservationsByVehicleId(vehicleId.intValue()));
            
            // Lấy thông tin nhóm sở hữu
            Map<String, Object> groupInfo = vehicleService.getVehicleGroupInfo(vehicleId);
            model.addAttribute("groupInfo", groupInfo);
            
            logger.info("🔥 Returning booking-form template");
            return "booking-form";

        } catch (Exception e) {
            logger.error("❌ Lỗi khi đặt lịch: {}", e.getMessage(), e);
            
            // Làm đẹp thông báo lỗi
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";
            }
            
            // Kiểm tra errorType từ message (format: errorType:message)
            boolean showErrorModal = false;
            String errorType = "general";
            String overlapDetails = null; // Chi tiết về lịch đặt trùng
            
            if (errorMessage.contains(":")) {
                String[] parts = errorMessage.split(":", 2);
                if (parts.length == 2) {
                    errorType = parts[0].trim();
                    String messageContent = parts[1].trim();
                    
                    // Nếu là lỗi overlap, parse thông tin chi tiết
                    if (errorType.equals("overlap")) {
                        overlapDetails = messageContent;
                        logger.info("🔍 Parsing overlap details: {}", overlapDetails);
                        // Format lại thông báo để hiển thị đẹp hơn
                        if (overlapDetails.contains("|")) {
                            String[] details = overlapDetails.split("\\|");
                            StringBuilder formattedMessage = new StringBuilder();
                            formattedMessage.append("<div style='text-align: center; margin-bottom: 15px;'><strong style='color: #dc2626; font-size: 16px;'>⚠️ Thời gian đặt lịch bị trùng!</strong></div>");
                            formattedMessage.append("<div style='padding: 10px 0; border-top: 1px solid #fecaca;'>");
                            for (String detail : details) {
                                detail = detail.trim();
                                if (detail.startsWith("Người đặt:")) {
                                    formattedMessage.append("<div style='margin-bottom: 12px; padding: 8px; background: white; border-radius: 6px;'><i class='bi bi-person-fill' style='color: #667eea; margin-right: 8px;'></i><strong>").append(detail).append("</strong></div>");
                                } else if (detail.startsWith("Thời gian:")) {
                                    // Format lại thời gian cho đẹp hơn
                                    String timeInfo = detail.substring(11).trim();
                                    formattedMessage.append("<div style='margin-bottom: 12px; padding: 8px; background: white; border-radius: 6px;'><i class='bi bi-calendar3' style='color: #10b981; margin-right: 8px;'></i><strong>Thời gian: ").append(timeInfo).append("</strong></div>");
                                } else if (detail.startsWith("Lý do:")) {
                                    formattedMessage.append("<div style='margin-bottom: 12px; padding: 8px; background: white; border-radius: 6px;'><i class='bi bi-chat-left-text' style='color: #f59e0b; margin-right: 8px;'></i><strong>").append(detail).append("</strong></div>");
                                }
                            }
                            formattedMessage.append("</div>");
                            errorMessage = formattedMessage.toString();
                            logger.info("✅ Formatted error message: {}", errorMessage);
                        } else {
                            errorMessage = "<div style='text-align: center;'><strong style='color: #dc2626;'>⚠️ " + overlapDetails + "</strong></div>";
                        }
                    } else {
                        errorMessage = messageContent;
                    }
                    
                    // Các loại lỗi nên hiển thị modal thất bại
                    if (errorType.equals("overlap") || errorType.equals("server") || errorType.equals("validation")) {
                        showErrorModal = true;
                    }
                }
            }
            
            // Kiểm tra thêm nếu không có prefix errorType
            if (!showErrorModal && (errorMessage.contains("overlap") || errorMessage.contains("trùng") || 
                errorMessage.contains("Time range overlaps") || errorMessage.contains("500"))) {
                if (errorMessage.contains("overlap") || errorMessage.contains("trùng") || errorMessage.contains("Time range overlaps")) {
                    errorMessage = "Thời gian đặt lịch bị trùng với lịch đã có. Vui lòng chọn thời gian khác.";
                } else if (errorMessage.contains("500")) {
                    errorMessage = "Không thể đặt lịch. Vui lòng kiểm tra lại thông tin hoặc thử lại sau.";
                }
                showErrorModal = true;
            }
            
            // Loại bỏ các thông tin kỹ thuật không cần thiết (chỉ nếu không phải overlap với chi tiết)
            if (!errorType.equals("overlap") || overlapDetails == null) {
                if (errorMessage.contains("(") && errorMessage.contains(")")) {
                    int lastParen = errorMessage.lastIndexOf("(");
                    if (lastParen > 0) {
                        errorMessage = errorMessage.substring(0, lastParen).trim();
                    }
                }
                
                // Loại bỏ các ký tự đặc biệt và thông tin kỹ thuật
                errorMessage = errorMessage.replace("❌", "").trim();
                errorMessage = errorMessage.replaceAll("\\d+ on POST request for.*", "").trim();
                errorMessage = errorMessage.replaceAll("\"timestamp\".*", "").trim();
            }
            
            // Hiển thị modal thất bại hoặc alert
            if (showErrorModal) {
                model.addAttribute("showErrorModal", true);
                model.addAttribute("errorMessage", errorMessage);
            } else {
                model.addAttribute("error", errorMessage);
                model.addAttribute("errorType", "danger");
            }

            // Tải lại form có lỗi
            // Lấy lại userId từ request
            Long userIdForError = userIdFromForm != null ? userIdFromForm : userIdFromCookie;
            List<Map<String, Object>> vehicles;
            if (userIdForError != null) {
                vehicles = vehicleService.getUserVehicles(userIdForError);
            } else {
                vehicles = List.of();
            }
            model.addAttribute("vehicles", vehicles);
            model.addAttribute("selectedVehicleId", vehicleId);

            Map<String, Object> selectedVehicle = vehicles.stream()
                    .filter(v -> ((Number) v.get("vehicleId")).longValue() == vehicleId)
                    .findFirst()
                    .orElse(null);
            model.addAttribute("selectedVehicle", selectedVehicle);

            model.addAttribute("reservations", reservationService.getReservationsByVehicleId(vehicleId.intValue()));
            
            // Lấy thông tin nhóm sở hữu
            Map<String, Object> groupInfo = vehicleService.getVehicleGroupInfo(vehicleId);
            model.addAttribute("groupInfo", groupInfo);
            
            return "booking-form";
        }
    }
}

