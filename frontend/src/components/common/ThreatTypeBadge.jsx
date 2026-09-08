import React from 'react';
import { Link2, Bug, ShieldAlert, Radio, Cpu, Lock, Radar, AlertTriangle, Mail } from 'lucide-react';

const ThreatTypeBadge = ({ type }) => {
  const t = type ? type.toUpperCase() : 'SUSPICIOUS';

  let Icon = AlertTriangle;
  let color = '#64748b';
  let label = type || 'Unknown';

  switch (t) {
    case 'PHISHING':
      Icon = Link2;
      color = '#2563eb';
      label = 'Phishing';
      break;
    case 'MALWARE':
      Icon = Bug;
      color = '#7c3aed';
      label = 'Malware';
      break;
    case 'FRAUD':
      Icon = ShieldAlert;
      color = '#dc2626';
      label = 'Fraud Report';
      break;
    case 'COMMAND_AND_CONTROL':
      Icon = Radio;
      color = '#b91c1c';
      label = 'C2 Server';
      break;
    case 'BOTNET':
      Icon = Cpu;
      color = '#0284c7';
      label = 'Botnet';
      break;
    case 'RANSOMWARE':
      Icon = Lock;
      color = '#e11d48';
      label = 'Ransomware';
      break;
    case 'SCANNING':
      Icon = Radar;
      color = '#059669';
      label = 'Scanning';
      break;
    case 'SPAM':
      Icon = Mail;
      color = '#d97706';
      label = 'Spam';
      break;
    default:
      Icon = AlertTriangle;
      color = '#f59e0b';
      label = 'Suspicious';
  }

  return (
    <span className="type-cell-badge">
      <span
        style={{
          width: '24px',
          height: '24px',
          borderRadius: '6px',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: `${color}18`,
          color: color
        }}
      >
        <Icon size={14} strokeWidth={2.2} />
      </span>
      <span>{label}</span>
    </span>
  );
};

export default ThreatTypeBadge;
