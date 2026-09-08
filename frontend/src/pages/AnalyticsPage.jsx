import React, { useState, useEffect } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Cell,
  PieChart,
  Pie
} from 'recharts';
import {
  ShieldAlert,
  ShieldCheck,
  Globe,
  AlertTriangle,
  TrendingUp,
  Activity,
  Layers,
  Lock,
  PhoneCall,
  ExternalLink,
  Clock
} from 'lucide-react';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { dashboardService } from '../services/dashboardService';
import { trackThreatFilter } from '../analytics/analytics';

const FILTER_OPTIONS = [
  { key: '24h', label: 'Last 24 Hours' },
  { key: '7d', label: 'Last Week' },
  { key: '30d', label: 'Last Month' },
  { key: '90d', label: 'Last 3 Months' },
  { key: '1y', label: 'Last 1 Year' },
  { key: 'all', label: 'All Time' }
];

const COUNTRY_FLAGS = {
  'India': '🇮🇳',
  'United States': '🇺🇸',
  'United Kingdom': '🇬🇧',
  'Germany': '🇩🇪',
  'Australia': '🇦🇺',
  'Canada': '🇨🇦',
  'France': '🇫🇷',
  'Japan': '🇯🇵',
  'Singapore': '🇸🇬',
  'Netherlands': '🇳🇱',
  'Brazil': '🇧🇷',
  'Russia': '🇷🇺',
  'China': '🇨🇳',
  'South Korea': '🇰🇷',
  'Italy': '🇮🇹',
  'Spain': '🇪🇸',
  'Switzerland': '🇨🇭',
  'Sweden': '🇸🇪',
  'United Arab Emirates': '🇦🇪',
  'Saudi Arabia': '🇸🇦',
  'Israel': '🇮🇱',
  'South Africa': '🇿🇦',
  'Mexico': '🇲🇽',
  'Indonesia': '🇮🇩',
  'Turkey': '🇹🇷',
  'Poland': '🇵🇱',
  'Norway': '🇳🇴',
  'Ireland': '🇮🇪',
  'New Zealand': 'NZ',
  'Argentina': '🇦🇷',
  'Malaysia': '🇲🇾',
  'Vietnam': '🇻🇳',
  'Thailand': '🇹🇭',
  'Philippines': '🇵🇭',
  'Egypt': '🇪🇬',
  'Nigeria': '🇳🇬',
  'Kenya': '🇰🇪',
  'Colombia': '🇨🇴',
  'Chile': '🇨🇱',
  'Belgium': '🇧🇪',
  'Austria': '🇦🇹',
  'Denmark': '🇩🇰',
  'Finland': '🇫🇮',
  'Portugal': '🇵🇹',
  'Greece': '🇬🇷',
  'Czech Republic': '🇨🇿',
  'Romania': '🇷🇴',
  'Hungary': '🇭🇺',
  'Taiwan': '🇹🇼',
  'Hong Kong': '🇭🇰',
  'Ukraine': '🇺🇦',
  'Pakistan': '🇵🇰',
  'Bangladesh': '🇧🇩'
};

