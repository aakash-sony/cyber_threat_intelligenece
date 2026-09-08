import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  ExternalLink,
  Copy,
  Check,
  ShieldAlert,
  Globe,
  Tag,
  ShieldCheck,
  Info,
  Layers,
  Clock
} from 'lucide-react';
import SeverityBadge from '../components/common/SeverityBadge';
import ThreatTypeBadge from '../components/common/ThreatTypeBadge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { threatService } from '../services/threatService';
import {
  getExactSourceUrl,
  getVirusTotalUrl,
  getSourceActionLabel
} from '../utils/sourceUrlHelper';
import {
  trackThreatDetailView,
  trackIndicatorCopy,
  trackExternalSourceClick
} from '../analytics/analytics';

const ThreatDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [threat, setThreat] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const fetchThreat = async () => {
      try {
        setLoading(true);
        const data = await threatService.getThreatById(id);
        setThreat(data);
        if (data) {
          trackThreatDetailView(data.id || id, data.threatType, data.severity);
        }
      } catch (err) {
        console.error('Failed to load threat details:', err);
        setError('Threat record not found or inaccessible.');
      } finally {
        setLoading(false);
      }
    };

    if (id) fetchThreat();
  }, [id]);

  const exactSourceUrl = threat ? getExactSourceUrl(threat) : null;
  const vtUrl = threat?.indicator ? getVirusTotalUrl(threat.indicator) : null;
  const actionLabel = threat ? getSourceActionLabel(threat) : 'View Original Source';

  const handleCopy = () => {
    if (threat?.indicator) {
      navigator.clipboard.writeText(threat.indicator);
      trackIndicatorCopy(threat.threatType);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleOpenSource = () => {
    if (exactSourceUrl) {
      trackExternalSourceClick(threat?.source || 'intelligence_source', exactSourceUrl);
      window.open(exactSourceUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const handleOpenVirusTotal = () => {
    if (vtUrl) {
      trackExternalSourceClick('VirusTotal', vtUrl);
      window.open(vtUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString('en-US', {
      dateStyle: 'medium',
      timeStyle: 'medium'
    });
  };

  if (loading) return <LoadingSpinner message={`Loading threat details for record #${id}...`} />;

  if (error || !threat) {
    return (
      <div className="card-container" style={{ textAlign: 'center', padding: '40px 20px' }}>
        <h3 style={{ color: '#ef4444', marginBottom: '8px' }}>Threat Record Not Found</h3>
        <p style={{ color: '#64748b', marginBottom: '20px' }}>{error || 'Invalid threat ID provided.'}</p>
        <button className="btn-secondary" onClick={() => navigate('/threats')}>
          <ArrowLeft size={16} /> Back to Threats
        </button>
      </div>
    );
  }

  return (
    <div className="threat-detail-page">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
        <button className="btn-secondary" onClick={() => navigate(-1)} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ArrowLeft size={16} /> Back to Threats
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          {vtUrl && (
            <button
              className="btn-secondary"
              onClick={handleOpenVirusTotal}
              title="Scan indicator across 70+ antivirus & security engines"
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <ShieldCheck size={16} color="#2563eb" />
              <span>VirusTotal OSINT</span>
            </button>
          )}

          {exactSourceUrl && (
            <button
              className="btn-view-source"
              onClick={handleOpenSource}
              title={`Opens ${actionLabel}`}
            >
              <span>{actionLabel}</span>
              <ExternalLink size={16} />
            </button>
          )}
        </div>
      </div>

      <div className="card-container" style={{ padding: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                width: '46px',
                height: '46px',
                borderRadius: '12px',
                backgroundColor: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <ShieldAlert size={26} />
            </div>
            <div>
              <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '1.6rem', fontWeight: 700, color: '#0f172a' }}>
                Threat Indicator Telemetry
              </h2>
              <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                Record #{threat.id} • Ingested from verified source: <strong>{threat.source}</strong>
              </p>
            </div>
          </div>

          <span
            style={{
              fontSize: '0.78rem',
              fontWeight: 600,
              padding: '4px 12px',
              borderRadius: '999px',
              backgroundColor: '#dcfce7',
              color: '#166534',
              border: '1px solid #86efac',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            <Globe size={13} />
            Verified OSINT Feed Record
          </span>
        </div>

        {/* Indicator Box */}
        <div className="detail-indicator-box" style={{ fontSize: '1.05rem', padding: '16px 20px', marginBottom: '24px' }}>
          <span style={{ wordBreak: 'break-all' }}>{threat.indicator}</span>
          <button className="copy-btn" onClick={handleCopy} style={{ padding: '6px 14px', fontSize: '0.82rem', flexShrink: 0 }}>
            {copied ? (
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#4ade80' }}>
                <Check size={16} /> Copied!
              </span>
            ) : (
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Copy size={16} /> Copy Indicator
              </span>
            )}
          </button>
        </div>

        {/* Feed Description & Technical Context */}
        {threat.description && (
          <div
            style={{
              backgroundColor: '#f8fafc',
              border: '1px solid #e2e8f0',
              borderRadius: '10px',
              padding: '16px 20px',
              marginBottom: '24px'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
              <Info size={16} color="#2563eb" />
              <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#1e293b', margin: 0 }}>
                Feed Telemetry & Description
              </h4>
            </div>
            <p style={{ fontSize: '0.88rem', color: '#475569', margin: 0, lineHeight: 1.6 }}>
              {threat.description}
            </p>
          </div>
        )}

        {/* 2-column Metadata Grid */}
        <div className="detail-grid" style={{ marginBottom: '28px' }}>
          <div className="detail-item">
            <span className="detail-label">Indicator Type</span>
            <span className="detail-value" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Layers size={14} color="#64748b" />
              {threat.indicatorType || 'URL'}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">Threat Classification</span>
            <div>
              <ThreatTypeBadge type={threat.threatType} />
            </div>
          </div>

          <div className="detail-item">
            <span className="detail-label">Severity Level</span>
            <div>
              <SeverityBadge severity={threat.severity} />
            </div>
          </div>

          <div className="detail-item">
            <span className="detail-label">Confidence Assessment</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
              <span className="detail-value">{threat.confidence ?? 'N/A'}%</span>
              {threat.confidence != null && (
                <div style={{ flex: 1, height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${threat.confidence}%`,
                      backgroundColor: threat.confidence > 90 ? '#ef4444' : threat.confidence > 75 ? '#f59e0b' : '#10b981',
                      borderRadius: '4px'
                    }}
                  ></div>
                </div>
              )}
            </div>
          </div>

          <div className="detail-item">
            <span className="detail-label">Origin / Host Country</span>
            <span className="detail-value" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Globe size={16} color="#64748b" />
              {threat.country || 'Unknown'} {threat.countryCode ? `(${threat.countryCode})` : ''}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">Geolocation Source</span>
            <span className="detail-value" style={{ fontSize: '0.88rem', color: '#64748b' }}>
              {threat.geoSource || 'Automated IP Geolocation'}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">Current Operational Status</span>
            <span style={{ fontSize: '0.92rem', fontWeight: 600, color: threat.status === 'ACTIVE' ? '#16a34a' : '#64748b' }}>
              ● {threat.status || 'ACTIVE'}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">Target Entity / Sector</span>
            <span className="detail-value" style={{ color: threat.target ? '#1e293b' : '#94a3b8' }}>
              {threat.target || 'Unspecified'}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">Original Intelligence Feed</span>
            <span className="detail-value" style={{ color: '#2563eb', fontWeight: 600 }}>
              {threat.source}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">First Seen Telemetry</span>
            <span className="detail-value" style={{ fontSize: '0.86rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Clock size={14} color="#64748b" />
              {formatDateTime(threat.firstSeen)}
            </span>
          </div>

          <div className="detail-item">
            <span className="detail-label">Last Detected Activity</span>
            <span className="detail-value" style={{ fontSize: '0.86rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Clock size={14} color="#64748b" />
              {formatDateTime(threat.lastSeen)}
            </span>
          </div>
        </div>

        {/* Tags */}
        {threat.tags && threat.tags.length > 0 && (
          <div style={{ marginBottom: '28px' }}>
            <span className="detail-label" style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
              <Tag size={14} /> Normalized IOC Tags
            </span>
            <div className="tags-list">
              {threat.tags.map((tag, idx) => (
                <span key={idx} className="threat-tag" style={{ fontSize: '0.82rem', padding: '4px 12px' }}>
                  #{tag}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* External Source Action Box */}
        {exactSourceUrl && (
          <div style={{ marginTop: '32px', paddingTop: '24px', borderTop: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#eff6ff', padding: '20px 24px', borderRadius: '12px', flexWrap: 'wrap', gap: '16px' }}>
            <div style={{ maxWidth: '600px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                <h4 style={{ fontSize: '1rem', fontWeight: 600, color: '#1e3a8a' }}>
                  External Intelligence Investigation Link
                </h4>
                <span
                  style={{
                    fontSize: '0.7rem',
                    color: '#16a34a',
                    backgroundColor: '#dcfce7',
                    border: '1px solid #86efac',
                    padding: '2px 8px',
                    borderRadius: '999px',
                    fontWeight: 600
                  }}
                >
                  Live Feed Record
                </span>
              </div>
              <p style={{ fontSize: '0.84rem', color: '#3b82f6', lineHeight: 1.5, marginBottom: '6px' }}>
                Inspect raw signatures, payload submissions, and incident telemetry on {threat.source}.
              </p>
              <div style={{ fontSize: '0.76rem', color: '#64748b', wordBreak: 'break-all', fontFamily: 'monospace' }}>
                Target: {exactSourceUrl}
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <button
                className="btn-view-source"
                onClick={handleOpenSource}
              >
                <span>{actionLabel}</span>
                <ExternalLink size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ThreatDetailPage;
