import React from 'react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';
import { ShieldCheck } from 'lucide-react';

const SourceDonutChart = ({ data = [], total = 0, country = 'all' }) => {
  const formattedTotal = total ? total.toLocaleString() : '0';
  const hasCountry = country && country !== 'all' && country !== 'All Countries';

  return (
    <div className="card-container" style={{ display: 'flex', flexDirection: 'column' }}>
      <div className="card-header-clean">
        <h3 className="card-title-clean">
          <ShieldCheck size={18} color="#2563eb" />
          <span>Threats by Source {hasCountry ? `(${country})` : ''}</span>
        </h3>
        <span style={{ fontSize: '0.76rem', color: '#64748b', fontWeight: 600, background: '#f1f5f9', padding: '2px 8px', borderRadius: '12px' }}>
          {data.length} Feeds
        </span>
      </div>

      {/* Flawless, Perfectly Round Donut Chart with Centered Total */}
      <div className="donut-chart-wrapper">
        <ResponsiveContainer width="100%" height={175}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={50}
              outerRadius={72}
              paddingAngle={3}
              dataKey="count"
              isAnimationActive={true}
              animationDuration={800}
            >
              {data.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={entry.color || '#3b82f6'}
                  stroke="#ffffff"
                  strokeWidth={2}
                />
              ))}
            </Pie>
            <Tooltip
              formatter={(value, name, item) => [
                `${value.toLocaleString()} threats (${item.payload.percentage}%)`,
                item.payload.source
              ]}
              contentStyle={{
                backgroundColor: '#ffffff',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                fontSize: '0.82rem',
                fontWeight: 500
              }}
            />
          </PieChart>
        </ResponsiveContainer>

        {/* Centered Total inside donut hole */}
        <div className="donut-center-label">
          <div className="donut-center-value">
            {formattedTotal}
          </div>
          <div className="donut-center-subtext">
            TOTAL
          </div>
        </div>
      </div>

      {/* Structured Source Legend Grid Below Donut */}
      <div className="donut-legend-container">
        {data.map((item, idx) => (
          <div key={idx} className="donut-legend-item" title={`${item.source}: ${item.count} indicators (${item.percentage}%)`}>
            <div className="donut-legend-left">
              <span
                className="donut-legend-dot"
                style={{ backgroundColor: item.color || '#3b82f6' }}
              />
              <span className="donut-legend-name">
                {item.source}
              </span>
            </div>

            <div className="donut-legend-right">
              <span>{item.percentage}%</span>
              <span style={{ color: '#94a3b8', fontSize: '0.7rem' }}>
                ({item.count.toLocaleString()})
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SourceDonutChart;
