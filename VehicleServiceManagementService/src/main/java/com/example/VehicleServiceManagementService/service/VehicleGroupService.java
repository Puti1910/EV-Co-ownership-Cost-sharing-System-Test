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
        // Trả về danh sách tất cả các nhóm xe từ repository
        return vehicleGroupRepository.findAll();
    }

    /**
     * Lấy danh sách nhóm xe chưa có xe nào
     * @return Danh sách nhóm xe chưa có xe
     */
    public List<Vehiclegroup> getVehicleGroupsWithoutVehicles() {
        // Lấy tất cả nhóm xe
        List<Vehiclegroup> allGroups = vehicleGroupRepository.findAll();
        
        // Lọc chỉ lấy những nhóm chưa có xe
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
    public List<Vehiclegroup> getAvailableVehicleGroups(String currentGroupId) {
        // Lấy tất cả nhóm xe
        List<Vehiclegroup> allGroups = vehicleGroupRepository.findAll();
        
        // Lọc lấy những nhóm chưa có xe, hoặc là nhóm hiện tại
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
    public Vehiclegroup getVehicleGroupById(String groupId) {
        Optional<Vehiclegroup> group = vehicleGroupRepository.findById(groupId);
        return group.orElse(null);
    }

    /**
     * Lấy danh sách xe trong nhóm
     * @param groupId ID của nhóm xe
     * @return Danh sách các xe trong nhóm
     */
    public List<Vehicle> getVehiclesByGroupId(String groupId) {
        return vehicleRepository.findByGroupId(groupId);
    }

    /**
     * Lọc nhóm xe theo tên và trạng thái
     * @param searchQuery Tìm kiếm theo tên nhóm
     * @param statusFilter Lọc theo trạng thái nhóm
     * @return Danh sách nhóm xe đã lọc
     */
    public List<Vehiclegroup> filterVehicleGroups(String searchQuery, String statusFilter) {
        List<Vehiclegroup> allGroups = vehicleGroupRepository.findAll();  // Lấy tất cả các nhóm xe

        // Lọc nhóm xe theo tên
        return allGroups.stream()
                .filter(group -> (searchQuery == null || group.getName().toLowerCase().contains(searchQuery.toLowerCase())))  // Lọc theo tên
                .collect(Collectors.toList());  // Trả về danh sách nhóm xe đã lọc
    }

    // Thêm nhóm xe mới
    public Vehiclegroup addVehicleGroup(Vehiclegroup vehicleGroup) {
        return vehicleGroupRepository.save(vehicleGroup);
    }

    /**
     * Sửa thông tin nhóm xe
     * Có thể sửa: tên, trạng thái, mô tả
     * 
     * @param groupId ID của nhóm xe cần sửa
     * @param vehicleGroup Đối tượng chứa thông tin cần cập nhật
     * @return Vehiclegroup đã được cập nhật, null nếu không tìm thấy nhóm xe
     */
    @Transactional
    public Vehiclegroup updateVehicleGroup(String groupId, Vehiclegroup vehicleGroup) {
        Optional<Vehiclegroup> existingGroup = vehicleGroupRepository.findById(groupId);
        if (existingGroup.isEmpty()) {
            return null; // Nếu không tìm thấy, trả về null
        }

        Vehiclegroup groupToUpdate = existingGroup.get();
        
        // Cập nhật tên nhóm xe (nếu có)
        if (vehicleGroup.getName() != null && !vehicleGroup.getName().trim().isEmpty()) {
            groupToUpdate.setName(vehicleGroup.getName().trim());
        }
        
        // Cập nhật mô tả (nếu có)
        if (vehicleGroup.getDescription() != null) {
            groupToUpdate.setDescription(vehicleGroup.getDescription());
        }
        
        return vehicleGroupRepository.save(groupToUpdate);
    }

    /**
     * Xóa nhóm xe theo groupId
     * Phương thức này sẽ tự động xóa tất cả xe trong nhóm trước khi xóa nhóm
     * 
     * @param groupId ID của nhóm xe cần xóa
     * @return Thông báo kết quả xóa (bao gồm số lượng xe đã xóa), null nếu không tìm thấy nhóm xe
     */
    @Transactional
    public String deleteVehicleGroup(String groupId) {
        // Kiểm tra xem nhóm xe có tồn tại trong cơ sở dữ liệu hay không
        Optional<Vehiclegroup> groupOptional = vehicleGroupRepository.findById(groupId);
        if (groupOptional.isEmpty()) {
            return null; // Nếu không tìm thấy, trả về null
        }

        Vehiclegroup group = groupOptional.get();
        String groupName = group.getName();

        // Tìm tất cả xe trong nhóm
        List<Vehicle> vehiclesInGroup = vehicleRepository.findByGroupId(groupId);
        int vehicleCount = vehiclesInGroup.size();
        
        // Disable foreign key checks trước khi xóa tất cả dữ liệu liên quan
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        
        try {
            // Xóa tất cả xe trong nhóm trước
            if (!vehiclesInGroup.isEmpty()) {
                vehicleRepository.deleteAll(vehiclesInGroup);
            }

            // Xóa tất cả vehiclehistory liên quan đến nhóm xe này
            // MySQL sẽ tự động convert groupId (String) sang INT nếu cần
            entityManager.createNativeQuery("DELETE FROM vehicle_management.vehiclehistory WHERE group_id = :groupId")
                    .setParameter("groupId", groupId)
                    .executeUpdate();

            // Sau đó xóa nhóm xe
            vehicleGroupRepository.deleteById(groupId);
        } finally {
            // Bật lại foreign key checks
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }
        
        // Tạo thông báo kết quả
        if (vehicleCount > 0) {
            return "Nhóm xe '" + groupName + "' và " + vehicleCount + " xe trong nhóm đã được xóa thành công.";
        } else {
            return "Nhóm xe '" + groupName + "' đã được xóa thành công.";
        }
    }

}
