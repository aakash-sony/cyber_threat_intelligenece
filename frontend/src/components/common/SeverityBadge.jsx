import React from 'react';

const SeverityBadge = ({ severity }) => {
  const sevStr = severity ? severity.toUpperCase() : 'UNKNOWN';
  return (
    <span className={`severity-pill ${sevStr}`}>
      {sevStr}
    </span>
  );
};

export default SeverityBadge;
