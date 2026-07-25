import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 seconds timeout
});

export const analyzeStock = async (symbol) => {
  try {
    const response = await apiClient.post('/analyze', { symbol });
    return response.data;
  } catch (error) {
    if (error.response && error.response.data) {
      // If backend returned a validation error or custom error structure
      throw new Error(error.response.data.message || 'An error occurred during stock analysis.');
    }
    throw new Error(error.message || 'Failed to connect to the IntelliStock server.');
  }
};

export const checkHealth = async () => {
  try {
    const response = await apiClient.get('/health');
    return response.data;
  } catch (error) {
    throw new Error('Backend server is currently unreachable.');
  }
};

export const getWatchlist = async () => {
  try {
    const response = await apiClient.get('/watchlist');
    return response.data;
  } catch (error) {
    throw new Error('Failed to load watchlist.');
  }
};

export const addToWatchlist = async (symbol) => {
  try {
    const response = await apiClient.post('/watchlist/add', { symbol });
    return response.data;
  } catch (error) {
    throw new Error('Failed to add stock to watchlist.');
  }
};

export const removeFromWatchlist = async (symbol) => {
  try {
    const response = await apiClient.delete(`/watchlist/remove/${symbol}`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to remove stock from watchlist.');
  }
};

export const checkIsWatched = async (symbol) => {
  try {
    const response = await apiClient.get(`/watchlist/check/${symbol}`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to check watchlist status.');
  }
};

export const getStockHistory = async (symbol) => {
  try {
    const response = await apiClient.get(`/stocks/${symbol}/history`);
    return response.data;
  } catch (error) {
    throw new Error('Failed to load price history.');
  }
};
