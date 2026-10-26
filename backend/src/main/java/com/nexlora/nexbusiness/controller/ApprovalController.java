package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
@CrossOrigin(origins = "*")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @GetMapping("/pending")
    public ResponseEntity<List<PendingApproval>> getAllApprovals() {
        return ResponseEntity.ok(approvalService.getPendingApprovals());
    }

    @PostMapping("/request")
    public ResponseEntity<PendingApproval> createRequest(@RequestBody Map<String, String> request) {
        PendingApproval.ActionType type = PendingApproval.ActionType.valueOf(request.get("actionType").toUpperCase());
        String payload = request.get("payloadJson");
        String session = request.get("session");

        PendingApproval saved = approvalService.createApprovalRequest(type, payload, session);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        PendingApproval.ApprovalStatus status = PendingApproval.ApprovalStatus.valueOf(request.get("status").toUpperCase());
        String result = approvalService.updateApprovalStatus(id, status);
        return ResponseEntity.ok(result);
    }
}
