package com.example.ui_service.controller;

import com.example.ui_service.client.CostPaymentClient;
import com.example.ui_service.client.GroupManagementClient;
import com.example.ui_service.dto.CostDto;
import com.example.ui_service.dto.CostSplitDto;
import com.example.ui_service.dto.GroupDto;
import com.example.ui_service.dto.PaymentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/costs")
public class CostController {

    @Autowired
    private CostPaymentClient costPaymentClient;

    @Autowired
    private GroupManagementClient groupManagementClient;


    @GetMapping("/{id}/splits")
    public String listCostSplits(@PathVariable Integer id, Model model) {
        List<CostSplitDto> splits = costPaymentClient.getCostSplits(id);
        model.addAttribute("splits", splits);
        model.addAttribute("costId", id);
        return "costs/splits";
    }

    @PostMapping("/{id}/splits")
    public String createCostSplit(@PathVariable Integer id, @ModelAttribute CostSplitDto splitDto) {
        costPaymentClient.createCostSplit(id, splitDto);
        return "redirect:/costs/" + id + "/splits";
    }

    @GetMapping("/payments")
    public String listPayments(Model model,
                               @RequestParam(value = "disputeSuccess", required = false) String disputeSuccess,
                               @RequestParam(value = "disputeError", required = false) String disputeError) {
        List<PaymentDto> payments = costPaymentClient.getAllPayments();
        List<GroupDto> groups = groupManagementClient.getAllGroups();
        model.addAttribute("payments", payments);
        model.addAttribute("groups", groups);
        model.addAttribute("disputeSuccess", disputeSuccess);
        model.addAttribute("disputeError", disputeError);
        return "costs/payments";
    }

    @PostMapping("/payments")
    public String createPayment(@ModelAttribute PaymentDto paymentDto) {
        costPaymentClient.createPayment(paymentDto);
        return "redirect:/costs/payments";
    }


    @PostMapping("/{id}/delete")
    @ResponseBody
    public String deleteCost(@PathVariable Integer id) {
        try {
            boolean deleted = costPaymentClient.deleteCost(id);
            if (deleted) {
                return "success";
            } else {
                return "error";
            }
        } catch (Exception e) {
            System.err.println("Error deleting cost: " + e.getMessage());
            return "error";
        }
    }


    @PostMapping("/sharing")
    @ResponseBody
    public String createCostForSharing(@RequestBody CostDto costDto) {
        try {
            costPaymentClient.createCost(costDto);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    // Cost Sharing API endpoints
    @GetMapping("/api/shares")
    @ResponseBody
    public List<CostSplitDto> getAllCostSharesApi() {
        return costPaymentClient.getAllCostShares();
    }

    @GetMapping("/api/shares/{id}")
    @ResponseBody
    public CostSplitDto getCostShareApi(@PathVariable Integer id) {
        return costPaymentClient.getCostShareById(id);
    }


    @GetMapping("/api/costs/{costId}/shares")
    @ResponseBody
    public List<CostSplitDto> getCostSharesByCostIdApi(@PathVariable Integer costId) {
        return costPaymentClient.getCostSharesByCostId(costId);
    }

    @PostMapping("/api/costs/{costId}/calculate-shares")
    @ResponseBody
    public List<CostSplitDto> calculateCostSharesApi(@PathVariable Integer costId, 
                                                   @RequestBody CostPaymentClient.CostShareRequest request) {
        return costPaymentClient.calculateCostShares(costId, request.getUserIds(), request.getPercentages());
    }

    @PutMapping("/api/shares/{id}")
    @ResponseBody
    public String updateCostShareApi(@PathVariable Integer id, @RequestBody CostSplitDto costShareDto) {
        try {
            costPaymentClient.updateCostShare(id, costShareDto);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @DeleteMapping("/api/shares/{id}")
    @ResponseBody
    public String deleteCostShareApi(@PathVariable Integer id) {
        try {
            boolean deleted = costPaymentClient.deleteCostShare(id);
            return deleted ? "success" : "error: Could not delete cost share";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/api/costs")
    @ResponseBody
    public List<CostDto> getCostsApi() {
        return costPaymentClient.getAllCosts();
    }

    @PostMapping("/api/costs")
    @ResponseBody
    public String createCostApi(@RequestBody CostDto costDto) {
        try {
            costPaymentClient.createCost(costDto);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/api/costs/{id}")
    @ResponseBody
    public CostDto getCostApi(@PathVariable Integer id) {
        return costPaymentClient.getCostById(id);
    }

    @PutMapping("/api/costs/{id}")
    @ResponseBody
    public String updateCostApi(@PathVariable Integer id, @RequestBody CostDto costDto) {
        try {
            costPaymentClient.updateCost(id, costDto);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @DeleteMapping("/api/costs/{id}")
    @ResponseBody
    public String deleteCostApi(@PathVariable Integer id) {
        try {
            boolean deleted = costPaymentClient.deleteCost(id);
            return deleted ? "success" : "error: Could not delete cost";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/{id}")
    public String viewCost(@PathVariable Integer id,
                           Model model,
                           @RequestParam(value = "disputeSuccess", required = false) String disputeSuccess,
                           @RequestParam(value = "disputeError", required = false) String disputeError) {
        try {
            CostDto cost = costPaymentClient.getCostById(id);
            if (cost != null) {
                model.addAttribute("cost", cost);
                model.addAttribute("groups", groupManagementClient.getAllGroups());
                model.addAttribute("disputeSuccess", disputeSuccess);
                model.addAttribute("disputeError", disputeError);
                return "costs/view";
            } else {
                return "redirect:/costs?error=notfound";
            }
        } catch (Exception e) {
            return "redirect:/costs?error=load";
        }
    }

}