package com.example.ui_service.controller.user;

import com.example.ui_service.client.GroupManagementClient;
import com.example.ui_service.external.model.LegalContractDTO;
import com.example.ui_service.external.service.LegalContractRestClient;
import com.example.ui_service.external.service.VehicleGroupRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Controller + lightweight API cho trang Quản lý Hợp đồng của User.
 * Trang này cho phép người dùng xem, lọc và ký hợp đồng trước khi tham gia nhóm xe.
 */
@Controller
@RequestMapping("/user")
public class UserContractsController {

    private final LegalContractRestClient legalContractRestClient;
    private final GroupManagementClient groupManagementClient;
    private final VehicleGroupRestClient vehicleGroupRestClient;

    public UserContractsController(LegalContractRestClient legalContractRestClient,
                                   GroupManagementClient groupManagementClient,
                                   VehicleGroupRestClient vehicleGroupRestClient) {
        this.legalContractRestClient = legalContractRestClient;
        this.groupManagementClient = groupManagementClient;
        this.vehicleGroupRestClient = vehicleGroupRestClient;
    }

    @GetMapping("/contracts")
    public String contracts(Authentication authentication, Model model) {
        UserPageModelHelper.populateCommonAttributes(authentication, model);
        model.addAttribute("activePage", "contracts");
        model.addAttribute("pageTitle", "Quản lý hợp đồng");
        model.addAttribute("pageSubtitle", "Ký hợp đồng để đủ điều kiện tham gia nhóm xe");
        return "user-dashboard";
    }

    /**
     * API trả về dữ liệu tổng hợp cho trang hợp đồng của user.
     * Logic mới: Hiển thị danh sách các nhóm cần ký hợp đồng.
     */
    @GetMapping("/contracts/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getContracts(Authentication authentication) {
        Integer userId = resolveUserId(authentication);

        // Lấy danh sách tất cả nhóm từ GroupManagementService
        List<Map<String, Object>> allGroups = groupManagementClient.getAllGroupsAsMap();
        if (allGroups == null) {
            allGroups = Collections.emptyList();
        }

        // Lấy danh sách hợp đồng đã có từ LegalContractService
        List<LegalContractDTO> existingContracts = Optional.ofNullable(legalContractRestClient.getAllContracts())
                .orElse(Collections.emptyList());

        // Tìm hợp đồng của user hiện tại trong mỗi nhóm
        // Ưu tiên tìm hợp đồng đã ký (thông qua ContractSignature)
        // Nếu không tìm thấy qua signature, kiểm tra contract code có chứa userId không
        Map<Integer, LegalContractDTO> userContractMap = new HashMap<>();
        for (LegalContractDTO contract : existingContracts) {
            if (contract != null && contract.getContractId() != null && contract.getGroupId() != null) {
                boolean isUserContract = false;
                
                // Cách 1: Kiểm tra qua ContractSignature (chính xác nhất)
                try {
                    List<Map<String, Object>> signatures = groupManagementClient.getContractSignatures(contract.getContractId());
                    if (signatures != null && !signatures.isEmpty()) {
                        for (Map<String, Object> signature : signatures) {
                            Object sigUserId = signature != null ? signature.get("userId") : null;
                            if (sigUserId != null) {
                                int sigUserIdInt = sigUserId instanceof Number 
                                    ? ((Number) sigUserId).intValue() 
                                    : Integer.parseInt(sigUserId.toString());
                                if (sigUserIdInt == userId) {
                                    isUserContract = true;
                                    break;
                                }
                            }
                        }
                    }
                } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                    // Contract chưa tồn tại trong GroupManagementService (404)
                    // Có thể là hợp đồng mới tạo, kiểm tra contract code
                    System.out.println("⚠️ Contract " + contract.getContractId() + " chưa có trong GroupManagementService, kiểm tra contract code");
                } catch (Exception e) {
                    // Bỏ qua lỗi khác
                    System.err.println("⚠️ Error checking signatures for contract " + contract.getContractId() + ": " + e.getMessage());
                }
                
                // Cách 2: Nếu không tìm thấy qua signature, kiểm tra contract code
                // Contract code format: LC-{GROUP_NAME}-{YEAR}-{USER_ID}-{RANDOM}
                if (!isUserContract && contract.getContractCode() != null) {
                    String contractCode = contract.getContractCode();
                    // Kiểm tra xem contract code có chứa userId không (format: ...-{USER_ID}-{RANDOM})
                    String[] parts = contractCode.split("-");
                    if (parts.length >= 4) {
                        try {
                            // Phần thứ 3 từ cuối (sau năm) là userId
                            String possibleUserId = parts[parts.length - 2];
                            int contractUserId = Integer.parseInt(possibleUserId);
                            if (contractUserId == userId) {
                                isUserContract = true;
                                System.out.println("✅ Tìm thấy hợp đồng của user qua contract code: " + contractCode);
                            }
                        } catch (NumberFormatException e) {
                            // Không phải format có userId, bỏ qua
                        }
                    }
                }
                
                // Nếu là hợp đồng của user, lưu vào map
                if (isUserContract) {
                    LegalContractDTO existingUserContract = userContractMap.get(contract.getGroupId());
                    if (existingUserContract == null || 
                        (contract.getCreationDate() != null && 
                         existingUserContract.getCreationDate() != null &&
                         contract.getCreationDate().isAfter(existingUserContract.getCreationDate()))) {
                        userContractMap.put(contract.getGroupId(), contract);
                    }
                }
            }
        }

