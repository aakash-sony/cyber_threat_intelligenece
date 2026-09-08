import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Filter, RotateCcw, Download, ChevronLeft, ChevronRight, ShieldAlert, ArrowUpDown } from 'lucide-react';
import ThreatTable from '../components/threats/ThreatTable';
import ThreatDetailsModal from '../components/threats/ThreatDetailsModal';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { threatService } from '../services/threatService';
import { dashboardService } from '../services/dashboardService';
import {
  trackThreatSearch,
  trackThreatFilter,
  trackThreatDetailView,
  trackThreatExport
} from '../analytics/analytics';

const AllThreatsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [threats, setThreats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(15);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedThreat, setSelectedThreat] = useState(null);
  const [dynamicSources, setDynamicSources] = useState([]);

  // Filter States
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [severity, setSeverity] = useState(searchParams.get('severity') || '');
  const [threatType, setThreatType] = useState(searchParams.get('threatType') || '');
  const [country, setCountry] = useState(searchParams.get('country') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [source, setSource] = useState(searchParams.get('source') || '');
  const [timeRange, setTimeRange] = useState(searchParams.get('timeRange') || '');
  const [sortBy, setSortBy] = useState('lastSeen');
  const [direction, setDirection] = useState('desc');
  const [dynamicCountries, setDynamicCountries] = useState([]);

  // Load dynamic sources and countries on mount
  useEffect(() => {
    threatService.getDistinctSources()
      .then((sources) => {
        if (Array.isArray(sources) && sources.length > 0) {
          setDynamicSources(sources);
        }
      })
      .catch((err) => console.warn('Could not load distinct sources:', err.message));

    dashboardService.getCountries()
      .then((countries) => {
        if (Array.isArray(countries) && countries.length > 0) {
          setDynamicCountries(countries);
        }
      })
      .catch(console.warn);
  }, []);

  const fetchThreats = async () => {
    try {
      setLoading(true);
      const params = {
        page,
        size: pageSize,
        sortBy,
        direction
      };

      if (keyword.trim()) params.keyword = keyword.trim();
      if (severity) params.severity = severity;
      if (threatType && threatType !== 'ALL') params.threatType = threatType;
      if (country && country !== 'ALL' && country !== 'all') params.country = country;
      if (status && status !== 'ALL') params.status = status;
      if (source && source !== 'ALL') params.source = source;
      if (timeRange && timeRange !== 'ALL' && timeRange !== 'all') params.timeRange = timeRange;

      const data = await threatService.getThreats(params);
      setThreats(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load threats:', err);
    } finally {
      setLoading(false);
    }
  };

  // Sync URL search params whenever they change
  useEffect(() => {
    const qKeyword = searchParams.get('keyword') || '';
    const qSeverity = searchParams.get('severity') || '';
    const qThreatType = searchParams.get('threatType') || '';
    const qCountry = searchParams.get('country') || '';
    const qStatus = searchParams.get('status') || '';
    const qSource = searchParams.get('source') || '';
    const qTimeRange = searchParams.get('timeRange') || '';

    setKeyword(qKeyword);
    setSeverity(qSeverity);
    setThreatType(qThreatType);
    setCountry(qCountry);
    setStatus(qStatus);
    setSource(qSource);
    setTimeRange(qTimeRange);
    setPage(0);
  }, [searchParams]);

  useEffect(() => {
    fetchThreats();
  }, [page, pageSize, sortBy, direction, severity, threatType, country, status, source, timeRange]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (keyword.trim()) {
      trackThreatSearch(keyword.trim(), totalElements);
    }
    setPage(0);
    fetchThreats();
  };

  const handleResetFilters = () => {
    setKeyword('');
    setSeverity('');
    setThreatType('');
    setCountry('');
    setStatus('');
    setSource('');
    setTimeRange('');
    setSortBy('lastSeen');
    setDirection('desc');
    setPage(0);
    setSearchParams({});
    setTimeout(fetchThreats, 50);
  };

  const handleExportCsv = () => {
    if (!threats || threats.length === 0) return;
    trackThreatExport('csv', threats.length);
    const headers = ['ID', 'Indicator', 'Country', 'Threat Type', 'Severity', 'Confidence', 'Source', 'Target', 'Status', 'Last Seen'];
    const rows = threats.map((t) => [
      t.id,
      `"${(t.indicator || '').replace(/"/g, '""')}"`,
      `"${t.country || 'Global'}"`,
      t.threatType,
      t.severity,
      t.confidence,
      `"${t.source || ''}"`,
      `"${(t.target || '').replace(/"/g, '""')}"`,
      t.status,
      t.lastSeen || t.createdAt
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `threat_indicators_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="threats-page">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '1.5rem', fontWeight: 700, color: '#0f172a' }}>
            All Threat Intelligence Records
          </h2>
          <p style={{ fontSize: '0.86rem', color: '#64748b' }}>
            Explore normalized indicators of compromise (IOCs), search by parameters, and inspect detailed telemetry.
          </p>
        </div>

        {threats.length > 0 && (
          <button
            className="btn-secondary"
            onClick={handleExportCsv}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px', fontSize: '0.84rem' }}
            title="Export currently displayed indicators to CSV format"
          >
            <Download size={15} color="#2563eb" />
            <span>Export Feed (CSV)</span>
          </button>
        )}
      </div>

      {/* Filter Toolbar */}
      <div className="filter-toolbar">
        <form onSubmit={handleSearchSubmit}>
          <div className="search-input-wrapper">
            <Search size={18} color="#64748b" />
            <input
              type="text"
              className="search-input"
              placeholder="Search by indicator (IP, URL, domain, hash), tags, country, or target..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
            <button type="submit" className="btn-apply-filters" style={{ width: 'auto', padding: '6px 18px' }}>
              Search
            </button>
          </div>
        </form>

        <div className="filter-dropdowns-row">
          <div>
            <label className="form-label-filter">Severity</label>
            <select
              className="form-select-filter"
              value={severity}
              onChange={(e) => {
                const val = e.target.value;
                setSeverity(val);
                if (val) trackThreatFilter('severity', val);
                setPage(0);
              }}
            >
              <option value="">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>

          <div>
            <label className="form-label-filter">Threat Type</label>
            <select
              className="form-select-filter"
              value={threatType}
              onChange={(e) => {
                const val = e.target.value;
                setThreatType(val);
                if (val) trackThreatFilter('threat_type', val);
                setPage(0);
              }}
            >
              <option value="">All Types</option>
              <option value="PHISHING">Phishing</option>
              <option value="MALWARE">Malware</option>
              <option value="FRAUD">Fraud</option>
              <option value="COMMAND_AND_CONTROL">Command & Control</option>
              <option value="BOTNET">Botnet</option>
              <option value="RANSOMWARE">Ransomware</option>
              <option value="SCANNING">Scanning</option>
              <option value="SPAM">Spam</option>
              <option value="SUSPICIOUS">Suspicious</option>
            </select>
          </div>

          <div>
            <label className="form-label-filter">Country</label>
            <select
              className="form-select-filter"
              value={country}
              onChange={(e) => {
                const val = e.target.value;
                setCountry(val);
                if (val) trackThreatFilter('country', val);
                setPage(0);
              }}
            >
              <option value="">All Countries</option>
              {dynamicCountries.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label-filter">Time Range</label>
            <select
              className="form-select-filter"
              value={timeRange}
              onChange={(e) => {
                const val = e.target.value;
                setTimeRange(val);
                if (val) trackThreatFilter('time_range', val);
                setPage(0);
              }}
            >
              <option value="">All Time</option>
              <option value="24h">Last 24 Hours</option>
              <option value="7d">Last 7 Days</option>
              <option value="30d">Last 30 Days</option>
              <option value="90d">Last 90 Days</option>
              <option value="1y">Last 1 Year</option>
            </select>
          </div>

          <div>
            <label className="form-label-filter">Source</label>
            <select
              className="form-select-filter"
              value={source}
              onChange={(e) => {
                const val = e.target.value;
                setSource(val);
                if (val) trackThreatFilter('source', val);
                setPage(0);
              }}
            >
              <option value="">All Sources</option>
              {dynamicSources.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label-filter">Status</label>
            <select
              className="form-select-filter"
              value={status}
              onChange={(e) => {
                const val = e.target.value;
                setStatus(val);
                if (val) trackThreatFilter('status', val);
                setPage(0);
              }}
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="RESOLVED">Resolved</option>
            </select>
          </div>

          <div>
            <label className="form-label-filter">Sort By</label>
            <select
              className="form-select-filter"
              value={`${sortBy}-${direction}`}
              onChange={(e) => {
                const [newSort, newDir] = e.target.value.split('-');
                setSortBy(newSort);
                setDirection(newDir);
                setPage(0);
              }}
            >
              <option value="lastSeen-desc">Latest Activity (Newest)</option>
              <option value="lastSeen-asc">Oldest Activity</option>
              <option value="confidence-desc">Highest Confidence</option>
              <option value="confidence-asc">Lowest Confidence</option>
              <option value="severity-desc">Severity (High to Low)</option>
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button
              type="button"
              className="btn-secondary"
              onClick={handleResetFilters}
              style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
            >
              <RotateCcw size={15} /> Reset
            </button>
          </div>
        </div>
      </div>

      {/* Threats Table */}
      {loading ? (
        <LoadingSpinner message="Querying PostgreSQL threat records..." />
      ) : threats.length === 0 ? (
        <EmptyState
          title="No matching threats found"
          message="Try broadening your filter criteria or searching for different indicators."
          actionText="Reset All Filters"
          onAction={handleResetFilters}
        />
      ) : (
        <>
          <ThreatTable
            threats={threats}
            title={`Threats List (${totalElements.toLocaleString()} Total Records)`}
            showViewAll={false}
            startIndex={page * pageSize + 1}
            onRowClick={(threat) => {
              trackThreatDetailView(threat.id, threat.threatType, threat.severity);
              setSelectedThreat(threat);
            }}
          />

          {/* Pagination Controls */}
          <div className="pagination-row">
            <div className="pagination-info">
              Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, totalElements)} of {totalElements} threats
            </div>
            <div className="pagination-actions">
              <button
                className="pagination-btn"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                <ChevronLeft size={16} /> Prev
              </button>
              <span style={{ fontSize: '0.84rem', fontWeight: 600, color: '#334155', padding: '0 8px' }}>
                Page {page + 1} of {totalPages}
              </span>
              <button
                className="pagination-btn"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Next <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </>
      )}

      {/* Detail Modal */}
      {selectedThreat && (
        <ThreatDetailsModal
          threat={selectedThreat}
          onClose={() => setSelectedThreat(null)}
        />
      )}
    </div>
  );
};

export default AllThreatsPage;
