import React, { useEffect } from 'react';
import { Shield, AlertTriangle, CheckCircle, PhoneCall, ExternalLink, HelpCircle, Eye, Lock } from 'lucide-react';

const AboutPage = () => {
  useEffect(() => {
    if (window.location.hash === '#contact' || window.location.hash === '#emergency-helpline') {
      const el = document.getElementById('contact') || document.getElementById('emergency-helpline');
      if (el) {
        setTimeout(() => {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 150);
      }
    }
  }, []);
  return (
    <div className="about-page" style={{ maxWidth: '1100px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontFamily: 'var(--font-display)', fontSize: '1.65rem', fontWeight: 700, color: '#0f172a', marginBottom: '8px' }}>
          Understanding the Cyber Threat & Scam Intelligence Portal
        </h2>
        <p style={{ fontSize: '0.9rem', color: '#64748b' }}>
          A public security resource created to help citizens, students, and businesses identify malicious websites, recognize cyber fraud traps, and browse the internet safely.
        </p>
      </div>

      {/* Overview Card */}
      <div className="card-container" style={{ padding: '26px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
          <Shield size={24} color="#2563eb" />
          <h3 style={{ fontSize: '1.2rem', fontWeight: 600, color: '#0f172a' }}>
            What is the Purpose of This Platform?
          </h3>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#334155', lineHeight: 1.7, marginBottom: '16px' }}>
          Every day, thousands of fraudulent links and scam campaigns are launched to steal money, personal identity documents, and passwords. This dashboard gathers verified data on active cyber threats from public safety agencies and international threat repositories. It translates complex cyber intelligence into clean, easy-to-understand metrics so anyone can check whether a link, message, or website has been flagged as dangerous.
        </p>
      </div>

      {/* 3 Major Threat Types Explained */}
      <div className="card-container" style={{ padding: '26px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <AlertTriangle size={22} color="#dc2626" />
          <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#0f172a' }}>
            Threat Categories Explained in Simple Terms
          </h3>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '18px' }}>
          <div style={{ backgroundColor: '#eff6ff', padding: '18px', borderRadius: '10px', border: '1px solid #bfdbfe' }}>
            <h4 style={{ fontSize: '0.98rem', fontWeight: 600, color: '#1e40af', marginBottom: '6px' }}>
              1. Phishing & Counterfeit Portals
            </h4>
            <p style={{ fontSize: '0.84rem', color: '#1e3a8a', lineHeight: 1.6 }}>
              Deceptive clone websites designed to look identical to legitimate banks (such as SBI, HDFC, ICICI), tax departments (Income Tax e-filing), or utility providers. Their objective is to trick you into typing your username, password, ATM PIN, or card details.
            </p>
          </div>

          <div style={{ backgroundColor: '#f5f3ff', padding: '18px', borderRadius: '10px', border: '1px solid #ddd6fe' }}>
            <h4 style={{ fontSize: '0.98rem', fontWeight: 600, color: '#6d28d9', marginBottom: '6px' }}>
              2. Malware & Dangerous Downloads
            </h4>
            <p style={{ fontSize: '0.84rem', color: '#5b21b6', lineHeight: 1.6 }}>
              Malicious files and links (such as rogue APKs, scripts, or executables) that secretly install viruses, spyware, or trojans onto your smartphone or laptop. Once installed, attackers can read your messages, intercept OTPs, and control your device.
            </p>
          </div>

          <div style={{ backgroundColor: '#fef2f2', padding: '18px', borderRadius: '10px', border: '1px solid #fecaca' }}>
            <h4 style={{ fontSize: '0.98rem', fontWeight: 600, color: '#b91c1c', marginBottom: '6px' }}>
              3. Cyber Financial Fraud & Scams
            </h4>
            <p style={{ fontSize: '0.84rem', color: '#991b1b', lineHeight: 1.6 }}>
              Social engineering schemes reported to law enforcement. Examples include <strong>Digital Arrest</strong> scams (fake video calls impersonating police/CBI), <strong>Electricity Bill cutoff</strong> threats, <strong>Part-Time Telegram Job</strong> investments, and fake instant loan apps.
            </p>
          </div>
        </div>
      </div>

      {/* How to Use This Website */}
      <div className="card-container" style={{ padding: '26px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <HelpCircle size={22} color="#059669" />
          <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#0f172a' }}>
            How to Use This Platform Effectively
          </h3>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
          <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid #e2e8f0', backgroundColor: '#f8fafc' }}>
            <div style={{ fontWeight: 600, color: '#0f172a', marginBottom: '4px' }}>Step 1: Check Suspicious Links</div>
            <p style={{ fontSize: '0.82rem', color: '#475569', lineHeight: 1.5 }}>
              Use the search bar on the Threats page to look up domain names or URLs received via SMS or email before clicking them.
            </p>
          </div>

          <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid #e2e8f0', backgroundColor: '#f8fafc' }}>
            <div style={{ fontWeight: 600, color: '#0f172a', marginBottom: '4px' }}>Step 2: Filter by Country</div>
            <p style={{ fontSize: '0.82rem', color: '#475569', lineHeight: 1.5 }}>
              Select your country (e.g. India 🇮🇳) to observe localized scam campaigns, banking phishing traps, and regional threat trends.
            </p>
          </div>

          <div style={{ padding: '14px', borderRadius: '8px', border: '1px solid #e2e8f0', backgroundColor: '#f8fafc' }}>
            <div style={{ fontWeight: 600, color: '#0f172a', marginBottom: '4px' }}>Step 3: Analyze Time Windows</div>
            <p style={{ fontSize: '0.82rem', color: '#475569', lineHeight: 1.5 }}>
              Toggle between Last 24 Hours, Last Week, Last 1 Month, and Last 3 Months to understand whether an attack campaign is surging or diminishing.
            </p>
          </div>
        </div>
      </div>

      {/* Golden Rules for Staying Safe Online */}
      <div className="card-container" style={{ padding: '26px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <Lock size={22} color="#2563eb" />
          <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#0f172a' }}>
            5 Golden Rules for Everyday Online Safety
          </h3>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.88rem', color: '#334155' }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
            <CheckCircle size={18} color="#16a34a" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>UPI PIN is only needed to SEND money, never to RECEIVE money:</strong> If someone asks you to enter your UPI PIN or scan a QR code to receive lottery or cashback, it is a guaranteed scam.
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
            <CheckCircle size={18} color="#16a34a" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>No Police or Court conducts arrests over video calls:</strong> The Indian Cybercrime Coordination Centre (I4C) explicitly clarifies that "Digital Arrest" is an extortion fraud. Police never ask for money transfers to clear case files.
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
            <CheckCircle size={18} color="#16a34a" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Inspect the website address (URL) closely:</strong> A fake site might look like <code>sbi-netbanking-portal.com</code> instead of the authentic <code>onlinesbi.sbi</code>. Verify the official domain before entering passwords.
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
            <CheckCircle size={18} color="#16a34a" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Never install screen-sharing applications for customer support:</strong> Scammers often instruct victims to download AnyDesk, TeamViewer, or QuickSupport to view their phone screen and steal OTPs.
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
            <CheckCircle size={18} color="#16a34a" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Avoid APK downloads from messaging apps:</strong> Official apps should only ever be downloaded from the Google Play Store or Apple App Store, never from WhatsApp or Telegram download links.
            </div>
          </div>
        </div>
      </div>

      {/* Emergency Cyber Helpline */}
      <div id="contact" className="card-container emergency-contact-card" style={{ padding: '26px', backgroundColor: '#f0fdf4', border: '1px solid #bbf7d0', scrollMarginTop: '80px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
          <PhoneCall size={22} color="#16a34a" />
          <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#166534' }}>
            Emergency Reporting & Victim Assistance
          </h3>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#14532d', lineHeight: 1.6, marginBottom: '14px' }}>
          If you or someone you know has fallen victim to cyber financial fraud in India, dial the national cyber emergency helpline <strong>1930</strong> immediately. Reporting within 1 to 2 hours enables financial institutions to freeze the scammer's bank account before the money is withdrawn.
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <a
            href="https://cybercrime.gov.in"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-apply-filters"
            style={{ width: 'auto', display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '8px 20px', textDecoration: 'none' }}
          >
            <span>Visit National Cyber Crime Portal (cybercrime.gov.in)</span>
            <ExternalLink size={15} />
          </a>
        </div>
      </div>
    </div>
  );
};

export default AboutPage;