        Set<Integer> joinedGroupIds = fetchJoinedGroupIds(userId);
        Map<Integer, List<Map<String, Object>>> vehicleCache = new HashMap<>();

        // Xây dựng danh sách view: mỗi nhóm sẽ có 1 entry (có hoặc chưa có hợp đồng)
        List<Map<String, Object>> contractViews = new ArrayList<>();
        for (Map<String, Object> group : allGroups) {
            if (group == null) {
                continue;
            }
            
            Integer groupId = extractGroupId(group);
            if (groupId == null) {
                continue;
            }

            // CHỈ lấy hợp đồng của user hiện tại trong nhóm này
            // Nếu user chưa có hợp đồng trong nhóm, contract sẽ là null
            LegalContractDTO userContract = userContractMap.get(groupId);
            Map<String, Object> view = buildGroupContractView(
                    group, userContract, vehicleCache, joinedGroupIds);
            contractViews.add(view);
        }

        Map<String, Object> stats = computeStats(contractViews);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("contracts", contractViews);
        response.put("stats", stats);
        response.put("filters", Map.of(
                "statuses", List.of(
                        Map.of("value", "all", "label", "Tất cả trạng thái"),
                        Map.of("value", "no-contract", "label", "Chờ tạo hợp đồng"),
                        Map.of("value", "pending", "label", "Chờ ký"),
                        Map.of("value", "signed", "label", "Đã ký"),
                        Map.of("value", "archived", "label", "Đã kết thúc")
                ),
                "vehicleTypes", extractVehicleTypes(contractViews)
        ));
        response.put("requiresSignatureBeforeJoin", true);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    private Integer extractGroupId(Map<String, Object> group) {
        Object id = group.get("groupId");
        if (id == null) {
            id = group.get("id");
        }
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        if (id != null) {
            try {
                return Integer.parseInt(id.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Map<String, Object> buildGroupContractView(
            Map<String, Object> group,
            LegalContractDTO contract,
            Map<Integer, List<Map<String, Object>>> vehicleCache,
            Set<Integer> joinedGroupIds) {

        Map<String, Object> view = new HashMap<>();
        Integer groupId = extractGroupId(group);

        // Thông tin nhóm
        String groupName = determineGroupName(group);
        String groupDescription = determineGroupDescription(group);
        view.put("groupId", groupId);
        view.put("groupName", groupName);
        view.put("groupDescription", groupDescription);
        view.put("groupStatus", group.getOrDefault("status", "unknown"));
        view.put("groupAdminId", group.getOrDefault("adminId", "-"));

        // Thông tin xe
        var vehicles = resolveVehicles(groupId, vehicleCache);
        Map<String, Object> primaryVehicle = vehicles.isEmpty() ? null : vehicles.get(0);
        view.put("vehicleName", primaryVehicle != null ? Objects.toString(primaryVehicle.getOrDefault("name", primaryVehicle.getOrDefault("vehicleName", "-")), "-") : "-");
        view.put("vehiclePlate", primaryVehicle != null ? Objects.toString(primaryVehicle.getOrDefault("vehicleNumber", "-"), "-") : "-");
        view.put("vehicleType", primaryVehicle != null ? Objects.toString(primaryVehicle.getOrDefault("vehicleType", primaryVehicle.getOrDefault("type", "-")), "-") : "-");
        view.put("vehicleId", primaryVehicle != null ? primaryVehicle.get("vehicleId") : null);

        // Thông tin hợp đồng
        if (contract != null) {
            // Đã có hợp đồng
            view.put("contractId", contract.getContractId());
            view.put("contractCode", contract.getContractCode());
            view.put("hasContract", true);
            
            String status = normalizeStatus(contract.getContractStatus());
            view.put("status", status);
            view.put("statusLabel", mapStatusToLabel(status));
            view.put("statusBadge", mapStatusToBadge(status));
            view.put("isSigned", "signed".equals(status));
            view.put("requiresSignature", !"signed".equals(status));
            
            view.put("creationDate", formatInstant(contract.getCreationDate()));
            view.put("signedDate", formatInstant(contract.getSignedDate()));
        } else {
            // Chưa có hợp đồng
            view.put("contractId", null);
            view.put("contractCode", null);
            view.put("hasContract", false);
            view.put("status", "no-contract");
            view.put("statusLabel", "Chờ tạo hợp đồng");
            view.put("statusBadge", "status-pending");
            view.put("isSigned", false);
            view.put("requiresSignature", true);
            view.put("creationDate", null);
            view.put("signedDate", null);
        }

        boolean alreadyJoined = groupId != null && joinedGroupIds.contains(groupId);
        view.put("alreadyJoined", alreadyJoined);
        view.put("canJoinGroup", !alreadyJoined && contract != null && "signed".equals(normalizeStatus(contract.getContractStatus())));

        return view;
    }

    /**
     * API cho phép user ký hợp đồng.
     * Nếu chưa có hợp đồng, sẽ tạo mới với contract_code tự động.
     * Nếu đã có hợp đồng, sẽ ký hợp đồng đó.
     */
    @PutMapping("/contracts/api/{groupId}/sign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> signContract(@PathVariable Integer groupId,
                                                            Authentication authentication) {
        if (groupId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Thiếu groupId"));
        }
        
        Integer userId = resolveUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Không xác định được người dùng"));
        }

        // Kiểm tra xem nhóm có tồn tại không
        Map<String, Object> groupInfo = groupManagementClient.getGroupByIdAsMap(groupId);
        if (groupInfo == null || groupInfo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Không tìm thấy nhóm"));
        }

        // Lấy tên nhóm để tạo contract_code
        String groupName = determineGroupName(groupInfo);
        String normalizedGroupName = groupName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalizedGroupName.isEmpty()) {
            normalizedGroupName = "GROUP";
        }

        // Tìm hợp đồng của user hiện tại trong nhóm này
        // CHỈ ký hợp đồng của chính user, không phải hợp đồng của user khác
        List<LegalContractDTO> existingContracts = Optional.ofNullable(
                legalContractRestClient.getContractsByGroupId(groupId))
                .orElse(Collections.emptyList());
        
        LegalContractDTO contractToSign = null;
        
        // Tìm hợp đồng mà user đã ký (thông qua ContractSignature)
        for (LegalContractDTO contract : existingContracts) {
            if (contract != null && contract.getContractId() != null) {
                try {
                    List<Map<String, Object>> signatures = groupManagementClient.getContractSignatures(contract.getContractId());
                    if (signatures != null) {
                        for (Map<String, Object> signature : signatures) {
                            Object sigUserId = signature != null ? signature.get("userId") : null;
                            if (sigUserId != null) {
                                int sigUserIdInt = sigUserId instanceof Number 
                                    ? ((Number) sigUserId).intValue() 
                                    : Integer.parseInt(sigUserId.toString());
                                if (sigUserIdInt == userId) {
                                    // Tìm thấy hợp đồng của user
                                    contractToSign = contract;
                                    break;
                                }
                            }
                        }
                        if (contractToSign != null) {
                            break; // Đã tìm thấy, không cần tìm tiếp
                        }
                    }
                } catch (Exception e) {
                    // Bỏ qua lỗi khi kiểm tra signatures
                    System.err.println("⚠️ Error checking signatures for contract " + contract.getContractId() + ": " + e.getMessage());
                }
            }
        }
        
        // Nếu user đã có hợp đồng, kiểm tra trạng thái và đảm bảo đã đồng bộ với GroupManagementService
        if (contractToSign != null) {
            String currentStatus = normalizeStatus(contractToSign.getContractStatus());
            if ("signed".equals(currentStatus)) {
                // Hợp đồng đã được ký rồi
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, 
                            "message", "Bạn đã ký hợp đồng này rồi. Contract code: " + contractToSign.getContractCode()));
            }
            
            // Đảm bảo contract đã được đồng bộ với GroupManagementService
            try {
                // Thử lấy signatures để kiểm tra contract có tồn tại trong GroupManagementService không
                groupManagementClient.getContractSignatures(contractToSign.getContractId());
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                // Contract chưa có trong GroupManagementService, cần đồng bộ
                System.out.println("⚠️ Contract " + contractToSign.getContractId() + " chưa có trong GroupManagementService, đang đồng bộ...");
                try {
                    Map<String, Object> groupContractData = new HashMap<>();
                    groupContractData.put("contractId", contractToSign.getContractId());
                    groupContractData.put("contractCode", contractToSign.getContractCode());
                    groupContractData.put("groupId", groupId);
                    groupContractData.put("contractStatus", currentStatus);
                    groupManagementClient.createContractAsMap(groupId, groupContractData);
                    System.out.println("✅ Đã đồng bộ hợp đồng " + contractToSign.getContractId() + " sang GroupManagementService");
                } catch (Exception syncError) {
                    System.err.println("⚠️ Không thể đồng bộ hợp đồng sang GroupManagementService: " + syncError.getMessage());
                }
            } catch (Exception e) {
                // Bỏ qua lỗi khác
                System.err.println("⚠️ Error checking contract in GroupManagementService: " + e.getMessage());
            }
        }

        LegalContractDTO result;
        if (contractToSign == null) {
            // User chưa có hợp đồng trong nhóm này, tạo mới với contract_code tự động
            // Contract code sẽ unique vì có userId và random string
            String contractCode = generateContractCode(normalizedGroupName, groupId, userId);
            
            Map<String, Object> contractData = new HashMap<>();
            contractData.put("groupId", groupId);
            contractData.put("contractCode", contractCode);
            contractData.put("contractStatus", "pending");
            contractData.put("contractContent", "Hợp đồng sở hữu chung nhóm xe: " + groupName);
            
            // Tạo hợp đồng trong LegalContractService
            LegalContractDTO newContract = legalContractRestClient.createContract(contractData);
            if (newContract == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("success", false, "message", "Không thể tạo hợp đồng. Vui lòng thử lại sau."));
            }
            contractToSign = newContract;
            
            // Đồng bộ hợp đồng sang GroupManagementService để có thể quản lý signatures
            try {
                Map<String, Object> groupContractData = new HashMap<>();
                groupContractData.put("contractId", newContract.getContractId());
                groupContractData.put("contractCode", contractCode);
                groupContractData.put("groupId", groupId);
                groupContractData.put("contractStatus", "pending");
                groupManagementClient.createContractAsMap(groupId, groupContractData);
                System.out.println("✅ Đã đồng bộ hợp đồng " + newContract.getContractId() + " sang GroupManagementService");
            } catch (Exception syncError) {
                // Không chặn luồng chính nhưng ghi log
                System.err.println("⚠️ Không thể đồng bộ hợp đồng sang GroupManagementService: " + syncError.getMessage());
            }
        }

