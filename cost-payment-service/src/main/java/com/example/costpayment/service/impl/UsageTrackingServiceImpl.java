package com.example.costpayment.service.impl;

import com.example.costpayment.dto.UsageTrackingDto;
import com.example.costpayment.entity.UsageTracking;
import com.example.costpayment.repository.UsageTrackingRepository;
import com.example.costpayment.service.UsageTrackingService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsageTrackingServiceImpl implements UsageTrackingService {

    @Autowired
    private UsageTrackingRepository usageTrackingRepository;

    @Override
    @Transactional
    public UsageTracking saveUsageTracking(UsageTracking usageTracking) {
        // Kiểm tra xem đã tồn tại chưa
        Optional<UsageTracking> existing = usageTrackingRepository
                .findByGroupIdAndUserIdAndMonthAndYear(
                        usageTracking.getGroupId(),
                        usageTracking.getUserId(),
                        usageTracking.getMonth(),
                        usageTracking.getYear()
                );

        if (existing.isPresent()) {
            // Update
            UsageTracking existingUsage = existing.get();
            existingUsage.setKmDriven(usageTracking.getKmDriven());
            return usageTrackingRepository.save(existingUsage);
        } else {
            // Create new
            return usageTrackingRepository.save(usageTracking);
        }
    }

    @Override
    public List<UsageTrackingDto> getGroupUsageInMonth(Integer groupId, Integer month, Integer year) {
        List<UsageTracking> usageList = usageTrackingRepository
                .findByGroupIdAndMonthAndYear(groupId, month, year);

        // Tính tổng km
        double totalKm = usageList.stream()
                .mapToDouble(u -> u.getKmDriven() != null ? u.getKmDriven() : 0)
                .sum();

        // Convert sang DTO và tính %
        return usageList.stream().map(usage -> {
            UsageTrackingDto dto = new UsageTrackingDto();
            BeanUtils.copyProperties(usage, dto);

            // Tính % km
            if (totalKm > 0 && usage.getKmDriven() != null) {
                dto.setPercentKm((usage.getKmDriven() / totalKm) * 100);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public UsageTracking getUserUsageInMonth(Integer groupId, Integer userId, Integer month, Integer year) {
        return usageTrackingRepository
                .findByGroupIdAndUserIdAndMonthAndYear(groupId, userId, month, year)
                .orElse(null);
    }

    @Override
    @Transactional
    public UsageTracking updateKmDriven(Integer groupId, Integer userId, Integer month, Integer year, Double kmDriven) {
        Optional<UsageTracking> existing = usageTrackingRepository
                .findByGroupIdAndUserIdAndMonthAndYear(groupId, userId, month, year);

        if (existing.isPresent()) {
            UsageTracking usage = existing.get();
            usage.setKmDriven(kmDriven);
            return usageTrackingRepository.save(usage);
        } else {
            // Tạo mới
            UsageTracking newUsage = new UsageTracking();
            newUsage.setGroupId(groupId);
            newUsage.setUserId(userId);
            newUsage.setMonth(month);
            newUsage.setYear(year);
            newUsage.setKmDriven(kmDriven);
            return usageTrackingRepository.save(newUsage);
        }
    }

    @Override
    public List<UsageTracking> getUserUsageHistory(Integer userId) {
        return usageTrackingRepository.findByUserIdOrderByYearDescMonthDesc(userId);
    }

    @Override
    public Map<Integer, Double> calculateKmPercentage(Integer groupId, Integer month, Integer year) {
        List<UsageTracking> usageList = usageTrackingRepository
                .findByGroupIdAndMonthAndYear(groupId, month, year);

        // Tính tổng km
        double totalKm = usageList.stream()
                .mapToDouble(u -> u.getKmDriven() != null ? u.getKmDriven() : 0)
                .sum();

        // Tính % cho từng user
        Map<Integer, Double> percentageMap = new HashMap<>();
        for (UsageTracking usage : usageList) {
            if (totalKm > 0 && usage.getKmDriven() != null) {
                double percent = (usage.getKmDriven() / totalKm) * 100;
                percentageMap.put(usage.getUserId(), percent);
            }
        }

        return percentageMap;
    }

    @Override
    @Transactional
    public void deleteUsageTracking(Integer usageId) {
        usageTrackingRepository.deleteById(usageId);
    }
}


