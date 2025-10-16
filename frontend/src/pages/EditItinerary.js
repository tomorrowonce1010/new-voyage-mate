import React, { useState, useEffect, lazy, useMemo, useRef, useCallback } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';
import './EditItinerary.css';
import RouteMap from "../components/RouteMap";

// 1. 路径规划函数
async function getRouteTime({ from, to, mode }) {
  return new Promise((resolve, reject) => {
    try {
    if (mode === '驾车') {
      window.AMap.plugin('AMap.Driving', function () {
          try {
        var driving = new window.AMap.Driving({ policy: 0 }); // 0:速度优先
        driving.search(from, to, function (status, result) {
          // 官方推荐写法
          console.log('[高德驾车路径规划] status:', status, 'result:', result);
          let duration = null;
          if (status === 'complete' && result && result.routes && result.routes.length > 0) {
            duration = result.routes[0].duration;
          }
          resolve(duration);
        });
          } catch (error) {
            console.warn('驾车路径规划服务初始化失败:', error);
            resolve(null);
          }
      });
    } else {
      window.AMap.plugin([
        'AMap.Transfer',
        'AMap.Walking',
        'AMap.Riding'
      ], function() {
          try {
        let planner;
        if (mode === '公交' || mode === '公共交通') {
          planner = new window.AMap.Transfer({ city: '上海' });
        } else if (mode === '步行') {
          planner = new window.AMap.Walking();
        } else if (mode === '骑行' || mode === '骑乘') {
          planner = new window.AMap.Riding();
        } else {
          resolve(null);
          return;
        }
        planner.search(
          from, to,
          function(status, result) {
            let duration = null;
            if (status === 'complete') {
              if (result.routes && result.routes.length > 0) {
                duration = result.routes[0].duration;
              } else if (result.plans && result.plans.length > 0) {
                duration = result.plans[0].duration;
              }
            }
            // 只打印计算结果
            console.log(`[高德路径规划] 方式:${mode} 起点:${from} 终点:${to} 推荐用时(秒):`, duration);
            resolve(duration); // 秒
          }
        );
          } catch (error) {
            console.warn('路径规划服务初始化失败:', error);
            resolve(null);
          }
      });
      }
    } catch (error) {
      console.warn('路径规划插件加载失败:', error);
      resolve(null);
    }
  });
}

// 工具函数：秒转小时分钟
function formatDurationToHourMin(timeStr) {
  // timeStr 可能是"1小时23分钟"或"45分钟"或"3600"（秒）
  if (!timeStr) return '';
  if (/\d+小时\d+分钟/.test(timeStr) || /\d+分钟/.test(timeStr)) return timeStr;
  let sec = parseInt(timeStr, 10);
  if (isNaN(sec)) return timeStr;
  const h = Math.floor(sec / 3600);
  const m = Math.round((sec % 3600) / 60);
  if (h > 0) return `${h}小时${m}分钟`;
  return `${m}分钟`;
}

