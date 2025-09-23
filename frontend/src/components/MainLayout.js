import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import './MainLayout.css';
import TransferStation from './TransferStation';

const MainLayout = ({ user, onLogout }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuVisible, setMobileMenuVisible] = useState(false);
  const [userMenuVisible, setUserMenuVisible] = useState(false);
  const [chatMenuOpen, setChatMenuOpen] = useState(false);
  const [transferStationVisible, setTransferStationVisible] = useState(false);

  const menuItems = [
    {
      key: '/explore',
      icon: '🔍',
      label: 'Explore',
      path: '/explore'
    },
    {
      key: '/manage',
      icon: '📝',
      label: 'Manage',
      path: '/manage'
    },
    {
      key: '/group-travel',
      icon: '🎒',
      label: 'Group Travel',
      path: '/group-travel'
    },
    {
      key: '/community',
      icon: '👥',
      label: 'Community',
      path: '/community'
    },
    {
      key: '/profile',
      icon: '👤',
      label: 'Profile',
      path: '/profile'
    },
    // 聊天菜单不在这里直接渲染
  ];

  const handleMenuClick = (path) => {
    navigate(path);
    setMobileMenuVisible(false);
  };

  const toggleMobileMenu = () => {
    setMobileMenuVisible(!mobileMenuVisible);
  };

  const closeMobileSidebar = () => {
    setMobileMenuVisible(false);
  };

  const toggleUserMenu = () => {
    setUserMenuVisible(!userMenuVisible);
  };

  const handleUserMenuClick = (action) => {
    setUserMenuVisible(false);
    if (action === 'profile') {
      navigate('/profile');
    } else if (action === 'logout') {
      onLogout();
    }
  };

  // 切换中转站显示状态
  const toggleTransferStation = () => {
    setTransferStationVisible(!transferStationVisible);
  };

  // 处理从中转站添加景点到行程
  const handleAddToItinerary = (attraction) => {
    // 如果当前在编辑行程页面，触发添加景点事件
    if (location.pathname.includes('/edit-itinerary/')) {
      // 通过自定义事件通知EditItinerary组件
      const event = new CustomEvent('addAttractionFromTransferStation', {
        detail: { attraction }
      });
      window.dispatchEvent(event);
    } else if (location.pathname === '/manage') {
      // 如果在管理页面，存储要添加的景点并提示用户
      localStorage.setItem('pendingAttractionToAdd', JSON.stringify(attraction));
      const shouldNavigate = window.confirm(
        `要将景点"${attraction.name}"添加到行程吗？\n\n点击"确定"将跳转到行程编辑页面，您需要先选择要编辑的行程。\n点击"取消"将取消操作。`
      );
      if (shouldNavigate) {
        // 导航到管理页面，用户可以选择要编辑的行程
        navigate('/manage');
        // 显示提示信息
        setTimeout(() => {
          alert('请先点击要编辑的行程卡片，然后在中转站中再次点击"➕"按钮添加景点。');
        }, 500);
      } else {
        // 用户取消，清除存储的景点
        localStorage.removeItem('pendingAttractionToAdd');
      }
    } else {
      // 如果不在编辑行程页面，提示用户
      alert('请在编辑行程页面使用此功能，或从管理页面选择要编辑的行程');
    }
  };

  // 窗口大小改变时的处理
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth > 768) {
        setMobileMenuVisible(false);
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // 点击外部区域关闭用户菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (userMenuVisible && !event.target.closest('.user-section')) {
        setUserMenuVisible(false);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, [userMenuVisible]);

  return (
    <div className="main-layout">
      {/* 遮罩层 */}
      <div 
        className={`sidebar-overlay ${mobileMenuVisible ? 'active' : ''}`}
        onClick={closeMobileSidebar}
      />

      {/* 顶部导航栏 */}
      <nav className="top-nav">
        <div className="logo-section">
          <button className="mobile-menu-btn" onClick={toggleMobileMenu}>☰</button>
          <div className="logo-icon">✈️</div>
          <div className="logo-text">Voyage Mate</div>
        </div>
        <div className="user-section" onClick={toggleUserMenu}>
          <div className="user-avatar">
            {user?.avatarUrl ? (
              <img
                src={`/api${user.avatarUrl}`}
                alt="用户头像"
                className="avatar-image"
                onError={(e) => {
                  console.log('头像加载失败:', e.target.src);
                  e.target.style.display = 'none';
                  // 显示默认头像
                  const defaultAvatar = e.target.nextSibling;
                  if (defaultAvatar) {
                    defaultAvatar.style.display = 'flex';
                  }
                }}
              />
            ) : (
              <div className="avatar-auto-generated">
                {user?.username ? user.username.charAt(0) : '张'}
              </div>
            )}
            {/* 默认头像（隐藏状态，用于头像加载失败时显示） */}
            {user?.avatarUrl && (
              <div className="avatar-auto-generated" style={{ display: 'none' }}>
                {user?.username ? user.username.charAt(0) : '张'}
              </div>
            )}
          </div>
          <div className="user-info">
            <div className="user-name">{user?.username || '张三'}</div>
            <div className="user-status">{user?.status || '探索者'}</div>
          </div>
          <div className="user-menu-arrow">▼</div>
          
          {/* 用户下拉菜单 */}
          {userMenuVisible && (
            <div className="user-dropdown-menu">
              <div 
                className="user-menu-item"
                onClick={() => handleUserMenuClick('profile')}
              >
                <span className="menu-icon">👤</span>
                <span>个人资料</span>
              </div>
              <div className="user-menu-divider"></div>
              <div 
                className="user-menu-item logout"
                onClick={() => handleUserMenuClick('logout')}
              >
                <span className="menu-icon">🚪</span>
                <span>退出登录</span>
              </div>
            </div>
          )}
        </div>
      </nav>

      {/* 主要容器 */}
      <div className="main-container">
        {/* 左侧边栏 */}
        <aside className={`sidebar ${mobileMenuVisible ? 'mobile-open' : ''}`}>
          <ul className="sidebar-menu">
            {menuItems.map(item => (
              <li key={item.key} className="sidebar-item">
                <a 
                  href="#"
                  className={`sidebar-link ${location.pathname === item.path ? 'active' : ''}`}
                  onClick={(e) => {
                    e.preventDefault();
                    handleMenuClick(item.path);
                  }}
                >
                  <span className="sidebar-icon">{item.icon}</span>
                  <span>{item.label}</span>
                </a>
              </li>
            ))}
            {/* 聊天菜单展开子项 */}
            <li className="sidebar-item">
              <div
                className={`sidebar-link ${location.pathname.startsWith('/chat') ? 'active' : ''}`}
                onClick={() => setChatMenuOpen(v => !v)}
                style={{cursor:'pointer',display:'flex',alignItems:'center',justifyContent:'space-between'}}
              >
                <span>
                  <span className="sidebar-icon">💬</span>
                  <span>Chat</span>
                </span>
                <span style={{fontSize:'0.9em',marginLeft:4}}>{chatMenuOpen ? '▲' : '▼'}</span>
              </div>
              {chatMenuOpen && (
                <ul className="sidebar-submenu">
                  <li className={`sidebar-subitem${location.pathname === '/chat/friend' ? ' active' : ''}`}
                    onClick={() => handleMenuClick('/chat/friend')}
                    style={{fontFamily: 'Righteous, Arial, sans-serif', letterSpacing: '0.5px'}}
                  >Friends</li>
                  <li className={`sidebar-subitem${location.pathname === '/chat/group' ? ' active' : ''}`}
                    onClick={() => handleMenuClick('/chat/group')}
                    style={{fontFamily: 'Righteous, Arial, sans-serif', letterSpacing: '0.5px'}}
                  >Groups</li>
                </ul>
              )}
            </li>
          </ul>
        </aside>

        {/* 主内容区域 */}
        <main className="content-area">
          <Outlet />
        </main>
      </div>

      {/* 中转站组件 */}
      <TransferStation
        isVisible={transferStationVisible}
        onToggle={toggleTransferStation}
        onAddToItinerary={handleAddToItinerary}
      />
    </div>
  );
};

export default MainLayout; 