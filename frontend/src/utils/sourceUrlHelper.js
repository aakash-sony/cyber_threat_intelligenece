/**
 * Utility functions for resolving deep links and search URLs across verified threat feeds.
 */

export const isGenericUrl = (url) => {
  if (!url) return true;
  const clean = url.trim().replace(/\/+$/, '');
  const genericRoots = [
    'https://urlhaus.abuse.ch',
    'https://urlhaus.abuse.ch/browse',
    'https://openphish.com',
    'https://threatfox.abuse.ch',
    'https://tweetfeed.live'
  ];
  return genericRoots.some((root) => clean === root);
};

export const getExactSourceUrl = (threat) => {
  if (!threat) return null;

  const { source, indicator, indicatorType, sourceUrl } = threat;
  const ind = (indicator || '').trim();

  if (sourceUrl && !isGenericUrl(sourceUrl)) {
    return sourceUrl;
  }

  const srcLower = (source || '').toLowerCase();

  // 1. TweetFeed (OSINT reports on X)
  if (srcLower.includes('tweetfeed')) {
    if (sourceUrl && (sourceUrl.includes('x.com') || sourceUrl.includes('twitter.com'))) {
      return sourceUrl;
    }
    return 'https://tweetfeed.live';
  }

  // 2. URLhaus
  if (srcLower.includes('urlhaus')) {
    return `https://urlhaus.abuse.ch/browse.php?search=${encodeURIComponent(ind)}`;
  }

  // 3. ThreatFox
  if (srcLower.includes('threatfox')) {
    if (/^[a-zA-Z0-9.\-_]+$/.test(ind)) {
      return `https://threatfox.abuse.ch/ioc/${ind}/`;
    }
    return `https://threatfox.abuse.ch/browse.php?search=ioc%3A${encodeURIComponent(ind)}`;
  }

  // 4. OpenPhish
  if (srcLower.includes('openphish')) {
    return `https://openphish.com/?url=${encodeURIComponent(ind)}`;
  }

  // 5. Default to VirusTotal search if specific provider search is unavailable
  return `https://www.virustotal.com/gui/search/${encodeURIComponent(ind)}`;
};

export const getVirusTotalUrl = (indicator) => {
  if (!indicator) return null;
  return `https://www.virustotal.com/gui/search/${encodeURIComponent(indicator.trim())}`;
};

export const getSourceActionLabel = (threat) => {
  if (!threat?.source) return 'View Original Source';
  const src = threat.source.trim();
  if (src.includes('TweetFeed')) return 'View Tweet / OSINT Report on X';
  if (src.includes('URLhaus')) return 'Search on URLhaus';
  if (src.includes('ThreatFox')) return 'Search on ThreatFox';
  if (src.includes('OpenPhish')) return 'Search on OpenPhish';
  return `Search on ${src}`;
};
