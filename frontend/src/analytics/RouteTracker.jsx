import React, { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { initGA, trackPageView } from './analytics';

const ROUTE_TITLES = {
  '/': 'SOC Cyber Threat Intelligence Dashboard',
  '/threats': 'All Threat Intelligence Records',
  '/threats/critical': 'Critical Severity Threat Indicators',
  '/threats/high': 'High Severity Threat Indicators',
  '/analytics': 'Threat Telemetry & Incident Analytics',
  '/sources': 'Connected Threat Intelligence Sources',
  '/about': 'About | Cyber Threat Intelligence Platform'
};

const resolveTitle = (pathname) => {
  if (ROUTE_TITLES[pathname]) return ROUTE_TITLES[pathname];
  if (pathname.startsWith('/threats/')) return 'Threat Indicator Deep Inspection';
  return document.title || 'Cyber Threat Intelligence';
};

/**
 * RouteTracker
 * React component mounted inside BrowserRouter.
 * Initializes GA4 on mount and dispatches single pageview events on SPA route changes.
 */
const RouteTracker = () => {
  const location = useLocation();

  // Initialize GA4 exactly once
  useEffect(() => {
    initGA();
  }, []);

  // Track SPA route navigation
  useEffect(() => {
    const pageTitle = resolveTitle(location.pathname);
    const fullPath = location.pathname + location.search;

    // Optional: dynamically set document title for better browser tab UX & GA4 accuracy
    if (pageTitle) {
      document.title = `${pageTitle} | Cyber Threat Intelligence`;
    }

    trackPageView(fullPath, pageTitle);
  }, [location.pathname, location.search]);

  return null;
};

export default RouteTracker;
