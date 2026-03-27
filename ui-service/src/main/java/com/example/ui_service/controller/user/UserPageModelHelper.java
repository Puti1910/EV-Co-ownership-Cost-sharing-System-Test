package com.example.ui_service.controller.user;

import com.example.ui_service.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

/**
 * Utility class for populating common user dashboard attributes.
 */
final class UserPageModelHelper {

    private UserPageModelHelper() {
    }

    static void populateCommonAttributes(Authentication authentication, Model model) {
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof AuthenticatedUser user) {
            model.addAttribute("userId", user.getUserId());
            model.addAttribute("userName", "User #" + user.getUserId());
            model.addAttribute("userEmail", user.getEmail());
        } else {
            model.addAttribute("userId", null);
            model.addAttribute("userName", "Người dùng");
            model.addAttribute("userEmail", "");
        }
    }
}

