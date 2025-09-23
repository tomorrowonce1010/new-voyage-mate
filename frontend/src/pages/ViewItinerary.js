import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import './EditItinerary.css';
import RouteMap from '../components/RouteMap';

const ViewItinerary = () => {
  const navigate = useNavigate();
  const { id: tripId } = useParams();
  const [searchParams] = useSearchParams();
  const [selectedDate, setSelectedDate] = useState('overview');
  const [itineraryData, setItineraryData] = useState(null);

  // 添加地图引用
  const mapRef = useRef(null);

  // 重新初始化地图的函数
  const reinitializeMap = useCallback(() => {
    if (mapRef.current && mapRef.current.reinitializeMap) {
      console.log('触发地图重新初始化');
      mapRef.current.reinitializeMap();
    }
  }, []);

  // 获取来源页面信息
  const fromPage = searchParams.get('from');

  // 根据来源页面确定返回地址和文本
  const getReturnInfo = () => {
    if (fromPage === 'travel-report') {
      return {
        path: '/travel-report',
        text: '返回旅行报告'
      };
    } else if (fromPage === 'community') {
      return {
        path: '/community',
        text: '返回社区'
      };
    } else if (fromPage === 'user-homepage') {
      const userId = searchParams.get('userId');
      const returnFrom = searchParams.get('returnFrom') || 'community';
      return {
        path: `/user-homepage/${userId}?from=${returnFrom}`,
        text: '返回用户主页'
      };
    } else {
      return {
        path: '/manage',
        text: '返回我的行程'
      };
    }
  };

  const returnInfo = getReturnInfo();



  // 从URL获取行程ID并加载数据
  useEffect(() => {
    if (tripId) {
      fetchItineraryData(tripId);
    } else {
      navigate('/manage');
    }
  }, [tripId, navigate]);

  // 从后端获取行程数据
  const fetchItineraryData = async (tripId) => {
    try {
      const response = await fetch(`/api/itineraries/${tripId}`, {
        method: 'GET',
        credentials: 'include'
      });

      if (response.status === 401) {
        // 用户未登录，重定向到登录页面
        console.log('用户未登录，重定向到登录页面');
        navigate('/login');
        return;
      }

      if (!response.ok) {
        throw new Error(`获取行程失败: ${response.status}`);
      }

      const itinerary = await response.json();
      
      let dailyPlan = [];
      const overallDestSet = new Set();
      if (itinerary.itineraryDays && itinerary.itineraryDays.length > 0) {
        // 先并行获取每个日程下的活动
        const activityLists = await Promise.all(
          itinerary.itineraryDays.map(day => 
            fetch(`/api/activities/day/${day.id}`, {
              method: 'GET',
              credentials: 'include'
            })
              .then(res => res.ok ? res.json() : [])
              .catch(() => [])
          )
        );

        dailyPlan = itinerary.itineraryDays.map((day, idx) => {
          // 获取该天的所有活动
          const allActivities = activityLists[idx];
          // 创建活动ID到活动对象的映射
          const activityMap = new Map(allActivities.map(activity => [activity.id, activity]));
          
          // 按链表顺序排序活动
          const orderedActivities = [];
          let currentId = day.firstActivityId;
          
          while (currentId && activityMap.has(currentId)) {
            const currentActivity = activityMap.get(currentId);
            orderedActivities.push(currentActivity);
            currentId = currentActivity.nextId;
          }
          
          return {
            dayId: day.id,
            day: day.dayNumber,
            date: day.date,
            city: (() => {
              const dayDestSet = new Set();
              orderedActivities.forEach(act => {
                if (act.attraction && act.attraction.destination && act.attraction.destination.name) {
                  dayDestSet.add(act.attraction.destination.name);
                }
              });
              dayDestSet.forEach(d => overallDestSet.add(d));
              return dayDestSet.size > 0 ? Array.from(dayDestSet).join('、') : '待规划';
            })(),
            title: day.title,
            activities: orderedActivities.map(activity => ({
              id: activity.id,
              location: activity.attraction?.name || '',
              activity: activity.title,
              time: activity.startTime && activity.endTime ? 
                `${activity.startTime.substring(0,5)}-${activity.endTime.substring(0,5)}` : 
                '时间待定',
              transport: activity.transportMode || '步行',
              notes: activity.attractionNotes || '',
              longitude: activity.attraction?.longitude || activity.longitude,
              latitude: activity.attraction?.latitude || activity.latitude,
              attraction: activity.attraction
            }))
          };
        });
      }
      
      // 构建前端需要的数据格式
      const formattedData = {
        id: itinerary.id,
        title: itinerary.title,
        destination: overallDestSet.size > 0 ? Array.from(overallDestSet).join('、') : '待规划目的地',
        startDate: itinerary.startDate,
        endDate: itinerary.endDate,
        duration: calculateDuration(itinerary.startDate, itinerary.endDate),
        description: '精彩的旅程回忆',
        participants: Array(itinerary.travelerCount || 1).fill().map((_, i) => i === 0 ? '我' : `同行者${i}`),
        travelerCount: itinerary.travelerCount || 1,
        tags: ['已完成'],
        budget: itinerary.budget ? `￥${itinerary.budget}` : '预算未记录',
        status: itinerary.travelStatus === '待出行' ? 'upcoming' : 'completed',
        visibility: itinerary.permissionStatus === '私人' ? 'private' : 'public',
        dailyPlan: dailyPlan
      };

      setItineraryData(formattedData);
    } catch (error) {
      console.error('获取行程数据失败:', error);
      alert(`获取行程数据失败: ${error.message}`);
      navigate('/manage');
    }
  };

  // 计算行程天数
  const calculateDuration = (startDate, endDate) => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return `${diffDays}天`;
  };

  // 切换日期标签
  const switchDateTab = (dateId) => {
    setSelectedDate(dateId);
    // 调试：切换天时打印对应天的dayData
    if (itineraryData && itineraryData.dailyPlan) {
      if (dateId === 'overview') {
        console.log('切换到 overview');
      } else {
        const dayData = itineraryData.dailyPlan.find(day => `day${day.day}` === dateId);
        if (dayData) {
          console.log(`切换到${dateId}，dayData:`, JSON.stringify(dayData, null, 2));
        }
      }
    }
  };

  // 渲染景点项目（只读版本，移除设置按钮）
  const renderAttractionItem = (attraction) => (
    <div key={attraction.name} className="attraction-item">
      <div className="attraction-header">
        <div className={`attraction-icon ${attraction.type}`}>
          {attraction.icon}
        </div>
        <div className="attraction-name">{attraction.name}</div>
      </div>
      <div className="attraction-details">{attraction.desc}</div>
      <div className="attraction-time">{attraction.time}</div>
    </div>
  );

  // 渲染交通栏（只读版本，移除点击事件）
  const renderTransportBar = (from, to, transportMode = '步行') => {
    // 获取交通方式的图标
    const getTransportIcon = (transport) => {
      const t = transport?.toLowerCase() || '';
      if (t.includes('步行') || t.includes('walking')) return '🚶';
      if (t.includes('骑行') || t.includes('自行车') || t.includes('cycling')) return '🚴';
      if (t.includes('驾车') || t.includes('开车') || t.includes('car') || t.includes('driving')) return '🚗';
      if (t.includes('公交') || t.includes('地铁') || t.includes('subway') || t.includes('bus')) return '🚇';
      if (t.includes('出租车') || t.includes('打车') || t.includes('taxi')) return '🚕';
      if (t.includes('飞机') || t.includes('航班') || t.includes('flight')) return '✈️';
      if (t.includes('火车') || t.includes('高铁') || t.includes('train')) return '🚄';
      return '🚇'; // 默认公共交通
    };

    return (
    <div className="transport-bar">
      <div className="transport-line"></div>
        <div className="transport-method">
          <span>{getTransportIcon(transportMode)}</span>
          <span>{transportMode}</span>
      </div>
      <div className="transport-line"></div>
    </div>
  );
  };

  // 获取活动图标
  const getActivityIcon = (activity) => {
    const location = activity.location?.toLowerCase() || '';
    const activityName = activity.activity?.toLowerCase() || '';
    
    if (location.includes('机场') || activityName.includes('航班') || activityName.includes('抵达')) return '✈️';
    if (location.includes('酒店') || activityName.includes('入住') || activityName.includes('退房')) return '🏨';
    if (location.includes('餐') || activityName.includes('餐') || activityName.includes('食')) return '🍽️';
    if (location.includes('站') || activityName.includes('乘坐') || activityName.includes('交通')) return '🚌';
    if (location.includes('寺') || location.includes('庙') || location.includes('宫')) return '🏛️';
    if (location.includes('公园') || location.includes('山') || location.includes('湖') || location.includes('景')) return '🏞️';
    if (activityName.includes('购物') || location.includes('街') || location.includes('市场')) return '🛍️';
    return '📍';
  };

  // 获取活动类型
  const getActivityType = (activity) => {
    const location = activity.location?.toLowerCase() || '';
    const activityName = activity.activity?.toLowerCase() || '';
    
    if (location.includes('机场') || activityName.includes('航班') || activityName.includes('乘坐')) return 'transport';
    if (location.includes('酒店') || activityName.includes('入住')) return 'hotel';
    if (location.includes('餐') || activityName.includes('餐') || activityName.includes('食')) return 'food';
    return 'location';
  };

  // 获取当前选中日期的活动标记（与 EditItinerary 保持一致）
  const getCurrentDayMarkers = () => {
    const markers = [];
    if (!itineraryData || !itineraryData.dailyPlan) return markers;
    if (selectedDate === 'overview') {
      itineraryData.dailyPlan.forEach((dayData, dayIndex) => {
        if (dayData.activities && dayData.activities.length > 0) {
          // 调试：打印每一天的activities
          console.log(`overview模式-第${dayIndex+1}天 activities:`, dayData.activities);
          dayData.activities.forEach((activity, activityIndex) => {
            if (activity.location) {
              let longitude = activity.longitude;
              let latitude = activity.latitude;
              if (!longitude && !latitude && activity.attraction) {
                longitude = activity.attraction.longitude;
                latitude = activity.attraction.latitude;
              }
              const locationInfo = {
                name: `${dayData.date} - ${activity.activity || `活动 ${activityIndex + 1}`}`,
                description: activity.location,
                address: activity.location,
                longitude: longitude,
                latitude: latitude,
                dayInfo: `第${dayData.day}天`,
                activityName: activity.activity || `活动 ${activityIndex + 1}`,
                transportMode: dayData.activities[activityIndex + 1]?.transport || null,
                transport: activity.transport || null
              };
              if (locationInfo.longitude && locationInfo.latitude) {
                markers.push(locationInfo);
              }
            }
          });
        }
      });
    } else {
      const currentDayData = itineraryData.dailyPlan.find(day => `day${day.day}` === selectedDate);
      if (currentDayData && currentDayData.activities && currentDayData.activities.length > 0) {
        // 调试：打印当前天的activities
        console.log(`单天模式-当前天 activities:`, currentDayData.activities);
        currentDayData.activities.forEach((activity, index) => {
          if (activity.location) {
            let longitude = activity.longitude;
            let latitude = activity.latitude;
            if (!longitude && !latitude && activity.attraction) {
              longitude = activity.attraction.longitude;
              latitude = activity.attraction.latitude;
            }
            const locationInfo = {
              name: activity.activity || `活动 ${index + 1}`,
              description: activity.location,
              address: activity.location,
              longitude: longitude,
              latitude: latitude,
              transportMode: currentDayData.activities[index + 1]?.transport || null,
              transport: activity.transport || null
            };
            if (locationInfo.longitude && locationInfo.latitude) {
              markers.push(locationInfo);
            }
          }
        });
      }
    }
    return markers;
  };

  useEffect(() => {
    if (itineraryData && itineraryData.dailyPlan) {
      if (selectedDate === 'overview') {
        console.log('切换到 overview');
      } else {
        const dayData = itineraryData.dailyPlan.find(day => `day${day.day}` === selectedDate);
        if (dayData) {
          console.log(`切换到${selectedDate}，dayData:`, JSON.stringify(dayData, null, 2));
        }
      }
    }
  }, [selectedDate, itineraryData]);

  if (!itineraryData) {
    return <div>加载中...</div>;
  }

  // 生成日期标签
  const generateDateTabs = () => {
    if (!itineraryData.dailyPlan) return [];
    return itineraryData.dailyPlan.map(day => ({
      key: `day${day.day}`,
      label: `第${day.day}天`,
      date: day.date
    }));
  };

  const dateTabs = generateDateTabs();

  return (
    <div className="view-itinerary-page">
      <a 
        href="#" 
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '4px',
          textDecoration: 'none',
          color: 'var(--text-primary)',
          fontSize: '1rem',
          fontWeight: '500',
          marginBottom: '12px'
        }}
        onClick={(e) => { e.preventDefault(); navigate(returnInfo.path); }}
      >
        <span style={{ fontSize: '1.2rem' }}>←</span>
        <span>{returnInfo.text}</span>
      </a>

      {/* 行程标题（只读版本，移除编辑功能） */}
      <div className="itinerary-header">
        <h1 className="itinerary-title" style={{ cursor: 'default' }}>
          {itineraryData.title}
        </h1>
        <p style={{ margin: '4px 0 0 0', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          已出行的行程仅可浏览详情╮( •́ω•̀ )╭
        </p>
        <div className="itinerary-meta">
          <div className="meta-item">
            <span>📅</span>
            <span style={{ cursor: 'default', padding: '2px 4px' }}>
              {itineraryData.startDate} - {itineraryData.endDate}
            </span>
          </div>
          <div className="meta-item">
            <span>📍</span>
            <span style={{ cursor: 'default', padding: '2px 4px' }}>
              {itineraryData.destination}
            </span>
          </div>
          <div className="meta-item">
            <span>👥</span>
            <span style={{ cursor: 'default', padding: '2px 4px' }}>
              {itineraryData.participants ? `${itineraryData.participants.length}人出行` : '1人出行'}
            </span>
          </div>
          <div className="meta-item">
            <span>💰</span>
            <span style={{ cursor: 'default', padding: '2px 4px' }}>
              {itineraryData.budget || '预算待定'}
            </span>
          </div>
        </div>
      </div>

      {/* 主要内容区域 */}
      <div className="itinerary-content">
        {/* 左侧日期视图 */}
        <div className="date-panel">
          <div className="date-panel-header">
            <div className="date-panel-title">行程安排</div>
            <div className="date-range">{itineraryData.duration}</div>
          </div>

          {/* 日期标签栏 */}
          <div className="date-tabs">
            <div 
              className={`date-tab overview ${selectedDate === 'overview' ? 'active' : ''}`}
              onClick={() => switchDateTab('overview')}
            >
              总览
            </div>
            {dateTabs.map(tab => (
              <div 
                key={tab.key}
                className={`date-tab ${selectedDate === tab.key ? 'active' : ''}`}
                onClick={() => switchDateTab(tab.key)}
              >
                {tab.label}
              </div>
            ))}
          </div>

          {/* 日期内容区域 */}
          <div className="date-content">
            {/* 总览内容 */}
            {selectedDate === 'overview' && (
              <div className="date-content-item active">
                {itineraryData.dailyPlan && itineraryData.dailyPlan.map(day => (
                  <div key={day.day}>
                    {renderAttractionItem({
                      icon: getActivityIcon({ location: day.title, activity: day.title }),
                      type: 'location',
                      name: `第${day.day}天: ${day.title}`,
                      desc: day.city,
                      time: day.date
                    })}
                  </div>
                ))}
                {!itineraryData.dailyPlan && (
                  <>
                    {renderAttractionItem({
                      icon: '📍',
                      type: 'location',
                      name: itineraryData.title,
                      desc: itineraryData.description,
                      time: `${itineraryData.startDate} - ${itineraryData.endDate}`
                    })}
                  </>
                )}
              </div>
            )}

            {/* 具体日期内容 */}
            {itineraryData.dailyPlan && dateTabs.find(tab => tab.key === selectedDate) && (

              <div className="date-content-item active">
                {(() => {
                  const dayData = itineraryData.dailyPlan.find(day => `day${day.day}` === selectedDate);
                  if (!dayData) return null;
                  
                  return (
                    <>
                      <div style={{ marginBottom: '20px', padding: '12px', background: 'var(--bg-container)', borderRadius: '8px' }}>
                        <h3 style={{ margin: '0 0 8px 0', color: 'var(--primary-color)', cursor: 'default' }}>
                          {dayData.title}
                        </h3>
                        <p style={{ margin: '0', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                          {dayData.city} • {dayData.date}
                        </p>
                      </div>
                      {dayData.activities && dayData.activities.map((activity, index) => (
                        <div key={activity.id || index}>
                          {renderAttractionItem({
                            icon: getActivityIcon(activity),
                            type: getActivityType(activity),
                            name: activity.activity,
                            desc: activity.location,
                            time: `${activity.time}`
                          })}
                          {index < dayData.activities.length - 1 && 
                            renderTransportBar(
                              activity.location, 
                              dayData.activities[index + 1].location,
                              dayData.activities[index + 1].transport
                            )
                          }
                        </div>
                      ))}
                      {!dayData.activities || dayData.activities.length === 0 && (
                        <div className="empty-day-state">
                        <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-secondary)' }}>
                          <div style={{ fontSize: '2rem', marginBottom: '12px' }}>📝</div>
                          <h4 style={{ margin: '0 0 8px 0', color: 'var(--text-primary)' }}>这一天还没有安排</h4>
                        </div>
                      </div>
                      )}
                    </>
                  );
                })()}
              </div>
            )}
          </div>
        </div>

        {/* 右侧地图视图 */}
        <div className="map-panel">
          <div className="map-panel-header">
            <div className="map-panel-title">地图视图</div>
          </div>
          <div className="map-container">
            <RouteMap ref={mapRef} markers={getCurrentDayMarkers()} isOverview={selectedDate === 'overview'} />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ViewItinerary; 