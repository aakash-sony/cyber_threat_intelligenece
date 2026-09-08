import api from './api';

export const dashboardService = {
  getSummary: async (timeRange = '24h', country = 'all') => {
    const params = { timeRange };
    if (country && country !== 'all' && country !== 'All Countries') params.country = country;
    const response = await api.get('/dashboard/summary', { params });
    return response.data;
  },

  getActivity: async (timeRange = '24h', country = 'all') => {
    const params = { timeRange };
    if (country && country !== 'all' && country !== 'All Countries') params.country = country;
    const response = await api.get('/dashboard/activity', { params });
    return response.data;
  },

  getBySource: async (timeRange = '24h', country = 'all') => {
    const params = { timeRange };
    if (country && country !== 'all' && country !== 'All Countries') params.country = country;
    const response = await api.get('/dashboard/by-source', { params });
    return response.data;
  },

  getBySeverity: async (timeRange = '24h', country = 'all') => {
    const params = { timeRange };
    if (country && country !== 'all' && country !== 'All Countries') params.country = country;
    const response = await api.get('/dashboard/by-severity', { params });
    return response.data;
  },

  getByType: async (timeRange = '24h', country = 'all') => {
    const params = { timeRange };
    if (country && country !== 'all' && country !== 'All Countries') params.country = country;
    const response = await api.get('/dashboard/by-type', { params });
    return response.data;
  },

  getCountryStats: async (timeRange = '24h') => {
    const response = await api.get('/dashboard/by-country', { params: { timeRange } });
    return response.data;
  },

  getCountries: async () => {
    const response = await api.get('/dashboard/countries');
    return response.data;
  },

  getDetailedSources: async () => {
    const response = await api.get('/dashboard/sources/detailed');
    return response.data;
  }
};