const AnalyticsPage = () => {
  const [timeRange, setTimeRange] = useState('90d');
  const [country, setCountry] = useState('all');
  const [countriesList, setCountriesList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const [summary, setSummary] = useState(null);
  const [severityStats, setSeverityStats] = useState([]);
  const [typeStats, setTypeStats] = useState([]);
  const [sourceStats, setSourceStats] = useState([]);
  const [countryStats, setCountryStats] = useState([]);

  // Load available countries on mount
  useEffect(() => {
    dashboardService.getCountries()
      .then((cList) => {
        if (Array.isArray(cList) && cList.length > 0) {
          setCountriesList(cList);
        }
      })
      .catch(console.warn);
  }, []);

  const fetchAnalytics = async (range = timeRange, c = country, isSilent = false) => {
    try {
      if (!isSilent) setLoading(true);
      else setIsRefreshing(true);

      const [sev, types, sources, summ, cStats] = await Promise.all([
        dashboardService.getBySeverity(range, c),
        dashboardService.getByType(range, c),
        dashboardService.getBySource(range, c),
        dashboardService.getSummary(range, c),
        dashboardService.getCountryStats(range)
      ]);

      setSeverityStats(sev);
      setTypeStats(types);
      setSourceStats(sources);
      setSummary(summ);
      setCountryStats(cStats || []);
    } catch (err) {
      console.error('Failed to load analytics:', err);
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchAnalytics(timeRange, country, false);
  }, [timeRange, country]);

  const getSeverityColor = (sev) => {
    switch (sev) {
      case 'CRITICAL': return '#dc2626';
      case 'HIGH': return '#ef4444';
      case 'MEDIUM': return '#f59e0b';
      case 'LOW': return '#10b981';
      default: return '#64748b';
    }
  };

  const currentOption = FILTER_OPTIONS.find(opt => opt.key === timeRange) || FILTER_OPTIONS[2];
  const countryName = (country === 'all' || !country) ? 'All Regions (Global)' : country;
  const flag = COUNTRY_FLAGS[country] || '🌐';

  return (
    <div className="analytics-page">
      {/* Header */}
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '1.6rem', fontWeight: 700, color: '#0f172a', marginBottom: '6px' }}>
          Cyber Threat Intelligence & Incident Analytics
        </h2>
        <p style={{ fontSize: '0.88rem', color: '#64748b' }}>
          Interactive telemetry analysis tracking malicious cyber activity, high-risk banking scams, and attack distributions.
        </p>
      </div>

      {/* Filter Control Bar */}
      <div className="time-filter-bar" style={{ marginBottom: '24px' }}>
        <div className="time-filter-left">
          {/* Time Filter Group */}
          <div className="time-filter-group-wrapper">
            <div className="time-filter-label">
              <Clock size={15} color="#2563eb" />
              <span>Timeframe:</span>
            </div>
            <div className="time-filter-group">
              {FILTER_OPTIONS.map((option) => (
                <button
                  key={option.key}
                  type="button"
                  className={`time-filter-btn ${timeRange === option.key ? 'active' : ''}`}
                  onClick={() => {
                    setTimeRange(option.key);
                    trackThreatFilter('analytics_time_range', option.key);
                  }}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          {/* Country Filter */}
          <div className="country-filter-wrapper">
            <div className="time-filter-label">
              <Globe size={15} color="#059669" />
              <span>Target Country:</span>
            </div>
            <select
              className="country-select-filter"
              value={country}
              onChange={(e) => {
                const val = e.target.value;
                setCountry(val);
                trackThreatFilter('analytics_country', val);
              }}
            >
              <option value="all">🌐 All Countries</option>
              {countriesList.map((c) => (
                <option key={c} value={c}>
                  {COUNTRY_FLAGS[c] ? `${COUNTRY_FLAGS[c]} ${c}` : `📍 ${c}`}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Live Filter Indicator */}
        <div className="time-filter-status">
          <span className="time-filter-badge">
            <span className="time-filter-pulse"></span>
            <span>
              {flag} {countryName} · <strong>{summary?.totalThreats?.toLocaleString() || 0}</strong> Verified Incidents
            </span>
          </span>
        </div>
      </div>

      {loading && !summary ? (
        <LoadingSpinner message="Aggregating cyber threat analytics from PostgreSQL database..." />
      ) : (
        <>
          {/* Executive Security KPI Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '18px', marginBottom: '24px' }}>
            <div className="card-container" style={{ padding: '20px', borderLeft: '4px solid #2563eb' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#64748b' }}>Total Analyzed Incidents</span>
                <Activity size={18} color="#2563eb" />
              </div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: '1.8rem', fontWeight: 700, color: '#0f172a' }}>
                {summary?.totalThreats?.toLocaleString() || 0}
              </div>
              <div style={{ fontSize: '0.76rem', color: '#64748b', marginTop: '6px' }}>
                Active in {countryName} ({currentOption.label})
              </div>
            </div>

            <div className="card-container" style={{ padding: '20px', borderLeft: '4px solid #dc2626' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#64748b' }}>Critical Risk Threats</span>
                <ShieldAlert size={18} color="#dc2626" />
              </div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: '1.8rem', fontWeight: 700, color: '#dc2626' }}>
                {summary?.criticalThreats?.toLocaleString() || 0}
              </div>
              <div style={{ fontSize: '0.76rem', color: '#991b1b', marginTop: '6px' }}>
                Requires immediate blocking by network admins
              </div>
            </div>

            <div className="card-container" style={{ padding: '20px', borderLeft: '4px solid #8b5cf6' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#64748b' }}>Phishing vs Malware</span>
                <Layers size={18} color="#8b5cf6" />
              </div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: '1.5rem', fontWeight: 700, color: '#2563eb' }}>
                  {summary?.phishingCount || 0}
                </span>
                <span style={{ color: '#94a3b8', fontSize: '0.9rem' }}>/</span>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: '1.5rem', fontWeight: 700, color: '#8b5cf6' }}>
                  {summary?.malwareCount || 0}
                </span>
              </div>
              <div style={{ fontSize: '0.76rem', color: '#64748b', marginTop: '6px' }}>
                Phishing URLs vs Malware Binaries
              </div>
            </div>

            <div className="card-container" style={{ padding: '20px', borderLeft: '4px solid #d97706' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#64748b' }}>Cyber Fraud & Scam Telemetry</span>
                <AlertTriangle size={18} color="#d97706" />
              </div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: '1.8rem', fontWeight: 700, color: '#d97706' }}>
                {summary?.fraudCount?.toLocaleString() || 0}
              </div>
              <div style={{ fontSize: '0.76rem', color: '#92400e', marginTop: '6px' }}>
                Reported financial scams & extortion cases
              </div>
            </div>
          </div>

          {/* Middle Charts Row: Geographic Distribution & Threat Classification */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))', gap: '24px', marginBottom: '24px' }}>
            {/* Country Distribution Bar Chart */}
            <div className="card-container" style={{ padding: '22px' }}>
              <div className="card-header-clean">
                <h3 className="card-title-clean">
                  <Globe size={18} color="#059669" />
                  <span>Geographic Incident Distribution (Top Countries)</span>
                </h3>
              </div>
              <div style={{ width: '100%', height: 280 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={countryStats.slice(0, 8)}
                    layout="vertical"
                    margin={{ top: 5, right: 30, left: 35, bottom: 5 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e2e8f0" />
                    <XAxis type="number" stroke="#64748b" style={{ fontSize: '0.76rem' }} />
                    <YAxis
                      dataKey="country"
                      type="category"
                      stroke="#64748b"
                      style={{ fontSize: '0.78rem', fontWeight: 500 }}
                      width={90}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px',
                        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                        fontSize: '0.82rem'
                      }}
                      formatter={(val, name, item) => [`${val.toLocaleString()} incidents (${item.payload.percentage}%)`, 'Total']}
                    />
                    <Bar dataKey="count" fill="#059669" radius={[0, 6, 6, 0]}>
                      {countryStats.slice(0, 8).map((entry, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={['#059669', '#2563eb', '#8b5cf6', '#d97706', '#0284c7', '#10b981', '#6366f1', '#ec4899'][index % 8]}
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Threat Type Breakdown */}
            <div className="card-container" style={{ padding: '22px' }}>
              <div className="card-header-clean">
                <h3 className="card-title-clean">
                  <Layers size={18} color="#2563eb" />
                  <span>Threat Category Breakdown ({countryName})</span>
                </h3>
              </div>
              <div style={{ width: '100%', height: 280 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={typeStats} layout="vertical" margin={{ top: 5, right: 30, left: 45, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e2e8f0" />
                    <XAxis type="number" stroke="#64748b" style={{ fontSize: '0.76rem' }} />
                    <YAxis
                      dataKey="threatType"
                      type="category"
                      stroke="#64748b"
                      style={{ fontSize: '0.76rem', fontWeight: 500 }}
                      width={100}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px',
                        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                        fontSize: '0.82rem'
                      }}
                      formatter={(val, name, item) => [`${val} threats (${item.payload.percentage}%)`, 'Count']}
                    />
                    <Bar dataKey="count" fill="#2563eb" radius={[0, 6, 6, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Lower Row: Severity Risk Assessment & Intelligence Feed Share */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(460px, 1fr))', gap: '24px', marginBottom: '24px' }}>
            {/* Severity Distribution */}
            <div className="card-container" style={{ padding: '22px' }}>
              <div className="card-header-clean">
                <h3 className="card-title-clean">
                  <ShieldAlert size={18} color="#dc2626" />
                  <span>Severity Impact & Risk Evaluation</span>
                </h3>
              </div>
              <div style={{ width: '100%', height: 250 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={severityStats} margin={{ top: 10, right: 20, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis dataKey="severity" stroke="#64748b" style={{ fontSize: '0.78rem' }} />
                    <YAxis stroke="#64748b" style={{ fontSize: '0.78rem' }} />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        border: '1px solid #e2e8f0',
                        borderRadius: '8px',
                        fontSize: '0.82rem'
                      }}
                      formatter={(val, name, item) => [`${val} (${item.payload.percentage}%)`, 'Count']}
                    />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                      {severityStats.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={getSeverityColor(entry.severity)} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Intelligence Source Feeds Table */}
            <div className="card-container" style={{ padding: '22px' }}>
              <div className="card-header-clean">
                <h3 className="card-title-clean">
                  <ShieldCheck size={18} color="#059669" />
                  <span>Intelligence Sources Active in {countryName}</span>
                </h3>
              </div>
              <div className="table-responsive">
                <table className="custom-table">
                  <thead>
                    <tr>
                      <th>Source Feed</th>
                      <th>Incidents Reported</th>
                      <th>Share</th>
                      <th>Reliability</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sourceStats.map((src, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 600, color: '#0f172a', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span style={{ width: '9px', height: '9px', borderRadius: '50%', backgroundColor: src.color || '#3b82f6' }}></span>
                          {src.source}
                        </td>
                        <td>{src.count.toLocaleString()}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span style={{ fontWeight: 600 }}>{src.percentage}%</span>
                            <div style={{ width: '60px', height: '6px', backgroundColor: '#e2e8f0', borderRadius: '3px', overflow: 'hidden' }}>
                              <div style={{ height: '100%', width: `${src.percentage}%`, backgroundColor: src.color || '#3b82f6' }}></div>
                            </div>
                          </div>
                        </td>
                        <td style={{ color: '#16a34a', fontWeight: 600, fontSize: '0.8rem' }}>
                          ✓ High Confidence
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Actionable Citizen Guidance & Scam Insights Banner */}
          <div className="card-container" style={{ padding: '22px', backgroundColor: '#f8fafc', border: '1px solid #e2e8f0' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px', flexWrap: 'wrap', gap: '10px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Lock size={18} color="#2563eb" />
                <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a' }}>
                  Actionable Security Advisory for {countryName}
                </h4>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.82rem' }}>
                <span style={{ color: '#64748b' }}>Emergency Assistance:</span>
                <a
                  href="https://cybercrime.gov.in"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ color: '#2563eb', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px', textDecoration: 'none' }}
                >
                  <span>Helpline 1930 / cybercrime.gov.in</span>
                  <ExternalLink size={13} />
                </a>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px', fontSize: '0.84rem' }}>
              <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div style={{ fontWeight: 600, color: '#dc2626', marginBottom: '4px' }}>🔴 Critical Risk URLs</div>
                <p style={{ color: '#475569', lineHeight: 1.5 }}>
                  Banking phishing and C2 server links stealing user credentials. Never enter passwords or card numbers on untrusted domains.
                </p>
              </div>

              <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div style={{ fontWeight: 600, color: '#f59e0b', marginBottom: '4px' }}>🟡 High Risk Social Scams</div>
                <p style={{ color: '#475569', lineHeight: 1.5 }}>
                  Fake courier notifications, electricity bill disconnection threats, and digital arrest calls aiming to induce panic.
                </p>
              </div>

              <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div style={{ fontWeight: 600, color: '#10b981', marginBottom: '4px' }}>🟢 Defense Best Practice</div>
                <p style={{ color: '#475569', lineHeight: 1.5 }}>
                  Always verify official domains (e.g. <code>.gov.in</code>, <code>.sbi</code>, <code>.bank</code>) and enable two-factor authentication on all sensitive accounts.
                </p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default AnalyticsPage;
