package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.repository.PendingApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApprovalService {

    @Autowired
    private PendingApprovalRepository approvalRepository;

    // Create a pending action request (triggered by AI agent)
    public PendingApproval createApprovalRequest(PendingApproval.ActionType actionType, String payloadJson, String session) {
        PendingApproval approval = new PendingApproval();
        approval.setActionType(actionType);
        approval.setPayloadJson(payloadJson);
        approval.setRequestedByAiSession(session);
        approval.setStatus(PendingApproval.ApprovalStatus.PENDING);
        
        return approvalRepository.save(approval);
    }

    // Get all pending requests for Admin review
    public List<PendingApproval> getPendingApprovals() {
        return approvalRepository.findAll();
    }

    // Admin approves or rejects
    public String updateApprovalStatus(Long id, PendingApproval.ApprovalStatus status) {
        Optional<PendingApproval> optionalApproval = approvalRepository.findById(id);
        if (optionalApproval.isPresent()) {
            PendingApproval approval = optionalApproval.get();
            approval.setStatus(status);
            approvalRepository.save(approval);
            return "Approval request " + id + " has been updated to " + status;
        }
        return "Approval request not found.";
    }
}
