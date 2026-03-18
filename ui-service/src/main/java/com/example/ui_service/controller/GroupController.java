package com.example.ui_service.controller;

import com.example.ui_service.client.GroupManagementClient;
import com.example.ui_service.dto.GroupDto;
import com.example.ui_service.dto.GroupMemberDto;
import com.example.ui_service.dto.VoteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupManagementClient groupManagementClient;


    @GetMapping("/{id}/members")
    public String listGroupMembers(@PathVariable Integer id, Model model) {
        List<GroupMemberDto> members = groupManagementClient.getGroupMembers(id);
        model.addAttribute("members", members);
        model.addAttribute("groupId", id);
        return "groups/members";
    }

    @PostMapping("/{id}/members")
    public String addGroupMember(@PathVariable Integer id, @ModelAttribute GroupMemberDto memberDto) {
        groupManagementClient.addGroupMember(id, memberDto);
        return "redirect:/groups/" + id + "/members";
    }

    @GetMapping("/{id}/votes")
    public String listGroupVotes(@PathVariable Integer id, Model model) {
        List<VoteDto> votes = groupManagementClient.getGroupVotes(id);
        model.addAttribute("votes", votes);
        model.addAttribute("groupId", id);
        return "groups/votes";
    }

    @PostMapping("/{id}/votes")
    public String createVote(@PathVariable Integer id, @ModelAttribute VoteDto voteDto) {
        groupManagementClient.createVote(id, voteDto);
        return "redirect:/groups/" + id + "/votes";
    }

    @GetMapping("/api/members")
    @ResponseBody
    public List<GroupMemberDto> getMembersApi() {
        // Get members from the first group for demo purposes
        // In a real application, you might want to get members from a specific group
        List<GroupDto> groups = groupManagementClient.getAllGroups();
        if (!groups.isEmpty()) {
            return groupManagementClient.getGroupMembers(groups.get(0).getGroupId());
        }
        return List.of();
    }
    
    /**
     * API endpoint for frontend JavaScript to fetch all groups
     */
    @GetMapping("/api/all")
    @ResponseBody
    public List<GroupDto> getGroupsApi() {
        return groupManagementClient.getAllGroups();
    }
    
    /**
     * Delete a group
     */
    @PostMapping("/{id}/delete")
    @ResponseBody
    public String deleteGroup(@PathVariable Integer id) {
        try {
            groupManagementClient.deleteGroup(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    
}