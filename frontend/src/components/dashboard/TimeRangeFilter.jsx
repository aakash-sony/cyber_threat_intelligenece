import React from 'react';
import { Clock, Globe, RefreshCw } from 'lucide-react';

const FILTER_OPTIONS = [
  { key: '24h', label: 'Last 24 Hours' },
  { key: '7d', label: 'Last Week' },
  { key: '30d', label: 'Last Month' },
  { key: '90d', label: 'Last 3 Months' },
  { key: '1y', label: 'Last 1 Year' },
  { key: 'all', label: 'All Time' }
];

const COUNTRY_FLAGS = {
  'India': '🇮🇳',
  'United States': '🇺🇸',
  'United Kingdom': '🇬🇧',
  'Germany': '🇩🇪',
  'Australia': '🇦🇺',
  'Canada': '🇨🇦',
  'France': '🇫🇷',
  'Japan': '🇯🇵',
  'Singapore': '🇸🇬',
  'Netherlands': '🇳🇱',
  'Brazil': '🇧🇷',
  'Russia': '🇷🇺',
  'China': '🇨🇳',
  'South Korea': '🇰🇷',
  'Italy': '🇮🇹',
  'Spain': '🇪🇸',
  'Switzerland': '🇨🇭',
  'Sweden': '🇸🇪',
  'United Arab Emirates': '🇦🇪',
  'Saudi Arabia': '🇸🇦',
  'Israel': '🇮🇱',
  'South Africa': '🇿🇦',
  'Mexico': '🇲🇽',
  'Indonesia': '🇮🇩',
  'Turkey': '🇹🇷',
  'Poland': '🇵🇱',
  'Norway': '🇳🇴',
  'Ireland': '🇮🇪',
  'New Zealand': '🇳🇿',
  'Argentina': '🇦🇷',
  'Malaysia': '🇲🇾',
  'Vietnam': '🇻🇳',
  'Thailand': '🇹🇭',
  'Philippines': '🇵🇭',
  'Egypt': '🇪🇬',
  'Nigeria': '🇳🇬',
  'Kenya': '🇰🇪',
  'Colombia': '🇨🇴',
  'Chile': '🇨🇱',
  'Belgium': '🇧🇪',
  'Austria': '🇦🇹',
  'Denmark': '🇩🇰',
  'Finland': '🇫🇮',
  'Portugal': '🇵🇹',
  'Greece': '🇬🇷',
  'Czech Republic': '🇨🇿',
  'Romania': '🇷🇴',
  'Hungary': '🇭🇺',
  'Taiwan': '🇹🇼',
  'Hong Kong': '🇭🇰',
  'Ukraine': '🇺🇦',
  'Pakistan': '🇵🇰',
  'Bangladesh': '🇧🇩'
};

const TimeRangeFilter = ({
  activeRange = '24h',
  onRangeChange,
  activeCountry = 'all',
  onCountryChange,
  countries = [],
  totalThreats = 0,
  isRefreshing = false
}) => {
  const currentOption = FILTER_OPTIONS.find(opt => opt.key === activeRange) || FILTER_OPTIONS[0];
  const countryDisplay = (activeCountry === 'all' || !activeCountry) ? 'All Regions (Global)' : activeCountry;
  const flag = (activeCountry && activeCountry !== 'all') ? (COUNTRY_FLAGS[activeCountry] || '📍') : '🌐';

  // Filter out any invalid country strings like "Unknown" or "Global" from the options
  const cleanCountries = countries.filter(
    (c) => c && typeof c === 'string' && c.trim() !== '' && !['unknown', 'global'].includes(c.trim().toLowerCase())
  );

  return (
    <div className="time-filter-bar">
      {/* Left controls: Time Range Pills & Country Dropdown */}
      <div className="time-filter-left">
        {/* Time Filter Group */}
        <div className="time-filter-group-wrapper">
          <div className="time-filter-label">
            <Clock size={15} color="#2563eb" />
            <span>Timeframe:</span>
          </div>

          <div className="time-filter-group">
            {FILTER_OPTIONS.map((option) => {
              const isActive = activeRange === option.key;
              return (
                <button
                  key={option.key}
                  type="button"
                  id={`time-filter-${option.key}`}
                  className={`time-filter-btn ${isActive ? 'active' : ''}`}
                  onClick={() => onRangeChange(option.key)}
                  title={`Filter incidents by ${option.label}`}
                >
                  {option.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Country Filter Dropdown */}
        <div className="country-filter-wrapper">
          <div className="time-filter-label">
            <Globe size={15} color="#059669" />
            <span>Country:</span>
          </div>

          <select
            id="country-select-dropdown"
            className="country-select-filter"
            value={activeCountry}
            onChange={(e) => onCountryChange(e.target.value)}
            title="Filter threat incidents by country"
          >
            <option value="all">🌐 All Countries (Global)</option>
            {cleanCountries.map((c) => (
              <option key={c} value={c}>
                {COUNTRY_FLAGS[c] ? `${COUNTRY_FLAGS[c]} ${c}` : `📍 ${c}`}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Right live status badge */}
      <div className="time-filter-status">
        <span className="time-filter-badge">
          <span className="time-filter-pulse"></span>
          <span>
            {flag} {countryDisplay} · <strong>{totalThreats.toLocaleString()}</strong> Incidents ({currentOption.label})
          </span>
        </span>
        {isRefreshing && (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', fontSize: '0.78rem', color: '#2563eb' }}>
            <RefreshCw size={13} className="spin" />
            <span>Updating...</span>
          </span>
        )}
      </div>
    </div>
  );
};

export default TimeRangeFilter;
