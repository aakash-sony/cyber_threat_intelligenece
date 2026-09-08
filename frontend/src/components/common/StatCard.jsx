import React from 'react';

const StatCard = ({ iconColor = 'blue', icon: Icon, label, value, trend, sourceText, onClick }) => {
  // Format numbers nicely with commas if numeric
  const formattedValue = typeof value === 'number' ? value.toLocaleString() : value;

  return (
    <div
      className="stat-card"
      onClick={onClick}
      style={{ cursor: onClick ? 'pointer' : 'default' }}
      title={onClick ? `Click to filter by ${label}` : undefined}
    >
      <div className={`stat-icon-wrapper ${iconColor}`}>
        {Icon && <Icon size={26} strokeWidth={2.2} />}
      </div>
      <div className="stat-content">
        <span className="stat-label">{label}</span>
        <span className="stat-value">{formattedValue}</span>
        {trend && (
          <div className="stat-sub-info">
            <span className="stat-trend">▲ {trend}</span>
          </div>
        )}
        {sourceText && (
          <span className="stat-source-text">{sourceText}</span>
        )}
      </div>
    </div>
  );
};

export default StatCard;
