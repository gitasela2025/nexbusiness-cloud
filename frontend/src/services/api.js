import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ---- Auth ----
const TOKEN_KEY = 'nex_token';
const USER_KEY = 'nex_user';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const getStoredUser = () => {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; }
};
export const storeSession = (token, user) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
};
export const clearSession = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
};

apiClient.interceptors.request.use((config) => {
    const t = getToken();
    if (t) config.headers.Authorization = `Bearer ${t}`;
    return config;
});

// token කල් ඉකුත් වුණොත්/වැරදි නම් logout
apiClient.interceptors.response.use(
    (r) => r,
    (err) => {
        const url = err.config && err.config.url ? err.config.url : '';
        if (err.response && err.response.status === 401 && !url.includes('/auth/')) {
            clearSession();
            window.dispatchEvent(new Event('nex-logout'));
        }
        return Promise.reject(err);
    }
);

export const signup = (name, email, password) =>
    apiClient.post('/auth/signup', { name, email, password });
export const login = (email, password) =>
    apiClient.post('/auth/login', { email, password });

// SecureDoc RAG API
export const uploadDocument = (formData, role) => {
    return apiClient.post('/securedoc/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data', 'X-Demo-Role': role },
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
