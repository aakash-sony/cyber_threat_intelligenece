import api from './api';

export const threatService = {
  getThreats: async (params = {}) => {
    const response = await api.get('/threats', { params });
    return response.data;
  },

  getLatestThreats: async (limit = 10, timeRange = '24h', country = 'all') => {
    const params = { limit, timeRange };
    if (country && country !== 'all' && country !== 'All Countries') params.country = country;
    const response = await api.get('/threats/latest', { params });
    return response.data;
  },

  getThreatById: async (id) => {
    const response = await api.get(`/threats/${id}`);
    return response.data;
  },

  getCriticalThreats: async (page = 0, size = 10) => {
    const response = await api.get('/threats/critical', { params: { page, size } });
    return response.data;
  },

  getHighThreats: async (page = 0, size = 10) => {
    const response = await api.get('/threats/high', { params: { page, size } });
    return response.data;
  },

  searchThreats: async (keyword, page = 0, size = 10) => {
    const response = await api.get('/threats/search', { params: { keyword, page, size } });
    return response.data;
  },

  createThreat: async (threatData) => {
    const response = await api.post('/threats', threatData);
    return response.data;
  },

  getSyncStatus: async () => {
    const response = await api.get('/threats/sync-status');
    return response.data;
  },

  getDistinctSources: async () => {
    const response = await api.get('/threats/sources');
    return response.data;
  },

  triggerSync: async () => {
    const response = await api.post('/threats/sync');
    return response.data;
  }
};
