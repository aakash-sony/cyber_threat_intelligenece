import React from 'react';
import { ShieldCheck } from 'lucide-react';

const EmptyState = ({
  title = 'No threats found',
  message = 'No records match your active search filters or time range.',
  actionText,
  onAction
}) => {
  return (
    <div className="empty-state">
      <div
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          backgroundColor: '#e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#64748b',
          marginBottom: '8px'
        }}
      >
        <ShieldCheck size={32} />
      </div>
      <h4 className="empty-state-title">{title}</h4>
      <p style={{ maxWidth: '420px', fontSize: '0.86rem' }}>{message}</p>
      {actionText && onAction && (
        <button
          className="btn-secondary"
          onClick={onAction}
          style={{ marginTop: '12px' }}
        >
          {actionText}
        </button>
      )}
    </div>
  );
};

export default EmptyState;
