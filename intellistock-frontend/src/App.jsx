import React, { useState, useEffect } from 'react';
import { 
  analyzeStock, 
  getWatchlist, 
  addToWatchlist, 
  removeFromWatchlist, 
  checkIsWatched, 
  getStockHistory 
} from './api';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip
} from 'recharts';

function App() {
  const [symbol, setSymbol] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  
  // Phase 4 states
  const [watchlist, setWatchlist] = useState([]);
  const [isWatched, setIsWatched] = useState(false);
  const [historyData, setHistoryData] = useState([]);
  const [historyRange, setHistoryRange] = useState(30); // days: 30 or 7

  // Quick Seed Stock search list
  const seedStocks = ['INFY', 'TCS', 'AAPL', 'MSFT', 'TSLA'];

  useEffect(() => {
    fetchWatchlist();
  }, []);

  const fetchWatchlist = async () => {
    try {
      const data = await getWatchlist();
      setWatchlist(data);
    } catch (err) {
      console.error('Failed to load watchlist:', err.message);
    }
  };

  const handleSearch = async (searchSymbol) => {
    const targetSymbol = (searchSymbol || symbol).trim().toUpperCase();
    if (!targetSymbol) {
      setError('Please enter a stock symbol.');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);
    setHistoryData([]);

    try {
      // 1. Fetch analysis
      const data = await analyzeStock(targetSymbol);
      setResult(data);
      setSymbol(targetSymbol);

      // 2. Check if symbol is watched
      try {
        const watched = await checkIsWatched(targetSymbol);
        setIsWatched(watched);
      } catch (e) {
        console.error('Failed to check watchlist status', e);
      }

      // 3. Fetch history data
      try {
        const historyRes = await getStockHistory(targetSymbol);
        if (historyRes && historyRes.history) {
          setHistoryData(historyRes.history);
        }
      } catch (e) {
        console.error('Failed to fetch stock history', e);
      }

    } catch (err) {
      setError(err.message || 'An error occurred during analysis.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleWatchlist = async () => {
    if (!result) return;
    const currentSymbol = result.symbol;
    try {
      if (isWatched) {
        await removeFromWatchlist(currentSymbol);
        setIsWatched(false);
      } else {
        await addToWatchlist(currentSymbol);
        setIsWatched(true);
      }
      fetchWatchlist();
    } catch (err) {
      setError('Failed to update watchlist. Please try again.');
    }
  };

  const handleDeleteWatchlistItem = async (e, deleteSymbol) => {
    e.stopPropagation(); // prevent triggering search
    try {
      await removeFromWatchlist(deleteSymbol);
      if (result && result.symbol.toUpperCase() === deleteSymbol.toUpperCase()) {
        setIsWatched(false);
      }
      fetchWatchlist();
    } catch (err) {
      console.error('Failed to delete watchlist item:', err);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleSearch();
  };

  // Helper to choose badge class for risk level
  const getRiskBadgeClass = (risk) => {
    switch (risk?.toLowerCase()) {
      case 'low': return 'badge-success';
      case 'medium': return 'badge-warning';
      case 'high': return 'badge-danger';
      default: return 'badge-info';
    }
  };

  // Helper to choose badge class for sentiment
  const getSentimentBadgeClass = (sentiment) => {
    switch (sentiment?.toLowerCase()) {
      case 'very positive':
      case 'positive':
        return 'badge-success';
      case 'neutral':
        return 'badge-info';
      case 'negative':
      case 'very negative':
        return 'badge-danger';
      default:
        return 'badge-info';
    }
  };

  // Helper to choose badge class for trend
  const getTrendBadgeClass = (trend) => {
    if (trend?.toLowerCase().includes('bullish')) return 'badge-success';
    if (trend?.toLowerCase().includes('bearish')) return 'badge-danger';
    return 'badge-info';
  };

  // Helper to format market cap
  const formatMarketCap = (marketCap) => {
    if (!marketCap) return 'N/A';
    if (marketCap >= 1000) {
      return `$${(marketCap / 1000).toFixed(2)}T`;
    }
    return `$${marketCap.toFixed(1)}B`;
  };

  // Helper to choose badge class for recommendation
  const getRecommendationBadgeClass = (rec) => {
    switch (rec?.toUpperCase()) {
      case 'BUY': return 'badge-success';
      case 'HOLD': return 'badge-warning';
      case 'SELL': return 'badge-danger';
      case 'WATCH': return 'badge-info';
      default: return 'badge-info';
    }
  };

  // Filter history data based on selected range (30 days vs 7 days)
  const getFilteredHistory = () => {
    if (!historyData || historyData.length === 0) return [];
    return historyData.slice(-historyRange);
  };

  // Custom tooltips for AreaChart
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{ 
          background: 'rgba(15, 23, 42, 0.95)', 
          border: '1px solid rgba(255, 255, 255, 0.1)', 
          padding: '0.65rem 0.85rem', 
          borderRadius: '8px',
          boxShadow: '0 4px 20px rgba(0,0,0,0.5)'
        }}>
          <p style={{ margin: 0, fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.2rem' }}>
            {payload[0].payload.date}
          </p>
          <p style={{ margin: 0, fontWeight: 700, fontSize: '0.95rem', color: 'var(--accent-light)' }}>
            Price: ${payload[0].value.toFixed(2)}
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="brand-wrapper">
          <div className="brand-icon">I</div>
          <h1 className="brand-title">IntelliStock</h1>
        </div>
        <p className="brand-subtitle">Automated Stock Research & Fundamental Analysis Dashboard</p>
      </header>

      {/* Main Workspace split layout */}
      <div className="main-workspace-layout">
        
        {/* Left Side: Watchlist Panel */}
        <div className="watchlist-sidebar">
          <h3 className="watchlist-header">
            <span>🌟 Watchlist</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>({watchlist.length})</span>
          </h3>
          
          {watchlist.length === 0 ? (
            <div className="watchlist-empty-state">
              No watched stocks.<br />Search a symbol and click "☆ Watch" to save it here.
            </div>
          ) : (
            <div className="watchlist-list">
              {watchlist.map((item) => (
                <div 
                  key={item.id} 
                  className={`watchlist-item ${result && result.symbol === item.symbol ? 'active-border' : ''}`}
                  onClick={() => handleSearch(item.symbol)}
                  style={result && result.symbol === item.symbol ? { borderColor: 'var(--accent-light)' } : {}}
                >
                  <div className="watchlist-info">
                    <span className="watchlist-symbol">{item.symbol}</span>
                    <span className="watchlist-name">{item.companyName}</span>
                  </div>
                  <button 
                    className="watchlist-delete-btn"
                    onClick={(e) => handleDeleteWatchlistItem(e, item.symbol)}
                    title="Remove from watchlist"
                  >
                    🗑️
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Side: Search and Workspace Content */}
        <div className="content-area">
          
          {/* Search Input Box */}
          <div className="search-container" style={{ marginTop: 0, width: '100%' }}>
            <form onSubmit={handleSubmit} className="search-form">
              <div className="search-input-wrapper">
                <input
                  type="text"
                  className="search-input"
                  placeholder="Enter stock symbol (e.g., INFY, AAPL, MSFT)"
                  value={symbol}
                  onChange={(e) => setSymbol(e.target.value)}
                  disabled={loading}
                  maxLength={10}
                />
              </div>
              <button type="submit" className="analyze-button" disabled={loading}>
                {loading ? 'Analyzing...' : 'Analyze'}
              </button>
            </form>
            
            <div className="seed-suggestion">
              <span>Popular Stocks:</span>
              {seedStocks.map((seed) => (
                <span
                  key={seed}
                  className="seed-badge"
                  onClick={() => {
                    if (!loading) {
                      setSymbol(seed);
                      handleSearch(seed);
                    }
                  }}
                >
                  {seed}
                </span>
              ))}
            </div>
          </div>

          {/* Error Output with Retry */}
          {error && (
            <div className="error-container" style={{ margin: '1rem 0' }}>
              <div className="error-icon">⚠️</div>
              <div className="error-message" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <span>{error}</span>
                <button 
                  onClick={() => handleSearch()}
                  style={{ 
                    alignSelf: 'flex-start',
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid rgba(255,255,255,0.1)',
                    color: 'var(--text-primary)',
                    padding: '0.25rem 0.5rem',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    fontSize: '0.8rem'
                  }}
                >
                  🔄 Retry Request
                </button>
              </div>
            </div>
          )}

          {/* Loading Skeletons */}
          {loading && (
            <div className="loading-wrapper">
              <div className="spinner"></div>
              <div className="loading-text">Retrieving real-time market data & computing scoring models...</div>
            </div>
          )}

          {/* Stock Report Dashboard */}
          {result && !loading && (
            <div className="dashboard-grid" style={{ width: '100%' }}>
              
              {/* Main profile banner */}
              <div className="dashboard-header-card">
                <div className="company-profile">
                  <div className="company-title-row" style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
                    <span className="company-symbol">{result.symbol}</span>
                    <h2 className="company-name">{result.companyName}</h2>
                    
                    {/* Add to Watchlist trigger */}
                    <button 
                      className="watchlist-toggle-btn" 
                      onClick={handleToggleWatchlist}
                      style={{ 
                        marginLeft: '1rem', 
                        background: 'rgba(255,255,255,0.03)', 
                        border: '1px solid var(--border-color)', 
                        color: 'var(--text-primary)', 
                        padding: '0.35rem 0.75rem', 
                        borderRadius: '8px', 
                        cursor: 'pointer',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '0.25rem',
                        fontSize: '0.85rem'
                      }}
                    >
                      {isWatched ? '⭐ Watched' : '☆ Watch'}
                    </button>
                  </div>
                  
                  <span className="company-meta">
                    Sector: {result.sector || 'N/A'} | Analysis Generated: {result.lastUpdated || new Date().toLocaleTimeString()}
                  </span>
                </div>
                
                {/* Cache Badge & Overall Recommendation */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', alignItems: 'flex-start' }}>
                  <span 
                    className={`badge ${getRecommendationBadgeClass(result.recommendation)}`} 
                    style={{ fontSize: '1.2rem', padding: '0.65rem 1.25rem', borderRadius: '12px', letterSpacing: '0.04em' }}
                  >
                    📢 {result.recommendation || 'HOLD'}
                  </span>
                  
                  {/* Fresh vs Cached indicators */}
                  <span className={`badge ${result.isCached ? 'badge-cache-stale' : 'badge-cache-fresh'}`} style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>
                    {result.isCached ? '💾 Cached Data' : '⚡ Fresh Live Data'}
                  </span>
                </div>

                <div className="price-container">
                  <span className="price-label">Current Price</span>
                  <span className="price-value">${result.currentPrice?.toFixed(2)}</span>
                </div>
              </div>

              {/* Card 1: Valuation & Ratios */}
              <div className="glass-card card-col-4">
                <h3 className="card-title">📊 Valuation & Size</h3>
                <div className="metric-row">
                  <span className="metric-label">Market Capitalization</span>
                  <span className="metric-value">{formatMarketCap(result.marketCap)}</span>
                </div>
                <div className="metric-row">
                  <span className="metric-label">P/E Ratio</span>
                  <span className="metric-value">{result.peRatio ? result.peRatio.toFixed(1) : 'N/A'}</span>
                </div>
                <div className="metric-row">
                  <span className="metric-label">Valuation Multiple</span>
                  <span className="metric-value">
                    {result.peRatio > 35 ? (
                      <span className="badge badge-danger">Premium</span>
                    ) : result.peRatio > 20 ? (
                      <span className="badge badge-warning">Moderate</span>
                    ) : (
                      <span className="badge badge-success">Value</span>
                    )}
                  </span>
                </div>
              </div>

              {/* Card 2: Financial Health */}
              <div className="glass-card card-col-4">
                <h3 className="card-title">⚡ Fundamentals</h3>
                <div className="metric-row">
                  <span className="metric-label">Return on Equity (ROE)</span>
                  <span className="metric-value">{result.roe ? `${result.roe.toFixed(1)}%` : 'N/A'}</span>
                </div>
                <div className="metric-row">
                  <span className="metric-label">Debt to Equity</span>
                  <span className="metric-value">{result.debtToEquity ? result.debtToEquity.toFixed(2) : 'N/A'}</span>
                </div>
                <div className="metric-row">
                  <span className="metric-label">Leverage Risk</span>
                  <span className="metric-value">
                    {result.debtToEquity > 1.5 ? (
                      <span className="badge badge-danger">High Leverage</span>
                    ) : result.debtToEquity > 0.8 ? (
                      <span className="badge badge-warning">Medium Leverage</span>
                    ) : (
                      <span className="badge badge-success">Low Leverage</span>
                    )}
                  </span>
                </div>
              </div>

              {/* Card 3: Overall Health Score circle */}
              <div className="glass-card card-col-4">
                <h3 className="card-title">🏆 Overall Health Score</h3>
                <div className="score-container">
                  <div 
                    className="radial-score" 
                    style={{ '--score': result.overallScore || 50 }}
                  >
                    <div className="score-container">
                      <span className="radial-score-value">{result.overallScore}</span>
                      <span className="radial-score-label">Score</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Card 4: Historical close price Chart */}
              <div className="glass-card card-col-6">
                <div className="chart-header">
                  <h3 className="card-title" style={{ border: 'none', padding: 0, margin: 0 }}>📈 Price History Chart</h3>
                  
                  {/* 1 Month vs 1 Week filter */}
                  <div className="chart-selectors">
                    <button 
                      className={`chart-selector-btn ${historyRange === 30 ? 'active' : ''}`}
                      onClick={() => setHistoryRange(30)}
                    >
                      30 Days
                    </button>
                    <button 
                      className={`chart-selector-btn ${historyRange === 7 ? 'active' : ''}`}
                      onClick={() => setHistoryRange(7)}
                    >
                      7 Days
                    </button>
                  </div>
                </div>

                {getFilteredHistory().length === 0 ? (
                  <div className="watchlist-empty-state" style={{ height: '220px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    Historical data not available.
                  </div>
                ) : (
                  <div className="chart-container-wrapper">
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart data={getFilteredHistory()} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                        <defs>
                          <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="var(--accent-light)" stopOpacity={0.4}/>
                            <stop offset="95%" stopColor="var(--accent-light)" stopOpacity={0.0}/>
                          </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(255,255,255,0.05)" />
                        <XAxis 
                          dataKey="date" 
                          stroke="var(--text-muted)" 
                          fontSize={10} 
                          tickLine={false} 
                          axisLine={false}
                          tickFormatter={(str) => {
                            if (!str) return '';
                            const parts = str.split('-');
                            if (parts.length < 3) return str;
                            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
                            return `${months[parseInt(parts[1], 10) - 1]} ${parts[2]}`;
                          }}
                        />
                        <YAxis 
                          stroke="var(--text-muted)" 
                          fontSize={10} 
                          tickLine={false} 
                          axisLine={false}
                          domain={['auto', 'auto']}
                        />
                        <Tooltip content={<CustomTooltip />} />
                        <Area 
                          type="monotone" 
                          dataKey="price" 
                          stroke="var(--accent-light)" 
                          strokeWidth={2}
                          fillOpacity={1} 
                          fill="url(#colorPrice)" 
                        />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </div>

              {/* Card 5: Detailed Scoring Progress System */}
              <div className="glass-card card-col-6">
                <h3 className="card-title">📊 Detailed Scoring System</h3>
                
                <div className="metric-row-score">
                  <div className="score-details-wrapper">
                    <span className="metric-label">Overall Rating Score</span>
                    <span className="metric-value" style={{ fontWeight: 700 }}>{result.overallScore}/100</span>
                  </div>
                  <div className="score-progress-bg">
                    <div className="score-progress-fill accent" style={{ width: `${result.overallScore}%` }}></div>
                  </div>
                </div>

                <div className="metric-row-score">
                  <div className="score-details-wrapper">
                    <span className="metric-label">Financial Health Score</span>
                    <span className="metric-value">{result.financialScore}/100</span>
                  </div>
                  <div className="score-progress-bg">
                    <div className="score-progress-fill success" style={{ width: `${result.financialScore}%` }}></div>
                  </div>
                </div>

                <div className="metric-row-score">
                  <div className="score-details-wrapper">
                    <span className="metric-label">Technical Momentum Score</span>
                    <span className="metric-value">{result.technicalScore}/100</span>
                  </div>
                  <div className="score-progress-bg">
                    <div className="score-progress-fill info" style={{ width: `${result.technicalScore}%` }}></div>
                  </div>
                </div>

                <div className="metric-row-score">
                  <div className="score-details-wrapper">
                    <span className="metric-label">News Sentiment Score</span>
                    <span className="metric-value">{result.newsScore}/100</span>
                  </div>
                  <div className="score-progress-bg">
                    <div className="score-progress-fill warning" style={{ width: `${result.newsScore}%` }}></div>
                  </div>
                </div>

                <div className="metric-row-score">
                  <div className="score-details-wrapper">
                    <span className="metric-label">Risk Exposure Score</span>
                    <span className="metric-value">{result.riskScore}/100</span>
                  </div>
                  <div className="score-progress-bg">
                    <div className="score-progress-fill danger" style={{ width: `${result.riskScore}%` }}></div>
                  </div>
                </div>
              </div>

              {/* Card 6: Analyst Summary and recommendation details */}
              <div className="glass-card card-col-12">
                <h3 className="card-title">💡 Analyst Intelligence Summary</h3>
                <p className="summary-narrative" style={{ marginBottom: '1.25rem' }}>{result.aiSummary || result.summary}</p>
                
                {/* Recommendation reason card */}
                <div className="recommendation-reason-card">
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700, marginBottom: '0.25rem' }}>
                    Recommendation Explanation
                  </div>
                  <div className="recommendation-reason-text">
                    {result.recommendationReason}
                  </div>
                </div>

                <div className="metric-row" style={{ border: 'none', paddingBottom: 0, marginTop: '1.25rem' }}>
                  <span className="metric-label" style={{ fontSize: '0.85rem', fontWeight: 600 }}>References & Sources:</span>
                  <span className="metric-label" style={{ fontSize: '0.85rem' }}>Confidence: <strong>{result.confidenceScore}%</strong></span>
                </div>
                
                <div className="sources-list" style={{ marginTop: '0.5rem' }}>
                  {result.dataSources && result.dataSources.map((src, index) => (
                    <span key={index} className="source-item">{src}</span>
                  ))}
                </div>
              </div>

            </div>
          )}

        </div>
      </div>
    </div>
  );
}

export default App;
