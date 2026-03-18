package com.example.ui_service.external.controller;

import com.example.ui_service.external.model.VehiclegroupDTO;
import com.example.ui_service.external.service.VehicleGroupRestClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ext/admin/staff-management")
public class StaffManagementController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final VehicleGroupRestClient vehicleGroupRestClient;

    public StaffManagementController(VehicleGroupRestClient vehicleGroupRestClient) {
        this.vehicleGroupRestClient = vehicleGroupRestClient;
    }

    @GetMapping
    public String staffManagementPage(Model model,
                                      @RequestParam(value = "searchQuery", required = false, defaultValue = "") String searchQuery,
                                      @RequestParam(value = "statusFilter", required = false, defaultValue = "Tất cả") String statusFilter,
                                      @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                      @RequestParam(value = "deleteGroupId", required = false) String deleteGroupId,
                                      @RequestParam(value = "viewGroupId", required = false) String viewGroupId) {

        handleDeletion(model, deleteGroupId);

        List<VehiclegroupDTO> allGroups = vehicleGroupRestClient.getAllVehicleGroups();
        long totalGroups = allGroups.size();
        long activeGroups = allGroups.stream()
                .filter(group -> "active".equalsIgnoreCase(defaultString(group.getActive())))
                .count();

        List<VehiclegroupDTO> filteredGroups = allGroups.stream()
                .filter(group -> matchesSearch(group, searchQuery))
                .filter(group -> matchesStatus(group, statusFilter))
                .sorted(Comparator.comparing(
                        (VehiclegroupDTO group) -> defaultString(group.getName()).toLowerCase(Locale.ROOT))
                        .thenComparing(group -> defaultString(group.getGroupId()).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        int totalFiltered = filteredGroups.size();
        int totalPages = totalFiltered == 0 ? 0 : (int) Math.ceil((double) totalFiltered / DEFAULT_PAGE_SIZE);
        int currentPage = adjustPage(page, totalPages);
        int startIndex = totalFiltered == 0 ? 0 : (currentPage - 1) * DEFAULT_PAGE_SIZE;
        int endIndex = totalFiltered == 0 ? 0 : Math.min(startIndex + DEFAULT_PAGE_SIZE, totalFiltered);

        List<VehiclegroupDTO> paginatedGroups = totalFiltered == 0
                ? List.of()
                : new ArrayList<>(filteredGroups.subList(startIndex, endIndex));

        model.addAttribute("pageTitle", "Quản Lý Nhóm Xe Đồng Sở Hữu");
        model.addAttribute("pageDescription", "Quản lý thông tin nhóm xe");
        model.addAttribute("totalGroups", totalGroups);
        model.addAttribute("activeGroups", activeGroups);
        model.addAttribute("filteredGroups", paginatedGroups);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("totalFiltered", totalFiltered);
        model.addAttribute("currentPage", currentPage == 0 ? 1 : currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startIndex", totalFiltered == 0 ? 0 : startIndex + 1);
        model.addAttribute("endIndex", endIndex);

        if (totalFiltered == 0) {
            model.addAttribute("noDataMessage", "Không tìm thấy nhóm xe phù hợp.");
        }

        if (StringUtils.hasText(viewGroupId)) {
            populateViewGroup(model, viewGroupId);
        }

        return "ext/admin/staff-management";
    }

    private void handleDeletion(Model model, String deleteGroupId) {
        if (!StringUtils.hasText(deleteGroupId)) {
            return;
        }
        VehicleGroupRestClient.DeleteResult result = vehicleGroupRestClient.deleteVehicleGroup(deleteGroupId);
        model.addAttribute("deleteSuccess", result.success());
        model.addAttribute("deleteStatusMessage", result.message());
    }

    private void populateViewGroup(Model model, String viewGroupId) {
        VehiclegroupDTO viewGroup = vehicleGroupRestClient.getVehicleGroupById(viewGroupId);
        if (viewGroup == null) {
            model.addAttribute("deleteSuccess", false);
            model.addAttribute("deleteStatusMessage", "Không tìm thấy nhóm xe với ID: " + viewGroupId);
            return;
        }

        List<Map<String, Object>> vehicles = vehicleGroupRestClient.getVehiclesByGroupId(viewGroupId).stream()
                .map(this::normalizeVehicle)
                .collect(Collectors.toList());

        model.addAttribute("viewGroup", viewGroup);
        model.addAttribute("viewGroupVehicles", vehicles);
        model.addAttribute("showViewModal", true);
    }

    private Map<String, Object> normalizeVehicle(Map<String, Object> rawVehicle) {
        return Map.of(
                "vehicleId", defaultString(rawVehicle.get("vehicleId")),
                "vehicleType", defaultString(rawVehicle.getOrDefault("vehicleType", rawVehicle.get("type"))),
                "vehicleNumber", defaultString(rawVehicle.getOrDefault("vehicleNumber", rawVehicle.get("plateNumber"))),
                "status", defaultString(rawVehicle.get("status"))
        );
    }

    private int adjustPage(int requestedPage, int totalPages) {
        if (totalPages == 0) {
            return 0;
        }
        if (requestedPage < 1) {
            return 1;
        }
        return Math.min(requestedPage, totalPages);
    }

    private boolean matchesSearch(VehiclegroupDTO group, String searchQuery) {
        if (!StringUtils.hasText(searchQuery)) {
            return true;
        }
        String keyword = normalize(searchQuery);
        return normalize(group.getGroupId()).contains(keyword)
                || normalize(group.getName()).contains(keyword)
                || normalize(group.getDescription()).contains(keyword);
    }

    private boolean matchesStatus(VehiclegroupDTO group, String statusFilter) {
        if (!StringUtils.hasText(statusFilter)) {
            return true;
        }
        String normalizedFilter = statusFilter.trim();
        if ("Tất cả".equalsIgnoreCase(normalizedFilter) || "all".equalsIgnoreCase(normalizedFilter)) {
            return true;
        }
        return normalizedFilter.equalsIgnoreCase(defaultString(group.getActive()));
    }

    private String normalize(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String defaultString(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        return text != null ? text.trim() : "";
    }
}

