import React from 'react';
import { ShieldCheck, PhoneCall, AlertTriangle, ExternalLink } from 'lucide-react';

const AboutCard = () => {
  return (
    <div className="card-container" style={{ padding: '20px', marginBottom: '24px', backgroundColor: '#f8fafc', border: '1px solid #e2e8f0' }}>
      <div className="card-header-clean" style={{ marginBottom: '14px' }}>
        <h3 className="card-title-clean" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <ShieldCheck size={20} color="#2563eb" />
          <span>Public Cyber Threat & Safety Advisory</span>
        </h3>
        <span style={{ fontSize: '0.78rem', color: '#059669', fontWeight: 600, backgroundColor: '#ecfdf5', padding: '4px 10px', borderRadius: '20px', border: '1px solid #a7f3d0' }}>
          Official Citizen Guidance
        </span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', fontSize: '0.84rem' }}>
        {/* Box 1: What is this portal? */}
        <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '8px', border: '1px solid #f1f5f9' }}>
          <div style={{ fontWeight: 600, color: '#0f172a', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ color: '#2563eb' }}>●</span> How This Dashboard Protects You
          </div>
          <p style={{ color: '#475569', lineHeight: 1.5 }}>
            This platform monitors live malicious websites, counterfeit banking portals (like SBI, HDFC, Income Tax), and online scams reported across intelligence networks. Use it to check suspicious links before opening them or sharing credentials.
          </p>
        </div>

        {/* Box 2: Common Scam Alerts */}
        <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '8px', border: '1px solid #f1f5f9' }}>
          <div style={{ fontWeight: 600, color: '#dc2626', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <AlertTriangle size={15} color="#dc2626" /> Warning Signs of Cyber Scams
          </div>
          <ul style={{ color: '#475569', lineHeight: 1.5, paddingLeft: '18px', margin: 0 }}>
            <li>Urgent SMS messages warning of electricity cutoff or SIM card block.</li>
            <li>Fake video calls claiming to be police/CBI staging "Digital Arrest".</li>
            <li>Telegram job offers promising daily profits for reviewing hotels/videos.</li>
          </ul>
        </div>

        {/* Box 3: Immediate Reporting & Helpline */}
        <div style={{ backgroundColor: '#ffffff', padding: '14px', borderRadius: '8px', border: '1px solid #f1f5f9' }}>
          <div style={{ fontWeight: 600, color: '#059669', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <PhoneCall size={15} color="#059669" /> Lost Money to a Scam? Report Immediately!
          </div>
          <p style={{ color: '#475569', lineHeight: 1.5, marginBottom: '8px' }}>
            Dial national cyber helpline <strong>1930</strong> immediately within the "golden hour" to freeze fraudulent transactions before money is withdrawn.
          </p>
          <a
            href="https://cybercrime.gov.in"
            target="_blank"
            rel="noopener noreferrer"
            style={{ color: '#2563eb', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '4px', textDecoration: 'none' }}
          >
            <span>Lodge complaint at cybercrime.gov.in</span>
            <ExternalLink size={13} />
          </a>
        </div>
      </div>
    </div>
  );
};

export default AboutCard;
