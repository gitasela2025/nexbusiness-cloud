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
