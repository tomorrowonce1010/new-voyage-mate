import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './TravelReport.css';

const TravelReport = () => {
  const navigate = useNavigate();
  const [userStats, setUserStats] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchUserStats();
  }, []);

  const fetchUserStats = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/users/travel-stats', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();
        setUserStats(data);
      } else if (response.status === 401) {
        navigate('/login');
      }
    } catch (error) {
      console.error('获取旅行统计失败:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTimelineItemClick = (destination) => {
    if (destination.hasItinerary) {
      // 跳转到浏览行程界面，并传递来源页面信息
      navigate(`/view-itinerary/${destination.itineraryId}?from=travel-report`);
    } else {
      alert('暂无行程信息');
    }
  };

  if (isLoading) {
    return (
      <div className="travel-report-container">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>正在生成您的旅行报告...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="travel-report-container">
      {/* 返回按钮 */}
      <button className="back-btn" onClick={() => navigate('/profile')}>
        ← 返回个人档案
      </button>

      {/* 欢迎部分 */}
      <div className="welcome-section">
        <div className="welcome-content">
          <h1>Hi! {userStats?.username || '旅行者'} 👋</h1>
          <p className='Welcome-subtitle'>
            今天是 Voyage Mate 陪伴你的第 {userStats?.companionDays || 0} 天
          </p>
          <div className="stats-cards">
            <div className="stat-card">
              <div className="stat-icon">🗺️</div>
              <div className="stat-content">
                <div className="stat-number">{userStats?.totalDestinations || 0}</div>
                <div className="stat-label">足迹目的地</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">📅</div>
              <div className="stat-content">
                <div className="stat-number">{userStats?.totalDays || 0}</div>
                <div className="stat-label">旅行天数</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">✈️</div>
              <div className="stat-content">
                <div className="stat-number">{userStats?.totalItineraries || 0}</div>
                <div className="stat-label">完成行程</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 时间轴部分 */}
      <div className="timeline-section">
        <h2>🕒 我的旅行时间轴</h2>
        <div className="timeline-container">
          {userStats?.timeline && userStats.timeline.length > 0 ? (
            userStats.timeline.map((item, index) => (
              <div key={index} className="timeline-item" onClick={() => handleTimelineItemClick(item)}>
                <div className="timeline-marker">
                  <div className="timeline-dot"></div>
                  {index < userStats.timeline.length - 1 && <div className="timeline-line"></div>}
                </div>
                <div className="timeline-content">
                  <div className="timeline-date">
                    {item.startDate && item.endDate ? 
                      `${item.startDate} 至 ${item.endDate}` : 
                      item.visitYearMonth
                    }
                  </div>
                  <div className="timeline-destination">
                    <h4>{item.name}</h4>
                    <p>{item.days}天 • {item.hasItinerary ? '有行程记录' : '手动添加'}</p>
                    {item.notes && <p className="timeline-notes">{item.notes}</p>}
                  </div>
                  {item.hasItinerary && <div className="timeline-arrow">→</div>}
                </div>
              </div>
            ))
          ) : (
            <div className="empty-timeline">
              <div className="empty-icon">🌟</div>
              <p>还没有足迹记录，快去添加你的第一个足迹吧！</p>
            </div>
          )}
        </div>
      </div>

      {/* 地理统计部分 */}
      <div className="geography-section">
        <h2>🧭 地理足迹统计</h2>
        <div className="geography-grid">
          <div className="geography-card">
            <div className="geography-icon">🌅</div>
            <div className="geography-content">
              <h4>最东</h4>
              <p>{userStats?.geography?.easternmost || '暂无记录'}</p>
            </div>
          </div>
          <div className="geography-card">
            <div className="geography-icon">🏖️</div>
            <div className="geography-content">
              <h4>最南</h4>
              <p>{userStats?.geography?.southernmost || '暂无记录'}</p>
            </div>
          </div>
          <div className="geography-card">
            <div className="geography-icon">🌄</div>
            <div className="geography-content">
              <h4>最西</h4>
              <p>{userStats?.geography?.westernmost || '暂无记录'}</p>
            </div>
          </div>
          <div className="geography-card">
            <div className="geography-icon">❄️</div>
            <div className="geography-content">
              <h4>最北</h4>
              <p>{userStats?.geography?.northernmost || '暂无记录'}</p>
            </div>
          </div>
          <div className="geography-card">
            <div className="geography-icon">📅</div>
            <div className="geography-content">
              <h4>最爱出行月份</h4>
              <p>{userStats?.geography?.favoriteMonth || '暂无记录'}</p>
            </div>
          </div>
          <div className="geography-card">
            <div className="geography-icon">🗓️</div>
            <div className="geography-content">
              <h4>旅行最多年份</h4>
              <p>{userStats?.geography?.mostTravelYear || '暂无记录'}</p>
            </div>
          </div>
        </div>
      </div>

      {/* 城市统计部分 */}
      <div className="city-stats-section">
        <h2>🏙️ 最常去的城市</h2>
        <div className="city-stats-container">
          {userStats?.topCities && userStats.topCities.length > 0 ? (
            userStats.topCities.map((city, index) => (
              <div key={index} className="city-stat-item">
                <div className="city-rank">#{index + 1}</div>
                <div className="city-info">
                  <h4>{city.name}</h4>
                  <p>{city.visitCount}次 • {city.totalDays}天</p>
                </div>
                <div className="city-progress">
                  <div 
                    className="city-progress-bar" 
                    style={{width: `${(city.visitCount / userStats.topCities[0].visitCount) * 100}%`}}
                  ></div>
                </div>
              </div>
            ))
          ) : (
            <div className="empty-cities">
              <div className="empty-icon">🏙️</div>
              <p>还没有足够的数据来分析最常去的城市</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default TravelReport; 