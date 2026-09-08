import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  X,
  ExternalLink,
  Copy,
  Check,
  ShieldAlert,
  Globe,
  Tag,
  ShieldCheck,
  Layers,
  Clock,
  Info
} from 'lucide-react';
import SeverityBadge from '../common/SeverityBadge';
import ThreatTypeBadge from '../common/ThreatTypeBadge';
import {
  getExactSourceUrl,
  getVirusTotalUrl,
  getSourceActionLabel
} from '../../utils/sourceUrlHelper';
import { trackIndicatorCopy, trackExternalSourceClick } from '../../analytics/analytics';

const ThreatDetailsModal = ({ threat, onClose }) => {
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  if (!threat) return null;

  const exactSourceUrl = getExactSourceUrl(threat);
  const vtUrl = getVirusTotalUrl(threat.indicator);
  const actionLabel = getSourceActionLabel(threat);

  const handleCopy = () => {
    navigator.clipboard.writeText(threat.indicator);
    trackIndicatorCopy(threat.threatType);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleOpenSource = () => {
    if (exactSourceUrl) {
      trackExternalSourceClick(threat.source || 'intelligence_source', exactSourceUrl);
      window.open(exactSourceUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const handleOpenVirusTotal = () => {
    if (vtUrl) {
      trackExternalSourceClick('VirusTotal', vtUrl);
      window.open(vtUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const handleNavigateDetail = () => {
    if (threat.id) {
      onClose();
      navigate(`/threats/${threat.id}`);
    }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    const d = new Date(dateStr);
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '680px' }}>
        <div className="modal-header">
          <div className="modal-title-row">
            <div
              style={{
                width: '38px',
                height: '38px',
                borderRadius: '8px',
                backgroundColor: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <ShieldAlert size={22} />
            </div>
            <div>
              <h3 className="modal-title">Threat Intelligence Telemetry</h3>
              <p style={{ fontSize: '0.8rem', color: '#64748b' }}>
                Verified telemetry record #{threat.id} • {threat.source}
              </p>
            </div>
          </div>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          {/* Indicator Box with Copy */}
          <div className="detail-indicator-box">
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', wordBreak: 'break-all' }}>
              {threat.indicator}
            </span>
            <button className="copy-btn" onClick={handleCopy} title="Copy indicator" style={{ flexShrink: 0 }}>
              {copied ? (
                <span style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#4ade80' }}>
                  <Check size={14} /> Copied
                </span>
              ) : (
                <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Copy size={14} /> Copy
                </span>
              )}
            </button>
          </div>

          {/* Feed Telemetry Description */}
          {threat.description && (
            <div style={{ marginBottom: '16px' }}>
              <span className="detail-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Info size={13} color="#2563eb" /> Feed Telemetry
              </span>
              <p style={{ fontSize: '0.88rem', color: '#1e293b', lineHeight: 1.6, marginTop: '4px', backgroundColor: '#f8fafc', padding: '12px 14px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                {threat.description}
              </p>
            </div>
          )}

          {/* Details Grid */}
          <div className="detail-grid">
            <div className="detail-item">
              <span className="detail-label">Origin / Host Country</span>
              <span className="detail-value" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Globe size={15} color="#64748b" />
                {threat.country || 'Unknown'} {threat.countryCode ? `(${threat.countryCode})` : ''}
              </span>
            </div>

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
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <span className="detail-value">{threat.confidence != null ? `${threat.confidence}%` : 'N/A'}</span>
                {threat.confidence != null && (
                  <div
                    style={{
                      flex: 1,
                      height: '6px',
                      backgroundColor: '#e2e8f0',
                      borderRadius: '4px',
                      overflow: 'hidden'
                    }}
                  >
                    <div
                      style={{
                        height: '100%',
                        width: `${threat.confidence}%`,
                        backgroundColor:
                          threat.confidence > 90
                            ? '#ef4444'
                            : threat.confidence > 75
                            ? '#f59e0b'
                            : '#10b981',
                        borderRadius: '4px'
                      }}
                    ></div>
                  </div>
                )}
              </div>
            </div>

            <div className="detail-item">
              <span className="detail-label">Status</span>
              <span
                style={{
                  fontSize: '0.84rem',
                  fontWeight: 600,
                  color: threat.status === 'ACTIVE' ? '#16a34a' : '#64748b'
                }}
              >
                ● {threat.status || 'ACTIVE'}
              </span>
            </div>

            <div className="detail-item">
              <span className="detail-label">First Seen</span>
              <span className="detail-value" style={{ fontSize: '0.84rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Clock size={13} color="#64748b" />
                {formatDateTime(threat.firstSeen)}
              </span>
            </div>

            <div className="detail-item">
              <span className="detail-label">Last Detected</span>
              <span className="detail-value" style={{ fontSize: '0.84rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Clock size={13} color="#64748b" />
                {formatDateTime(threat.lastSeen)}
              </span>
            </div>

            <div className="detail-item">
              <span className="detail-label">Intelligence Source</span>
              <span className="detail-value" style={{ color: '#2563eb', fontWeight: 600 }}>
                {threat.source}
              </span>
            </div>

            <div className="detail-item">
              <span className="detail-label">Target Sector</span>
              <span className="detail-value" style={{ color: threat.target ? '#1e293b' : '#94a3b8' }}>
                {threat.target || 'Unspecified'}
              </span>
            </div>
          </div>

          {/* Tags */}
          {threat.tags && threat.tags.length > 0 && (
            <div style={{ marginTop: '16px' }}>
              <span className="detail-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Tag size={13} /> Associated Tags
              </span>
              <div className="tags-list">
                {threat.tags.map((tag, idx) => (
                  <span key={idx} className="threat-tag">
                    #{tag}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button className="btn-secondary" onClick={onClose}>
              Close
            </button>
            {threat.id && (
              <button
                className="btn-secondary"
                onClick={handleNavigateDetail}
                style={{ fontSize: '0.82rem' }}
                title="Navigate to dedicated investigation page"
              >
                Full Investigation Page
              </button>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            {vtUrl && (
              <button
                className="btn-secondary"
                onClick={handleOpenVirusTotal}
                title="Scan indicator on VirusTotal multi-engine OSINT"
                style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.82rem' }}
              >
                <ShieldCheck size={15} color="#2563eb" />
                <span>VirusTotal</span>
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
      </div>
    </div>
  );
};

export default ThreatDetailsModal;
