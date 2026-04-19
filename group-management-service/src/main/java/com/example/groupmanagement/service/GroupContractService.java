package com.example.groupmanagement.service;

import com.example.groupmanagement.entity.GroupContract;
import com.example.groupmanagement.entity.ContractSignature;
import com.example.groupmanagement.entity.GroupMember;
import com.example.groupmanagement.repository.GroupContractRepository;
import com.example.groupmanagement.repository.ContractSignatureRepository;
import com.example.groupmanagement.repository.GroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GroupContractService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupContractService.class);
    
    @Autowired
    private GroupContractRepository groupContractRepository;
    
    @Autowired
    private ContractSignatureRepository contractSignatureRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    /**
     * Sign a contract with authorization and status validation
     * 
     * ============ BUG 1 FIX: Authorization check ============
     * ============ BUG 2 FIX: Contract status validation ============
     */
    @Transactional
    public ContractSignature signContractWithAuthorization(Integer contractId, Integer userId, String signatureMethod, String ipAddress) throws IllegalAccessException {
        logger.info("🔵 [GroupContractService] Attempting to sign contract {} by user {}", contractId, userId);
        
        // Step 1: Fetch contract
        Optional<GroupContract> contractOpt = groupContractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            logger.error("❌ [GroupContractService] Contract {} not found", contractId);
            throw new IllegalArgumentException("Hợp đồng không tồn tại (Contract ID: " + contractId + ")");
        }
        
        GroupContract contract = contractOpt.get();
        Integer groupId = contract.getGroup().getGroupId();
        
        // ============ BUG 2 FIX: Check contract status is PENDING ============
        if (contract.getContractStatus() != GroupContract.ContractStatus.PENDING) {
            logger.warn("⚠️ [GroupContractService] Contract {} is not in PENDING status. Current status: {}", 
                    contractId, contract.getContractStatus());
            throw new IllegalStateException(
                "Hợp đồng này đã được ký hoàn tất bởi tất cả các đồng sở hữu, không thể thao tác thêm. " +
                "Trạng thái hiện tại: " + contract.getContractStatus()
            );
        }
        
        // ============ BUG 1 FIX: Authorization check - verify user is member of group ============
        Optional<GroupMember> memberOpt = groupMemberRepository.findByGroup_GroupIdAndUserId(groupId, userId);
        if (memberOpt.isEmpty()) {
            logger.warn("❌ [GroupContractService] User {} is NOT a member of group {}. Authorization DENIED", userId, groupId);
            throw new IllegalAccessException(
                "Bạn không có quyền ký hợp đồng này. Chỉ các thành viên của nhóm mới có thể ký. " +
                "(User ID: " + userId + " không phải thành viên của Group ID: " + groupId + ")"
            );
        }
        
        logger.info("✅ [GroupContractService] Authorization passed: User {} is member of group {}", userId, groupId);
        
        // Step 2: Check if user already signed this contract
        boolean alreadySigned = contractSignatureRepository.existsByGroupContractAndUserId(contract, userId);
        if (alreadySigned) {
            logger.info("ℹ️ [GroupContractService] User {} already signed contract {}. Returning existing signature.", userId, contractId);
            // User already signed, return the existing signature without creating duplicate
            Optional<ContractSignature> existingSig = contractSignatureRepository.findByGroupContractAndUserId(contract, userId);
            if (existingSig.isPresent()) {
                return existingSig.get();
            }
        }
        
        // Step 3: Create new signature
        ContractSignature signature = new ContractSignature();
        signature.setGroupContract(contract);
        signature.setUserId(userId);
        signature.setSignedAt(LocalDateTime.now());
        signature.setSignatureMethod(signatureMethod != null ? signatureMethod : "electronic");
        signature.setIpAddress(ipAddress != null ? ipAddress : "unknown");
        
        ContractSignature savedSignature = contractSignatureRepository.save(signature);
        logger.info("✍️ [GroupContractService] Signature created: signatureId={}, contractId={}, userId={}", 
                savedSignature.getSignatureId(), contractId, userId);
        
        // Step 4: Check if all members have signed, then update contract status to SIGNED
        long totalMembers = groupMemberRepository.countByGroup_GroupId(groupId);
        long signedMembers = contractSignatureRepository.countByGroupContract(contract);
        
        logger.info("📊 [GroupContractService] Contract signature progress: {}/{} members signed", signedMembers, totalMembers);
        
        if (signedMembers >= totalMembers && totalMembers > 0) {
            logger.info("🎉 [GroupContractService] All members have signed contract {}. Updating status to SIGNED.", contractId);
            contract.setContractStatus(GroupContract.ContractStatus.SIGNED);
            contract.setSignedDate(LocalDateTime.now());
            groupContractRepository.save(contract);
            logger.info("✅ [GroupContractService] Contract {} status updated to SIGNED", contractId);
        }
        
        return savedSignature;
    }
}
