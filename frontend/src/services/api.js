import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// SecureDoc RAG API
export const uploadDocument = (formData) => {
    return apiClient.post('/securedoc/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

export const askDocument = (namespace, question, role) => {
    return apiClient.post('/securedoc/ask', { namespace, question }, {
        headers: { 'X-Demo-Role': role },
    });
};

export const listDocuments = (namespace, role) => {
    return apiClient.get('/securedoc/documents', {
        params: { namespace },
        headers: { 'X-Demo-Role': role },
    });
};

export const deleteDocument = (docId, role) => {
    return apiClient.delete(`/securedoc/documents/${docId}`, {
        headers: { 'X-Demo-Role': role },
    });
};

// OmniServe AI Agent Chat API
export const sendAiChat = (message, session) => {
    return apiClient.post('/omniserve/chat', { message, session });
};

// Human-in-the-Loop Approvals API
export const getPendingApprovals = () => {
    return apiClient.get('/approvals/pending');
};

export const updateApprovalStatus = (id, status) => {
    return apiClient.put(`/approvals/${id}/status`, { status });
};

// MemoryCore API
export const getUserMemories = (userId) => {
    return apiClient.get(`/memories/user/${userId}`);
};

export const saveUserMemory = (userId, fact) => {
    return apiClient.post('/memories/save', { userId, fact });
};

export const deleteUserMemory = (id) => {
    return apiClient.delete(`/memories/${id}`);
};
