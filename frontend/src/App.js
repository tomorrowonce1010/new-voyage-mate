import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import MainLayout from './components/MainLayout';
import Explore from './pages/Explore';
import Manage from './pages/Manage';
import Community from './pages/Community';
import Profile from './pages/Profile';
import EditItinerary from './pages/EditItinerary';
import ViewItinerary from './pages/ViewItinerary';
import DestinationDetail from './pages/DestinationDetail';
import TravelReport from './pages/TravelReport';
import UserHomepage from './pages/UserHomepage';
import GroupTravel from './pages/GroupTravel';
import CreateGroup from './pages/CreateGroup';
import GroupDetail from './pages/GroupDetail';
import Chat from './pages/Chat';
import { installFetchInterceptor } from './utils/fetchInterceptor';

// 安装全局 fetch 拦截器
installFetchInterceptor();

// 获取 API 基础 URL（根据环境变量）
const API_BASE = process.env.REACT_APP_API_BASE_URL;

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // 检查登录状态
  const checkLoginStatus = async () => {
    try {
      const response = await fetch(`${API_BASE}/auth/status`, {
        method: 'GET',
        credentials: 'include',
      });
      const result = await response.json();

      if (result.success) {
        setIsAuthenticated(true);
        setUser({
          id: result.userId,
          username: result.username,
          email: result.email
        });

        // 获取用户 profile
        try {
          const response2 = await fetch(`${API_BASE}/users/profile`, {
            method: 'GET',
            credentials: 'include',
          });

          if (response2.ok) {
            const result2 = await response2.json();
            console.log('用户档案数据:', result2);

            if (result2.avatarUrl) {
              setUser(prevUser => ({
                ...prevUser,
                avatarUrl: result2.avatarUrl
              }));
              console.log('设置头像URL:', result2.avatarUrl);
            }
          } else {
            console.warn('获取用户档案失败，状态码:', response2.status);
          }
        } catch (profileError) {
          console.warn('获取用户档案时发生错误:', profileError);
        }
      } else {
        setIsAuthenticated(false);
        setUser(null);
      }
    } catch (error) {
      console.error('检查登录状态失败:', error);
      setIsAuthenticated(false);
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  // 应用启动时检查登录状态
  useEffect(() => {
    checkLoginStatus();
  }, []);

  // 处理登录成功
  const handleLogin = async (loginResult) => {
    setIsAuthenticated(true);
    setUser({
      id: loginResult.userId,
      username: loginResult.username,
      email: loginResult.email
    });

    // 登录后获取用户档案
    try {
      const response = await fetch(`${API_BASE}/users/profile`, {
        method: 'GET',
        credentials: 'include',
      });

      if (response.ok) {
        const result = await response.json();
        console.log('登录后获取用户档案数据:', result);

        if (result.avatarUrl) {
          setUser(prevUser => ({
            ...prevUser,
            avatarUrl: result.avatarUrl
          }));
          console.log('登录后设置头像URL:', result.avatarUrl);
        }
      } else {
        console.warn('登录后获取用户档案失败，状态码:', response.status);
      }
    } catch (profileError) {
      console.warn('登录后获取用户档案时发生错误:', profileError);
    }
  };

  // 处理登出
  const handleLogout = async () => {
    try {
      await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
      });
    } catch (error) {
      console.error('登出失败:', error);
    } finally {
      setIsAuthenticated(false);
      setUser(null);
    }
  };

  // 更新用户信息（Profile 页面修改后）
  const updateUser = (userData) => {
    setUser(prevUser => ({
      ...prevUser,
      ...userData
    }));
    console.log(user.avatarUrl);
  };

  if (loading) {
    return (
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          fontSize: '18px'
        }}>
          正在加载...
        </div>
    );
  }

  return (
      <Router>
        <Routes>
          {!isAuthenticated ? (
              <>
                <Route path="/login" element={<Login onLogin={handleLogin} />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
              </>
          ) : (
              <Route path="/" element={<MainLayout user={user} onLogout={handleLogout} />}>
                <Route index element={<Navigate to="/explore" replace />} />
                <Route path="explore" element={<Explore />} />
                <Route path="manage" element={<Manage />} />
                <Route path="community" element={<Community />} />
                <Route path="chat">
                  <Route index element={<Navigate to="/chat/friend" replace />} />
                  <Route path="friend" element={<Chat />} />
                  <Route path="group" element={<Chat />} />
                </Route>
                <Route path="profile" element={<Profile user={user} onLogout={handleLogout} onUpdateUser={updateUser} />} />
                <Route path="edit-itinerary/:id" element={<EditItinerary />} />
                <Route path="edit-itinerary" element={<EditItinerary />} />
                <Route path="view-itinerary/:id" element={<ViewItinerary />} />
                <Route path="view-itinerary" element={<ViewItinerary />} />
                <Route path="destination/:id" element={<DestinationDetail />} />
                <Route path="travel-report" element={<TravelReport />} />
                <Route path="user-homepage/:userId" element={<UserHomepage />} />
                <Route path="group-travel" element={<GroupTravel />} />
                <Route path="create-group" element={<CreateGroup />} />
                <Route path="group-detail/:id" element={<GroupDetail />} />
                <Route path="login" element={<Navigate to="/explore" replace />} />
                <Route path="*" element={<Navigate to="/explore" replace />} />
              </Route>
          )}
        </Routes>
      </Router>
  );
}

export default App;
