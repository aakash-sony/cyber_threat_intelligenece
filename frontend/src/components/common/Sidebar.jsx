import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { LayoutDashboard, ShieldAlert, LineChart, Database, Info, Shield, ChevronDown, ChevronRight } from 'lucide-react';

const Sidebar = () => {
  const location = useLocation();
  const [threatsExpanded, setThreatsExpanded] = useState(
    location.pathname.startsWith('/threats')
  );

  return (
    <aside className="sidebar">
      <div className="sidebar-nav">
        <NavLink
          to="/"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <LayoutDashboard size={19} />
          <span>Dashboard</span>
        </NavLink>

        <div>
          <div
            className={`nav-link ${location.pathname.startsWith('/threats') ? 'active' : ''}`}
            onClick={() => setThreatsExpanded(!threatsExpanded)}
            style={{ cursor: 'pointer', justifyContent: 'space-between' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <ShieldAlert size={19} />
              <span>Threats</span>
            </div>
            {threatsExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          </div>

          {threatsExpanded && (
            <div className="nav-sub-list">
              <NavLink
                to="/threats"
                end
                className={({ isActive }) => `nav-sub-link ${isActive ? 'active' : ''}`}
              >
                <span>All Threats</span>
              </NavLink>
              <NavLink
                to="/threats/critical"
                className={({ isActive }) => `nav-sub-link ${isActive ? 'active' : ''}`}
              >
                <span>Critical Threats</span>
              </NavLink>
              <NavLink
                to="/threats/high"
                className={({ isActive }) => `nav-sub-link ${isActive ? 'active' : ''}`}
              >
                <span>High Threats</span>
              </NavLink>
            </div>
          )}
        </div>

        <NavLink
          to="/analytics"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <LineChart size={19} />
          <span>Analytics</span>
        </NavLink>

        <NavLink
          to="/sources"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <Database size={19} />
          <span>Sources</span>
        </NavLink>

        <NavLink
          to="/about"
          className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
        >
          <Info size={19} />
          <span>About</span>
        </NavLink>
      </div>

      <div className="sidebar-footer">
        <div className="security-badge">
          <Shield size={18} color="#3b82f6" />
          <span>Together against cyber fraud</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
