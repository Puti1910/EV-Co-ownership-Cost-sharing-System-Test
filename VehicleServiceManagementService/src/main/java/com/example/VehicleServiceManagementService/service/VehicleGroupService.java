package com.example.VehicleServiceManagementService.service;

import com.example.VehicleServiceManagementService.model.Vehiclegroup;
import com.example.VehicleServiceManagementService.model.Vehicle;
import com.example.VehicleServiceManagementService.repository.VehicleGroupRepository;
import com.example.VehicleServiceManagementService.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehicleGroupService {

    @Autowired
    private VehicleGroupRepository vehicleGroupRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lấy tất cả các nhóm xe
     * @return Danh sách tất cả nhóm xe
     */
    public List<Vehiclegroup> getAllVehicleGroups() {
        return vehicleGroupRepository.findAll();
    }

    /**
     * Lấy danh sách nhóm xe chưa có xe nào
     * @return Danh sách nhóm xe chưa có xe
     */
    public List<Vehiclegroup> getVehicleGroupsWithoutVehicles() {
        List<Vehiclegroup> allGroups = vehicleGroupRepository.findAll();
        
        return allGroups.stream()
                .filter(group -> {
                    long vehicleCount = vehicleRepository.countByGroupId(group.getGroupId());
                    return vehicleCount == 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách nhóm xe chưa có xe, bao gồm cả nhóm hiện tại (nếu có)
     * @param currentGroupId ID của nhóm hiện tại (có thể null)
     * @return Danh sách nhóm xe chưa có xe + nhóm hiện tại
     */
    public List<Vehiclegroup> getAvailableVehicleGroups(Long currentGroupId) {
        List<Vehiclegroup> allGroups = vehicleGroupRepository.findAll();
        
        return allGroups.stream()
                .filter(group -> {
                    // Nếu là nhóm hiện tại, luôn bao gồm
                    if (currentGroupId != null && currentGroupId.equals(group.getGroupId())) {
                        return true;
                    }
                    // Nếu không phải nhóm hiện tại, chỉ lấy nhóm chưa có xe
                    long vehicleCount = vehicleRepository.countByGroupId(group.getGroupId());
                    return vehicleCount == 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết nhóm xe theo groupId
     * @param groupId ID của nhóm xe
     * @return Vehiclegroup nếu tìm thấy, null nếu không
     */
    public Vehiclegroup getVehicleGroupById(Long groupId) {
        Optional<Vehiclegroup> group = vehicleGroupRepository.findById(groupId);
        return group.orElse(null);
    }

    /**
     * Lấy danh sách xe trong nhóm
     * @param groupId ID của nhóm xe
     * @return Danh sách các xe trong nhóm
     */
    public List<Vehicle> getVehiclesByGroupId(Long groupId) {
        return vehicleRepository.findByGroupId(groupId);
    }

    /**
     * Lọc nhóm xe theo tên và trạng thái
     * @param searchQuery Tìm kiếm theo tên nhóm
     * @param statusFilter Lọc theo trạng thái nhóm
     * @return Danh sách nhóm xe đã lọc
     */
    public List<Vehiclegroup> filterVehicleGroups(String searchQuery, String statusFilter) {
        List<Vehiclegroup> allGroups = vehicleGroupRepository.findAll();

        return allGroups.stream()
                .filter(group -> (searchQuery == null || group.getName().toLowerCase().contains(searchQuery.toLowerCase())))
                .collect(Collectors.toList());
    }

    // Thêm nhóm xe mới
    public Vehiclegroup addVehicleGroup(Vehiclegroup vehicleGroup) {
        return vehicleGroupRepository.save(vehicleGroup);
    }

    /**
     * Sửa thông tin nhóm xe
     * @param groupId ID của nhóm xe cần sửa
     * @param vehicleGroup Đối tượng chứa thông tin cần cập nhật
     * @return Vehiclegroup đã được cập nhật, null nếu không tìm thấy nhóm xe
     */
    @Transactional
    public Vehiclegroup updateVehicleGroup(Long groupId, Vehiclegroup vehicleGroup) {
        Optional<Vehiclegroup> existingGroup = vehicleGroupRepository.findById(groupId);
        if (existingGroup.isEmpty()) {
            return null;
        }

        Vehiclegroup groupToUpdate = existingGroup.get();
        
        if (vehicleGroup.getName() != null && !vehicleGroup.getName().trim().isEmpty()) {
            groupToUpdate.setName(vehicleGroup.getName().trim());
        }
        
        if (vehicleGroup.getDescription() != null) {
            groupToUpdate.setDescription(vehicleGroup.getDescription());
        }
        
        return vehicleGroupRepository.save(groupToUpdate);
    }

    /**
     * Xóa nhóm xe theo groupId
     * @param groupId ID của nhóm xe cần xóa
     * @return Thông báo kết quả xóa, null nếu không tìm thấy nhóm xe
     */
    @Transactional
    public String deleteVehicleGroup(Long groupId) {
        Optional<Vehiclegroup> groupOptional = vehicleGroupRepository.findById(groupId);
        if (groupOptional.isEmpty()) {
            return null;
        }

        Vehiclegroup group = groupOptional.get();
        String groupName = group.getName();

        List<Vehicle> vehiclesInGroup = vehicleRepository.findByGroupId(groupId);
        int vehicleCount = vehiclesInGroup.size();
        
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        
        try {
            if (!vehiclesInGroup.isEmpty()) {
                vehicleRepository.deleteAll(vehiclesInGroup);
            }

            entityManager.createNativeQuery("DELETE FROM vehicle_management.vehiclehistory WHERE group_id = :groupId")
                    .setParameter("groupId", groupId)
                    .executeUpdate();

            vehicleGroupRepository.deleteById(groupId);
        } finally {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }
        
        if (vehicleCount > 0) {
            return "Nhóm xe '" + groupName + "' và " + vehicleCount + " xe trong nhóm đã được xóa thành công.";
        } else {
            return "Nhóm xe '" + groupName + "' đã được xóa thành công.";
        }
    }

}
