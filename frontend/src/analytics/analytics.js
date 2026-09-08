/**
 * Google Analytics 4 (GA4) Integration Abstraction
 * Cyber Threat Intelligence SOC Dashboard
 *
 * Production-grade, lightweight, privacy-first analytics wrapper.
 * - Initializes strictly once (guarded against React 18 StrictMode double-mounting).
 * - Safe no-op when VITE_GA4_MEASUREMENT_ID is missing or invalid.
 * - Prevents duplicate SPA pageviews via send_page_view: false and path caching.
 * - Comprehensive PII filtering: Never transmits tokens, credentials, or personal data.
 * - Total failure isolation: Analytics errors can NEVER crash or degrade the application.
 */

// Module-level singleton state
let isInitialized = false;
let lastTrackedPath = null;
let lastTrackedTime = 0;

// Sensitive parameter key patterns to redact for privacy compliance
const SENSITIVE_KEY_REGEX = /(token|jwt|auth|secret|password|passwd|(^|_)key($|_)|api_?key|credential|bearer|session|cookie|email|phone|ssn)/i;

// Sensitive value patterns (e.g., email address, JWT header)
const EMAIL_REGEX = /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b/;
const JWT_REGEX = /^ey[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+/=]*$/;

const isDev = () => {
  try {
    return Boolean(typeof import.meta !== 'undefined' && import.meta.env?.DEV);
  } catch {
    return false;
  }
};

/**
 * Validates GA4 Measurement ID format (e.g. G-XXXXXXXXXX)
 */
export const isValidMeasurementId = (id) => {
  return typeof id === 'string' && /^G-[A-Za-z0-9]+$/.test(id.trim());
};

/**
 * Gets configured Measurement ID from Vite environment
 */
export const getMeasurementId = () => {
  try {
    return (typeof import.meta !== 'undefined' && import.meta.env?.VITE_GA4_MEASUREMENT_ID?.trim()) || '';
  } catch {
    return '';
  }
};

/**
 * Deep sanitization of event parameter objects to strip PII and enforce GA4 constraints
 */
export const sanitizeParams = (params = {}) => {
  if (!params || typeof params !== 'object') return {};

  const clean = {};
  for (const [key, rawVal] of Object.entries(params)) {
    // Drop sensitive keys completely
    if (SENSITIVE_KEY_REGEX.test(key)) {
      continue;
    }

    if (rawVal === null || rawVal === undefined) {
      continue;
    }

    // Numbers & Booleans pass through directly
    if (typeof rawVal === 'number' || typeof rawVal === 'boolean') {
      clean[key] = rawVal;
      continue;
    }

    // Strings: check for email or JWT, truncate to 100 chars (GA4 max value length)
    if (typeof rawVal === 'string') {
      const trimmed = rawVal.trim();
      if (EMAIL_REGEX.test(trimmed) || JWT_REGEX.test(trimmed)) {
        // Redact any string containing credentials or email
        continue;
      }
      clean[key] = trimmed.slice(0, 100);
    }
  }

  return clean;
};

/**
 * Sanitizes URL paths before sending to GA4 to ensure query tokens or sensitive IDs are excluded
 */
export const sanitizePath = (rawPath) => {
  if (!rawPath || typeof rawPath !== 'string') return '/';
  try {
    const url = new URL(rawPath, 'https://dashboard.local');
    const searchParams = new URLSearchParams();

    // Only allow known safe SOC query parameters
    const safeParams = ['page', 'size', 'severity', 'threatType', 'country', 'status', 'source', 'timeRange', 'sortBy', 'direction', 'keyword'];
    for (const [key, value] of url.searchParams.entries()) {
      if (safeParams.includes(key) && !SENSITIVE_KEY_REGEX.test(key)) {
        searchParams.append(key, value.slice(0, 60));
      }
    }

    const queryStr = searchParams.toString();
    return url.pathname + (queryStr ? `?${queryStr}` : '');
  } catch {
    return rawPath.split('?')[0] || '/';
  }
};

/**
 * Extracts a safe hostname domain from external URLs for outbound analytics
 */
const extractDomain = (urlStr) => {
  try {
    if (!urlStr) return 'unknown';
    const parsed = new URL(urlStr);
    return parsed.hostname;
  } catch {
    return 'external';
  }
};

/**
 * Initializes GA4 script and dataLayer exactly once
 * Returns boolean indicating whether GA4 was initialized
 */
