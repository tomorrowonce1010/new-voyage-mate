import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import './UserHomepage.css';

const UserHomepage = () => {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [userInfo, setUserInfo] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // 获取来源页面信息
  const fromPage = searchParams.get('from') || 'community';

  useEffect(() => {
    fetchUserHomepage();
  }, [userId]);

  const fetchUserHomepage = async () => {
    try {
      setIsLoading(true);
      const response = await fetch(`/api/users/homepage/${userId}`, {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        setUserInfo(data);
        setError(null);
      } else {
        setError('用户不存在或信息不可访问');
      }
    } catch (error) {
      console.error('获取用户主页失败:', error);
      setError('获取用户信息失败，请稍后再试');
    } finally {
      setIsLoading(false);
    }
  };

  const handleItineraryClick = (itineraryId) => {
    // 跳转到浏览行程页面，并设置返回来源为用户主页
    navigate(`/view-itinerary/${itineraryId}?from=user-homepage&userId=${userId}&returnFrom=${fromPage}`);
  };

  const handleBackClick = () => {
    if (fromPage === 'community') {
      navigate('/community');
    } else if (fromPage === 'chat') {
      navigate('/chat/friend');
    } else {
      navigate('/'); // 默认返回首页
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '未知';
    try {
      const date = new Date(dateString);
      return date.getFullYear() + '年' + (date.getMonth() + 1) + '月' + date.getDate() + '日';
    } catch {
      return '未知';
    }
  };

  const formatBirthday = (birthday) => {
    if (!birthday) return '未设置';
    try {
      const date = new Date(birthday);
      return (date.getMonth() + 1) + '月' + date.getDate() + '日';
    } catch {
      return '未设置';
    }
  };

  if (isLoading) {
    return (
      <div className="user-homepage-container">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>正在加载用户信息...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="user-homepage-container">
        <div className="error-container">
          <div className="error-icon">😔</div>
          <h3>无法获取用户信息</h3>
          <p>{error}</p>
          <button className="back-btn" onClick={handleBackClick}>
            返回{fromPage === 'community' ? '社区' : fromPage === 'chat' ? '聊天' : '首页'}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="user-homepage-container">
      {/* 返回按钮 */}
      <button className="back-btn" onClick={handleBackClick}>
        ← 返回{fromPage === 'community' ? '社区' : fromPage === 'chat' ? '聊天' : '首页'}
      </button>

      {/* 用户信息区域 */}
      <div className="user-info-section">
        <div className="user-homepage-avatar">
          {userInfo.avatarUrl ? (<img
                 src={ `/api${userInfo.avatarUrl}`}
                 alt="用户头像"
                 className="avatar-image"
               />):(<div className="avatar-auto-generated">
                {userInfo.username ? userInfo.username.charAt(0) : '用'}
              </div>
          )}
        </div>

        <div className="user-details">
          <h1 className="profile-user-name">{userInfo.username}</h1>

          {/* 个性签名 - 直接显示为斜体文本 */}
          {userInfo.signature && (
              <div className="user-signature">{userInfo.signature}</div>
          )}

          <div className="user-basic-info">
            <div className="info-item">
              <span className="info-label">生日:</span>
              <span className="info-value">{formatBirthday(userInfo.birthday)}</span>
            </div>
          </div>

          {/* 个人简介 - 直接显示无容器 */}
          {userInfo.bio && (
              <div className="user-bio">
                <h3 style={{color: 'var(--text-primary)', marginBottom: '8px'}}>个人简介</h3>
                <p>{userInfo.bio}</p>
              </div>
          )}
        </div>
      </div>

      {/* 公开行程区域 */}
      <div className="public-itineraries-section">
        <h2>
          <span className="section-icon">✈️</span>
          {userInfo.username}的公开行程
          <span className="itinerary-count">({userInfo.publicItineraries ? userInfo.publicItineraries.length : 0})</span>
        </h2>

        {userInfo.publicItineraries && userInfo.publicItineraries.length > 0 ? (
          <div className="trip-grid">
            {userInfo.publicItineraries.map((itinerary) => (
              <div
                key={itinerary.id}
                className="user-homepage-trip-card"
                onClick={() => handleItineraryClick(itinerary.id)}
              >
                <div className="trip-image">
                  {itinerary.imageUrl ? (
                    <img src={`/api${itinerary.imageUrl}`} alt={itinerary.title} />
                  ) : (
                    <div className="default-image">
                      <span className="image-icon">🗺️</span>
                    </div>
                  )}
                  <div className="trip-duration">{itinerary.duration}</div>
                </div>

                <div className="trip-content">
                  <h3 className="trip-title">{itinerary.title}</h3>
                  <p className="trip-description">{itinerary.description}</p>
                  
                  <div className="trip-meta">
                    <div className="trip-dates">
                      <span className="date-icon">📅</span>
                      {itinerary.startDate && itinerary.endDate ? (
                        <span>
                          {formatDate(itinerary.startDate)} - {formatDate(itinerary.endDate)}
                        </span>
                      ) : (
                        <span>日期待定</span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="trip-overlay">
                  <span>点击查看详情</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-itineraries">
            <div className="empty-icon">📋</div>
            <h3>暂无公开行程</h3>
            <p>{userInfo.username}还没有设置任何公开行程</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default UserHomepage; 