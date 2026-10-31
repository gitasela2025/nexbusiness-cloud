package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private String docId;

    private String fileName;

    private String projectNamespace;

    @Enumerated(EnumType.STRING)
    private AccessRole accessRole;

    private int version;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    public enum AccessRole {
        ADMIN_ONLY, PUBLIC
    }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getProjectNamespace() { return projectNamespace; }
    public void setProjectNamespace(String projectNamespace) { this.projectNamespace = projectNamespace; }

    public AccessRole getAccessRole() { return accessRole; }
    public void setAccessRole(AccessRole accessRole) { this.accessRole = accessRole; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
