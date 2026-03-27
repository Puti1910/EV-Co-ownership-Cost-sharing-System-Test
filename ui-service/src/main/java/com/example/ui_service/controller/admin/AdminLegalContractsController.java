package com.example.ui_service.controller.admin;

import com.example.ui_service.external.model.LegalContractDTO;
import com.example.ui_service.external.service.LegalContractRestClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/legal-contracts")
public class AdminLegalContractsController {

    private final LegalContractRestClient legalContractRestClient;

    public AdminLegalContractsController(LegalContractRestClient legalContractRestClient) {
        this.legalContractRestClient = legalContractRestClient;
    }

    @GetMapping
    public String legalContractsPage(
            Model model,
            @RequestParam(value = "searchQuery", required = false, defaultValue = "") String searchQuery,
            @RequestParam(value = "statusFilter", required = false, defaultValue = "all") String statusFilter,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        
        model.addAttribute("pageTitle", "Quản lý hợp đồng pháp lý điện tử");
        model.addAttribute("pageSubtitle", "Quản lý và theo dõi các hợp đồng pháp lý điện tử trong hệ thống");
        model.addAttribute("activePage", "legal-contracts");

        System.out.println("🔵 [AdminLegalContractsController] Loading contracts page...");
        List<LegalContractDTO> allContracts = legalContractRestClient.getAllContracts();
        System.out.println("✅ [AdminLegalContractsController] Loaded " + allContracts.size() + " contracts");

        List<LegalContractDTO> filteredContracts = allContracts.stream()
                .filter(contract -> {
                    boolean matchesSearch = searchQuery.isEmpty()
                            || (contract.getContractCode() != null && contract.getContractCode().toLowerCase().contains(searchQuery.toLowerCase()))
                            || (contract.getContractId() != null && contract.getContractId().toString().contains(searchQuery));

                    boolean matchesStatus = "all".equals(statusFilter)
                            || (contract.getContractStatus() != null && contract.getContractStatus().equalsIgnoreCase(statusFilter));

                    return matchesSearch && matchesStatus;
                })
                .collect(Collectors.toList());

        long totalContracts = allContracts.size();
        long pendingContracts = allContracts.stream().filter(c -> "pending".equalsIgnoreCase(c.getContractStatus())).count();
        long signedContracts = allContracts.stream().filter(c -> "signed".equalsIgnoreCase(c.getContractStatus())).count();
        long archivedContracts = allContracts.stream().filter(c -> "archived".equalsIgnoreCase(c.getContractStatus())).count();

        int totalPages = filteredContracts.isEmpty() ? 1 : (int) Math.ceil((double) filteredContracts.size() / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredContracts.size());
        List<LegalContractDTO> pagedContracts = filteredContracts.isEmpty()
                ? filteredContracts : filteredContracts.subList(startIndex, endIndex);

        model.addAttribute("contracts", pagedContracts);
        model.addAttribute("totalContracts", totalContracts);
        model.addAttribute("pendingContracts", pendingContracts);
        model.addAttribute("signedContracts", signedContracts);
        model.addAttribute("archivedContracts", archivedContracts);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalFiltered", filteredContracts.size());
        model.addAttribute("startIndex", filteredContracts.isEmpty() ? 0 : startIndex + 1);
        model.addAttribute("endIndex", endIndex);

        return "admin-legal-contracts";
    }

    @GetMapping("/edit/{contractId}")
    public String editContractPage(@PathVariable Integer contractId, Model model) {
        model.addAttribute("pageTitle", "Chỉnh sửa hợp đồng");
        model.addAttribute("pageSubtitle", "Cập nhật thông tin hợp đồng pháp lý điện tử");
        model.addAttribute("activePage", "legal-contracts");
        
        try {
            LegalContractDTO contract = legalContractRestClient.getContractById(contractId);
            if (contract == null) {
                model.addAttribute("error", "Không tìm thấy hợp đồng với ID: " + contractId);
                return "admin-legal-contracts-edit";
            }
            model.addAttribute("contract", contract);
            return "admin-legal-contracts-edit";
        } catch (Exception e) {
            System.err.println("❌ [AdminLegalContractsController] Error loading edit page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Đã xảy ra lỗi khi tải thông tin hợp đồng: " + e.getMessage());
            return "admin-legal-contracts-edit";
        }
    }

    @GetMapping("/api/{contractId}")
    @ResponseBody
    public Map<String, Object> getContractDetails(@PathVariable Integer contractId) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("🔵 [AdminLegalContractsController] Getting contract details for ID: " + contractId);
            LegalContractDTO contract = legalContractRestClient.getContractById(contractId);
            if (contract == null) {
                System.err.println("❌ [AdminLegalContractsController] Contract not found: " + contractId);
                response.put("error", "Không tìm thấy hợp đồng với ID: " + contractId);
                return response;
            }
            System.out.println("✅ [AdminLegalContractsController] Contract found: " + contract.getContractCode());
            
