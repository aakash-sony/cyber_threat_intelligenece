import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PhoneCall, ShieldAlert } from 'lucide-react';
import { trackEvent } from '../../analytics/analytics';

const FloatingContactButton = () => {
  const navigate = useNavigate();
  const [isHovered, setIsHovered] = useState(false);

  const isMobileDevice = () => {
    return (
      /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ||
      (window.matchMedia && window.matchMedia('(max-width: 768px)').matches)
    );
  };

  const handleClick = (e) => {
    e.preventDefault();
    const isMobile = isMobileDevice();
    trackEvent('helpline_click', { device_type: isMobile ? 'mobile' : 'desktop' });
    if (isMobile) {
      // Mobile / Phone: Open native phone dialing screen immediately
      window.location.href = 'tel:1930';
    } else {
      // Desktop: Redirect to About page bottom (Emergency Cyber Helpline & Reporting section)
      if (window.location.pathname === '/about') {
        const contactSection = document.getElementById('contact') || document.getElementById('emergency-helpline');
        if (contactSection) {
          contactSection.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      } else {
        navigate('/about#contact');
      }
    }
  };

  return (
    <aside
      className="floating-contact-container"
      aria-label="Emergency cyber helpline and assistance"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Tooltip on hover for desktop */}
      <div className={`floating-contact-tooltip ${isHovered ? 'visible' : ''}`}>
        <span className="tooltip-title">Cyber Emergency Helpline</span>
        <span className="tooltip-sub">Dial 1930 (Mobile) · View Guidance (Desktop)</span>
      </div>

      {/* Floating Action Button */}
      <button
        id="floating-contact-btn"
        className="floating-contact-btn"
        onClick={handleClick}
        title="Emergency Cyber Helpline & Incident Reporting"
        aria-label="Emergency Cyber Helpline 1930"
      >
        {/* Animated Ripple Waves */}
        <span className="contact-pulse-ring"></span>
        <span className="contact-pulse-ring delay"></span>

        <div className="contact-btn-inner">
          <PhoneCall size={22} className="contact-icon" />
          <span className="contact-badge-label">1930</span>
        </div>
      </button>
    </aside>
  );
};

export default FloatingContactButton;
