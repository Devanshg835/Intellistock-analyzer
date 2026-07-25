import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from './App';
import * as api from './api';

// Mock Recharts to avoid jsdom SVG dimension warning errors
vi.mock('recharts', () => {
  return {
    ResponsiveContainer: ({ children }) => children,
    AreaChart: ({ children }) => <svg data-testid="area-chart">{children}</svg>,
    Area: () => <g data-testid="area-node" />,
    XAxis: () => <g data-testid="xaxis-node" />,
    YAxis: () => <g data-testid="yaxis-node" />,
    CartesianGrid: () => <g data-testid="grid-node" />,
    Tooltip: () => <div data-testid="tooltip" />,
  };
});

// Mock the API client functions
vi.mock('./api', () => ({
  analyzeStock: vi.fn(),
  getWatchlist: vi.fn(),
  addToWatchlist: vi.fn(),
  removeFromWatchlist: vi.fn(),
  checkIsWatched: vi.fn(),
  getStockHistory: vi.fn(),
}));

describe('IntelliStock App Component', () => {

  beforeEach(() => {
    vi.clearAllMocks();
    api.getWatchlist.mockResolvedValue([
      { id: 1, symbol: 'INFY', companyName: 'Infosys Limited' }
    ]);
  });

  it('renders brand headers and search inputs successfully', async () => {
    render(<App />);

    // Header check
    expect(screen.getByText('IntelliStock')).toBeInTheDocument();
    
    // Watchlist header check
    await waitFor(() => {
      expect(screen.getByText('🌟 Watchlist')).toBeInTheDocument();
      expect(screen.getByText('INFY')).toBeInTheDocument();
    });

    // Input checks
    const input = screen.getByPlaceholderText(/Enter stock symbol/i);
    expect(input).toBeInTheDocument();
  });

  it('updates text inputs and registers search queries', async () => {
    render(<App />);
    
    const input = screen.getByPlaceholderText(/Enter stock symbol/i);
    fireEvent.change(input, { target: { value: 'AAPL' } });
    expect(input.value).toBe('AAPL');
  });

  it('shows popular stock suggestions', () => {
    render(<App />);
    expect(screen.getByText('Popular Stocks:')).toBeInTheDocument();
    expect(screen.getByText('MSFT')).toBeInTheDocument();
  });
});