        // Ký hợp đồng thông qua LegalContractService
        result = legalContractRestClient.signContract(contractToSign.getContractId());
        
        // Đồng bộ trạng thái ký với GroupManagementService để cho phép self-join
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("signatureMethod", "ui-service");
            groupManagementClient.signContractAsMap(contractToSign.getContractId(), payload);
            System.out.println("✅ Đã tạo ContractSignature cho user " + userId + " và contract " + contractToSign.getContractId());
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // Contract chưa có trong GroupManagementService, cần tạo trước
            System.out.println("⚠️ Contract " + contractToSign.getContractId() + " chưa có trong GroupManagementService, đang tạo...");
            try {
                Map<String, Object> groupContractData = new HashMap<>();
                groupContractData.put("contractId", contractToSign.getContractId());
                groupContractData.put("contractCode", contractToSign.getContractCode());
                groupContractData.put("groupId", groupId);
                groupContractData.put("contractStatus", "signed");
                groupManagementClient.createContractAsMap(groupId, groupContractData);
                
                // Sau khi tạo contract, thử ký lại
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", userId);
                payload.put("signatureMethod", "ui-service");
                groupManagementClient.signContractAsMap(contractToSign.getContractId(), payload);
                System.out.println("✅ Đã tạo và ký hợp đồng trong GroupManagementService");
            } catch (Exception retryError) {
                System.err.println("⚠️ Không thể tạo/ký hợp đồng trong GroupManagementService: " + retryError.getMessage());
            }
        } catch (Exception syncError) {
            // Không chặn luồng chính nhưng ghi log để dễ truy vết
            System.err.println("⚠️ Không thể đồng bộ chữ ký với GroupManagementService: " + syncError.getMessage());
        }
        if (result == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("success", false, "message", "Không thể ký hợp đồng. Vui lòng thử lại sau."));
        }

        // Build view để trả về
        Map<String, Object> contractView = buildGroupContractView(
                groupInfo,
                result,
                new HashMap<>(),
                fetchJoinedGroupIds(userId));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã ký hợp đồng thành công. Contract code: " + result.getContractCode(),
                "contract", contractView
        ));
    }

    /**
     * Tạo contract_code tự động theo format: LC-{GROUP_NAME}-{YEAR}-{USER_ID}-{RANDOM}
     * Ví dụ: LC-ALPHA-2025-003-A7B2C9
     * Đảm bảo unique bằng cách thêm userId và random string
     * Mỗi user trong cùng nhóm sẽ có contract code riêng
     */
    private String generateContractCode(String groupName, Integer groupId, Integer userId) {
        int year = java.time.LocalDate.now().getYear();
        // Thêm userId để mỗi user có contract code riêng
        String userPart = userId != null ? String.format("%03d", userId) : "000";
        
        // Tạo random string 6 ký tự để đảm bảo unique
        String randomPart = java.util.UUID.randomUUID().toString()
            .replace("-", "")
            .substring(0, 6)
            .toUpperCase();
        
        // Giới hạn độ dài groupName để tránh contract code quá dài
        String shortGroupName = groupName.length() > 20 
            ? groupName.substring(0, 20) 
            : groupName;
        
        return String.format("LC-%s-%d-%s-%s", shortGroupName, year, userPart, randomPart);
    }

    private Integer resolveUserId(Authentication authentication) {
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof com.example.ui_service.security.AuthenticatedUser user) {
            Long id = user.getUserId();
            return id != null ? id.intValue() : null;
        }
        return null;
    }

    private Set<Integer> fetchJoinedGroupIds(Integer userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptySet();
        }
        try {
            List<Map<String, Object>> groups = groupManagementClient.getGroupsByUserIdAsMap(userId);
            if (groups == null || groups.isEmpty()) {
                return Collections.emptySet();
            }
            Set<Integer> result = new HashSet<>();
            for (Map<String, Object> group : groups) {
                if (group == null) {
                    continue;
                }
                
                Integer groupId = null;
                Object id = group.get("groupId");
                if (id instanceof Number number) {
                    groupId = number.intValue();
                } else if (id != null) {
                    try {
                        groupId = Integer.parseInt(id.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                
                if (groupId == null) {
                    continue;
                }

                boolean hasOwnershipFlag = false;
                Object hasOwnershipObj = group.get("hasOwnership");
                if (hasOwnershipObj instanceof Boolean b) {
                    hasOwnershipFlag = b;
                } else if (hasOwnershipObj != null) {
                    hasOwnershipFlag = Boolean.parseBoolean(hasOwnershipObj.toString());
                }

                Double ownershipPercent = null;
                Object percentObj = group.get("ownershipPercent");
                if (percentObj instanceof Number number) {
                    ownershipPercent = number.doubleValue();
                } else if (percentObj != null) {
                    try {
                        ownershipPercent = Double.parseDouble(percentObj.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }

                boolean treatAsJoined;
                if (hasOwnershipObj != null) {
                    treatAsJoined = hasOwnershipFlag;
                } else if (ownershipPercent != null) {
                    treatAsJoined = ownershipPercent > 0.0;
                } else {
                    // Backward compatibility: assume joined if service hasn't started sending metadata yet
                    treatAsJoined = true;
                }

                if (treatAsJoined) {
                    result.add(groupId);
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }


    private String determineGroupName(Map<String, Object> groupInfo) {
        if (groupInfo == null || groupInfo.isEmpty()) {
            return "Nhóm chưa xác định";
        }
        Object name = groupInfo.get("name");
        if (name == null || name.toString().isBlank()) {
            name = groupInfo.get("groupName");
        }
        if (name == null || name.toString().isBlank()) {
            name = groupInfo.get("group_name");
        }
        return name != null && !name.toString().isBlank()
                ? name.toString()
                : "Nhóm chưa xác định";
    }

    private String determineGroupDescription(Map<String, Object> groupInfo) {
        if (groupInfo == null || groupInfo.isEmpty()) {
            return "";
        }
        Object description = groupInfo.get("description");
        if (description == null || description.toString().isBlank()) {
            description = groupInfo.get("note");
        }
        return description != null ? description.toString() : "";
    }

    private List<Map<String, Object>> resolveVehicles(Integer groupId, Map<Integer, List<Map<String, Object>>> cache) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        return cache.computeIfAbsent(groupId, id -> {
            try {
                return vehicleGroupRestClient.getVehiclesByGroupId(id.toString());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        });
    }

    private Map<String, Object> computeStats(List<Map<String, Object>> contracts) {
        long total = contracts.size();
        long noContract = contracts.stream().filter(c -> "no-contract".equals(c.get("status"))).count();
        long pending = contracts.stream().filter(c -> "pending".equals(c.get("status"))).count();
        long active = contracts.stream().filter(c -> "signed".equals(c.get("status"))).count();
        long archived = contracts.stream().filter(c -> "archived".equals(c.get("status"))).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("noContract", noContract);
        stats.put("pending", pending);
        stats.put("active", active);
        stats.put("archived", archived);
        return stats;
    }

    private List<Map<String, String>> extractVehicleTypes(List<Map<String, Object>> contracts) {
        Set<String> types = new HashSet<>();
        for (Map<String, Object> contract : contracts) {
            Object type = contract.get("vehicleType");
            if (type != null) {
                String normalized = type.toString().trim();
                if (!normalized.isEmpty()) {
                    types.add(normalized);
                }
            }
        }
        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of("value", "all", "label", "Tất cả loại xe"));
        types.stream()
                .sorted()
                .forEach(type -> options.add(Map.of("value", type, "label", type)));
        return options;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "pending";
        }
        return status.trim().toLowerCase(Locale.ENGLISH);
    }

    private String mapStatusToLabel(String status) {
        return switch (status) {
            case "no-contract" -> "Chờ tạo hợp đồng";
            case "pending" -> "Chờ ký";
            case "signed" -> "Đã ký";
            case "archived" -> "Đã kết thúc";
            default -> "Chờ ký";
        };
    }

    private String mapStatusToBadge(String status) {
        return switch (status) {
            case "no-contract" -> "status-pending";
            case "pending" -> "status-pending";
            case "signed" -> "status-active";
            case "archived" -> "status-archived";
            default -> "status-pending";
        };
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(instant);
    }
}