            List<Map<String, Object>> history = legalContractRestClient.getContractHistory(contractId);
            System.out.println("✅ [AdminLegalContractsController] History found: " + (history != null ? history.size() : 0) + " entries");
            
            // Convert contract to map for JSON serialization
            Map<String, Object> contractMap = new HashMap<>();
            contractMap.put("contractId", contract.getContractId());
            contractMap.put("contractCode", contract.getContractCode());
            contractMap.put("groupId", contract.getGroupId());
            contractMap.put("contractStatus", contract.getContractStatus());
            contractMap.put("creationDate", contract.getCreationDate() != null ? contract.getCreationDate().toString() : null);
            contractMap.put("signedDate", contract.getSignedDate() != null ? contract.getSignedDate().toString() : null);
            
            response.put("contract", contractMap);
            response.put("history", history != null ? history : Collections.emptyList());
            response.put("success", true);
            return response;
        } catch (Exception e) {
            System.err.println("❌ [AdminLegalContractsController] Error getting contract details: " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Đã xảy ra lỗi khi lấy thông tin hợp đồng: " + e.getMessage());
            response.put("success", false);
            return response;
        }
    }

    @PostMapping("/api/create")
    @ResponseBody
    public Map<String, Object> createContract(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("🔵 [AdminLegalContractsController] Creating contract with data: " + requestData);
            LegalContractDTO created = legalContractRestClient.createContract(requestData);
            if (created != null) {
                System.out.println("✅ [AdminLegalContractsController] Contract created successfully: " + created.getContractId());
                response.put("success", true);
                response.put("message", "Tạo hợp đồng thành công!");
                response.put("data", created);
            } else {
                System.err.println("❌ [AdminLegalContractsController] Failed to create contract - returned null");
                response.put("success", false);
                response.put("message", "Không thể tạo hợp đồng. Vui lòng kiểm tra lại thông tin và thử lại.");
            }
        } catch (Exception e) {
            System.err.println("❌ [AdminLegalContractsController] Error creating contract: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi khi tạo hợp đồng: " + e.getMessage());
        }
        return response;
    }

    @PutMapping("/api/update/{contractId}")
    @ResponseBody
    public Map<String, Object> updateContract(@PathVariable Integer contractId,
                                             @RequestBody Map<String, Object> contractData) {
        Map<String, Object> response = new HashMap<>();
        LegalContractDTO updated = legalContractRestClient.updateContract(contractId, contractData);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Cập nhật hợp đồng thành công!");
            response.put("data", updated);
        } else {
            response.put("success", false);
            response.put("message", "Không thể cập nhật hợp đồng. Vui lòng thử lại.");
        }
        return response;
    }

    @PutMapping("/api/sign/{contractId}")
    @ResponseBody
    public Map<String, Object> signContract(@PathVariable Integer contractId) {
        Map<String, Object> response = new HashMap<>();
        LegalContractDTO signed = legalContractRestClient.signContract(contractId);
        if (signed != null) {
            response.put("success", true);
            response.put("message", "Ký hợp đồng thành công!");
            response.put("data", signed);
        } else {
            response.put("success", false);
            response.put("message", "Không thể ký hợp đồng. Vui lòng thử lại.");
        }
        return response;
    }

    @PutMapping("/api/archive/{contractId}")
    @ResponseBody
    public Map<String, Object> archiveContract(@PathVariable Integer contractId) {
        Map<String, Object> response = new HashMap<>();
        LegalContractDTO archived = legalContractRestClient.archiveContract(contractId);
        if (archived != null) {
            response.put("success", true);
            response.put("message", "Lưu trữ hợp đồng thành công!");
            response.put("data", archived);
        } else {
            response.put("success", false);
            response.put("message", "Không thể lưu trữ hợp đồng. Vui lòng thử lại.");
        }
        return response;
    }

    @DeleteMapping("/api/delete/{contractId}")
    @ResponseBody
    public Map<String, Object> deleteContract(@PathVariable Integer contractId) {
        System.out.println("🔵 [DELETE CONTRACT] Contract ID: " + contractId);
        
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = legalContractRestClient.deleteContract(contractId);
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "Xóa hợp đồng thành công!");
                System.out.println("✅ [DELETE CONTRACT] Đã xóa hợp đồng: " + contractId);
            } else {
                response.put("success", false);
                response.put("message", "Không thể xóa hợp đồng. Vui lòng kiểm tra lại hợp đồng có tồn tại không.");
                System.err.println("❌ [DELETE CONTRACT] Không thể xóa hợp đồng: " + contractId);
            }
        } catch (Exception e) {
            System.err.println("❌ [DELETE CONTRACT] Lỗi: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Đã xảy ra lỗi khi xóa hợp đồng: " + e.getMessage());
            response.put("error", e.getMessage());
        }
        
        return response;
    }
}

