package com.example.ui_service.controller.user;

import com.example.ui_service.client.GroupManagementClient;
import com.example.ui_service.external.service.DisputeRestClient;
import com.example.ui_service.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserDisputePageController {

    @Autowired
    private DisputeRestClient disputeRestClient;

    @Autowired
    private GroupManagementClient groupManagementClient;

    @GetMapping("/disputes")
    public String disputes(Authentication authentication,
                           Model model,
                           @RequestParam(value = "disputeSuccess", required = false) String disputeSuccess,
                           @RequestParam(value = "disputeError", required = false) String disputeError) {

        UserPageModelHelper.populateCommonAttributes(authentication, model);

        Integer currentUserId = extractUserId(authentication);
        List<Map<String, Object>> myDisputes =
            currentUserId != null ? disputeRestClient.getDisputesByCreator(currentUserId) : Collections.emptyList();

        Map<Integer, Map<String, Object>> resolutions = new HashMap<>();
        for (Map<String, Object> dispute : myDisputes) {
            Object disputeIdObj = dispute.get("disputeId");
            Object statusObj = dispute.get("status");
            if (disputeIdObj instanceof Number disputeIdNumber &&
                statusObj instanceof String status &&
                ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status))) {
                int disputeId = disputeIdNumber.intValue();
                Map<String, Object> resolution = disputeRestClient.getResolution(disputeId);
                if (resolution != null) {
                    resolutions.put(disputeId, resolution);
                }
            }
        }

        model.addAttribute("activePage", "disputes");
        model.addAttribute("pageCss", new String[]{"/css/user-dashboard.css"});
        model.addAttribute("pageJs", new String[]{"/js/user-dashboard.js"});
        model.addAttribute("myDisputes", myDisputes);
        model.addAttribute("disputeResolutions", resolutions);
        // Chỉ lấy nhóm mà user hiện tại là thành viên
        List<Map<String, Object>> userGroups = currentUserId != null 
            ? groupManagementClient.getGroupsByUserIdAsMap(currentUserId) 
            : Collections.emptyList();
        model.addAttribute("groups", userGroups);
        model.addAttribute("disputeSuccess", disputeSuccess);
        model.addAttribute("disputeError", disputeError);

        return "user-dashboard";
    }

    private Integer extractUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            Long userId = authenticatedUser.getUserId();
            return userId != null ? userId.intValue() : null;
        }
        return null;
    }
}


