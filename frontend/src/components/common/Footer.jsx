import React from 'react';
import { Link } from 'react-router-dom';
import { Shield, PhoneCall, ExternalLink, ShieldCheck, Database, Activity, Lock, AlertTriangle } from 'lucide-react';

const Footer = () => {
  return (
    <footer className="portal-footer">
      <div className="portal-footer-container">
        {/* Top Grid: 4 Columns */}
        <div className="footer-grid">
          {/* Col 1: Brand & Purpose */}
          <div className="footer-col brand-col">
            <div className="footer-brand-header">
              <div className="footer-logo-icon">
                <Shield size={20} strokeWidth={2.5} />
              </div>
              <div>
                <h4 className="footer-brand-title">Cyber Threat Intelligence</h4>
                <p className="footer-brand-subtitle">Open-Source Threat Intelligence & Scam Defense</p>
              </div>
            </div>
            <p className="footer-description">
              A comprehensive open cyber intelligence and fraud defense portal providing real-time indicators of compromise (IOCs), malicious URL tracking, and citizen guidance to identify scams and safeguard digital identities.
            </p>
            <div className="footer-soc-badge">
              <span className="footer-soc-pulse"></span>
              <span>Automated 15-Minute Feed Synchronization Active</span>
            </div>
          </div>

          {/* Col 2: Navigation */}
          <div className="footer-col">
            <h5 className="footer-heading">Platform Navigation</h5>
            <ul className="footer-links">
              <li>
                <Link to="/">Threat SOC Dashboard</Link>
              </li>
              <li>
                <Link to="/threats">Live Incident Feed</Link>
              </li>
              <li>
                <Link to="/threats/critical">Critical Severity Threats</Link>
              </li>
              <li>
                <Link to="/threats/high">High Severity Threats</Link>
              </li>
              <li>
                <Link to="/analytics">Analytics & Country Trends</Link>
              </li>
              <li>
                <Link to="/sources">Intelligence Providers</Link>
              </li>
              <li>
                <Link to="/about">Citizen Safety & Guidance</Link>
              </li>
            </ul>
          </div>

          {/* Col 3: Verified Intelligence Streams */}
          <div className="footer-col">
            <h5 className="footer-heading">Threat Intelligence Feeds</h5>
            <ul className="footer-links">
              <li>
                <a href="https://tweetfeed.live" target="_blank" rel="noopener noreferrer" className="footer-external-link">
                  <span>TweetFeed OSINT Telemetry</span>
                  <ExternalLink size={12} />
                </a>
              </li>
              <li>
                <a href="https://urlhaus.abuse.ch" target="_blank" rel="noopener noreferrer" className="footer-external-link">
                  <span>URLhaus (abuse.ch)</span>
                  <ExternalLink size={12} />
                </a>
              </li>
              <li>
                <a href="https://threatfox.abuse.ch" target="_blank" rel="noopener noreferrer" className="footer-external-link">
                  <span>ThreatFox IOCs (abuse.ch)</span>
                  <ExternalLink size={12} />
                </a>
              </li>
              <li>
                <a href="https://openphish.com" target="_blank" rel="noopener noreferrer" className="footer-external-link">
                  <span>OpenPhish Telemetry</span>
                  <ExternalLink size={12} />
                </a>
              </li>
            </ul>
          </div>

          {/* Col 4: Emergency Contacts & Helpline */}
          <div className="footer-col helpline-col">
            <h5 className="footer-heading">Emergency Cyber Helpline</h5>
            <div className="footer-helpline-box">
              <div className="helpline-icon-wrap">
                <PhoneCall size={20} color="#22c55e" />
              </div>
              <div>
                <span className="helpline-call-label">National Cyber Emergency Number</span>
                <a href="tel:1930" className="helpline-call-number" title="Call Emergency Cyber Helpline 1930">
                  1930
                </a>
              </div>
            </div>
            <p className="helpline-tip">
              Toll-free 24/7 victim assistance. If affected by online financial fraud or unauthorized transactions, report immediately within the golden hour.
            </p>
            <a
              href="https://cybercrime.gov.in"
              target="_blank"
              rel="noopener noreferrer"
              className="footer-cybercrime-btn"
            >
              <span>National Cyber Crime Portal</span>
              <ExternalLink size={13} />
            </a>
          </div>
        </div>

        {/* Bottom Bar: Copyright, All Rights Reserved & Status */}
        <div className="footer-bottom-bar">
          <div className="footer-copyright">
            © 2026 <strong>Cyber Threat & Scam Intelligence Portal</strong>. All Rights Reserved.
          </div>
          <div className="footer-status-pill">
            <span className="status-indicator-dot"></span>
            <span>Real-time SOC Monitoring Operational</span>
          </div>
          <div className="footer-meta">
            <span>ISO 27001 Threat Taxonomy Standard</span>
            <span className="meta-separator">•</span>
            <span>Zero-Trust Verification</span>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
