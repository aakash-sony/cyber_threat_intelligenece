import React from 'react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip
} from 'recharts';

const TIME_RANGE_LABELS = {
  '24h': 'Threat Activity – Last 24 Hours',
  '7d': 'Threat Activity – Last Week (7 Days)',
  '30d': 'Threat Activity – Last 1 Month (30 Days)',
  '90d': 'Threat Activity – Last 3 Months (90 Days)',
  '1y': 'Threat Activity – Last 1 Year',
  'all': 'Threat Activity – All Time'
};

const ActivityChart = ({ data = [], timeRange = '24h', country = 'all' }) => {
  const periodLabel = TIME_RANGE_LABELS[timeRange] || 'Threat Activity';
  const hasCountry = country && country !== 'all' && country !== 'All Countries';
  const chartTitle = hasCountry
    ? `Threat Activity in ${country} – ${periodLabel.replace('Threat Activity – ', '')}`
    : periodLabel;

  return (
    <div className="card-container" style={{ display: 'flex', flexDirection: 'column' }}>
      <div className="card-header-clean">
        <h3 className="card-title-clean">{chartTitle}</h3>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', fontSize: '0.8rem' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#2563eb' }}></span>
            <span style={{ color: '#475569', fontWeight: 500 }}>Phishing</span>
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#8b5cf6' }}></span>
            <span style={{ color: '#475569', fontWeight: 500 }}>Malware</span>
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#ef4444' }}></span>
            <span style={{ color: '#475569', fontWeight: 500 }}>Fraud</span>
          </span>
        </div>
      </div>

      <div style={{ width: '100%', height: 260 }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 10, right: 15, left: -15, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
            <XAxis
              dataKey="time"
              stroke="#94a3b8"
              tickLine={false}
              axisLine={{ stroke: '#cbd5e1' }}
              style={{ fontSize: '0.78rem' }}
            />
            <YAxis
              stroke="#94a3b8"
              tickLine={false}
              axisLine={false}
              style={{ fontSize: '0.78rem' }}
              tickFormatter={(v) => v.toLocaleString()}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                fontSize: '0.82rem'
              }}
            />
            <Line
              type="monotone"
              dataKey="phishing"
              stroke="#2563eb"
              strokeWidth={2.5}
              dot={{ r: 3.5, fill: '#2563eb', strokeWidth: 1.5, stroke: '#ffffff' }}
              activeDot={{ r: 6 }}
              name="Phishing"
            />
            <Line
              type="monotone"
              dataKey="malware"
              stroke="#8b5cf6"
              strokeWidth={2.5}
              dot={{ r: 3.5, fill: '#8b5cf6', strokeWidth: 1.5, stroke: '#ffffff' }}
              activeDot={{ r: 6 }}
              name="Malware"
            />
            <Line
              type="monotone"
              dataKey="fraud"
              stroke="#ef4444"
              strokeWidth={2.5}
              dot={{ r: 3.5, fill: '#ef4444', strokeWidth: 1.5, stroke: '#ffffff' }}
              activeDot={{ r: 6 }}
              name="Fraud"
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default ActivityChart;