export const initGA = () => {
  if (typeof window === 'undefined') return false;
  if (isInitialized) return true;

  const measurementId = getMeasurementId();

  if (!measurementId) {
    if (isDev()) {
      console.info(
        '%c[GA4 Analytics]%c Inactive: VITE_GA4_MEASUREMENT_ID is not configured in frontend/.env. Analytics tracking safely disabled.',
        'color: #38bdf8; font-weight: bold;',
        'color: inherit;'
      );
    }
    return false;
  }

  if (!isValidMeasurementId(measurementId)) {
    console.warn(
      `[GA4 Analytics] Invalid Measurement ID format: "${measurementId}". Expected format: G-XXXXXXXXXX`
    );
    return false;
  }

  try {
    // 1. Inject official Google tag script asynchronously
    const existingScript = document.querySelector(`script[src*="googletagmanager.com/gtag/js?id=${measurementId}"]`);
    if (!existingScript) {
      const script = document.createElement('script');
      script.async = true;
      script.src = `https://www.googletagmanager.com/gtag/js?id=${measurementId}`;
      document.head.appendChild(script);
    }

    // 2. Initialize window.dataLayer and window.gtag
    window.dataLayer = window.dataLayer || [];
    window.gtag = function gtag() {
      window.dataLayer.push(arguments);
    };

    // 3. Establish timestamp and baseline configuration
    window.gtag('js', new Date());

    // NOTE: send_page_view: false is essential in React SPA to prevent double pageviews
    // between browser load and client-side React Router mount.
    window.gtag('config', measurementId, {
      send_page_view: false,
      debug_mode: isDev(),
      anonymize_ip: true,
      cookie_flags: 'SameSite=None;Secure'
    });

    isInitialized = true;

    if (isDev()) {
      console.info(
        `%c[GA4 Analytics]%c Initialized successfully with Stream ID: %c${measurementId}`,
        'color: #10b981; font-weight: bold;',
        'color: inherit;',
        'color: #38bdf8; font-weight: bold;'
      );
    }

    return true;
  } catch (err) {
    console.error('[GA4 Analytics] Initialization failed non-critically:', err.message);
    return false;
  }
};

/**
 * Tracks SPA page navigation
 * Safe against React StrictMode double invocations
 */
export const trackPageView = (path, title) => {
  if (typeof window === 'undefined') return;

  const cleanPath = sanitizePath(path || window.location.pathname + window.location.search);
  const now = Date.now();

  // Deduplication guard: ignore exact path requests within 300ms
  if (lastTrackedPath === cleanPath && now - lastTrackedTime < 300) {
    return;
  }

  lastTrackedPath = cleanPath;
  lastTrackedTime = now;

  if (!isInitialized || typeof window.gtag !== 'function') {
    if (isDev() && getMeasurementId()) {
      console.debug('[GA4 Analytics] (Pending Init) Page View:', cleanPath, title);
    }
    return;
  }

  try {
    window.gtag('event', 'page_view', {
      page_path: cleanPath,
      page_title: title || document.title,
      page_location: window.location.origin + cleanPath
    });

    if (isDev()) {
      console.debug('[GA4 Page View]', cleanPath, title || document.title);
    }
  } catch (err) {
    console.warn('[GA4 Analytics] Pageview tracking failed non-critically:', err.message);
  }
};

/**
 * Tracks a custom or recommended GA4 event with sanitized parameters
 */
export const trackEvent = (eventName, params = {}) => {
  if (typeof window === 'undefined') return;
  if (!eventName || typeof eventName !== 'string') return;

  const cleanEventName = eventName.trim().slice(0, 40);
  const cleanParams = sanitizeParams(params);

  if (!isInitialized || typeof window.gtag !== 'function') {
    if (isDev() && !getMeasurementId()) {
      // In dev mode without an ID, provide quiet developer trace so actions can be inspected
      console.debug(`[GA4 Event (mock)] %c${cleanEventName}`, 'color: #a855f7; font-weight: bold;', cleanParams);
    }
    return;
  }

  try {
    window.gtag('event', cleanEventName, cleanParams);

    if (isDev()) {
      console.debug(`[GA4 Event] %c${cleanEventName}`, 'color: #10b981; font-weight: bold;', cleanParams);
    }
  } catch (err) {
    console.warn(`[GA4 Analytics] Event "${cleanEventName}" failed non-critically:`, err.message);
  }
};

// ============================================================================
// Granular Application Domain Event Helpers (Cyber Threat Dashboard)
// ============================================================================

/**
 * Tracks threat intelligence keyword search queries
 */
export const trackThreatSearch = (keyword, totalResults) => {
  if (!keyword || typeof keyword !== 'string') return;
  trackEvent('threat_search', {
    search_term: keyword.trim().slice(0, 60),
    result_count: typeof totalResults === 'number' ? totalResults : undefined
  });
};

/**
 * Tracks filter dropdown or timeframe adjustments
 */
export const trackThreatFilter = (filterType, filterValue) => {
  if (!filterType || !filterValue) return;
  trackEvent('threat_filter_change', {
    filter_name: String(filterType).slice(0, 30),
    filter_value: String(filterValue).slice(0, 40)
  });
};

/**
 * Tracks user inspecting a threat incident detail modal or page
 */
export const trackThreatDetailView = (threatId, threatType, severity) => {
  if (!threatId) return;
  trackEvent('threat_detail_view', {
    threat_id: String(threatId),
    threat_type: threatType || 'UNKNOWN',
    severity: severity || 'UNKNOWN'
  });
};

/**
 * Tracks threat indicators data export (CSV/JSON)
 */
export const trackThreatExport = (format = 'csv', recordCount = 0) => {
  trackEvent('threat_export_download', {
    file_format: format,
    record_count: recordCount
  });
};

/**
 * Tracks outbound navigation to external threat intelligence portals
 */
export const trackExternalSourceClick = (sourceName, destinationUrl) => {
  trackEvent('external_threat_source_click', {
    source_name: sourceName || 'external_portal',
    destination_domain: extractDomain(destinationUrl)
  });
};

/**
 * Tracks copying an indicator to the clipboard for SOC investigation
 */
export const trackIndicatorCopy = (threatType = 'IOC') => {
  trackEvent('threat_indicator_copy', {
    threat_type: threatType
  });
};

/**
 * Tracks manual or automated data sync requests
 */
export const trackDashboardSync = (triggerSource = 'manual_button') => {
  trackEvent('threat_sync_triggered', {
    trigger_source: triggerSource
  });
};
