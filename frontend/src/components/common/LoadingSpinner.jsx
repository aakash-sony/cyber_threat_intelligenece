import React from 'react';

const LoadingSpinner = ({ message = 'Loading threat intelligence data...' }) => {
  return (
    <div className="loading-box">
      <div className="spinner"></div>
      <p>{message}</p>
    </div>
  );
};

export default LoadingSpinner;
