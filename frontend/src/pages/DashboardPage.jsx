import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Link2, Bug, ShieldAlert, Database, Activity, RefreshCw } from 'lucide-react';
import StatCard from '../components/common/StatCard';
import TimeRangeFilter from '../components/dashboard/TimeRangeFilter';
import ActivityChart from '../components/dashboard/ActivityChart';
import SourceDonutChart from '../components/dashboard/SourceDonutChart';
import AboutCard from '../components/dashboard/AboutCard';
import ThreatTable from '../components/threats/ThreatTable';
import ThreatDetailsModal from '../components/threats/ThreatDetailsModal';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { dashboardService } from '../services/dashboardService';
import { threatService } from '../services/threatService';
import { trackThreatFilter, trackThreatDetailView } from '../analytics/analytics';

const DashboardPage = ({ refreshKey }) => {
  const navigate = useNavigate();
  const [timeRange, setTimeRange] = useState('24h');
  const [country, setCountry] = useState('all');
  const [countriesList, setCountriesList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [summary, setSummary] = useState(null);
  const [activityData, setActivityData] = useState([]);
  const [sourceData, setSourceData] = useState([]);
  const [latestThreats, setLatestThreats] = useState([]);
  const [selectedThreat, setSelectedThreat] = useState(null);

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

  const fetchDashboardData = useCallback(async (range = timeRange, c = country, isSilent = false) => {
    try {
      if (!isSilent) setLoading(true);
      else setIsRefreshing(true);
      setError(null);

      const [summaryRes, activityRes, sourceRes, threatsRes] = await Promise.all([
        dashboardService.getSummary(range, c),
        dashboardService.getActivity(range, c),
        dashboardService.getBySource(range, c),
        threatService.getLatestThreats(10, range, c)
      ]);

      setSummary(summaryRes);
      setActivityData(activityRes);
      setSourceData(sourceRes);
      setLatestThreats(threatsRes);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      if (!isSilent) {
        setError('Unable to connect to the threat intelligence server. Please ensure the backend application is running on port 8080.');
      }
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  }, [timeRange, country]);

  // Initial fetch on mount or global refreshKey change
  useEffect(() => {
    fetchDashboardData(timeRange, country, false);
  }, [refreshKey]);

  // Handle user changing the time filter
  const handleTimeRangeChange = (newRange) => {
    setTimeRange(newRange);
    trackThreatFilter('time_range', newRange);
    fetchDashboardData(newRange, country, false);
  };

  // Handle user changing the country filter
  const handleCountryChange = (newCountry) => {
    setCountry(newCountry);
    trackThreatFilter('country', newCountry);
    fetchDashboardData(timeRange, newCountry, false);
  };

  // Automated background polling every 30 seconds so normal users always see fresh DB data
  useEffect(() => {
    const interval = setInterval(() => {
      fetchDashboardData(timeRange, country, true);
    }, 30000);
    return () => clearInterval(interval);
  }, [fetchDashboardData, timeRange, country]);

  if (loading && !summary) {
    return <LoadingSpinner message="Aggregating dynamic threat telemetry from PostgreSQL database..." />;
  }

  if (error && !summary) {
    return (
      <div className="card-container" style={{ textAlign: 'center', padding: '40px 20px' }}>
        <div style={{ color: '#ef4444', marginBottom: '12px' }}>
          <ShieldAlert size={44} style={{ margin: '0 auto' }} />
        </div>
        <h3 style={{ fontSize: '1.2rem', marginBottom: '8px', color: '#0f172a' }}>
          Database Connection Offline
        </h3>
        <p style={{ color: '#64748b', maxWidth: '500px', margin: '0 auto 20px', fontSize: '0.9rem' }}>
          {error}
        </p>
        <button className="btn-apply-filters" style={{ width: 'auto', padding: '8px 24px' }} onClick={() => fetchDashboardData(timeRange, country, false)}>
          Retry Connection
        </button>
      </div>
    );
  }

  const formatTrend = (dailyInc) => {
    switch (timeRange) {
      case '24h': return `+${dailyInc || 0} today`;
      case '7d': return 'Last 7 Days';
      case '30d': return 'Last 30 Days';
      case '90d':
      case '3m': return 'Past 90 Days';
      case '1y': return 'Past 1 Year';
      case 'all': return 'All Time';
      default: return 'Active Period';
    }
  };

  const getTimeRangeLabel = (range) => {
    switch (range) {
      case '24h': return 'Last 24 Hours';
      case '7d': return 'Last 7 Days';
      case '30d': return 'Last 30 Days';
      case '90d':
      case '3m': return 'Last 90 Days';
      case '1y': return 'Last 1 Year';
      case 'all': return 'All Time';
      default: return 'Last 24 Hours';
    }
  };

  const getThreatsUrl = (threatType) => {
    const params = new URLSearchParams();
    if (threatType) params.set('threatType', threatType);
    if (country && country !== 'all' && country !== 'All Countries') params.set('country', country);
    const query = params.toString();
    return query ? `/threats?${query}` : '/threats';
  };

  const countryDisplayName = (country === 'all' || !country) ? 'All Regions (Global)' : country;

  return (
    <div className="dashboard-page">
      {/* Live Ingestion Telemetry Banner */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: '16px',
        padding: '10px 16px',
        backgroundColor: '#f8fafc',
        borderRadius: '8px',
        border: '1px solid #e2e8f0',
        fontSize: '0.82rem',
        color: '#64748b',
        flexWrap: 'wrap',
        gap: '8px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Activity size={16} color="#2563eb" />
          <span>
            Database Telemetry: <strong style={{ color: '#0f172a' }}>{summary?.totalThreats?.toLocaleString() || 0}</strong> verified incidents in <strong style={{ color: '#2563eb' }}>{countryDisplayName}</strong> ({getTimeRangeLabel(timeRange)})
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{
            display: 'inline-block',
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            backgroundColor: '#10b981'
          }}></span>
          <span style={{ color: '#166534', fontWeight: 600 }}>Live Threat Protection Online</span>
          {isRefreshing && (
            <RefreshCw size={12} className="spin" style={{ marginLeft: '4px', color: '#2563eb' }} />
          )}
        </div>
      </div>

      {/* Dynamic Time Range & Country Filter Bar */}
      <TimeRangeFilter
        activeRange={timeRange}
        onRangeChange={handleTimeRangeChange}
        activeCountry={country}
        onCountryChange={handleCountryChange}
        countries={countriesList}
        totalThreats={summary?.totalThreats ?? 0}
        isRefreshing={isRefreshing}
      />

      {/* 4 Stat Cards Row - 100% Dynamic from Database based on Time & Country Filter */}
      <section className="stat-cards-grid">
        <StatCard
          iconColor="blue"
          icon={Link2}
          label="Phishing URLs"
          value={summary?.phishingCount ?? 0}
          trend={formatTrend(summary?.todayPhishingIncrease)}
          sourceText="Source: OpenPhish + TweetFeed"
          onClick={() => navigate(getThreatsUrl('PHISHING'))}
        />

        <StatCard
          iconColor="purple"
          icon={Bug}
          label="Malware URLs"
          value={summary?.malwareCount ?? 0}
          trend={formatTrend(summary?.todayMalwareIncrease)}
          sourceText="Source: URLhaus + ThreatFox"
          onClick={() => navigate(getThreatsUrl('MALWARE'))}
        />

        <StatCard
          iconColor="red"
          icon={ShieldAlert}
          label="Fraud Reports"
          value={summary?.fraudCount ?? 0}
          trend={formatTrend(summary?.todayFraudIncrease)}
          sourceText="Source: TweetFeed OSINT Telemetry"
          onClick={() => navigate(getThreatsUrl('FRAUD'))}
        />

        <StatCard
          iconColor="green"
          icon={Database}
          label="Total Threats"
          value={summary?.totalThreats ?? 0}
          trend={formatTrend(summary?.todayTotalIncrease)}
          sourceText="Combined verified threat telemetry"
          onClick={() => navigate(getThreatsUrl())}
        />
      </section>

      {/* Middle Grid: Balanced 2 Columns fitting full page width and height */}
      <section className="dashboard-middle-grid">
        <ActivityChart
          data={activityData}
          timeRange={timeRange}
          country={country}
        />

        <SourceDonutChart
          data={sourceData}
          total={summary?.totalThreats ?? 0}
          country={country}
        />
      </section>

      {/* Citizen Safety & Incident Guidance Card */}
      <AboutCard />

      {/* Lower Section: Threats Table filtered by time window and country */}
      <section style={{ marginBottom: '32px' }}>
        <ThreatTable
          threats={latestThreats}
          title={`Threat Incidents (${countryDisplayName} · ${getTimeRangeLabel(timeRange)})`}
          showViewAll={true}
          viewAllUrl={getThreatsUrl()}
          onRowClick={(threat) => {
            trackThreatDetailView(threat.id, threat.threatType, threat.severity);
            setSelectedThreat(threat);
          }}
        />
      </section>

      {/* Interactive Threat Details Modal */}
      {selectedThreat && (
        <ThreatDetailsModal
          threat={selectedThreat}
          onClose={() => setSelectedThreat(null)}
        />
      )}
    </div>
  );
};

export default DashboardPage;
