import React, { useState, useEffect } from 'react';
import { Shield, Clock, RefreshCw } from 'lucide-react';
import { threatService } from '../../services/threatService';
import { trackDashboardSync } from '../../analytics/analytics';

const Navbar = ({ onRefresh }) => {
  const [syncStatus, setSyncStatus] = useState(null);
  const [secondsRemaining, setSecondsRemaining] = useState(900);
  const [lastSyncStr, setLastSyncStr] = useState('');
  const [isManualSyncing, setIsManualSyncing] = useState(false);

  const formatTimestamp = (date) => {
    if (!date || isNaN(date.getTime())) return 'Live Telemetry';
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
  };

  const formatCountdown = (secs) => {
    if (secs <= 0) return 'Syncing...';
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const fetchStatus = async () => {
    try {
      const data = await threatService.getSyncStatus();
      setSyncStatus(data);
      if (data.secondsUntilNextSync !== undefined) {
        setSecondsRemaining(Math.max(0, data.secondsUntilNextSync));
      }
      if (data.lastSyncTime) {
        setLastSyncStr(formatTimestamp(new Date(data.lastSyncTime)));
      }
    } catch (err) {
      console.warn('Sync status check delayed:', err.message);
    }
  };

  const handleManualSync = async () => {
    if (isManualSyncing || syncStatus?.status === 'RUNNING') return;
    try {
      setIsManualSyncing(true);
      await threatService.triggerSync();
      trackDashboardSync('navbar_manual_sync_btn');
      await fetchStatus();
      if (onRefresh) onRefresh();
    } catch (err) {
      console.error('Manual threat sync failed:', err);
    } finally {
      setIsManualSyncing(false);
    }
  };

  // Poll sync status every 30s
  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 30000);
    return () => clearInterval(interval);
  }, []);

  // 1-second timer for the countdown
  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          // When countdown reaches 0, background sync is executing, re-fetch status & refresh data
          fetchStatus();
          if (onRefresh) onRefresh();
          return 900;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [onRefresh]);

  const isRunning = syncStatus?.status === 'RUNNING' || isManualSyncing || secondsRemaining <= 0;

  return (
    <header className="top-header">
      <div className="header-brand">
        <div className="header-logo-icon">
          <Shield size={22} strokeWidth={2.5} />
        </div>
        <div className="header-brand-titles">
          <span className="header-brand-name">Cyber Threat Intelligence</span>
          <span className="header-brand-sub">Real-time SOC Indicators & Automated 15m Ingestion</span>
        </div>
      </div>

      <div className="header-actions">
        {/* Live Status Badge */}
        <div className="live-indicator" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className={`live-dot ${isRunning ? 'pulsing' : ''}`}></span>
          <span style={{ fontWeight: 600, fontSize: '0.82rem' }}>
            {isRunning ? 'Syncing Feeds...' : 'Live 15m Telemetry'}
          </span>
        </div>

        {/* Auto-sync countdown pill */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            backgroundColor: 'rgba(255, 255, 255, 0.08)',
            padding: '4px 12px',
            borderRadius: '20px',
            fontSize: '0.78rem',
            color: '#cbd5e1',
            border: '1px solid rgba(255, 255, 255, 0.12)'
          }}
          title="Automatic background sync polls external threat APIs every 15 minutes"
        >
          <Clock size={13} color="#38bdf8" />
          <span>Next auto-sync: <strong style={{ color: '#38bdf8', fontFamily: 'monospace' }}>{formatCountdown(secondsRemaining)}</strong></span>
        </div>

        {/* Manual Sync Now Button */}
        <button
          onClick={handleManualSync}
          disabled={isRunning}
          className="sync-now-btn"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            backgroundColor: isRunning ? 'rgba(56, 189, 248, 0.12)' : 'rgba(37, 99, 235, 0.25)',
            border: '1px solid rgba(56, 189, 248, 0.35)',
            color: '#38bdf8',
            padding: '5px 12px',
            borderRadius: '6px',
            fontSize: '0.78rem',
            fontWeight: 600,
            cursor: isRunning ? 'not-allowed' : 'pointer',
            transition: 'all 0.2s ease',
          }}
          title="Fetch fresh live threat indicators from public APIs into PostgreSQL immediately"
        >
          <RefreshCw size={12} className={isRunning ? 'spin' : ''} />
          <span>{isRunning ? 'Syncing...' : 'Sync Now'}</span>
        </button>

        {/* Last updated text from actual DB sync */}
        <span className="last-updated-text" style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
          Updated: {lastSyncStr || 'Live Telemetry'}
        </span>
      </div>
    </header>
  );
};

export default Navbar;
