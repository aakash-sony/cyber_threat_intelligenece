import React, { useState, useEffect } from 'react';
import { ShieldAlert, ChevronLeft, ChevronRight } from 'lucide-react';
import ThreatTable from '../components/threats/ThreatTable';
import ThreatDetailsModal from '../components/threats/ThreatDetailsModal';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { threatService } from '../services/threatService';
import { trackThreatDetailView } from '../analytics/analytics';

const CriticalThreatsPage = () => {
  const [threats, setThreats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(15);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedThreat, setSelectedThreat] = useState(null);

  const fetchCriticalThreats = async () => {
    try {
      setLoading(true);
      const data = await threatService.getCriticalThreats(page, pageSize);
      setThreats(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load critical threats:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCriticalThreats();
  }, [page]);

  return (
    <div className="critical-threats-page">
      <div style={{ marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div
          style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            backgroundColor: '#fee2e2',
            color: '#dc2626',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <ShieldAlert size={24} />
        </div>
        <div>
          <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '1.5rem', fontWeight: 700, color: '#991b1b' }}>
            Critical Severity Threat Alerts
          </h2>
          <p style={{ fontSize: '0.86rem', color: '#64748b' }}>
            High-priority indicators requiring immediate triage, automated blocklisting, and perimeter isolation.
          </p>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner message="Filtering critical threats from database..." />
      ) : threats.length === 0 ? (
        <EmptyState
          title="No Critical Threats"
          message="Zero critical threats are currently recorded in the intelligence database."
        />
      ) : (
        <>
          <ThreatTable
            threats={threats}
            title={`Active Critical Threats (${totalElements} Records)`}
            showViewAll={false}
            startIndex={page * pageSize + 1}
            onRowClick={(threat) => {
              trackThreatDetailView(threat.id, threat.threatType, threat.severity);
              setSelectedThreat(threat);
            }}
          />

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

      {selectedThreat && (
        <ThreatDetailsModal
          threat={selectedThreat}
          onClose={() => setSelectedThreat(null)}
        />
      )}
    </div>
  );
};

export default CriticalThreatsPage;
