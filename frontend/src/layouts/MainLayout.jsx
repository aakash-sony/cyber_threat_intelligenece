import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from '../components/common/Navbar';
import Sidebar from '../components/common/Sidebar';
import Footer from '../components/common/Footer';
import FloatingContactButton from '../components/common/FloatingContactButton';

const MainLayout = ({ onRefresh }) => {
  return (
    <div className="app-container">
      <Navbar onRefresh={onRefresh} />
      <div className="main-wrapper">
        <Sidebar />
        <main className="content-area">
          <div className="page-content-wrapper">
            <Outlet />
          </div>
          <Footer />
        </main>
      </div>
      <FloatingContactButton />
    </div>
  );
};

export default MainLayout;
