import React from 'react';
import { FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import ThreatTypeBadge from '../common/ThreatTypeBadge';
import SeverityBadge from '../common/SeverityBadge';

const COUNTRY_FLAGS = {
  'India': '🇮🇳',
  'United States': '🇺🇸',
  'United Kingdom': '🇬🇧',
  'Germany': '🇩🇪',
  'Russia': '🇷🇺',
  'China': '🇨🇳',
  'Netherlands': '🇳🇱',
  'Brazil': '🇧🇷',
  'Canada': '🇨🇦',
  'Australia': '🇦🇺',
  'France': '🇫🇷',
  'Japan': '🇯🇵',
  'Singapore': '🇸🇬'
};

const ThreatTable = ({
  threats = [],
  title = 'Latest Threats',
  showViewAll = true,
  viewAllUrl = '/threats',
  onRowClick,
  startIndex = 1
}) => {
  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'Recent';
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return 'Recent';
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const month = months[date.getMonth()];
      const day = date.getDate();
      const year = date.getFullYear();
      let hours = date.getHours();
      const minutes = date.getMinutes().toString().padStart(2, '0');
      const ampm = hours >= 12 ? 'PM' : 'AM';
      hours = hours % 12;
      hours = hours ? hours : 12;
      return `${month} ${day}, ${year} ${hours}:${minutes} ${ampm}`;
    } catch {
      return 'Recent';
    }
  };

  return (
    <div className="threats-table-container">
      <div className="card-header-clean">
        <h3 className="card-title-clean">
          <FileText size={18} color="#2563eb" />
          <span>{title}</span>
        </h3>
        {showViewAll && (
          <Link to={viewAllUrl} className="card-link">
            View All Threats
          </Link>
        )}
      </div>

      <div className="table-responsive">
        <table className="custom-table">
          <thead>
            <tr>
              <th style={{ width: '40px' }}>#</th>
              <th>Threat Category</th>
              <th>Country</th>
              <th>Threat Indicator (Link / Domain / IP)</th>
              <th>Intelligence Source</th>
              <th>Target Sector</th>
              <th>Detected Date</th>
              <th>Risk Level</th>
            </tr>
          </thead>
          <tbody>
            {threats.map((threat, index) => {
              const country = threat.country || 'Unknown';
              const flag = threat.country ? (COUNTRY_FLAGS[threat.country] || '📍') : '🌐';

              return (
                <tr
                  key={threat.id || index}
                  onClick={() => onRowClick && onRowClick(threat)}
                  title="Click to inspect threat incident details"
                >
                  <td className="table-row-index">{startIndex + index}</td>
                  <td>
                    <ThreatTypeBadge type={threat.threatType} />
                  </td>
                  <td>
                    <span style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '5px',
                      fontSize: '0.8rem',
                      fontWeight: 500,
                      color: '#1e293b',
                      whiteSpace: 'nowrap'
                    }}>
                      <span>{flag}</span>
                      <span>{country}</span>
                    </span>
                  </td>
                  <td className="indicator-cell" title={threat.indicator}>
                    {threat.indicator}
                  </td>
                  <td style={{ color: '#475569', fontWeight: 500 }}>
                    {threat.source}
                  </td>
                  <td style={{ color: '#64748b' }}>
                    {threat.target || 'Unspecified'}
                  </td>
                  <td style={{ color: '#64748b', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>
                    {formatDateTime(threat.lastSeen || threat.createdAt)}
                  </td>
                  <td>
                    <SeverityBadge severity={threat.severity} />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ThreatTable;
