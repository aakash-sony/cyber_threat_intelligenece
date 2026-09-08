import React, { useState, useEffect } from 'react';
import { Database, ExternalLink, ShieldCheck, Clock, Globe2, Radio, Server, CheckCircle2 } from 'lucide-react';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { dashboardService } from '../services/dashboardService';
import { trackExternalSourceClick } from '../analytics/analytics';

const SourcesPage = () => {
  const [sources, setSources] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchSources = async () => {
    try {
      setLoading(true);
      const data = await dashboardService.getDetailedSources();
      setSources(data || []);
    } catch (err) {
      console.error('Failed to load sources:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSources();
  }, []);

  const formatLastDetected = (dateStr) => {
    if (!dateStr) return 'No records ingested yet';
    try {
      const d = new Date(dateStr);
      return (
        d.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }) +
        ' ' +
        d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      );
    } catch {
      return 'Timestamp unavailable';
    }
  };

  if (loading) {
    return <LoadingSpinner message="Querying verified threat intelligence feeds and official portal endpoints..." />;
  }

  return (
    <div className="sources-page">
      {/* Header Banner */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '28px',
          flexWrap: 'wrap',
          gap: '16px'
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <Server size={22} color="#2563eb" />
            <h2
              style={{
                fontFamily: 'var(--font-display)',
                fontSize: '1.6rem',
                fontWeight: 700,
                color: '#0f172a',
                margin: 0
              }}
            >
              Connected Threat Intelligence Sources
            </h2>
          </div>
          <p style={{ fontSize: '0.88rem', color: '#64748b', margin: 0 }}>
            Automated intelligence synchronization aggregating verified threat indicators from active cyber threat feeds and OSINT repositories.
          </p>
        </div>

        {/* Live Status Badge */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            backgroundColor: '#f0fdf4',
            border: '1px solid #bbf7d0',
            color: '#166534',
            padding: '8px 18px',
            borderRadius: '999px',
            fontSize: '0.82rem',
            fontWeight: 600,
            boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
          }}
        >
          <span
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: '#22c55e',
              animation: 'pulse 2s infinite'
            }}
          ></span>
          <span>Threat Feeds Online & Synchronizing</span>
        </div>
      </div>

      {/* Sources Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))',
          gap: '24px'
        }}
      >
        {sources.map((src, index) => (
          <div
            key={index}
            className="card-container"
            style={{
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              padding: '24px',
              borderRadius: '12px',
              transition: 'transform 0.2s ease, box-shadow 0.2s ease',
              border: '1px solid #e2e8f0',
              background: '#ffffff'
            }}
          >
            <div>
              {/* Card Header: Icon, Name & Verified Badge */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  marginBottom: '16px',
                  gap: '12px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div
                    style={{
                      width: '44px',
                      height: '44px',
                      borderRadius: '10px',
                      backgroundColor: `${src.color || '#2563eb'}14`,
                      color: src.color || '#2563eb',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0
                    }}
                  >
                    <Database size={22} />
                  </div>
                  <div>
                    <h3
                      style={{
                        fontSize: '1.1rem',
                        fontWeight: 700,
                        color: '#0f172a',
                        margin: '0 0 3px 0'
                      }}
                    >
                      {src.name}
                    </h3>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span
                        style={{
                          width: '7px',
                          height: '7px',
                          borderRadius: '50%',
                          backgroundColor: '#16a34a'
                        }}
                      ></span>
                      <span style={{ fontSize: '0.75rem', color: '#16a34a', fontWeight: 600 }}>
                        {src.status || 'Active Feed'}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Verified Provider Badge */}
                <span
                  style={{
                    fontSize: '0.74rem',
                    fontWeight: 600,
                    color: '#059669',
                    backgroundColor: '#ecfdf5',
                    border: '1px solid #a7f3d0',
                    padding: '4px 10px',
                    borderRadius: '999px',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    whiteSpace: 'nowrap'
                  }}
                >
                  <ShieldCheck size={13} color="#059669" />
                  <span>Verified Source</span>
                </span>
              </div>

              {/* Provider Description */}
              <p
                style={{
                  fontSize: '0.85rem',
                  color: '#475569',
                  lineHeight: 1.6,
                  marginBottom: '18px'
                }}
              >
                {src.description}
              </p>

              {/* Provider Specifications with Real Ingested Database Counts */}
              <div
                style={{
                  fontSize: '0.8rem',
                  color: '#64748b',
                  marginBottom: '20px',
                  backgroundColor: '#f8fafc',
                  padding: '12px 14px',
                  borderRadius: '8px',
                  border: '1px solid #e2e8f0',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ color: '#64748b' }}>Intelligence Type:</span>
                  <span
                    style={{
                      fontWeight: 600,
                      color: '#0f172a',
                      backgroundColor: '#e2e8f0',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontSize: '0.75rem'
                    }}
                  >
                    {src.type || 'Security Telemetry'}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ color: '#64748b' }}>Ingested Indicators:</span>
                  <span style={{ fontWeight: 600, color: '#0f172a', fontSize: '0.82rem' }}>
                    {src.count != null ? src.count.toLocaleString() : 0}
                    {src.percentage != null ? ` (${src.percentage}% of DB)` : ''}
                  </span>
                </div>

                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    paddingTop: '6px',
                    borderTop: '1px solid #edf2f7'
                  }}
                >
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px', color: '#64748b' }}>
                    <Clock size={12} />
                    <span>Telemetry Status:</span>
                  </span>
                  <span style={{ color: '#0f172a', fontWeight: 500, fontSize: '0.78rem' }}>
                    {formatLastDetected(src.lastDetected)}
                  </span>
                </div>
              </div>
            </div>

            {/* Bottom Action: Official Portal Link */}
            <div style={{ borderTop: '1px solid #edf2f7', paddingTop: '16px', marginTop: 'auto' }}>
              {src.url ? (
                <a
                  href={src.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={() => trackExternalSourceClick(src.name, src.url)}
                  className="btn-view-source"
                  style={{
                    width: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px',
                    padding: '10px 16px',
                    fontSize: '0.85rem',
                    fontWeight: 600,
                    textDecoration: 'none',
                    borderRadius: '8px',
                    boxShadow: '0 1px 3px rgba(37, 99, 235, 0.15)',
                    transition: 'all 0.2s ease',
                    boxSizing: 'border-box'
                  }}
                  title={`Open official ${src.name} portal in a new tab`}
                >
                  <span>Visit Feed Portal</span>
                  <ExternalLink size={15} />
                </a>
              ) : (
                <div
                  style={{
                    textAlign: 'center',
                    padding: '8px',
                    color: '#64748b',
                    fontSize: '0.8rem',
                    fontStyle: 'italic'
                  }}
                >
                  Direct Feed URL Unavailable
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SourcesPage;