// useMarkers hook
function useMarkers(itineraryData, selectedDate) {
  return useMemo(() => {
    const markers = [];
    if (!itineraryData || !itineraryData.dailyPlan) return markers;
    if (selectedDate === 'overview') {
      itineraryData.dailyPlan.forEach((dayData, dayIndex) => {
        if (dayData.activities && dayData.activities.length > 0) {
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
  }, [itineraryData, selectedDate]);
}

const EditItinerary = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams(); // 使用路径参数获取行程ID
  const [selectedDate, setSelectedDate] = useState('overview');
  const [settingsDropdowns, setSettingsDropdowns] = useState({});
  const [errorModalVisible, setErrorModalVisible] = useState(false);
  const [errorModal, setErrorModal] = useState({ message: '' });

  // 添加地图引用
  const mapRef = useRef(null);

  const [transportModal, setTransportModal] = useState({ 
    show: false, 
    element: null,
    activityId: null,
    dayIndex: -1,
    activityIndex: -1,
    customTransport: ''
  });
  const [currentTransportElement, setCurrentTransportElement] = useState(null);
  const [addAttractionModal, setAddAttractionModal] = useState({ 
    show: false, 
    dateId: '', 
    destination: '',
    customTitle: '',
    startTime: '', 
    endTime: '',
    insertIndex: -1,
    nextActivityId: null,
    insertBeforeAttraction: null,
    earliestStart: '',
    latestEnd: ''
  });
  const [editActivityModal, setEditActivityModal] = useState({
    show: false,
    activityId: null,
    dayIndex: -1,
    activityIndex: -1,
    currentActivity: null,
    title: '',
    startTime: '',
    endTime: '',
    notes: '',
    earliestStart: '',
    latestEnd: ''
  });
  const [attractionSearch, setAttractionSearch] = useState({
    query: '',
    results: [],
    selectedAttraction: null,
    showDropdown: false,
    loading: false,
    limit: 8
  });
  const [editingDayTitle, setEditingDayTitle] = useState({ show: false, dayIndex: -1, title: '' });
  const [editingTime, setEditingTime] = useState({ show: false, dayIndex: -1, activityIndex: -1, startTime: '', endTime: '' });
  const [editingBudget, setEditingBudget] = useState({ show: false, budget: '' });
  const [editingTravelerCount, setEditingTravelerCount] = useState({ show: false, count: 1 });
  const [editingTitle, setEditingTitle] = useState({ show: false, title: '' });
  // 开始日期选择弹窗状态
  const [startDateModal, setStartDateModal] = useState({ show: false, date: '' });
  const [isNewTrip, setIsNewTrip] = useState(false);
  const [itineraryData, setItineraryData] = useState(null);

  // 删除景点确认弹窗状态
  const [deleteConfirmModal, setDeleteConfirmModal] = useState({
    show: false,
    dayIndex: -1,
    activityIndex: -1,
    attractionName: ''
  });

  // 新增高德地图搜索相关状态
  const [showAMapModal, setShowAMapModal] = useState(false);
  const [amapResults, setAmapResults] = useState([]);
  const [amapLoading, setAmapLoading] = useState(false);

  // 2. 推荐用时 state
  const [routeTimes, setRouteTimes] = useState({}); // key: `${dayIndex}_${activityIndex}`, value: 秒

  // 1. 新增 state 保存当前路线详情
  const [routeDetail, setRouteDetail] = useState(null);
  const [showRouteDetailModal, setShowRouteDetailModal] = useState(false);

  // 在组件顶层直接调用hook
  const markers = useMarkers(itineraryData, selectedDate);
  
  // 重新初始化地图的函数
  const reinitializeMap = useCallback(() => {
    if (mapRef.current && mapRef.current.reinitializeMap) {
      console.log('触发地图重新初始化');
      mapRef.current.reinitializeMap();
    }
  }, []);

  // 初始化行程数据
  useEffect(() => {
    if (id) {
      // 从后端API获取行程数据
      fetchItineraryData(id);
        } else {
      navigate('/manage');
    }
  }, [id, navigate]);

  // 添加全局错误处理器
  useEffect(() => {
    const handleError = (event) => {
      // 阻止默认的错误弹窗
      event.preventDefault();
      
      // 过滤掉地图相关的已知错误
      const error = event.error;
      if (error) {
        const errorMessage = error.message || error.toString();
        
        // 过滤掉已知的地图相关错误
        if (errorMessage.includes('Cannot read properties of undefined (reading \'add\')') ||
            errorMessage.includes('Component unmounted or map destroyed') ||
            errorMessage.includes('Invalid map instance') ||
            errorMessage.includes('Script error.')) {
          // 这些是已知的地图生命周期错误，不需要显示
          return;
        }
        
        // 过滤掉高德地图API相关的错误
        if (errorMessage.includes('AMap') || errorMessage.includes('maps?callback=')) {
          console.warn('地图API错误:', errorMessage);
          return;
        }
        
        // 过滤掉React开发模式的错误
        if (errorMessage.includes('Script error.') || errorMessage.includes('handleError')) {
          console.warn('React开发模式错误:', errorMessage);
          return;
        }
        
        console.error('全局错误:', error);
        console.error('错误堆栈:', error.stack);
      }
    };

    const handleUnhandledRejection = (event) => {
      // 阻止默认的Promise拒绝弹窗
      event.preventDefault();
      
      const reason = event.reason;
      if (reason) {
        const errorMessage = reason.message || reason.toString();
        
        // 过滤掉已知的地图相关错误
        if (errorMessage.includes('Component unmounted or map destroyed') ||
            errorMessage.includes('Invalid map instance') ||
            errorMessage.includes('Script error.')) {
          return;
        }
        
        console.error('未处理的Promise拒绝:', reason);
      }
    };

    // 阻止React开发模式的错误弹窗
    const originalConsoleError = console.error;
    console.error = (...args) => {
      const errorMessage = args.join(' ');
      if (errorMessage.includes('Script error.') || 
          errorMessage.includes('handleError') ||
          errorMessage.includes('Cannot read properties of undefined')) {
        console.warn('已过滤的错误:', errorMessage);
        return;
      }
      originalConsoleError.apply(console, args);
    };

    window.addEventListener('error', handleError, true);
    window.addEventListener('unhandledrejection', handleUnhandledRejection);

    return () => {
      window.removeEventListener('error', handleError, true);
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      console.error = originalConsoleError;
    };
  }, []);

  // 页面离开时设置编辑状态为完成
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (itineraryData && id) {
        // 使用sendBeacon确保请求能够发送
        // sendBeacon无法设置credentials，改用同步fetch
        try {
          const xhr = new XMLHttpRequest();
          xhr.open('PUT', `/api/itineraries/${id}/edit-complete`, false); // 同步请求
          xhr.withCredentials = true;
          xhr.setRequestHeader('Content-Type', 'application/json');
          xhr.send(JSON.stringify({}));
        } catch (error) {
          console.error('同步设置编辑完成状态失败:', error);
        }
      }
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden' && itineraryData && id) {
        setEditComplete();
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      // 组件卸载时也设置完成状态
      if (itineraryData && id) {
        setEditComplete();
      }
    };
  }, [itineraryData, id]);

  // 监听中转站添加景点事件
  useEffect(() => {
    const handleAddAttractionFromTransferStation = (event) => {
      const { attraction } = event.detail;
      console.log('收到来自中转站的景点:', attraction);
      
      // 自动打开添加景点模态框并填入景点信息
      setAddAttractionModal({
        show: true,
        dateId: selectedDate === 'overview' ? 'day1' : selectedDate,
        destination: attraction.destinationName || '',
        location: attraction.name || '',
        customTitle: '',
        startTime: '',
        endTime: '',
        insertIndex: -1,
        nextActivityId: null,
        insertBeforeAttraction: null,
        earliestStart: '',
        latestEnd: '',
        preSelectedAttraction: attraction // 添加预选景点
      });
      
      // 同时更新景点搜索状态，让活动地点输入框显示景点名称
      setAttractionSearch(prev => ({
        ...prev,
        query: attraction.name || '',
        selectedAttraction: attraction,
        showDropdown: false,
        loading: false
      }));
    };

    // 检查是否有待添加的景点（从sessionStorage）
    const pendingAttraction = sessionStorage.getItem('pendingAttractionToAdd');
    if (pendingAttraction) {
      try {
        const attraction = JSON.parse(pendingAttraction);
        console.log('发现待添加的景点:', attraction);
        
        // 清除sessionStorage中的待添加景点
        sessionStorage.removeItem('pendingAttractionToAdd');
        
        // 延迟执行，确保组件完全加载
        setTimeout(() => {
          handleAddAttractionFromTransferStation({ detail: { attraction } });
        }, 1000);
      } catch (error) {
        console.error('解析待添加景点失败:', error);
        sessionStorage.removeItem('pendingAttractionToAdd');
      }
    }

    window.addEventListener('addAttractionFromTransferStation', handleAddAttractionFromTransferStation);

    return () => {
      window.removeEventListener('addAttractionFromTransferStation', handleAddAttractionFromTransferStation);
    };
  }, [selectedDate]);

  // 设置编辑完成状态
  const setEditComplete = async () => {
    if (!id) return;
    
    try {
      const response = await fetch(`/api/itineraries/${id}/edit-complete`, {
        method: 'PUT',
        credentials: 'include'
      });
      
      if (response.status === 401) {
        // 用户登录状态可能已过期，这是正常情况，不需要报错
        console.log('用户登录状态可能已过期，跳过编辑状态设置');
        return;
      }
      
      if (!response.ok) {
        console.warn('设置编辑完成状态失败:', response.status);
      }
    } catch (error) {
      console.error('设置编辑完成状态失败:', error);
    }
  };

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
      
      // 判断是否为新创建的行程（没有日程数据或日程为空）
      const isNewlyCreated = !itinerary.itineraryDays || itinerary.itineraryDays.length === 0;
      
      setIsNewTrip(isNewlyCreated);
      
      let dailyPlan = [];
      const overallDestSet = new Set();
      if (!isNewlyCreated && itinerary.itineraryDays) {
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
          
          // 统计当天访问的目的地
          const dayDestSet = new Set();
          orderedActivities.forEach(act => {
            if (act.attraction && act.attraction.destination && act.attraction.destination.name) {
              dayDestSet.add(act.attraction.destination.name);
            }
          });
          const dayCity = dayDestSet.size > 0 ? Array.from(dayDestSet).join('、') : '待规划';
          // 将当天目的地加入整体统计
          dayDestSet.forEach(d => overallDestSet.add(d));

          return {
            dayId: day.id,
            day: day.dayNumber,
            date: day.date,
            city: dayCity,
            title: day.title,
            activities: orderedActivities.map(activity => ({
              id: activity.id,
              location: activity.attraction?.name || activity.title,
              activity: activity.title,
              time: activity.startTime && activity.endTime ? 
                `${activity.startTime.substring(0,5)}-${activity.endTime.substring(0,5)}` : 
                '时间待定',
              transport: activity.transportMode || '步行',
              notes: activity.attractionNotes || '',
              nextId: activity.nextId,
              prevId: activity.prevId,
              // 添加经纬度信息
              longitude: activity.attraction?.longitude || activity.longitude,
              latitude: activity.attraction?.latitude || activity.latitude,
              // 保留完整的attraction对象以便后续使用z
              attraction: activity.attraction
            }))
          };
        });
      } else {
        dailyPlan = generateEmptyDailyPlan(itinerary.startDate, itinerary.endDate);
      }
      
      // 构建前端需要的数据格式
      const formattedData = {
        id: itinerary.id,
        title: itinerary.title,
        destination: overallDestSet.size > 0 ? Array.from(overallDestSet).join('、') : '待规划目的地',
        startDate: itinerary.startDate,
        endDate: itinerary.endDate,
        duration: calculateDuration(itinerary.startDate, itinerary.endDate),
        description: '开始规划您的旅程吧！',
        participants: Array(itinerary.travelerCount || 1).fill().map((_, i) => i === 0 ? '我' : `同行者${i}`),
        tags: ['待规划'],
        budget: itinerary.budget ? `￥${itinerary.budget}` : '预算待定',
        status: itinerary.travelStatus === '待出行' ? 'upcoming' : 'completed',
        visibility: itinerary.permissionStatus === '私人' ? 'private' : 'public',
        dailyPlan: dailyPlan
      };

      // 添加调试日志
      console.log('【EditItinerary】从后端获取的行程数据:', {
        id: itinerary.id,
        title: itinerary.title,
        travelerCount: itinerary.travelerCount,
        participants: formattedData.participants
      });

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

  // 生成空的日程计划
  const generateEmptyDailyPlan = (startDate, endDate) => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const plan = [];
    let currentDate = new Date(start);
    let day = 1;

    while (currentDate <= end) {
      plan.push({
        day: day,
        date: currentDate.toISOString().split('T')[0],
        city: '待规划',
        title: `第${day}天 - 待规划`,
        activities: []
      });
      currentDate.setDate(currentDate.getDate() + 1);
      day++;
    }
    return plan;
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

  // 生成日期内容映射
  const dateContents = React.useMemo(() => {
    if (!itineraryData || !itineraryData.dailyPlan) return {};
    
    const contents = {};
    itineraryData.dailyPlan.forEach(dayPlan => {
      const dateKey = `${dayPlan.day}.${new Date(dayPlan.date).getDate()}`;
      contents[dateKey] = dayPlan.activities.map(activity => ({
        icon: getActivityIcon(activity),
        type: getActivityType(activity),
        name: activity.location || activity.activity,
        desc: activity.activity || activity.location,
        time: activity.time || '时间待定'
      }));
    });
    return contents;
  }, [itineraryData]);

  // 关闭所有下拉菜单
  const closeAllDropdowns = () => {
    setSettingsDropdowns({});
    // 移除所有dropdown-open类
    document.querySelectorAll('.attraction-item.dropdown-open').forEach(item => {
      item.classList.remove('dropdown-open');
    });
  };

  const closeErrorModal = () => {
    setErrorModalVisible(false);
  };

  // 切换日期标签
  const switchDateTab = (dateId) => {
    setSelectedDate(dateId);
    closeAllDropdowns();
  };

  // 切换设置下拉菜单
  const toggleSettingsDropdown = (attractionName, event) => {
    event.stopPropagation();
    
    // 先关闭所有下拉菜单并移除所有dropdown-open类
    document.querySelectorAll('.attraction-item.dropdown-open').forEach(item => {
      item.classList.remove('dropdown-open');
    });
    
    // 如果当前下拉菜单已经打开，则关闭它
    if (settingsDropdowns[attractionName]) {
      setSettingsDropdowns(prev => ({
        ...prev,
        [attractionName]: false
      }));
      return;
    }
    
    // 关闭其他所有下拉菜单
    setSettingsDropdowns({});
    
    // 为当前景点项目添加高z-index类
    const attractionItem = event.currentTarget.closest('.attraction-item');
    if (attractionItem) {
      attractionItem.classList.add('dropdown-open');
    }
    
    // 打开当前下拉菜单
    setSettingsDropdowns(prev => ({
      ...prev,
      [attractionName]: true
    }));
  };


  // 添加新景点
  const addNewAttraction = (attractionName, dayIndex, activityIndex) => {
    const dayData = itineraryData.dailyPlan[dayIndex];
    const prevEnd = getPrevEndTime(dayIndex, activityIndex - 1);
    const nextStart = getNextStartTime(dayIndex, activityIndex);
    console.log("prevEnd", prevEnd);
    console.log("nextStart", nextStart);
    
    setAddAttractionModal({ 
      show: true, 
      dateId: `day${dayData.day}`, 
      destination: '',
      customTitle: '',
      startTime: '', 
      endTime: '',
      insertIndex: activityIndex,
      nextActivityId: dayData.activities[activityIndex]?.id,
      insertBeforeAttraction: attractionName,
      earliestStart: prevEnd,
      latestEnd: nextStart
    });
    closeAllDropdowns();
  };

  // 删除景点
  const deleteAttraction = (attractionName, dayIndex = -1, activityIndex = -1) => {
    // 打开自定义确认弹窗
    setDeleteConfirmModal({
      show: true,
      dayIndex,
      activityIndex,
      attractionName
    });
    closeAllDropdowns();
  };

  const handleCancelDeleteAttraction = () => {
    setDeleteConfirmModal({ show: false, dayIndex: -1, activityIndex: -1, attractionName: '' });
  };

  const handleConfirmDeleteAttraction = async () => {
    const { dayIndex, activityIndex, attractionName } = deleteConfirmModal;
    // 待规划项目
    if (dayIndex === -1) {
      alert('请选择要删除的活动');
      return;
    } else {
      const activity = itineraryData.dailyPlan[dayIndex]?.activities[activityIndex];
      if (activity && activity.id) {
        try {
          const response = await fetch(`/api/activities/${activity.id}`, {
            method: 'DELETE',
            credentials: 'include'
          });

          if (response.ok) {
            const updatedItineraryData = { ...itineraryData };
            updatedItineraryData.dailyPlan[dayIndex].activities.splice(activityIndex, 1);
            setItineraryData(updatedItineraryData);
            setTimeout(() => {
              reinitializeMap();
            }, 200);
          } else {
            throw new Error('删除失败');
          }
        } catch (error) {
          console.error('删除景点失败:', error);
          alert('删除景点失败，请重试');
        }
      } else {
        const updatedItineraryData = { ...itineraryData };
        updatedItineraryData.dailyPlan[dayIndex].activities.splice(activityIndex, 1);
        setItineraryData(updatedItineraryData);
        setTimeout(() => {
          reinitializeMap();
        }, 200);
      }
    }
    setDeleteConfirmModal({ show: false, dayIndex: -1, activityIndex: -1, attractionName: '' });
  };

  // 打开交通方式选择弹窗
  const openTransportModal = (event, activityId = null, dayIndex = -1, activityIndex = -1) => {
    event.stopPropagation();
    const element = event.currentTarget;
    setCurrentTransportElement(element);
    setTransportModal({ 
      show: true, 
      element,
      activityId,
      dayIndex,
      activityIndex,
      customTransport: ''
    });
  };

  // 关闭交通方式选择弹窗
  const closeTransportModal = () => {
    setTransportModal({ 
      show: false, 
      element: null,
      activityId: null,
      dayIndex: -1,
      activityIndex: -1,
      customTransport: ''
    });
    setCurrentTransportElement(null);
  };

  // 交通方式映射表，支持英文key
  const transportInfo = {
    walking: { name: '步行', icon: '🚶', class: 'walking', mode: '步行' },
    cycling: { name: '骑行', icon: '🚴', class: 'cycling', mode: '骑行' },
    driving: { name: '驾车', icon: '🚗', class: 'driving', mode: '驾车' },
    public: { name: '公共交通', icon: '🚇', class: 'public', mode: '公共交通' }
  };

  // 1. useEffect: 弹窗打开时批量计算四种方式的用时
  useEffect(() => {
    if (transportModal.show && itineraryData && transportModal.dayIndex >= 0 && transportModal.activityIndex >= 0) {
      const dayData = itineraryData.dailyPlan[transportModal.dayIndex];
      const activities = dayData.activities;
      const from = activities[transportModal.activityIndex];
      const to = activities[transportModal.activityIndex + 1];
      if (from && to && from.longitude && from.latitude && to.longitude && to.latitude) {
        ['walking','cycling','public','driving'].forEach(type => {
          const info = transportInfo[type];
          const key = `${transportModal.dayIndex}_${transportModal.activityIndex}_${type}`;
          if (!routeTimes[key]) {
            // 只缓存用时，点击后再查详情
            getRouteTime({
              from: [from.longitude, from.latitude],
              to: [to.longitude, to.latitude],
              mode: info.mode
            }).then(duration => {
              setRouteTimes(prev => ({
                ...prev,
                [key]: duration
              }));
            });
          }
        });
      }
    }
    // eslint-disable-next-line
  }, [transportModal.show, transportModal.dayIndex, transportModal.activityIndex]);

  // 2. 点击选项后弹窗显示详情
  const selectTransport = async (type) => {
    // 临时禁用错误弹窗
    const originalErrorHandler = window.onerror;
    const originalUnhandledRejectionHandler = window.onunhandledrejection;
    
    window.onerror = () => true; // 阻止错误弹窗
    window.onunhandledrejection = (event) => {
      event.preventDefault();
      return true;
    };
    
    try {
    const info = transportInfo[type];
    if (!info) return;
      
      // 更新UI显示
    if (info && currentTransportElement) {
      currentTransportElement.className = `transport-method ${info.class}`;
      currentTransportElement.innerHTML = `
        <span>${info.icon}</span>
        <span>${info.name}</span>
      `;
    }
      
      // 计算路线详情（仅在需要时）
    if (transportModal.dayIndex >= 0 && transportModal.activityIndex >= 0 && itineraryData) {
      const dayData = itineraryData.dailyPlan[transportModal.dayIndex];
      const activities = dayData.activities;
      const from = activities[transportModal.activityIndex];
      const to = activities[transportModal.activityIndex + 1];
        
      if (from && to && from.longitude && from.latitude && to.longitude && to.latitude) {
          try {
        if (type === 'driving') {
          window.AMap.plugin('AMap.Driving', function () {
                try {
            var driving = new window.AMap.Driving({ policy: 0 });
            driving.search([from.longitude, from.latitude], [to.longitude, to.latitude], function (status, result) {
              if (status === 'complete' && result && result.routes && result.routes.length > 0) {
                const route = result.routes[0];
                setRouteDetail({
                  time: route.time,
                  steps: route.steps,
                  mode: info.name,
                  fromName: from.address || from.location,
                  toName: to.address || to.location
                });
                setShowRouteDetailModal(true);
              } else {
                setRouteDetail(null);
                setShowRouteDetailModal(false);
              }
            });
                } catch (error) {
                  console.warn('驾车路径规划失败:', error);
                  setRouteDetail(null);
                  setShowRouteDetailModal(false);
                }
          });
        } else if (type === 'walking') {
          window.AMap.plugin('AMap.Walking', function () {
                try {
            var walking = new window.AMap.Walking();
            walking.search([from.longitude, from.latitude], [to.longitude, to.latitude], function (status, result) {
              if (status === 'complete' && result && result.routes && result.routes.length > 0) {
                const route = result.routes[0];
                setRouteDetail({
                  time: route.time,
                  steps: route.steps,
                  mode: info.name,
                  fromName: from.address || from.location,
                  toName: to.address || to.location
                });
                setShowRouteDetailModal(true);
              } else {
                setRouteDetail(null);
                setShowRouteDetailModal(false);
              }
            });
                } catch (error) {
                  console.warn('步行路径规划失败:', error);
                  setRouteDetail(null);
                  setShowRouteDetailModal(false);
                }
          });
        } else if (type === 'cycling') {
          window.AMap.plugin('AMap.Riding', function () {
                try {
            var riding = new window.AMap.Riding();
            riding.search([from.longitude, from.latitude], [to.longitude, to.latitude], function (status, result) {
              if (status === 'complete' && result && result.routes && result.routes.length > 0) {
                const route = result.routes[0];
                setRouteDetail({
                  time: route.time,
                  steps: route.rides, // 修正为rides
                  mode: info.name,
                  fromName: from.address || from.location,
                  toName: to.address || to.location
                });
                setShowRouteDetailModal(true);
              } else {
                setRouteDetail(null);
                setShowRouteDetailModal(false);
              }
            });
                } catch (error) {
                  console.warn('骑行路径规划失败:', error);
                  setRouteDetail(null);
                  setShowRouteDetailModal(false);
                }
          });
        } else if (type === 'public') {
          window.AMap.plugin('AMap.Transfer', function () {
                try {
            var transfer = new window.AMap.Transfer({ city: '上海' });
            transfer.search([from.longitude, from.latitude], [to.longitude, to.latitude], function (status, result) {
              if (status === 'complete' && result && result.plans && result.plans.length > 0) {
                const plan = result.plans[0];
                setRouteDetail({
                  time: plan.time,
                  steps: plan.segments,
                  mode: info.name,
                  isPublic: true,
                  fromName: from.address || from.location,
                  toName: to.address || to.location
                });
                setShowRouteDetailModal(true);
              } else {
                setRouteDetail(null);
                setShowRouteDetailModal(false);
              }
            });
                } catch (error) {
                  console.warn('公共交通路径规划失败:', error);
                  setRouteDetail(null);
                  setShowRouteDetailModal(false);
                }
          });
        }
          } catch (error) {
            console.warn('路径规划服务初始化失败:', error);
            setRouteDetail(null);
            setShowRouteDetailModal(false);
      }
    }
      }
      
      // 更新后端数据
    if (transportModal.activityId) {
        try {
      // 先更新后端
      await updateTransportMode(transportModal.activityId, info.name, transportModal.dayIndex, transportModal.activityIndex);
          
          // 强制拉取最新activities，保证顺序和数据绝对一致
      const dayIndex = transportModal.dayIndex;
      const itineraryDayId = itineraryData.dailyPlan[dayIndex]?.dayId;
      if (itineraryDayId) {
            const response = await fetch(`/api/activities/day/${itineraryDayId}`, {
          method: 'GET',
          credentials: 'include'
            });
            
            if (response.ok) {
              const activities = await response.json();
            const updatedItineraryData = { ...itineraryData };
            updatedItineraryData.dailyPlan[dayIndex].activities = activities.map(activity => ({
              id: activity.id,
              location: activity.attraction?.name || activity.title,
              activity: activity.title,
              time: activity.startTime && activity.endTime ? 
                `${activity.startTime.substring(0,5)}-${activity.endTime.substring(0,5)}` : 
                activity.startTime ? activity.startTime.substring(0,5) :
                activity.endTime ? `至 ${activity.endTime.substring(0,5)}` : 
                '时间待定',
              transport: activity.transportMode || '步行',
              notes: activity.attractionNotes || '',
              nextId: activity.nextId,
              prevId: activity.prevId,
              longitude: activity.attraction?.longitude || activity.longitude,
              latitude: activity.attraction?.latitude || activity.latitude,
              attraction: activity.attraction
            }));
            setItineraryData(updatedItineraryData);
              
              // 交通方式切换完成后，重新初始化地图
    setTimeout(() => {
                reinitializeMap();
              }, 200);
            } else {
              console.warn('获取活动列表失败:', response.status);
            }
          }
        } catch (error) {
          console.warn('更新交通方式失败:', error);
          // 不显示alert，避免用户体验问题
        }
      }
      
    closeTransportModal();
    } finally {
      // 恢复原始错误处理器
      window.onerror = originalErrorHandler;
      window.onunhandledrejection = originalUnhandledRejectionHandler;
    }
  };

  // 更新交通方式到后端
  const updateTransportMode = (activityId, transportMode, dayIndex, activityIndex) => {
    return new Promise(async (resolve, reject) => {
      try {
        const response = await fetch(`/api/activities/${activityId}/transport`, {
          method: 'PUT',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ transportMode: transportMode })
        });
        if (response.ok) {
          // 更新本地状态
          const updatedItineraryData = { ...itineraryData };
          if (updatedItineraryData.dailyPlan[dayIndex] && 
              updatedItineraryData.dailyPlan[dayIndex].activities[activityIndex]) {
            updatedItineraryData.dailyPlan[dayIndex].activities[activityIndex].transport = transportMode;
            setItineraryData(updatedItineraryData);
          }
          console.log(`交通方式已更新为: ${transportMode}`);
          resolve(updatedItineraryData);
        } else {
          reject('更新失败');
        }
      } catch (error) {
        console.error('更新交通方式失败:', error);
        alert('更新交通方式失败，请重试');
        reject(error);
      }
    });
  };

  // 编辑标题
  const editTitle = () => {
    setEditingTitle({ show: true, title: itineraryData.title });
  };

  // 关闭编辑标题弹窗
  const closeEditTitle = () => {
    setEditingTitle({ show: false, title: '' });
  };

  // 确认修改标题
  const confirmEditTitle = async () => {
    if (!editingTitle.title.trim()) {
      return;
    }

    try {
      const response = await fetch(`/api/itineraries/${id}/basic`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: editingTitle.title })
      });

      if (response.ok) {
        setItineraryData(prev => ({ ...prev, title: editingTitle.title }));
        closeEditTitle();
      } else {
        throw new Error('更新失败');
      }
    } catch (error) {
      console.error('更新行程标题失败:', error);
      alert('更新行程标题失败，请重试');
    }
  };

  // 编辑预算
  const editBudget = () => {
    const currentBudget = itineraryData.budget ? itineraryData.budget.replace('￥', '') : '';
    setEditingBudget({ show: true, budget: currentBudget });
  };

  // 关闭编辑预算弹窗
  const closeEditBudget = () => {
    setEditingBudget({ show: false, budget: '' });
  };

  // 确认修改预算
  const confirmEditBudget = async () => {
    const budgetValue = parseFloat(editingBudget.budget);
    if (isNaN(budgetValue) || budgetValue < 0) {
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: '请输入有效的预算金额'
      });
      return;
    }

    try {
      const response = await fetch(`/api/itineraries/${id}/basic`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ budget: budgetValue })
      });

      if (response.ok) {
        setItineraryData(prev => ({ ...prev, budget: `￥${budgetValue}` }));
        closeEditBudget();
      } else {
        throw new Error('更新失败');
      }
    } catch (error) {
      console.error('更新预算失败:', error);
      alert('更新预算失败，请重试');
    }
  };

  // 编辑出行人数
  const editTravelerCount = () => {
    const currentCount = itineraryData.participants ? itineraryData.participants.length : 1;
    setEditingTravelerCount({ show: true, count: currentCount });
  };

  // 关闭编辑出行人数弹窗
  const closeEditTravelerCount = () => {
    setEditingTravelerCount({ show: false, count: 1 });
  };

  // 确认修改出行人数
  const confirmEditTravelerCount = async () => {
    const count = parseInt(editingTravelerCount.count);
    if (isNaN(count) || count < 1) {
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: '请输入有效的出行人数（至少1人）'
      });
      return;
    }

    try {
      const response = await fetch(`/api/itineraries/${id}/basic`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ travelerCount: count })
      });

      if (response.ok) {
        // 更新participants数组
        const participants = Array(count).fill().map((_, i) => i === 0 ? '我' : `同行者${i}`);
        setItineraryData(prev => ({ ...prev, participants }));
        closeEditTravelerCount();
      } else {
        throw new Error('更新失败');
      }
    } catch (error) {
      console.error('更新出行人数失败:', error);
      alert('更新出行人数失败，请重试');
    }
  };

  // 添加景点/活动
  const addAttraction = (dateId, insertIndex = -1) => {
    let earliest = '';
    let latest = '';
    if (dateId.startsWith('day')) {
      const dayNumber = parseInt(dateId.replace('day',''));
      const dIdx = dayNumber - 1;
      const acts = itineraryData.dailyPlan[dIdx]?.activities || [];
      if (acts.length>0) {
        earliest = getPrevEndTime(dIdx, acts.length-1);
        latest = getNextStartTime(dIdx, acts.length);
      }
    }
    setAddAttractionModal({ 
      show: true, 
      dateId, 
      destination: '', 
      startTime: '', 
      endTime: '',
      insertIndex,
      nextActivityId: null,
      insertBeforeAttraction: null,
      customTitle: '',
      earliestStart: earliest,
      latestEnd: latest
    });
  };

  // 编辑活动
  const editActivity = (dayIndex, activityIndex) => {
    if (!itineraryData || !itineraryData.dailyPlan[dayIndex] || !itineraryData.dailyPlan[dayIndex].activities[activityIndex]) {
      return;
    }
    
    const activity = itineraryData.dailyPlan[dayIndex].activities[activityIndex];
    
    // 解析时间
    let startTime = '';
    let endTime = '';
    if (activity.time && activity.time !== '时间待定') {
      const timeMatch = activity.time.match(/(\d{2}:\d{2})-(\d{2}:\d{2})/);
      if (timeMatch) {
        startTime = timeMatch[1];
        endTime = timeMatch[2];
      }
    }
    
    setEditActivityModal({
      show: true,
      activityId: activity.id,
      dayIndex,
      activityIndex,
      currentActivity: activity,
      title: activity.activity,
      startTime,
      endTime,
      notes: activity.notes || '',
      earliestStart: getPrevEndTime(dayIndex, activityIndex - 1),
      latestEnd: getNextStartTime(dayIndex, activityIndex + 1)
    });
    
    // 初始化搜索框
    setAttractionSearch({
      query: activity.location || '',
      results: [],
      selectedAttraction: null,
      showDropdown: false,
      loading: false,
      limit: 8
    });
  };

  // 关闭编辑活动模态框
  const closeEditActivityModal = () => {
    setEditActivityModal({
      show: false,
      activityId: null,
      dayIndex: -1,
      activityIndex: -1,
      currentActivity: null,
      title: '',
      startTime: '',
      endTime: '',
      notes: '',
      earliestStart: '',
      latestEnd: ''
    });
    setAttractionSearch({
      query: '',
      results: [],
      selectedAttraction: null,
      showDropdown: false,
      loading: false,
      limit: 8
    });
  };

  // 确认编辑活动
  const confirmEditActivity = async () => {
    // 新增：打印当前编辑活动弹窗和景点搜索状态
    console.log('【editActivityModal】', JSON.stringify(editActivityModal, null, 2));
    console.log('【attractionSearch】', JSON.stringify(attractionSearch, null, 2));
    if (!editActivityModal.activityId) {
      alert('活动数据错误');
      return;
    }

    // 时间合法性校验
    if (editActivityModal.startTime && editActivityModal.endTime) {
      if (editActivityModal.endTime < editActivityModal.startTime) {
        setErrorModalVisible(true);
        setErrorModal({
          message: `结束时间不能早于开始时间`
        });
        return;
      }
    }

    if (editActivityModal.earliestStart) {
      if ((editActivityModal.startTime && editActivityModal.startTime < editActivityModal.earliestStart) ||
          (editActivityModal.endTime && editActivityModal.endTime < editActivityModal.earliestStart)) {
        setErrorModalVisible(true);
        setErrorModal({
          message: `活动时间不能早于上一活动的结束时间 (${editActivityModal.earliestStart})`
        });
        return;
      }
    }

    if (editActivityModal.latestEnd) {
      if ((editActivityModal.startTime && editActivityModal.startTime > editActivityModal.latestEnd) ||
          (editActivityModal.endTime && editActivityModal.endTime > editActivityModal.latestEnd)) {
        setErrorModalVisible(true);
        setErrorModal({
          message: `活动时间不能晚于下一活动的开始时间 (${editActivityModal.latestEnd})`
        });
        return;
      }
    }


    try {
      const promises = [];
      
      // 1. 更新景点（如果选择了新景点）
      if (attractionSearch.selectedAttraction) {
        let attractionId = attractionSearch.selectedAttraction.id;
        // 判断是否为高德地图新地点
        if (typeof attractionId === 'string' && attractionId.startsWith('B')) {
          // 构造postBody，地点信息用attractionInfo包裹
          const postBody = {
            itineraryDayId: itineraryData.dailyPlan[editActivityModal.dayIndex]?.dayId,
            attractionInfo: {
              id: attractionSearch.selectedAttraction.id,
              name: attractionSearch.selectedAttraction.name,
              address: attractionSearch.selectedAttraction.address || '',
              city: attractionSearch.selectedAttraction.city || '上海',
              description: attractionSearch.selectedAttraction.description || '',
              longitude: attractionSearch.selectedAttraction.longitude,
              latitude: attractionSearch.selectedAttraction.latitude,
              tel: attractionSearch.selectedAttraction.tel || '',
              type: attractionSearch.selectedAttraction.type || ''
            }
          };
          // 新增：打印postBody
          console.log('【confirmEditActivity postBody】', JSON.stringify(postBody, null, 2));
          const amapRes = await fetch(`/api/activities/${editActivityModal.activityId}/amap-attraction`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(postBody)
          });
          if (amapRes.ok) {
            // 新增：高德景点保存成功后，先刷新当天活动列表
            const itineraryDayId = itineraryData.dailyPlan[editActivityModal.dayIndex]?.dayId;
            if (itineraryDayId) {
              const dayRes = await fetch(`/api/activities/day/${itineraryDayId}`, {
                method: 'GET',
                credentials: 'include'
              });
              if (dayRes.ok) {
                const activities = await dayRes.json();
                const updatedItineraryData = { ...itineraryData };
                updatedItineraryData.dailyPlan[editActivityModal.dayIndex].activities = activities.map(activity => ({
                  id: activity.id,
                  location: activity.attraction?.name || activity.title,
                  activity: activity.title,
                  time: activity.startTime && activity.endTime ? 
                    `${activity.startTime.substring(0,5)}-${activity.endTime.substring(0,5)}` : 
                    activity.startTime ? activity.startTime.substring(0,5) :
                    activity.endTime ? `至 ${activity.endTime.substring(0,5)}` : 
                    '时间待定',
                  transport: activity.transportMode || '步行',
                  notes: activity.attractionNotes || '',
                  nextId: activity.nextId,
                  prevId: activity.prevId,
                  longitude: activity.attraction?.longitude || activity.longitude,
                  latitude: activity.attraction?.latitude || activity.latitude,
                  attraction: activity.attraction
                }));
                setItineraryData(updatedItineraryData);
                console.log('高德地图新景点编辑成功，已刷新活动列表:', activities);
                setTimeout(() => {
                  reinitializeMap();
                }, 200);
                closeEditActivityModal();
                return;
              }
            }
          } else {
            alert('添加新地点失败，请重试');
            return;
          }
        } else {
          // 已有景点
          const res = await fetch(`/api/activities/${editActivityModal.activityId}/attraction`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ attractionId })
          });
          if (res.ok) {
            // 刷新当天活动列表
            const itineraryDayId = itineraryData.dailyPlan[editActivityModal.dayIndex]?.dayId;
            if (itineraryDayId) {
              const dayRes = await fetch(`/api/activities/day/${itineraryDayId}`, {
                method: 'GET',
                credentials: 'include'
              });
              if (dayRes.ok) {
                const activities = await dayRes.json();
                const updatedItineraryData = { ...itineraryData };
                updatedItineraryData.dailyPlan[editActivityModal.dayIndex].activities = activities.map(activity => ({
                  id: activity.id,
                  location: activity.attraction?.name || activity.title,
                  activity: activity.title,
                  time: activity.startTime && activity.endTime ? 
                    `${activity.startTime.substring(0,5)}-${activity.endTime.substring(0,5)}` : 
                    activity.startTime ? activity.startTime.substring(0,5) :
                    activity.endTime ? `至 ${activity.endTime.substring(0,5)}` : 
                    '时间待定',
                  transport: activity.transportMode || '步行',
                  notes: activity.attractionNotes || '',
                  nextId: activity.nextId,
                  prevId: activity.prevId,
                  longitude: activity.attraction?.longitude || activity.longitude,
                  latitude: activity.attraction?.latitude || activity.latitude,
                  attraction: activity.attraction
                }));
                setItineraryData(updatedItineraryData);
                console.log('已有景点编辑成功，已刷新活动列表:', activities);
                setTimeout(() => {
                  reinitializeMap();
                }, 200);
                closeEditActivityModal();
                return;
              }
            }
          }
        }
      }
      // 1.5 更新标题
      // 判断标题是否有修改，避免无变化或被清空时覆盖原标题
      const newTitle = editActivityModal.title ? editActivityModal.title.trim() : '';
      const originalTitle = editActivityModal.currentActivity?.activity || '';
      const titleChanged = newTitle && newTitle !== originalTitle;

      if (titleChanged) {
        promises.push(
          fetch(`/api/activities/${editActivityModal.activityId}/title`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title: newTitle })
          })
        );
      }
      
      // 2. 更新时间（如果有修改）
      if (editActivityModal.startTime || editActivityModal.endTime) {
        promises.push(
          fetch(`/api/activities/${editActivityModal.activityId}/time`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 
              startTime: editActivityModal.startTime || null,
              endTime: editActivityModal.endTime || null
            })
          })
        );
      }
      
      // 3. 更新备注
      promises.push(
        fetch(`/api/activities/${editActivityModal.activityId}/notes`, {
          method: 'PUT',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ attractionNotes: editActivityModal.notes })
        })
      );

      const responses = await Promise.all(promises);
      
      // 检查所有请求是否成功
      const allSuccess = responses.every(response => response.ok);
      
      if (allSuccess) {
        // 更新本地状态
        const updatedItineraryData = { ...itineraryData };
        const activity = updatedItineraryData.dailyPlan[editActivityModal.dayIndex].activities[editActivityModal.activityIndex];
        
        if (attractionSearch.selectedAttraction) {
          // 仅更新地点信息，不修改活动标题
          activity.location = attractionSearch.selectedAttraction.name;
        }
        
        if (editActivityModal.startTime || editActivityModal.endTime) {
          if (editActivityModal.startTime && editActivityModal.endTime) {
            activity.time = `${editActivityModal.startTime}-${editActivityModal.endTime}`;
          } else if (editActivityModal.startTime) {
            activity.time = editActivityModal.startTime;
          } else if (editActivityModal.endTime) {
            activity.time = `至 ${editActivityModal.endTime}`;
          }
        }
        
        if (titleChanged) {
          activity.activity = newTitle;
        }
        
        activity.notes = editActivityModal.notes;
        setItineraryData(updatedItineraryData);
        setTimeout(() => {
          reinitializeMap();
        }, 200);
        closeEditActivityModal();
      } else {
        throw new Error('部分更新失败');
      }
    } catch (error) {
      console.error('更新活动失败:', error);
      alert('更新活动失败，请重试');
    }
  };

  // 关闭添加景点弹窗
  const closeAddAttractionModal = () => {
    setAddAttractionModal({ 
      show: false, 
      dateId: '', 
      destination: '', 
      startTime: '', 
      endTime: '',
      insertIndex: -1,
      nextActivityId: null,
      insertBeforeAttraction: null,
      earliestStart: '',
      latestEnd: '',
      preSelectedAttraction: null // 清除预选景点
    });
    // 重置搜索状态
    setAttractionSearch({
      query: '',
      results: [],
      selectedAttraction: null,
      showDropdown: false,
      loading: false,
      limit: 8
    });
  };

  // 搜索景点
  const searchAttractions = async (keyword, customLimit = null) => {
    if (!keyword || keyword.trim().length < 1) {
      setAttractionSearch(prev => ({
        ...prev,
        results: [],
        showDropdown: false
      }));
      return;
    }

    setAttractionSearch(prev => ({ ...prev, loading: true }));
    console.log("attractionSearch length", attractionSearch.results.length);

    try {
      const limitToUse = customLimit !== null ? customLimit : attractionSearch.limit;
      const response = await fetch(`/api/attractions/search?keyword=${encodeURIComponent(keyword)}&limit=${limitToUse}`, {
        method: 'GET',
        credentials: 'include'
      });

      if (response.ok) {
        const attractions = await response.json();
        console.log('后端返回的景点搜索JSON包:', attractions); // 仅增加日志打印
        setAttractionSearch(prev => ({
          ...prev,
          results: attractions,
          showDropdown: true,
          loading: false,
          limit: limitToUse
        }));
      } else {
        throw new Error('搜索失败');
      }
    } catch (error) {
      console.error('搜索景点失败:', error);
      setAttractionSearch(prev => ({
        ...prev,
        results: [],
        showDropdown: false,
        loading: false
      }));
    }
  };

  // 选择景点
  const selectAttraction = (attraction) => {
    // 确保选中的景点包含所有必要字段
    const selectedAttraction = {
      id: attraction.id,
      name: attraction.name,
      description: attraction.description || '',
      longitude: attraction.longitude,
      latitude: attraction.latitude,
      city: attraction.city || '',
      address: attraction.address || ''
    };

    setAttractionSearch(prev => ({
      ...prev,
      query: attraction.name,
      selectedAttraction: selectedAttraction,
      showDropdown: false
    }));
    setAddAttractionModal(prev => ({
      ...prev,
      destination: attraction.name
    }));
  };

  // 处理搜索输入
  const handleSearchInput = (value) => {
    setAttractionSearch(prev => ({
      ...prev,
      query: value,
      selectedAttraction: null,
      limit: 8
    }));

    // 防抖搜索
    clearTimeout(window.searchTimeout);
    window.searchTimeout = setTimeout(() => {
      searchAttractions(value);
    }, 300);
  };

  // 编辑每日标题
  const editDayTitle = (dayIndex, currentTitle) => {
    setEditingDayTitle({ show: true, dayIndex, title: currentTitle });
  };

  // 关闭编辑每日标题弹窗
  const closeEditDayTitle = () => {
    setEditingDayTitle({ show: false, dayIndex: -1, title: '' });
  };

  // 确认修改每日标题
  const confirmEditDayTitle = () => {
    if (!editingDayTitle.title.trim()) {
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: '请输入标题'
      });
      return;
    }

    const dayData = itineraryData.dailyPlan[editingDayTitle.dayIndex];
    const dayId = dayData.dayId;

    // 调用后端API更新日程标题
    fetch(`/api/itineraries/${id}/days/${dayId}/title`, {
      method: 'PUT',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: editingDayTitle.title })
    })
      .then(res => res.ok ? res.json() : Promise.reject(res))
      .then(data => {
        // 更新本地状态
        const updatedItineraryData = { ...itineraryData };
        if (updatedItineraryData.dailyPlan[editingDayTitle.dayIndex]) {
          updatedItineraryData.dailyPlan[editingDayTitle.dayIndex].title = editingDayTitle.title;
          setItineraryData(updatedItineraryData);
        }
        closeEditDayTitle();
      })
      .catch(err => {
        console.error('更新日程标题失败:', err);
        alert('更新日程标题失败，请重试');
      });
  };

  // 上传封面图片
  const uploadCoverImage = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // 验证文件类型
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(file.type)) {
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: '请上传 JPG、PNG 或 GIF 格式的图片'
      });
      return;
    }

    // 验证文件大小（限制为10MB）
    if (file.size > 10 * 1024 * 1024) {
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: '图片文件大小不能超过10MB'
      });
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`/api/itineraries/${id}/cover`, {
        method: 'POST',
        credentials: 'include',
        body: formData
      });

      if (response.ok) {
        const data = await response.json();
        // 更新本地状态
        setItineraryData(prev => ({ ...prev, coverImage: data.imageUrl }));
      } else {
        throw new Error('上传失败');
      }
    } catch (error) {
      console.error('上传封面图片失败:', error);
      alert('上传封面图片失败，请重试');
    }

    // 清空input
    event.target.value = '';
  };

  // 编辑时间
  const editTime = (dayIndex, activityIndex, currentTime) => {
    // 解析当前时间，支持多种格式
    let startTime = '';
    let endTime = '';
    
    if (currentTime && currentTime !== '时间待定') {
      const timeMatch = currentTime.match(/(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})/);
      if (timeMatch) {
        startTime = timeMatch[1];
        endTime = timeMatch[2];
      } else {
        // 如果只有一个时间，作为开始时间
        const singleTimeMatch = currentTime.match(/(\d{1,2}:\d{2})/);
        if (singleTimeMatch) {
          startTime = singleTimeMatch[1];
        }
      }
    }
    
    setEditingTime({ 
      show: true, 
      dayIndex, 
      activityIndex, 
      startTime, 
      endTime 
    });
  };

  // 关闭编辑时间弹窗
  const closeEditTime = () => {
    setEditingTime({ show: false, dayIndex: -1, activityIndex: -1, startTime: '', endTime: '' });
  };

  // 确认修改时间
  const confirmEditTime = () => {
    const { dayIndex, activityIndex, startTime, endTime } = editingTime;
    
    let newTimeString = '时间待定';
    if (startTime && endTime) {
      newTimeString = `${startTime} - ${endTime}`;
    } else if (startTime) {
      newTimeString = startTime;
    } else if (endTime) {
      newTimeString = `至 ${endTime}`;
    }

    const updatedItineraryData = { ...itineraryData };
    if (updatedItineraryData.dailyPlan[dayIndex] && 
        updatedItineraryData.dailyPlan[dayIndex].activities[activityIndex]) {
      updatedItineraryData.dailyPlan[dayIndex].activities[activityIndex].time = newTimeString;
      setItineraryData(updatedItineraryData);
    }

    closeEditTime();
  };

  // 地图视图切换
  const switchMapView = (viewType) => {
    console.log(`切换地图视图: ${viewType}`);
  };

  // 点击页面其他地方关闭下拉菜单
  useEffect(() => {
    const handleClick = (e) => {
      if (!e.target.closest('.attraction-settings')) {
        closeAllDropdowns();
      }
      // 关闭景点搜索下拉框
      if (!e.target.closest('.form-group')) {
        setAttractionSearch(prev => ({ ...prev, showDropdown: false }));
      }
    };

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        closeTransportModal();
        closeEditActivityModal();
      }
    };

    document.addEventListener('click', handleClick);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('click', handleClick);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  // 渲染景点项目
  const renderAttractionItem = (attraction, showSettings = true, dayIndex = -1, activityIndex = -1) => (
    <div key={attraction.name} className="attraction-item">
      {showSettings && (
        <div className="attraction-settings">
          <button 
            className="settings-btn" 
            onClick={(e) => toggleSettingsDropdown(attraction.name, e)}
          >
            ⚙️
          </button>
          <div className={`settings-dropdown ${settingsDropdowns[attraction.name] ? 'show' : ''}`}>
            <div className="settings-option" onClick={() => addNewAttraction(attraction.name, dayIndex, activityIndex)}>
              <i>➕</i>
              <span>在前新增活动</span>
            </div>
            <div className="settings-option" onClick={() => deleteAttraction(attraction.name, dayIndex, activityIndex)}>
              <i>🗑️</i>
              <span>删除当前活动</span>
            </div>
          </div>
        </div>
      )}
      <div 
        className="attraction-content"
        onClick={() => {
          // 点击卡片打开编辑弹窗
          if (dayIndex >= 0 && activityIndex >= 0) {
            editActivity(dayIndex, activityIndex);
          }
        }}
        style={{
          cursor: dayIndex >= 0 && activityIndex >= 0 ? 'pointer' : 'default'
        }}
        onMouseEnter={(e) => {
          if (dayIndex >= 0 && activityIndex >= 0) {
            e.target.style.background = 'rgba(147, 204, 195, 0.05)';
          }
        }}
        onMouseLeave={(e) => {
          if (dayIndex >= 0 && activityIndex >= 0) {
            e.target.style.background = 'transparent';
          }
        }}
        title={dayIndex >= 0 && activityIndex >= 0 ? "点击编辑活动" : ""}
      >
        <div className="attraction-header">
          <div className={`attraction-icon ${attraction.type}`}>
            {attraction.icon}
          </div>
          <div className="attraction-name">{attraction.name}</div>
        </div>
        <div className="attraction-details">{attraction.desc}</div>
        <div className="attraction-time">{attraction.time}</div>
      </div>
    </div>
  );

  // 渲染交通栏
  const renderTransportBar = (from, to, transportType = 'public', activityId = null, dayIndex = -1, activityIndex = -1, isFirstActivity = false) => {
    const getTransportClass = (transport) => {
      if (transport === '步行') return 'walking';
      if (transport === '骑行') return 'cycling';
      if (transport === '公共交通' || transport === '公交') return 'public';
      if (transport === '驾车') return 'driving';
      return '';
    };
    const getTransportIcon = (transport) => {
      if (transport === '步行') return '🚶';
      if (transport === '骑行') return '🚴';
      if (transport === '公共交通' || transport === '公交') return '🚇';
      if (transport === '驾车') return '🚗';
      return '🚇';
    };
    // 推荐用时
    let duration = null;
    if (dayIndex >= 0 && activityIndex >= 0) {
      duration = routeTimes[`${dayIndex}_${activityIndex}`];
    }
    return (
      <div className="transport-bar">
        <div className="transport-line"></div>
        <div className={`transport-method ${getTransportClass(transportType)}`}
          onClick={(e) => openTransportModal(e, activityId, dayIndex, activityIndex)}
          style={{ cursor: 'pointer' }}>
          <span>{getTransportIcon(transportType)}</span>
          <span>{transportType}</span>
          {duration && (
            <span style={{ marginLeft: 8, color: '#888', fontSize: '0.9em' }}>
              推荐用时：{Math.round(duration / 60)}分钟
            </span>
          )}
        </div>
        <div className="transport-line"></div>
      </div>
    );
  };

  // 确认添加景点
  const confirmAddAttraction = () => {
    // 临时禁用错误弹窗
    const originalErrorHandler = window.onerror;
    const originalUnhandledRejectionHandler = window.onunhandledrejection;
    
    window.onerror = () => true; // 阻止错误弹窗
    window.onunhandledrejection = (event) => {
      event.preventDefault();
      return true;
    };
    
    try {
    // 时间合法性校验：结束时间不能早于开始时间
    if (addAttractionModal.startTime && addAttractionModal.endTime) {
      if (addAttractionModal.endTime < addAttractionModal.startTime) {
        setErrorModalVisible(true);
        setErrorModal({
          type: 'error',
          message: '结束时间不能早于开始时间'
        });
        return;
      }
    }
    
    // 确定目的地：优先使用输入框的值，如果没有则使用选中的景点名称
    let destination = addAttractionModal.destination.trim();
    if (!destination && attractionSearch.selectedAttraction) {
      destination = attractionSearch.selectedAttraction.name;
        console.log('使用选中的景点名称作为目的地:', destination);
    }
    
    // 如果既没有目的地也没有选中景点，则退出
    if (!destination) {
      return;
    }

    // 检查开始/结束时间是否在允许范围内
    if (addAttractionModal.earliestStart) {
      if ((addAttractionModal.startTime && addAttractionModal.startTime < addAttractionModal.earliestStart) ||
          (addAttractionModal.endTime && addAttractionModal.endTime < addAttractionModal.earliestStart)) {
        setErrorModalVisible(true);
        setErrorModal({
          type: 'error',
          message: `活动时间不能早于上一活动的结束时间 (${addAttractionModal.earliestStart})`
        });
        return;
      }
    }

    if (addAttractionModal.latestEnd) {
      if ((addAttractionModal.startTime && addAttractionModal.startTime > addAttractionModal.latestEnd) ||
          (addAttractionModal.endTime && addAttractionModal.endTime > addAttractionModal.latestEnd)) {
        setErrorModalVisible(true);
        setErrorModal({
          type: 'error',
          message: `活动时间不能晚于下一活动的开始时间 (${addAttractionModal.latestEnd})`
        });
        return;
      }
    }

      // 构建时间字符串
      let timeString = '时间待定';
      if (addAttractionModal.startTime && addAttractionModal.endTime) {
        timeString = `${addAttractionModal.startTime} - ${addAttractionModal.endTime}`;
      } else if (addAttractionModal.startTime) {
        timeString = addAttractionModal.startTime;
      } else if (addAttractionModal.endTime) {
        timeString = `至 ${addAttractionModal.endTime}`;
      }

      // 创建新的活动项
      const newActivity = {
        location: destination,
        activity: addAttractionModal.customTitle || destination,
        time: timeString,
        duration: '待定'
      };

    // 解析日期ID，获取对应的日期数据
    const dayMatch = addAttractionModal.dateId.match(/day(\d+)/);
      console.log('日期ID匹配结果:', dayMatch);
      console.log('itineraryData:', itineraryData);

    if (dayMatch && itineraryData && itineraryData.dailyPlan) {
      const dayNumber = parseInt(dayMatch[1]);
      const dayIndex = dayNumber - 1;
        console.log('解析出的日期信息 - dayNumber:', dayNumber, 'dayIndex:', dayIndex);

        // 后端同步：如果拿到了 itineraryDayId，就立即调用接口
      const itineraryDayId = itineraryData.dailyPlan[dayIndex]?.dayId;
        console.log('获取到的 itineraryDayId:', itineraryDayId);

        if (itineraryDayId) {
          // 检查是否有选中的景点（来自搜索或中转站）
          const selectedAttraction = attractionSearch.selectedAttraction || addAttractionModal.preSelectedAttraction;
          if (!selectedAttraction) {
            console.log('错误：没有选中景点');
            setErrorModalVisible(true);
            setErrorModal({
              type: 'error',
              message: '请从搜索结果中选择一个景点'
            });
            return;
          }

          // 获取插入位置的活动ID
        const postBody = {
          itineraryDayId: itineraryDayId,
          title: addAttractionModal.customTitle || destination,
          transportMode: '步行',
          startTime: addAttractionModal.startTime || null,
          endTime: addAttractionModal.endTime || null,
        nextId: addAttractionModal.nextActivityId || null
      };

          // 判断是否是从高德地图选择的景点
          const isFromAMap = selectedAttraction && 
                           selectedAttraction.id && 
                           typeof selectedAttraction.id === 'string' && 
                           selectedAttraction.id.startsWith('B');

      if (isFromAMap) {
            // 从高德地图选择的景点，使用amap API
        postBody.attractionInfo = {
              id: selectedAttraction.id,
              name: selectedAttraction.name,
              address: selectedAttraction.address || '',
              city: selectedAttraction.city || '上海',
              description: selectedAttraction.description || '',
              longitude: selectedAttraction.longitude,
              latitude: selectedAttraction.latitude,
              tel: selectedAttraction.tel || '',
              type: selectedAttraction.type || ''
        };
      } else {
            // 从数据库搜索结果选择的景点，使用普通API
        postBody.attractionId = selectedAttraction.id;
      }

          // 新增：打印postBody
          console.log('【confirmAddActivity postBody】', JSON.stringify(postBody, null, 2));
          // 确定API端点
        const apiEndpoint = isFromAMap ? '/api/activities/amap' : '/api/activities';

          fetch(apiEndpoint, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(postBody)
          })
              .then(res => {
                if (res.ok) {
                  return res.json();
                } else {
                  return Promise.reject(res);
                }
              })
              .then(data => {
                // 重新获取该天的所有活动，以确保顺序正确
                fetch(`/api/activities/day/${itineraryDayId}`, {
              method: 'GET',
              credentials: 'include'
                })
                    .then(res => {
                      if (res.ok) {
                        return res.json();
                      } else {
                        return Promise.reject(res);
                      }
                    })
                    .then(activities => {
                      // 直接使用后端返回的有序活动列表

                const updatedItineraryData = { ...itineraryData };
                      updatedItineraryData.dailyPlan[dayIndex].activities = activities.map(activity => {
                        const mappedActivity = {
                    id: activity.id,
                    location: activity.attraction?.name || activity.title,
                    activity: activity.title,
                    time: activity.startTime && activity.endTime ? 
                      `${activity.startTime.substring(0,5)}-${activity.endTime.substring(0,5)}` : 
                      activity.startTime ? activity.startTime.substring(0,5) :
                      activity.endTime ? `至 ${activity.endTime.substring(0,5)}` : 
                      '时间待定',
                    transport: activity.transportMode || '步行',
                    notes: activity.attractionNotes || '',
                    nextId: activity.nextId,
                    prevId: activity.prevId,
                          // 添加经纬度信息
                    longitude: activity.attraction?.longitude || activity.longitude,
                    latitude: activity.attraction?.latitude || activity.latitude,
                          // 保留完整的attraction对象
                    attraction: activity.attraction
                        };
                        return mappedActivity;
                      });

                setItineraryData(updatedItineraryData);

                      // 添加景点完成后，重新初始化地图
                      setTimeout(() => {
                        reinitializeMap();
                      }, 200);
                    })
                    .catch(err => {
                      console.warn('获取活动列表失败:', err);
                    });
              })
              .catch(err => {
                console.warn('创建活动失败:', err);
              });
        }

      closeAddAttractionModal();
      } else {
        console.log('没有找到有效的 itineraryDayId，跳过后端同步');
    }
    } finally {
      // 恢复原始错误处理器
      window.onerror = originalErrorHandler;
      window.onunhandledrejection = originalUnhandledRejectionHandler;
    }
  };

  // 如果数据还在加载，显示加载状态
  if (!itineraryData) {
    return (
      <div className="edit-itinerary-page">
        <div className="text-center p-50">
          <div>加载中...</div>
        </div>
      </div>
    );
  }

  // 获取指定日程中某索引之前最近的结束时间
  const getPrevEndTime = (dayIndex, activityIndex) => {
    if (!itineraryData || !itineraryData.dailyPlan[dayIndex]) return '';
    const acts = itineraryData.dailyPlan[dayIndex].activities;
    let idx = activityIndex;
    while (idx >= 0) {
      const timeStr = acts[idx].time;
      if (timeStr && timeStr !== '时间待定') {
        const match = timeStr.match(/-\s*(\d{1,2}:\d{2})/); // 匹配结束时间
        if (match) return match[1];
      }
      idx--;
    }
    return '';
  };

  // 获取指定日程中某索引之后最近的开始时间
  const getNextStartTime = (dayIndex, activityIndex) => {
    if (!itineraryData || !itineraryData.dailyPlan[dayIndex]) return '';
    const acts = itineraryData.dailyPlan[dayIndex].activities;
    let idx = activityIndex;
    while (idx < acts.length) {
      const timeStr = acts[idx].time;
      if (timeStr && timeStr !== '时间待定') {
        const match = timeStr.match(/(\d{1,2}:\d{2})/); // 匹配开始时间
        if (match) return match[1];
      }
      idx++;
    }
    return '';
  };

  // 加载更多景点
  const loadMoreAttractions = () => {
    const newLimit = attractionSearch.limit + 8;
    // 先更新limit
    setAttractionSearch(prev => ({ ...prev, limit: newLimit }));
    // 使用新的limit重新搜索
    searchAttractions(attractionSearch.query, newLimit);
  };

  // 打开开始日期选择弹窗
  const changeStartDate = () => {
    setStartDateModal({ show: true, date: itineraryData.startDate });
  };

  // 关闭开始日期弹窗
  const closeStartDateModal = () => {
    setStartDateModal({ show: false, date: '' });
  };

  // 确认修改开始日期
  const confirmStartDate = async () => {
    const newDate = startDateModal.date;
    if (!newDate || newDate === itineraryData.startDate) {
      closeStartDateModal();
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(newDate)) {
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: '日期格式不正确，请使用 YYYY-MM-DD'
      });
      return;
    }
    try {
      const res = await fetch(`/api/itineraries/${id}/shift`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newStartDate: newDate })
      });
      if (res.ok) {
        fetchItineraryData(id);
      } else if (res.status === 401) {
        navigate('/login');
      } else {
        throw new Error('更新失败');
      }
    } catch (e) {
      console.error('更新开始日期失败', e);
      alert('更新开始日期失败');
    } finally {
      closeStartDateModal();
    }
  };

  // 新增高德地图搜索函数
  const searchAMapPoints = async (keyword) => {
    setAmapLoading(true);
    try {
      // 确保高德地图API已加载
      if (!window.AMapLoader) {
        const loaderScript = document.createElement('script');
        loaderScript.src = 'https://webapi.amap.com/loader.js';
        loaderScript.async = true;
        loaderScript.onload = () => {
          initAMapSearch(keyword);
        };
        document.body.appendChild(loaderScript);
      } else {
        initAMapSearch(keyword);
      }
    } catch (error) {
      setAmapLoading(false);
      console.error('高德地图搜索失败:', error);
    }
  };

  // 初始化高德地图搜索
  const initAMapSearch = (keyword) => {
    window.AMapLoader.load({
      key: process.env.REACT_APP_AMAP_KEY || '',
      version: '2.0',
      plugins: ['AMap.PlaceSearch', 'AMap.Geocoder']
    }).then((AMap) => {
      const placeSearch = new AMap.PlaceSearch({
        pageSize: 15,
        pageIndex: 1,
        city: '全国'
      });
      
      const geocoder = new AMap.Geocoder();
      
      placeSearch.search(keyword, (status, result) => {
        setAmapLoading(false);
        if (status === 'complete' && result && result.poiList && result.poiList.pois.length > 0) {
          // 使用Promise.all来处理所有POI的地理编码
          const poisWithCity = result.poiList.pois.map(poi => {
            return new Promise((resolve) => {
              // 使用地理编码获取城市信息
              geocoder.getAddress(poi.location, (status, result) => {
                let city = '';
                
                if (status === 'complete' && result && result.regeocode) {
                  const addressComponent = result.regeocode.addressComponent;
                  
                  // 优先使用city字段，如果没有则使用province
                  if (addressComponent.city) {
                    city = addressComponent.city.replace(/(市|盟|自治州)$/, '');
                  } else if (addressComponent.province) {
                    city = addressComponent.province.replace(/(省|市|盟|自治州)$/, '');
                  }
                }
                
                // 如果地理编码失败，尝试从POI字段获取
                if (!city) {
                  if (poi.cityname) {
                    city = poi.cityname.replace(/(市|盟|自治州)$/, '');
                  } else if (poi.city) {
                    city = poi.city.replace(/(市|盟|自治州)$/, '');
                  } else if (poi.adname) {
                    city = poi.adname.replace(/(市|盟|自治州)$/, '');
                  }
                }
                
                resolve({
                  id: poi.id,
                  name: poi.name,
                  address: poi.address,
                  location: poi.location,
                  longitude: poi.location.lng,
                  latitude: poi.location.lat,
                  type: poi.type,
                  tel: poi.tel,
                  city: city || '上海', // 默认城市（已省略后缀）
                  description: poi.address || ''
                });
              });
            });
          });
          
          // 等待所有地理编码完成
          Promise.all(poisWithCity).then(pois => {
            setAmapResults(pois);
            setShowAMapModal(true);
          });
        } else {
          alert('高德地图搜索无结果');
        }
      });
    }).catch((error) => {
      setAmapLoading(false);
      console.error('高德地图API加载失败:', error);
    });
  };

  // 选择高德地图结果
  const selectAMapResult = (result) => {
    const selectedAttraction = {
      id: result.id,
      name: result.name,
      address: result.address,
      city: result.city || '上海', // 已省略后缀
      description: result.description || result.address || '',
      longitude: result.longitude,
      latitude: result.latitude,
      tel: result.tel || '',
      type: result.type || ''
    };

    setAttractionSearch(prev => ({
      ...prev,
      selectedAttraction: selectedAttraction,
      query: result.name
    }));
    setShowAMapModal(false);
    setAmapResults([]);
  };

  return (
    <div className="edit-itinerary-page">
      <a 
        href="#" 
        className="back-link"
        onClick={(e) => { 
          e.preventDefault(); 
          // 从URL中获取来源参数
          const params = new URLSearchParams(location.search);
          const from = params.get('from');
          const groupId = params.get('groupId');
          
          if (from === 'group' && groupId) {
            navigate(`/group-detail/${groupId}`);
          } else {
            navigate('/manage');
          }
        }}
      >
        <span className="back-link-icon">←</span>
        <span>返回{location.search.includes('from=group') ? '团队页面' : '我的行程'}</span>
      </a>

      {/* 新行程提示 */}
      {isNewTrip && (
        <div className="new-trip-banner">
          🎉 欢迎创建新行程！您可以开始添加景点、活动和详细安排。
        </div>
      )}

      {/* 行程标题 */}
      <div className="itinerary-header">
        <h1 className="itinerary-title" onClick={editTitle}>
          {itineraryData.title}
        </h1>
        <div className="mt-8">
          <label className="upload-cover-btn">
            📷 上传封面图片
            <input 
              type="file" 
              accept="image/*" 
              onChange={uploadCoverImage}
              className="d-none"
            />
          </label>
          {itineraryData.coverImage && (
            <span className="ml-8 fs-08 text-secondary">
              ✓ 已上传
            </span>
          )}
        </div>
        <div className="itinerary-meta">
          <div className="meta-item">
            <span>📅</span>
            <span className="editable-field" title="点击修改开始日期" onClick={changeStartDate}>
              {itineraryData.startDate} - {itineraryData.endDate}
            </span>
          </div>
          <div className="meta-item">
            <span>📍</span>
            <span>{itineraryData.destination}</span>
          </div>
          <div className="meta-item">
            <span>👥</span>
            <span className="editable-field" onClick={editTravelerCount}>
              {itineraryData.participants ? `${itineraryData.participants.length}人出行` : '1人出行'}
            </span>
          </div>
          <div className="meta-item">
            <span>💰</span>
            <span className="editable-field" onClick={editBudget}>
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
          <div className="date-tabs-container">
            <div className="date-tabs">
              <div 
                className={`date-tab overview ${selectedDate === 'overview' ? 'active' : ''}`}
                onClick={() => switchDateTab('overview')}
              >
                总览
              </div>
              {itineraryData.dailyPlan && itineraryData.dailyPlan.map(day => (
                <div 
                  key={`day${day.day}`}
                  className={`date-tab ${selectedDate === `day${day.day}` ? 'active' : ''}`}
                  onClick={() => switchDateTab(`day${day.day}`)}
                >
                  第{day.day}天
                </div>
              ))}
            </div>
          </div>

          {/* 日期内容区域 */}
          <div className="date-content">
            {/* 总览内容 */}
            {selectedDate === 'overview' && (
              <div className="date-content-item active">
                {isNewTrip ? (
                  <div className="empty-state">
                    <div className="text-center p-40-20 text-secondary">
                      <div className="fs-3rem mb-20">✈️</div>
                      <h3 className="mb-8" style={{ color: 'var(--text-primary)' }}>开始规划您的行程</h3>
                      <p className="mb-20">点击左侧的日期开始添加景点和活动</p>
                      <button 
                        className="btn-primary"
                        onClick={() => itineraryData.dailyPlan && switchDateTab(`day${itineraryData.dailyPlan[0].day}`)}
                      >
                        开始规划第一天
                      </button>
                    </div>
                  </div>
                ) : (
                  itineraryData.dailyPlan && itineraryData.dailyPlan.map(day => (
                    <div key={day.day}>
                      {renderAttractionItem({
                        icon: '📍',
                        type: 'location',
                        name: `第${day.day}天: ${day.title}`,
                        desc: day.city,
                        time: day.date
                      }, false)}
                    </div>
                  ))
                )}
              </div>
            )}

            {/* 具体日期内容 */}
            {itineraryData.dailyPlan && itineraryData.dailyPlan.find(day => `day${day.day}` === selectedDate) && (
              <div className="date-content-item active">
                {(() => {
                  const dayData = itineraryData.dailyPlan.find(day => `day${day.day}` === selectedDate);
                  const dayIndex = dayData.day - 1;
                  
                  return (
                    <>
                      <div className="edit-day-title-bg-container">
                        <h3 
                          onClick={() => editDayTitle(dayIndex, dayData.title)}
                          onMouseEnter={(e) => {
                            e.target.style.textDecoration = 'underline';
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.textDecoration = 'none';
                          }}
                          title="点击编辑标题"
                        >
                          {dayData.title}
                        </h3>
                        <p className="text-secondary fs-09 m-0">
                          {dayData.city} • {dayData.date}
                        </p>
                      </div>
                      
                      {dayData.activities && dayData.activities.length > 0 ? (
                        dayData.activities.map((activity, index) => (
                          <div key={index}>
                            {/* 如果不是第一个活动，显示到达该活动的交通方式 */}
                            {index > 0 && 
                              renderTransportBar(
                                              dayData.activities[index - 1].location,
                                              activity.location,
                                              activity.transport,
                                activity.id,
                                dayIndex,
                                index - 1, // 修正为本段起点下标
                                false
                              )
                            }
                            {renderAttractionItem({
                              icon: getActivityIcon(activity),
                              type: getActivityType(activity),
                              name: activity.activity,
                              desc: activity.location,
                              time: `${activity.time}`
                            }, true, dayIndex, index)}
                          </div>
                        ))
                      ) : (
                        <div className="empty-day-state">
                          <div className="text-center p-40-20 text-secondary">
                            <div className="fs-2rem mb-12">📝</div>
                            <h4 className="mb-8" style={{ color: 'var(--text-primary)' }}>这一天还没有安排</h4>
                            <p className="mb-16 fs-09">开始添加景点、餐厅或活动</p>
                          </div>
                        </div>
                      )}
                      
                      <div className="mt-20 text-center">
                        <button 
                          className="btn-outline"
                          onClick={() => addAttraction(`day${dayData.day}`)}
                        >
                          + 添加更多活动
                        </button>
                      </div>
                    </>
                  );
                })()}
              </div>
            )}
            {/* 其他日期内容 */}
            {dateContents[selectedDate] && (
              <div className="date-content-item active">
                {dateContents[selectedDate].map((item, index) => (
                  <div key={index}>
                    {renderAttractionItem(item)}
                    {index < dateContents[selectedDate].length - 1 && 
                      renderTransportBar(item.name, dateContents[selectedDate][index + 1].name)
                    }
                  </div>
                ))}
                <button className="add-attraction-btn" onClick={() => addAttraction(selectedDate)}>
                  <span>➕</span>
                  <span>添加地点/活动</span>
                </button>
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
              <RouteMap 
                ref={mapRef}
                key={JSON.stringify(itineraryData?.dailyPlan)} 
                markers={markers} 
                isOverview={selectedDate === 'overview'} 
              />
            {/* 调试信息 */}
            <div style={{ 
              position: 'absolute', 
              top: '10px', 
              right: '10px', 
              background: 'rgba(0,0,0,0.7)', 
              color: 'white', 
              padding: '5px 10px', 
              borderRadius: '4px', 
              fontSize: '12px',
              zIndex: 1000
            }}>
              {selectedDate === 'overview' ? '总览模式' : `第${selectedDate.replace('day', '')}天`}: {markers.length}个标记
            </div>
          </div>
        </div>
      </div>

        {/* 交通方式选择弹窗 */}
        {transportModal.show && (() => {
          // 日志：弹窗打开时打印当前起点、终点、交通方式
          if (itineraryData && transportModal.dayIndex >= 0 && transportModal.activityIndex >= 0) {
            const dayData = itineraryData.dailyPlan[transportModal.dayIndex];
            const activities = dayData.activities;
            const from = activities[transportModal.activityIndex];
            const to = activities[transportModal.activityIndex + 1];
            if (from && to) {
              console.log('[交通方式选择弹窗] 起点:', from.name || from.activity || from.location, '经纬度:', from.longitude, from.latitude);
              console.log('[交通方式选择弹窗] 终点:', to.name || to.activity || to.location, '经纬度:', to.longitude, to.latitude);
              ['walking','cycling','public','driving'].forEach(type => {
                const info = transportInfo[type];
                console.log(`[交通方式选项] ${info.name} (${type})`);
              });
            }
          }
          return (
              <div className="transport-modal show" onClick={(e) => {
                if (e.target === e.currentTarget) closeTransportModal();
              }}>
                <div className="transport-modal-content">
                  <div className="transport-modal-header">
                    <div className="transport-modal-title">选择交通方式</div>
                    <button className="transport-modal-close" onClick={closeTransportModal}>×</button>
              </div>
                  <div className="transport-options">
                    {['walking','cycling','public','driving'].map(type => (
                        <div className={`transport-option ${type}`} key={type} onClick={() => selectTransport(type)}>
                          <div className="transport-option-icon">{transportInfo[type].icon}</div>
                          <div className="transport-option-name">{transportInfo[type].name}</div>
                          <div className="transport-option-time">
                            {(() => {
                              // 预估用时key
                              const key = `${transportModal.dayIndex}_${transportModal.activityIndex}_${type}`;
                              const duration = routeTimes[key];
                              return duration ? `约${Math.round(duration/60)}分钟` : '';
                            })()}
            </div>
          </div>
                    ))}
        </div>
                </div>
              </div>
          );
        })()}



      {/* 添加景点弹窗 */}
      {addAttractionModal.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeAddAttractionModal();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">
              {addAttractionModal.insertBeforeAttraction 
                ? `在"${addAttractionModal.insertBeforeAttraction}"前添加活动`
                : '添加新活动'
              }
            </h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">活动标题 *</label>
                <div style={{ position: 'relative', width: '70%', margin: '0 auto' }} >
                <input
                  type="text"
                  className="form-input"
                  placeholder="请输入活动标题（吃早餐等...）"
                  value={addAttractionModal.customTitle}
                  onChange={(e) => setAddAttractionModal(prev => ({ ...prev, customTitle: e.target.value }))}
                />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">活动地点 *</label>
                <div style={{ position: 'relative', width: '70%', margin: '0 auto' }} >
                <input
                  type="text"
                  className="form-input"
                    placeholder="搜索景点名称..."
                    value={attractionSearch.query}
                    onChange={(e) => handleSearchInput(e.target.value)}
                    autoComplete="off"
                  />
                  {attractionSearch.selectedAttraction && (
                    <div className="search-dropdown-item" >
                      ✓ 已选择: {attractionSearch.selectedAttraction.name}
                    </div>
                  )}
                  {attractionSearch.loading && (
                    <div className="search-loading">
                      搜索中...
                    </div>
                  )}
                  
                  {/* 搜索结果下拉框 */}
                  {attractionSearch.showDropdown && attractionSearch.results.length > 0 && (
                    <div className="search-dropdown" >
                      {attractionSearch.results.map((attraction, index) => (
                        <div className="search-dropdown-item"
                          key={attraction.id}
                          style={{ borderBottom: index < attractionSearch.results.length - 1 ? '1px solid var(--border-color)' : 'none' }}
                          onMouseEnter={(e) => e.target.style.backgroundColor = 'var(--accent-color)'}
                          onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
                          onClick={() => selectAttraction(attraction)}
                        >
                          <div style={{ fontWeight: '500', color: 'var(--text-primary)' }}>
                            {attraction.name}
                          </div>
                          <div style={{ fontSize: '0.92rem', color: '#888', margin: '2px 0 2px 0' }}>
                            经纬度：{(attraction.longitude && attraction.latitude) ? `${attraction.longitude}, ${attraction.latitude}` : '无坐标'}
                          </div>
                          {attraction.description && (
                            <div className="search-dropdown-item-desc">
                              {attraction.description}
                            </div>
                          )}
                        </div>
                      ))}
                      {/* 加载更多按钮 */}
                      {attractionSearch.results.length >= attractionSearch.limit && (
                        <div className="search-more-btn" onClick={loadMoreAttractions}>
                          加载更多...
                        </div>
                      )}
                      {/* 高德地图搜索按钮 */}
                      <div className="search-dropdown-item" style={{borderTop: '1px solid #eee', paddingTop: '8px'}}>
                        <button 
                          className="modal-btn primary" 
                          style={{fontSize: '0.9rem', padding: '4px 8px'}}
                          onClick={() => searchAMapPoints(attractionSearch.query)}
                        >
                          没有我想要的地点，使用高德地图搜索
                        </button>
                      </div>
                    </div>
                  )}
                  
                  {/* 无搜索结果提示 */}
                  {attractionSearch.showDropdown && attractionSearch.results.length === 0 && !attractionSearch.loading && attractionSearch.query.length >= 2 && (
                    <div className="search-dropdown-item">
                      未找到相关景点
                      <button 
                        className="modal-btn primary" 
                        style={{marginTop: 8, fontSize: '0.9rem', padding: '4px 8px'}}
                        onClick={() => searchAMapPoints(attractionSearch.query)}
                      >
                        没有我想要的地点，使用高德地图搜索
                      </button>
                    </div>
                  )}
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">起始时间（可选）</label>
                  <input
                    type="time"
                    className="form-input"
                    value={addAttractionModal.startTime}
                    onChange={(e) => setAddAttractionModal(prev => ({ ...prev, startTime: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">结束时间（可选）</label>
                  <input
                    type="time"
                    className="form-input"
                    value={addAttractionModal.endTime}
                    min={addAttractionModal.earliestStart || addAttractionModal.startTime || undefined}
                    max={addAttractionModal.latestEnd || addAttractionModal.endTime || undefined}
                    onChange={(e) => setAddAttractionModal(prev => ({ ...prev, endTime: e.target.value }))}
                  />
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeAddAttractionModal}>取消</button>
              <button 
                className="modal-btn primary" 
                onClick={confirmAddAttraction}
              >
                确定
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑活动弹窗 */}
      {editActivityModal.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeEditActivityModal();
        }}>
          <div className="modal-content modal-w550">
            <h3 className="modal-title">编辑活动</h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">活动标题 *</label>
                <div style={{ position: 'relative', width: '70%', margin: '0 auto' }} >
                <input
                  type="text"
                  className="form-input"
                  value={editActivityModal.title}
                  onChange={(e) => setEditActivityModal(prev => ({ ...prev, title: e.target.value }))}
                />
                </div>
              </div>
              {/* 更换景点 */}
              <div className="form-group">
                <label className="form-label">重新选择活动地点</label>
                <div style={{ position: 'relative', width: '70%', margin: '0 auto' }} >
                  <input
                    type="text"
                    className="form-input"
                    placeholder={`当前：${editActivityModal.currentActivity?.location || '未知景点'}`}
                    value={attractionSearch.query}
                    onChange={(e) => handleSearchInput(e.target.value)}
                    autoComplete="off"
                  />
                  {attractionSearch.loading && (
                    <div className="search-loading">
                      搜索中...
                    </div>
                  )}
                  
                  {/* 搜索结果下拉框 */}
                  {attractionSearch.showDropdown && attractionSearch.results.length > 0 && (
                    <div style={{
                      position: 'absolute',
                      top: '100%',
                      left: '0',
                      right: '0',
                      backgroundColor: 'white',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                      zIndex: 1000,
                      maxHeight: '150px',
                      overflowY: 'auto'
                    }}>
                      {attractionSearch.results.map((attraction, index) => (
                        <div
                          key={attraction.id}
                          style={{
                            padding: '8px 12px',
                            cursor: 'pointer',
                            borderBottom: index < attractionSearch.results.length - 1 ? '1px solid var(--border-color)' : 'none',
                            transition: 'background-color 0.2s'
                          }}
                          onMouseEnter={(e) => e.target.style.backgroundColor = 'var(--accent-color)'}
                          onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
                          onClick={() => selectAttraction(attraction)}
                        >
                          <div style={{ fontWeight: '500', color: 'var(--text-primary)', fontSize: '0.9rem' }}>
                            {attraction.name}
                          </div>
                          <div style={{ fontSize: '0.92rem', color: '#888', margin: '2px 0 2px 0' }}>
                            经纬度：{(attraction.longitude && attraction.latitude) ? `${attraction.longitude}, ${attraction.latitude}` : '无坐标'}
                          </div>
                          {attraction.description && (
                            <div className="search-dropdown-item-desc">
                              {attraction.description}
                            </div>
                          )}
                        </div>
                      ))}
                      {/* 加载更多按钮 */}
                      {attractionSearch.results.length >= attractionSearch.limit && (
                        <div
                          style={{
                            padding: '8px 12px',
                            textAlign: 'center',
                            cursor: 'pointer',
                            color: 'var(--primary-color)',
                            fontWeight: '500',
                            borderTop: '1px solid var(--border-color)'
                          }}
                          onClick={loadMoreAttractions}
                        >
                          加载更多...
                        </div>
                      )}
                      {/* 高德地图搜索按钮 */}
                      <div
                        style={{
                          padding: '8px 12px',
                          textAlign: 'center',
                          borderTop: '1px solid var(--border-color)'
                        }}
                      >
                        <button 
                          className="modal-btn primary" 
                          style={{fontSize: '0.9rem', padding: '4px 8px'}}
                          onClick={() => searchAMapPoints(attractionSearch.query)}
                        >
                          没有我想要的地点，使用高德地图搜索
                        </button>
                      </div>
                    </div>
                  )}
                  
                  {/* 无搜索结果提示 */}
                  {attractionSearch.showDropdown && attractionSearch.results.length === 0 && !attractionSearch.loading && attractionSearch.query.length >= 2 && (
                    <div style={{
                      position: 'absolute',
                      top: '100%',
                      left: '0',
                      right: '0',
                      backgroundColor: 'white',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                      zIndex: 1000,
                      padding: '15px',
                      textAlign: 'center',
                      color: 'var(--text-secondary)',
                      fontSize: '0.85rem'
                    }}>
                      未找到相关景点
                      <button 
                        className="modal-btn primary" 
                        style={{marginTop: 8, fontSize: '0.9rem', padding: '4px 8px'}}
                        onClick={() => searchAMapPoints(attractionSearch.query)}
                      >
                        没有我想要的地点，使用高德地图搜索
                      </button>
                    </div>
                  )}
                </div>
                {attractionSearch.selectedAttraction && (
                  <div style={{
                    marginTop: '8px',
                    padding: '8px 12px',
                    backgroundColor: 'var(--accent-color)',
                    border: '1px solid var(--primary-color)',
                    borderRadius: '6px',
                    fontSize: '0.9rem'
                  }}>
                    ✓ 已选择: {attractionSearch.selectedAttraction.name}
                  </div>
                )}
              </div>
              
              {/* 设置时间 */}
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">开始时间</label>
                  <input
                    type="time"
                    className="form-input"
                    min={editActivityModal.earliestStart || undefined}
                    max={editActivityModal.latestEnd || undefined}
                    value={editActivityModal.startTime}
                    onChange={(e) => setEditActivityModal(prev => ({ ...prev, startTime: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">结束时间</label>
                  <input
                    type="time"
                    className="form-input"
                    min={editActivityModal.earliestStart || editActivityModal.startTime || undefined}
                    max={editActivityModal.latestEnd || editActivityModal.endTime || undefined}
                    value={editActivityModal.endTime}
                    onChange={(e) => setEditActivityModal(prev => ({ ...prev, endTime: e.target.value }))}
                  />
                </div>
              </div>
              
              {/* 添加备注 */}
              <div className="form-group">
                <label className="form-label">活动备注</label>
                <textarea
                  className="form-input"
                  placeholder="记录活动相关的注意事项、小贴士等..."
                  value={editActivityModal.notes}
                  onChange={(e) => setEditActivityModal(prev => ({ ...prev, notes: e.target.value }))}
                  rows="3"
                  style={{
                    resize: 'vertical',
                    minHeight: '60px',
                    maxHeight: '120px'
                  }}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeEditActivityModal}>取消</button>
              <button className="modal-btn primary" onClick={confirmEditActivity}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑每日标题弹窗 */}
      {editingDayTitle.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeEditDayTitle();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">编辑每日标题</h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">标题 *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="请输入每日标题"
                  value={editingDayTitle.title}
                  onChange={(e) => setEditingDayTitle(prev => ({ ...prev, title: e.target.value }))}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeEditDayTitle}>取消</button>
              <button className="modal-btn primary" onClick={confirmEditDayTitle}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑时间弹窗 */}
      {editingTime.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeEditTime();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">编辑时间</h3>
            <div className="create-trip-form">
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">起始时间</label>
                  <input
                    type="time"
                    className="form-input"
                    value={editingTime.startTime}
                    onChange={(e) => setEditingTime(prev => ({ ...prev, startTime: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">结束时间</label>
                  <input
                    type="time"
                    className="form-input"
                    value={editingTime.endTime}
                    onChange={(e) => setEditingTime(prev => ({ ...prev, endTime: e.target.value }))}
                  />
                </div>
              </div>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', margin: '8px 0 0 0' }}>
                可以只填写其中一个时间，或者两个都不填写
              </p>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeEditTime}>取消</button>
              <button className="modal-btn primary" onClick={confirmEditTime}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑行程标题弹窗 */}
      {editingTitle.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeEditTitle();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">编辑行程标题</h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">标题 *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="请输入行程标题"
                  value={editingTitle.title}
                  onChange={(e) => setEditingTitle(prev => ({ ...prev, title: e.target.value }))}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeEditTitle}>取消</button>
              <button className="modal-btn primary" onClick={confirmEditTitle}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑预算弹窗 */}
      {editingBudget.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeEditBudget();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">编辑预算</h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">预算金额 (￥) *</label>
                <input
                  type="number"
                  className="form-input"
                  placeholder="请输入预算金额"
                  min="0"
                  step="0.01"
                  value={editingBudget.budget}
                  onChange={(e) => setEditingBudget(prev => ({ ...prev, budget: e.target.value }))}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeEditBudget}>取消</button>
              <button className="modal-btn primary" onClick={confirmEditBudget}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 编辑出行人数弹窗 */}
      {editingTravelerCount.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeEditTravelerCount();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">编辑出行人数</h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">出行人数 *</label>
                <input
                  type="number"
                  className="form-input"
                  placeholder="请输入出行人数"
                  min="1"
                  value={editingTravelerCount.count}
                  onChange={(e) => setEditingTravelerCount(prev => ({ ...prev, count: e.target.value }))}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeEditTravelerCount}>取消</button>
              <button className="modal-btn primary" onClick={confirmEditTravelerCount}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 删除景点确认弹窗 */}
      {deleteConfirmModal.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) handleCancelDeleteAttraction();
        }}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">确认删除</h3>
            <p className="modal-message">确定要删除 "{deleteConfirmModal.attractionName}" 吗？此操作不可撤销。</p>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={handleCancelDeleteAttraction}>取消</button>
              <button className="modal-btn primary" onClick={handleConfirmDeleteAttraction}>删除</button>
            </div>
          </div>
        </div>
      )}

      {/* 错误提示弹窗 */}
      {errorModalVisible && (
            <div className="modal-overlay show" onClick={closeErrorModal}>
          <div className="modal-content modal-w450">
            <h3 className="modal-title">...好像有哪里不对...</h3>
            <p className="modal-message">{errorModal.message}</p>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeErrorModal}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 开始日期选择弹窗 */}
      {startDateModal.show && (
            <div className="modal-overlay show" onClick={(e) => {
          if (e.target === e.currentTarget) closeStartDateModal();
        }}>
          <div className="modal-content modal-w400">
            <h3 className="modal-title">选择新的开始日期</h3>
            <div className="create-trip-form">
              <div className="form-group">
                <label className="form-label">开始日期 *</label>
                <input
                  type="date"
                  className="form-input"
                  value={startDateModal.date}
                  onChange={(e) => setStartDateModal(prev => ({ ...prev, date: e.target.value }))}
                />
              </div>
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={closeStartDateModal}>取消</button>
              <button className="modal-btn primary" onClick={confirmStartDate}>确定</button>
            </div>
          </div>
        </div>
      )}

      {/* 高德地图搜索结果弹窗 */}
      {showAMapModal && (
            <div className="modal-overlay show" onClick={e => { if (e.target === e.currentTarget) setShowAMapModal(false); }}>
          <div className="modal-content modal-w550" style={{maxHeight: '80vh', overflow: 'auto'}}>
            <h3 className="modal-title">高德地图搜索结果</h3>
            <div style={{maxHeight: 400, overflowY: 'auto'}}>
              {amapLoading && <div style={{padding: 20, textAlign: 'center'}}>搜索中...</div>}
              {amapResults.map((result, index) => (
                <div 
                  key={result.id} 
                  className="search-dropdown-item" 
                  style={{cursor: 'pointer', padding: '12px', borderBottom: index < amapResults.length - 1 ? '1px solid #eee' : 'none'}}
                  onClick={() => selectAMapResult(result)}
                >
                  <div style={{fontWeight: '600', color: '#1a2a6c', marginBottom: 4}}>{result.name}</div>
                  <div style={{fontSize: '0.9rem', color: '#666', marginBottom: 2}}>{result.address}</div>
                  <div style={{fontSize: '0.85rem', color: '#888'}}>
                    经纬度：{result.longitude}, {result.latitude}
                  </div>
                  {result.tel && <div style={{fontSize: '0.85rem', color: '#888'}}>电话：{result.tel}</div>}
                </div>
              ))}
            </div>
            <div className="modal-actions">
              <button className="modal-btn secondary" onClick={() => setShowAMapModal(false)}>关闭</button>
            </div>
          </div>
        </div>
      )}

      {/* 路线详情弹窗 */}
      {showRouteDetailModal && routeDetail && (
        <div className="route-detail-modal" onClick={() => setShowRouteDetailModal(false)} style={{position:'fixed',top:0,left:0,right:0,bottom:0,background:'rgba(0,0,0,0.35)',zIndex:9999,display:'flex',alignItems:'center',justifyContent:'center'}}>
          <div className="route-detail-content" onClick={e => e.stopPropagation()} style={{
            background:'#fff',
            borderRadius:16,
            width:420,
            height:520,
            boxShadow:'0 8px 32px rgba(0,0,0,0.18)',
            overflow:'hidden',
            display:'flex',
            flexDirection:'column',
            position:'relative',
            maxWidth:'96vw',
            maxHeight:'96vh',
            padding:0
          }}>
            {/* 关闭按钮固定右上角 */}
            <button onClick={() => setShowRouteDetailModal(false)} style={{
              position:'absolute',
              top:14,
              right:18,
              background:'none',
              border:'none',
              fontSize:'1.7rem',
              color:'#aaa',
              cursor:'pointer',
              zIndex:2,
              borderRadius:'50%',
              width:36,
              height:36,
              display:'flex',
              alignItems:'center',
              justifyContent:'center',
              transition:'background 0.2s,color 0.2s'
            }}
            onMouseOver={e=>e.currentTarget.style.background='#f0f0f0'}
            onMouseOut={e=>e.currentTarget.style.background='none'}
            >×</button>
            {/* 内容区flex:1滚动 */}
            <div style={{flex:1,overflowY:'auto',padding:'32px 28px 0 28px',display:'flex',flexDirection:'column'}}>
              <div style={{fontSize:'1.1rem',color:'#555',marginBottom:8,marginTop:8}}>
                <span>起点：</span><b>{routeDetail.fromName}</b>
                <span style={{margin:'0 10px'}}>-</span>
                <span>终点：</span><b>{routeDetail.toName}</b>
              </div>
              <div style={{fontSize:'1.45rem',fontWeight:700,marginBottom:8,color:'#1976d2',letterSpacing:1}}>
                {routeDetail.mode}推荐路线
              </div>
              <div style={{fontSize:'1.1rem',fontWeight:600,color:'#43a047',marginBottom:12}}>
                推荐用时：{formatDurationToHourMin(routeDetail.time)}
              </div>
              <div className="route-detail-steps" style={{flex:1,minHeight:0,overflowY:'auto',background:'#f8fafc',borderRadius:8,padding:'16px 12px',marginBottom:8}}>
                <div style={{fontWeight:500,marginBottom:8,color:'#1976d2'}}>路线规划：</div>
                {routeDetail.isPublic ? (
                  <ol style={{paddingLeft:20}}>
                    {routeDetail.steps && routeDetail.steps.map((seg, idx) => (
                      <li key={idx} style={{marginBottom:10,lineHeight:1.7}}>
                        {seg.instruction || (seg.transit && seg.transit.instructions) || '...'}
                        {seg.transit && seg.transit.buslines && seg.transit.buslines.length > 0 && (
                          <div style={{color:'#888',fontSize:'0.95em',marginTop:2}}>
                            {seg.transit.buslines.map((bus, i) => (
                              <span key={i}>{bus.name}{i < seg.transit.buslines.length-1 ? ' → ' : ''}</span>
                            ))}
                          </div>
                        )}
                      </li>
                    ))}
                  </ol>
                ) : (
                  <ol style={{paddingLeft:20}}>
                    {routeDetail.steps && routeDetail.steps.map((step, idx) => (
                      <li key={idx} style={{marginBottom:10,lineHeight:1.7}}>{step.instruction}</li>
                    ))}
                  </ol>
                )}
              </div>
            </div>
            {/* 底部操作按钮固定 */}
            <div style={{padding:'0 28px 20px 28px',background:'white',borderTop:'1px solid #eee',display:'flex',justifyContent:'center'}}>
              <button style={{marginTop:8,padding:'10px 0',width:'100%',fontSize:'1.08rem',borderRadius:8,background:'#1976d2',color:'#fff',border:'none',cursor:'pointer',fontWeight:600,letterSpacing:1,boxShadow:'0 2px 8px rgba(25,118,210,0.08)'}} onClick={() => setShowRouteDetailModal(false)}>关闭</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EditItinerary; 