import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import DashboardPage from './pages/DashboardPage';
import AllThreatsPage from './pages/AllThreatsPage';
import CriticalThreatsPage from './pages/CriticalThreatsPage';
import HighThreatsPage from './pages/HighThreatsPage';
import ThreatDetailPage from './pages/ThreatDetailPage';
import AnalyticsPage from './pages/AnalyticsPage';
import SourcesPage from './pages/SourcesPage';
import AboutPage from './pages/AboutPage';
import RouteTracker from './analytics/RouteTracker';

function App() {
  const [refreshKey, setRefreshKey] = useState(0);

  const handleGlobalRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  return (
    <BrowserRouter>
      <RouteTracker />
      <Routes>
        <Route element={<MainLayout onRefresh={handleGlobalRefresh} />}>
          <Route path="/" element={<DashboardPage refreshKey={refreshKey} />} />
          <Route path="/threats" element={<AllThreatsPage />} />
          <Route path="/threats/critical" element={<CriticalThreatsPage />} />
          <Route path="/threats/high" element={<HighThreatsPage />} />
          <Route path="/threats/:id" element={<ThreatDetailPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/sources" element={<SourcesPage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
