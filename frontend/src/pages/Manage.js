import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import ShareModal from '../components/ShareModal';
import './Manage.css';

const Manage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [shareModalVisible, setShareModalVisible] = useState(false);
  const [createTripModalVisible, setCreateTripModalVisible] = useState(false);
  const [tripToDelete, setTripToDelete] = useState(null);
  const [errorModalVisible, setErrorModalVisible] = useState(false);
  const [errorModal, setErrorModal] = useState({ message: '' });
  const [shareCode, setShareCode] = useState('');
  const [shareDescription, setShareDescription] = useState('');
  const [isGeneratingShare, setIsGeneratingShare] = useState(false);
  const [currentSharingTripId, setCurrentSharingTripId] = useState(null);
  const [shareTip, setShareTip] = useState('');
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);

  // 创建行程表单数据（已简化：不再包含城市数量及名称）
  const [newTripData, setNewTripData] = useState({
    title: '',
    startDate: '',
    endDate: '',
    coverImage: ''
  });
  const [trips, setTrips] = useState([]);
  const [personalTrips, setPersonalTrips] = useState([]);
  const [teamTrips, setTeamTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openMenuId, setOpenMenuId] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);

  // 获取当前用户信息
  const fetchCurrentUser = async () => {
    try {
      const response = await fetch('/api/auth/status', {
        method: 'GET',
        credentials: 'include'
      });

      if (response.ok) {
        const userData = await response.json();
        setCurrentUser(userData);
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  // 检查用户是否可以编辑团队行程
  const canEditTeamItinerary = (trip) => {
    if (!trip.isTeamItinerary) return true; // 个人行程可以编辑
    // 对于团队行程，需要检查是否是发起人
    // 这里暂时返回true，实际应该检查用户权限
    return true;
  };

  // 从后端获取行程列表
  const fetchTrips = async () => {
    try {
      setLoading(true);

      // 并行获取个人行程和团队行程
      const [personalResponse, teamResponse] = await Promise.all([
        fetch('/api/itineraries/user/personal?page=0&size=9', {
          method: 'GET',
          credentials: 'include'
        }),
        fetch('/api/itineraries/user/team?page=0&size=9', {
          method: 'GET',
          credentials: 'include'
        })
      ]);

      if (personalResponse.status === 401 || teamResponse.status === 401) {
        // 用户未登录，重定向到登录页面
        console.log('用户未登录，重定向到登录页面');
        navigate('/login');
        return;
      }

      if (!personalResponse.ok || !teamResponse.ok) {
        // 检查响应是否为JSON
        const contentType = personalResponse.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          const errorData = await personalResponse.json();
          throw new Error(errorData.message || `获取行程列表失败: ${personalResponse.status}`);
        } else {
          console.error('API响应不是JSON格式，可能是HTML错误页面');
          throw new Error('服务器返回了非JSON响应，请检查API服务');
        }
      }

      const personalData = await personalResponse.json();
      const teamData = await teamResponse.json();

      console.log('收到个人行程数据:', personalData);
      console.log('收到团队行程数据:', teamData);

      // 转换个人行程数据为前端格式
      const formattedPersonalTrips = personalData.map(itinerary => {
        const startDate = new Date(itinerary.startDate);
        const endDate = new Date(itinerary.endDate);
        const duration = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;

        return {
          id: itinerary.id,
          title: itinerary.title,
          destination: Array.isArray(itinerary.destinationNames) && itinerary.destinationNames.length > 0
              ? itinerary.destinationNames.join('、')
              : '待规划目的地',
          startDate: itinerary.startDate,
          endDate: itinerary.endDate,
          duration: `${duration}天`,
          places: Array.isArray(itinerary.destinationNames) && itinerary.destinationNames.length > 0
              ? `${itinerary.destinationNames.length}个目的地`
              : '待确定',
          status: itinerary.travelStatus === '待出行' ? 'upcoming' : 'completed',
          visibility: itinerary.permissionStatus === '私人' ? 'private' :
              itinerary.permissionStatus === '仅获得链接者可见' ? 'link' : 'public',
          editStatus: itinerary.editStatus,
          coverImage: itinerary.imageUrl ? `/api/static${itinerary.imageUrl}` : null,
          isTeamItinerary: false,
          isGroupCreator: false,
          groupTitle: null,
          travelerCount: itinerary.travelerCount || 1
        };
      });

      // 转换团队行程数据为前端格式
      const formattedTeamTrips = teamData.map(itinerary => {
        const startDate = new Date(itinerary.startDate);
        const endDate = new Date(itinerary.endDate);
        const duration = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;

        return {
          id: itinerary.id,
          title: itinerary.title,
          destination: Array.isArray(itinerary.destinationNames) && itinerary.destinationNames.length > 0
              ? itinerary.destinationNames.join('、')
              : '待规划目的地',
          startDate: itinerary.startDate,
          endDate: itinerary.endDate,
          duration: `${duration}天`,
          places: Array.isArray(itinerary.destinationNames) && itinerary.destinationNames.length > 0
              ? `${itinerary.destinationNames.length}个目的地`
              : '待确定',
          status: itinerary.travelStatus === '待出行' ? 'upcoming' : 'completed',
          visibility: itinerary.permissionStatus === '私人' ? 'private' :
              itinerary.permissionStatus === '仅获得链接者可见' ? 'link' : 'public',
          editStatus: itinerary.editStatus,
          coverImage: itinerary.imageUrl ? `/api/static${itinerary.imageUrl}` : null,
          isTeamItinerary: true,
          isGroupCreator: itinerary.isGroupCreator || false,
          groupTitle: itinerary.groupTitle,
          groupId: itinerary.groupId,
          travelerCount: itinerary.travelerCount || 1,
          userRole: itinerary.userRole
        };
      });

      // 分别设置个人行程和团队行程
      setPersonalTrips(formattedPersonalTrips);
      setTeamTrips(formattedTeamTrips);
      setTrips(formattedPersonalTrips.concat(formattedTeamTrips)); // 合并显示

      console.log('成功设置行程列表，个人行程:', formattedPersonalTrips.length, '团队行程:', formattedTeamTrips.length);
    } catch (error) {
      console.error('获取行程列表失败:', error);
      setErrorModalVisible(true);
      setErrorModal({
        type: 'error',
        message: error.message || '获取行程列表失败'
      });
    } finally {
      setLoading(false);
    }
  };

  const closeErrorModal = () => {
    setErrorModalVisible(false);
  };

  // 调试功能：测试健康检查
  const testHealthCheck = async () => {
    try {
      console.log('测试健康检查...');
      const response = await fetch('/api/itineraries/health', {
        method: 'GET',
        credentials: 'include'
      });
      const result = await response.json();
      console.log('健康检查结果:', result);
    } catch (error) {
      console.error('健康检查失败:', error);
    }
  };

  // 调试功能：测试用户状态
  const testUserStatus = async () => {
    try {
      console.log('测试用户状态...');
      const response = await fetch('/api/itineraries/test', {
        method: 'GET',
        credentials: 'include'
      });
      const result = await response.json();
      console.log('用户状态测试结果:', result);
    } catch (error) {
      console.error('用户状态测试失败:', error);
    }
  };

  // 页面加载动画和URL参数处理
  useEffect(() => {
    const timer = setTimeout(() => {
      const pageContent = document.querySelector('.page-content');
      if (pageContent) {
        pageContent.classList.add('loaded');
      }
    }, 100);
    
    // 先进行健康检查
    testHealthCheck();

    // 获取用户信息
    fetchCurrentUser();

    // 然后获取行程列表
    fetchTrips();


    // 检查URL参数，如果有action=create，自动打开创建新行程弹窗
    const searchParams = new URLSearchParams(location.search);
    const action = searchParams.get('action');

    if (action === 'create') {
      setCreateTripModalVisible(true);
      // 清除URL参数
      navigate('/manage', { replace: true });
    }

    return () => clearTimeout(timer);
  }, [location.search, navigate]);

  // 创建新行程
  const createNewTrip = () => {
    // 只显示弹窗，不做其他操作
    setCreateTripModalVisible(true);
  };

  // 处理新行程数据变化
  const handleNewTripDataChange = (field, value) => {
    // 如果是封面图片，进行文件大小验证
    if (field === 'coverImage' && value) {
      // 验证文件类型
      const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
      if (!allowedTypes.includes(value.type)) {
        setErrorModalVisible(true);
        setErrorModal({
          message: '请上传 JPG、PNG 或 GIF 格式的图片'
        });
        return;
      }

      // 验证文件大小（不能超过10MB）
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (value.size > maxSize) {
        setErrorModalVisible(true);
        setErrorModal({
          message: '图片文件大小不能超过10MB'
        });
        return;
      }
    }
    
    setNewTripData(prev => ({ ...prev, [field]: value }));
  };

  // 验证表单数据
  const validateNewTripData = () => {
    if (!newTripData.title.trim()) {
      setErrorModalVisible(true);
      setErrorModal({
        message: '请输入行程标题_(:з」∠)_'
      });
      return false;
    }
    if (!newTripData.startDate) {
      setErrorModalVisible(true);
      setErrorModal({
        message: '请选择开始日期'
      });
      return false;
    }
    if (!newTripData.endDate) {
      setErrorModalVisible(true);
      setErrorModal({
        message: '请选择结束日期'
      });
      return false;
    }
    if (new Date(newTripData.startDate) > new Date(newTripData.endDate)) {
      setErrorModalVisible(true);
      setErrorModal({
        message: '结束日期不能早于开始日期'
      });
      return false;
    }
    return true;
  };

  // 确认创建新行程
  const confirmCreateTrip = async () => {
    if (!validateNewTripData()) {
      return;
    }

    // 准备API请求数据
    const requestData = {
      title: newTripData.title,
      startDate: newTripData.startDate,
      endDate: newTripData.endDate,
      budget: null, // 可以后续在编辑页面设置
      travelerCount: 1, // 默认1人
      travelStatus: '待出行', // 默认待出行
      permissionStatus: '私人' // 默认私人
    };

    try {
      // 调用后端API创建行程
      const response = await fetch('/api/itineraries', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 包含session cookie
        body: JSON.stringify(requestData)
      });

      if (response.status === 401) {
        // 用户未登录，重定向到登录页面
        console.log('用户未登录，重定向到登录页面');
        navigate('/login');
        return;
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `创建行程失败: ${response.status}`);
      }

      const createdItinerary = await response.json();

      // 关闭模态框并重置数据
      setCreateTripModalVisible(false);
      setNewTripData({
        title: '',
        startDate: '',
        endDate: '',
        coverImage: ''
      });

      // 刷新行程列表
      await fetchTrips();
      
      // 跳转到编辑页面，使用创建的行程ID
      navigate(`/edit-itinerary/${createdItinerary.id}`);

      // 如果选择了封面图片，单独上传
      if (newTripData.coverImage) {
        const formData = new FormData();
        formData.append('file', newTripData.coverImage);
        try {
          await fetch(`/api/itineraries/${createdItinerary.id}/cover`, {
            method: 'POST',
            credentials: 'include',
            body: formData
          });
        } catch (e) {
          console.error('上传封面失败', e);
        }
      }

    } catch (error) {
      console.error('创建行程失败:', error);
      alert(`创建行程失败: ${error.message}`);
    }
  };

  // 关闭创建新行程模态框
  const closeCreateTripModal = () => {
    setCreateTripModalVisible(false);
    setNewTripData({
      title: '',
      startDate: '',
      endDate: '',
      coverImage: ''
    });
  };

  // 导航到编辑行程页面
  const navigateToEditItinerary = (tripId) => {
    console.log('跳转到编辑页面:', `/edit-itinerary/${tripId}`);
    navigate(`/edit-itinerary/${tripId}`);
  };

  // 导航到浏览行程页面
  const navigateToViewItinerary = (tripId) => {
    navigate(`/view-itinerary/${tripId}`);
  };

  // 处理行程卡片点击
  const handleTripCardClick = (trip) => {
    if (trip.status === 'completed') {
      // 已完成行程只能浏览
      console.log('跳转到浏览页面:', `/view-itinerary/${trip.id}`);
      navigate(`/view-itinerary/${trip.id}`);
    } else {
      // 未完成行程可以编辑（包括团队行程）
      console.log('跳转到编辑页面:', `/edit-itinerary/${trip.id}`);
      navigate(`/edit-itinerary/${trip.id}`);
    }
  };

  // 设置菜单控制  
  const toggleSettingsMenu = (tripId, event) => {
    event.stopPropagation();
    // 先关闭所有其他下拉框
    setOpenMenuId(openMenuId === tripId ? null : tripId);
  };

  // 设置可见性
  const setVisibility = async (tripId, visibility, event) => {
    event.stopPropagation();
    
    // 转换前端可见性状态到后端格式
    const permissionStatus = visibility === 'public' ? '所有人可见' :
        visibility === 'link' ? '仅获得链接者可见' : '私人';

    // 当目标状态为"所有人可见"时，先检查社区中是否已有该行程条目
    if (visibility === 'public') {
      try {
        const checkRes = await fetch(`/api/community/itinerary/${tripId}`, {
          method: 'GET',
          credentials: 'include'
        });
        if (checkRes.status === 404) {
          // 未找到社区条目，需弹出"公开行程"分享弹窗
          setCurrentSharingTripId(tripId);
          setShareTip('所有人都可以在社区内查看您的行程');
          setIsShareModalOpen(true);
          setOpenMenuId(null);
          return;
        }
        // status 200 表示已存在条目，可直接更新权限
      } catch (err) {
        console.error('检查社区条目失败:', err);
        // 出现错误时，默认走分享流程
        setCurrentSharingTripId(tripId);
        setShareTip('所有人都可以在社区内查看您的行程');
        setIsShareModalOpen(true);
        setOpenMenuId(null);
        return;
      }
    }

    try {
      const response = await fetch(`/api/itineraries/${tripId}/permission?permissionStatus=${encodeURIComponent(permissionStatus)}`, {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '更新失败');
      }

      // 更新本地状态
      setTrips(prev => prev.map(trip =>
          trip.id === tripId
              ? { ...trip, visibility: visibility }
              : trip
      ));
      
      // 同时更新个人行程和团队行程的状态
      setPersonalTrips(prev => prev.map(trip =>
          trip.id === tripId
              ? { ...trip, visibility: visibility }
              : trip
      ));
      setTeamTrips(prev => prev.map(trip =>
          trip.id === tripId
              ? { ...trip, visibility: visibility }
              : trip
      ));
      
      console.log(`行程可见性已更新为: ${permissionStatus}`);

      // 如果设为私人，提示用户
      if (visibility === 'private') {
        alert('该行程已设为私人，其他用户将无法查看此行程。');
      }
      
      // 不关闭菜单，让用户看到更新后的状态
      // setOpenMenuId(null);
    } catch (error) {
      console.error('更新行程可见性失败:', error);
      alert('更新行程可见性失败，请重试');
      setOpenMenuId(null);
    }
  };

  const handleShareClick = (tripId, visibility) => {
    setCurrentSharingTripId(tripId);
    if (visibility === '私人') {
      setShareTip('确认分享后，私人行程将变为仅获得链接者可见');
    } else {
      setShareTip('');
    }
    setIsShareModalOpen(true);
    
  };

  const handleShareComplete = async (shareCode) => {
    // 根据分享提示判断是否为"所有人可见"
    const isPublicShare = shareTip === '所有人都可以在社区内查看您的行程';

    if (isPublicShare) {
      // 设置为"所有人可见"
      try {
        const response = await fetch(`/api/itineraries/${currentSharingTripId}/permission?permissionStatus=${encodeURIComponent('所有人可见')}`, {
          method: 'PUT',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || '更新失败');
        }

        // 更新本地状态为"所有人可见"
        setTrips(prevTrips => prevTrips.map(trip => {
          if (trip.id === currentSharingTripId) {
            return {
              ...trip,
              visibility: 'public'
            };
          }
          return trip;
        }));
        console.log('行程可见性已更新为: 所有人可见');
      } catch (error) {
        console.error('更新行程可见性失败:', error);
        alert('更新行程可见性失败，请重试');
      }
    } else {
      // 更新行程列表中的分享状态为"仅获得链接者可见"
      setTrips(prevTrips => prevTrips.map(trip => {
        if (trip.id === currentSharingTripId) {
          return {
            ...trip,
            visibility: 'link'
          };
        }
        return trip;
      }));
    }

    // 不在这里关闭 ShareModal，让用户在弹窗中查看并复制分享码
    // 也不主动复制剪贴板，交由 ShareModal 的复制按钮处理
  };

  // 删除行程
  const deleteTrip = async (tripId, event) => {
    event.stopPropagation();
    setTripToDelete(tripId); // 设置要删除的行程ID
    setDeleteModalVisible(true);
  };

  // 确认删除
  const confirmDelete = async () => {
    if (!tripToDelete) return;

    try {
      const response = await fetch(`/api/itineraries/${tripToDelete}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (response.ok) {
        // 从本地状态中移除已删除的行程
        setTrips(prev => prev.filter(trip => trip.id !== tripToDelete));
        setPersonalTrips(prev => prev.filter(trip => trip.id !== tripToDelete));
        setTeamTrips(prev => prev.filter(trip => trip.id !== tripToDelete));

        console.log(`行程 ${tripToDelete} 删除成功`);
      } else {
        const errorData = await response.json();
        throw new Error(errorData.message || '删除失败');
      }
    } catch (error) {
      console.error('删除行程失败:', error);
      alert(`删除行程失败: ${error.message}`);
    } finally {
      setDeleteModalVisible(false);
      setTripToDelete(null);
    }
  };

  // 切换行程状态
  const toggleTravelStatus = async (tripId, currentStatus, event) => {
    event.stopPropagation();
    
    const newStatus = currentStatus === 'upcoming' ? '已出行' : '待出行';
    
    try {
      const response = await fetch(`/api/itineraries/${tripId}/status?status=${encodeURIComponent(newStatus)}`, {
        method: 'PUT',
        credentials: 'include'
      });

      if (response.ok) {
        // 更新本地状态
        const updateTripStatus = (trip) =>
          trip.id === tripId
            ? { ...trip, status: newStatus === '待出行' ? 'upcoming' : 'completed' }
            : trip;

        setTrips(prev => prev.map(updateTripStatus));
        setPersonalTrips(prev => prev.map(updateTripStatus));
        setTeamTrips(prev => prev.map(updateTripStatus));

        console.log(`行程状态已更新为: ${newStatus}`);
        
        // 如果状态切换为"已出行"，自动添加历史目的地
        if (newStatus === '已出行') {
          try {
            const historyResponse = await fetch('/api/users/destinations/history/auto-add', {
              method: 'POST',
              credentials: 'include'
            });
            
            if (historyResponse.ok) {
              const historyResult = await historyResponse.json();
              if (historyResult.success && historyResult.addedCount > 0) {
              } else {
                console.log('历史目的地添加结果:', historyResult.message);
              }
            } else {
              console.error('自动添加历史目的地失败:', historyResponse.status);
            }
          } catch (historyError) {
            console.error('自动添加历史目的地时发生错误:', historyError);
            // 不影响主要功能，只记录错误
          }
        }
        
        // 如果状态切换为"待出行"，删除该行程自动添加的历史目的地
        if (newStatus === '待出行') {
          try {
            const removeResponse = await fetch(`/api/users/destinations/history/auto-remove/${tripId}`, {
              method: 'DELETE',
              credentials: 'include'
            });
            
            if (removeResponse.ok) {
              const removeResult = await removeResponse.json();
              if (removeResult.success && removeResult.removedCount > 0) {
                console.log(`删除历史目的地结果: ${removeResult.message}`);
              } else {
                console.log('删除历史目的地结果:', removeResult.message);
              }
            } else {
              console.error('自动删除历史目的地失败:', removeResponse.status);
            }
          } catch (removeError) {
            console.error('自动删除历史目的地时发生错误:', removeError);
            // 不影响主要功能，只记录错误
          }
        }
      } else {
        throw new Error('更新失败');
      }
    } catch (error) {
      console.error('更新行程状态失败:', error);
      alert('更新行程状态失败，请重试');
    }
  };

  // 关闭删除模态框
  const closeDeleteModal = () => {
    setDeleteModalVisible(false);
    setTripToDelete(null);
  };

  // 关闭分享模态框
  const closeShareModal = () => {
    setShareModalVisible(false);
  };

  useEffect(() => {
    // 重置所有卡片的dropdown-open类
    document.querySelectorAll('.trip-card.dropdown-open').forEach(card => {
      card.classList.remove('dropdown-open');
    });
    
    if (openMenuId !== null) {
      const card = document.querySelector(`.trip-card[data-id='${openMenuId}']`);
      const btn = card?.querySelector('.trip-settings');
      // const menu = menuRefs.current[openMenuId]; // menuRefs is removed

      if (btn && card) { // menuRefs is removed
        // 给当前卡片添加dropdown-open类
        card.classList.add('dropdown-open');
        
        const btnRect = btn.getBoundingClientRect();
        const cardRect = card.getBoundingClientRect();
        
        // 设置样式：菜单定位在齿轮图标正下方，与卡片右边缘对齐
        // menu.style.position = 'absolute'; // menuRefs is removed
        // menu.style.top = `${btnRect.bottom - cardRect.top}px`; // menuRefs is removed
        // menu.style.right = '28px'; // menuRefs is removed
        // menu.style.left = 'auto'; // menuRefs is removed
        // menu.style.display = 'block'; // menuRefs is removed
        // menu.style.zIndex = '100'; // menuRefs is removed
      }
    }

    // 隐藏其他菜单
    // Object.keys(menuRefs.current).forEach(id => { // menuRefs is removed
    //   if (parseInt(id) !== openMenuId && menuRefs.current[id]) { // menuRefs is removed
    //     menuRefs.current[id].style.display = 'none'; // menuRefs is removed
    //   }
    // }); // menuRefs is removed
  }, [openMenuId]);

  // 处理菜单鼠标离开事件
  const handleMenuMouseLeave = () => {
    setOpenMenuId(null);
  };

  return (
      <div className="page-content active" id="manage-page">
        <div className="page-header">
          <div className="manage-header">
            <div>
              <h1 className="manage-page-title">管理您的行程</h1>
            </div>
            <div>
              <p className="manage-page-subtitle">过去、现在与未来的所有旅行计划</p>
            </div>
            <div className="action-buttons">

            <button className="create-trip-btn" onClick={createNewTrip}>
              <span className="btn-icon">➕</span>
              <span>创建新行程</span>
            </button>
            </div>

          </div>
        </div>



        <div className="personal-itineraries-section">
          <h2 className="section-title">个人行程</h2>
          <div className="trips-grid">
          {loading ? (
              <div className="text-center p-2rem" style={{ gridColumn: '1 / -1' }}>
                <p>加载中...</p>
                <div className="mt-1rem">
                  <button onClick={testHealthCheck} className="btn-small mr-10">
                    测试健康检查
                  </button>
                  <button onClick={testUserStatus} className="btn-small">
                    测试用户状态
                  </button>
                </div>
              </div>
          ) : personalTrips.length === 0 ? (
              <div className="text-center p-2rem" style={{ gridColumn: '1 / -1' }}>
                <p>还没有个人行程，点击"创建新行程"开始规划吧！</p>
              </div>
          ) : (
              personalTrips.map((trip, index) => (
                  <div
                      key={trip.id}
                      className="trip-card"
                      data-id={trip.id}
                      onClick={() => handleTripCardClick(trip)}
                      style={{ cursor: 'pointer' }}
                      data-status={trip.status}
                  >
                    {/* 封面图片 */}
                    <div className="trip-cover">
                      {trip.coverImage ? (
                          <img
                              src={trip.coverImage}
                              alt={trip.title}
                              className="trip-cover-image"
                              style={{
                                width: '100%',
                                height: '160px',
                                objectFit: 'cover',
                                borderRadius: '8px 8px 0 0'
                              }}
                              onError={(e) => {
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'flex';
                              }}
                          />
                      ) : (
                          <div
                              className="trip-cover-placeholder"
                              style={{
                                width: '100%',
                                height: '160px',
                                backgroundColor: 'var(--accent-color)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: '8px 8px 0 0',
                                color: 'var(--text-secondary)',
                                fontSize: '2rem'
                              }}
                          >
                            🗺️
                          </div>
                      )}
                      {trip.coverImage && (
                          <div
                              className="trip-cover-placeholder"
                              style={{
                                width: '100%',
                                height: '160px',
                                backgroundColor: 'var(--accent-color)',
                                display: 'none',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: '8px 8px 0 0',
                                color: 'var(--text-secondary)',
                                fontSize: '2rem'
                              }}
                          >
                            🗺️
                          </div>
                      )}
                    </div>
                    <div className="trip-header">
                      <div
                          className={`trip-status ${trip.status}`}
                          onClick={e => {
                            e.stopPropagation();
                            toggleTravelStatus(trip.id, trip.status, e);
                          }}
                          style={{
                            cursor: 'pointer',
                            transition: 'all 0.3s ease',
                            position: 'relative'
                          }}
                          title={`点击切换为${trip.status === 'completed' ? '待出行' : '已出行'}`}
                      >
                        {trip.status === 'completed' ? '已出行' : '待出行'}
                        <span style={{
                          fontSize: '0.7rem',
                          marginLeft: '4px',
                          opacity: '0.7'
                        }}>
                  ↻
                </span>
                      </div>
                      <div
                          className="trip-settings"
                          onClick={e => toggleSettingsMenu(trip.id, e)}
                      >
                        ⚙️
                      </div>
                      <div
                          className="settings-menu"
                          style={{ display: openMenuId === trip.id ? 'block' : 'none' }}
                          onMouseLeave={handleMenuMouseLeave}
                      >
                        <div className="menu-section">
                          <div className="menu-title">可见性</div>
                          <div
                              className={`menu-item ${trip.visibility === 'public' ? 'active' : ''}`}
                              onClick={(e => setVisibility(trip.id, 'public', e))}
                          >
                            <span className="check-icon">{trip.visibility === 'public' ? '✓' : ''}</span>
                            所有人可见
                          </div>
                          <div
                              className={`menu-item ${trip.visibility === 'link' ? 'active' : ''}`}
                              onClick={(e => setVisibility(trip.id, 'link', e))}
                          >
                            <span className="check-icon">{trip.visibility === 'link' ? '✓' : ''}</span>
                            仅获得链接者可见
                          </div>
                          <div
                              className={`menu-item ${trip.visibility === 'private' ? 'active' : ''}`}
                              onClick={(e => setVisibility(trip.id, 'private', e))}
                          >
                            <span className="check-icon">{trip.visibility === 'private' ? '✓' : ''}</span>
                            私人
                          </div>
                        </div>
                        <div className="menu-divider"></div>
                        {trip.visibility !== 'public' && (
                            <div className="menu-item" onClick={e => { e.stopPropagation();
                              handleShareClick(trip.id, trip.visibility); }}>
                              <span className="menu-icon">📤</span>分享
                            </div>
                        )}
                        <div className="menu-item delete" onClick={e => { e.stopPropagation(); deleteTrip(trip.id, e); }}>
                          <span className="menu-icon">🗑️</span>删除
                        </div>
                      </div>
                    </div>
                    <div className="trip-dates">
                      <div className="date-range">
                        <span className="date-label">开始</span>
                        <span className="date-value">{trip.startDate}</span>
                      </div>
                      <div className="date-range">
                        <span className="date-label">结束</span>
                        <span className="date-value">{trip.endDate}</span>
                      </div>
                    </div>
                    <h3 className="trip-title">{trip.title}</h3>
                    <div className="trip-destination">{trip.destination}</div>
                    <div className="trip-meta">
                      <span className="trip-places">{trip.places}</span>
                      <span className="trip-travelers" style={{ fontSize: '0.8rem', color: '#666', marginTop: '4px' }}>
                        👥 {trip.travelerCount || 1}人出行
                      </span>
                      <span className="trip-action-hint fs-12" style={{ color: '#666', marginTop: '8px' }}>
                {trip.status === 'completed' ? '点击查看行程详情 👁️' : '点击编辑行程 ✏️'}
              </span>
                    </div>
                  </div>
              ))
          )}
          </div>
        </div>

        {/* 团队行程部分 */}
        <div className="team-itineraries-section" style={{ marginTop: '2rem' }}>
          <h2 className="section-title">团队行程</h2>
          <div className="trips-grid">
          {loading ? (
              <div className="text-center p-2rem" style={{ gridColumn: '1 / -1' }}>
                <p>加载中...</p>
              </div>
          ) : teamTrips.length === 0 ? (
              <div className="text-center p-2rem" style={{ gridColumn: '1 / -1' }}>
                <p>还没有参与任何团队行程，快去加入一个旅游团吧！</p>
              </div>
          ) : (
              teamTrips.map((trip, index) => (
                  <div
                      key={trip.id}
                      className="trip-card"
                      data-id={trip.id}
                      onClick={() => handleTripCardClick(trip)}
                      style={{ cursor: 'pointer' }}
                      data-status={trip.status}
                  >
                    {/* 封面图片 */}
                    <div className="trip-cover">
                      {trip.coverImage ? (
                          <img
                              src={trip.coverImage}
                              alt={trip.title}
                              className="trip-cover-image"
                              style={{
                                width: '100%',
                                height: '160px',
                                objectFit: 'cover',
                                borderRadius: '8px 8px 0 0'
                              }}
                              onError={(e) => {
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'flex';
                              }}
                          />
                      ) : (
                          <div
                              className="trip-cover-placeholder"
                              style={{
                                width: '100%',
                                height: '160px',
                                backgroundColor: 'var(--accent-color)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: '8px 8px 0 0',
                                color: 'var(--text-secondary)',
                                fontSize: '2rem'
                              }}
                          >
                            🗺️
                          </div>
                      )}
                      {trip.coverImage && (
                          <div
                              className="trip-cover-placeholder"
                              style={{
                                width: '100%',
                                height: '160px',
                                backgroundColor: 'var(--accent-color)',
                                display: 'none',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: '8px 8px 0 0',
                                color: 'var(--text-secondary)',
                                fontSize: '2rem'
                              }}
                          >
                            🗺️
                          </div>
                      )}
                    </div>
                    <div className="trip-header">
                      <div
                          className={`trip-status ${trip.status}`}
                          onClick={e => {
                            e.stopPropagation();
                            toggleTravelStatus(trip.id, trip.status, e);
                          }}
                          style={{
                            cursor: 'pointer',
                            transition: 'all 0.3s ease',
                            position: 'relative'
                          }}
                          title={`点击切换为${trip.status === 'completed' ? '待出行' : '已出行'}`}
                      >
                        {trip.status === 'completed' ? '已出行' : '待出行'}
                        <span style={{
                          fontSize: '0.7rem',
                          marginLeft: '4px',
                          opacity: '0.7'
                        }}>
                  ↻
                </span>
                      </div>
                      <div
                          className="trip-settings"
                          onClick={e => toggleSettingsMenu(trip.id, e)}
                      >
                        ⚙️
                      </div>
                      <div
                          className="settings-menu"
                          style={{ display: openMenuId === trip.id ? 'block' : 'none' }}
                          onMouseLeave={handleMenuMouseLeave}
                      >
                        {trip.isTeamItinerary && (
                          <div className="menu-section">
                            <div className="menu-title" style={{ color: '#667eea', fontSize: '0.8rem' }}>
                              团队行程 - {trip.groupTitle}
                            </div>
                            {!trip.isGroupCreator && (
                              <div style={{ fontSize: '0.7rem', color: '#999', marginTop: '4px' }}>
                                仅发起人可修改权限
                              </div>
                            )}
                          </div>
                        )}
                        <div className="menu-section">
                          <div className="menu-title">可见性</div>
                          <div
                              className={`menu-item ${trip.visibility === 'public' ? 'active' : ''} ${trip.isTeamItinerary && !trip.isGroupCreator ? 'disabled' : ''}`}
                              onClick={trip.isTeamItinerary && !trip.isGroupCreator ? null : (e => setVisibility(trip.id, 'public', e))}
                              style={{
                                opacity: trip.isTeamItinerary && !trip.isGroupCreator ? 0.5 : 1,
                                cursor: trip.isTeamItinerary && !trip.isGroupCreator ? 'not-allowed' : 'pointer'
                              }}
                          >
                            <span className="check-icon">{trip.visibility === 'public' ? '✓' : ''}</span>
                            所有人可见
                          </div>
                          <div
                              className={`menu-item ${trip.visibility === 'link' ? 'active' : ''} ${trip.isTeamItinerary && !trip.isGroupCreator ? 'disabled' : ''}`}
                              onClick={trip.isTeamItinerary && !trip.isGroupCreator ? null : (e => setVisibility(trip.id, 'link', e))}
                              style={{
                                opacity: trip.isTeamItinerary && !trip.isGroupCreator ? 0.5 : 1,
                                cursor: trip.isTeamItinerary && !trip.isGroupCreator ? 'not-allowed' : 'pointer'
                              }}
                          >
                            <span className="check-icon">{trip.visibility === 'link' ? '✓' : ''}</span>
                            仅获得链接者可见
                          </div>
                          <div
                              className={`menu-item ${trip.visibility === 'private' ? 'active' : ''} ${trip.isTeamItinerary && !trip.isGroupCreator ? 'disabled' : ''}`}
                              onClick={trip.isTeamItinerary && !trip.isGroupCreator ? null : (e => setVisibility(trip.id, 'private', e))}
                              style={{
                                opacity: trip.isTeamItinerary && !trip.isGroupCreator ? 0.5 : 1,
                                cursor: trip.isTeamItinerary && !trip.isGroupCreator ? 'not-allowed' : 'pointer'
                              }}
                          >
                            <span className="check-icon">{trip.visibility === 'private' ? '✓' : ''}</span>
                            私人
                          </div>
                        </div>
                        <div className="menu-divider"></div>
                        {trip.visibility !== 'public' && trip.isGroupCreator && (
                            <div className="menu-item" onClick={e => { e.stopPropagation();
                              handleShareClick(trip.id, trip.visibility); }}>
                              <span className="menu-icon">📤</span>分享
                            </div>
                        )}
                        {trip.isGroupCreator ? (
                          <div className="menu-item delete" onClick={e => { e.stopPropagation(); deleteTrip(trip.id, e); }}>
                            <span className="menu-icon">🗑️</span>删除
                          </div>
                        ) : (
                          <div className="menu-item disabled" style={{ opacity: 0.5, cursor: 'not-allowed' }}>
                            <span className="menu-icon">🗑️</span>删除（仅发起人可删除）
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="trip-dates">
                      <div className="date-range">
                        <span className="date-label">开始</span>
                        <span className="date-value">{trip.startDate}</span>
                      </div>
                      <div className="date-range">
                        <span className="date-label">结束</span>
                        <span className="date-value">{trip.endDate}</span>
                      </div>
                    </div>
                    <h3 className="trip-title">
                      {trip.title}
                      {trip.isTeamItinerary && (
                        <span className="team-badge" style={{
                          marginLeft: '8px',
                          padding: '2px 6px',
                          backgroundColor: '#667eea',
                          color: 'white',
                          fontSize: '0.7rem',
                          borderRadius: '4px',
                          fontWeight: 'normal'
                        }}>
                          团队行程
                        </span>
                      )}
                      {trip.userRole && (
                        <span className="role-badge" style={{
                          marginLeft: '8px',
                          padding: '2px 6px',
                          backgroundColor: trip.userRole === '创建者' ? '#ff6b6b' : '#51cf66',
                          color: 'white',
                          fontSize: '0.7rem',
                          borderRadius: '4px',
                          fontWeight: 'normal'
                        }}>
                          {trip.userRole}
                        </span>
                      )}
                    </h3>
                    <div className="trip-destination">{trip.destination}</div>
                    <div className="trip-meta">
                      <span className="trip-places">{trip.places}</span>
                      <span className="trip-travelers" style={{ fontSize: '0.8rem', color: '#666', marginTop: '4px' }}>
                        👥 {trip.travelerCount || 1}人出行
                      </span>
                      {trip.groupTitle && (
                        <div style={{ fontSize: '0.8rem', color: '#667eea', marginTop: '4px' }}>
                          来自团队：{trip.groupTitle}
                        </div>
                      )}
                      <span className="trip-action-hint fs-12" style={{ color: '#666', marginTop: '8px' }}>
                {trip.status === 'completed' ? '点击查看行程详情 👁️' : '点击编辑行程 ✏️'}
              </span>
                    </div>
                  </div>
              ))
          )}
        </div>
        </div>

        {/* 删除确认模态框 */}
        {deleteModalVisible && (
            <div className="manage-modal-overlay show" id="delete-modal">
              <div className="modal-content">
                <h3 className="modal-title">确认删除</h3>
                <p className="modal-message">确定要删除这个行程吗？此操作无法撤销。</p>
                <div className="modal-actions">
                  <button className="modal-btn secondary" onClick={closeDeleteModal}>取消</button>
                  <button className="modal-btn primary" onClick={confirmDelete}>删除</button>
                </div>
              </div>
            </div>
        )}

        {/* 创建新行程模态框 */}
        {createTripModalVisible && (
            <div className="modal-overlay show" id="create-trip-modal">
              <div className="modal-content create-trip-modal">
                <h3 className="modal-title">创建新行程</h3>
                <div className="create-trip-form">
                  <div className="form-group">
                    <label className="form-label">行程标题 *</label>
                    <input
                        type="text"
                        className="form-input"
                        placeholder="请输入行程标题"
                        value={newTripData.title}
                        onChange={(e) => handleNewTripDataChange('title', e.target.value)}
                    />
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">开始日期 *</label>
                      <input
                          type="date"
                          className="form-input"
                          value={newTripData.startDate}
                          onChange={(e) => handleNewTripDataChange('startDate', e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">结束日期 *</label>
                      <input
                          type="date"
                          className="form-input"
                          value={newTripData.endDate}
                          onChange={(e) => handleNewTripDataChange('endDate', e.target.value)}
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="form-label">行程封面 *</label>
                    <input
                        type="file"
                        className="form-input"
                        accept="image/*"
                        onChange={(e) => handleNewTripDataChange('coverImage', e.target.files[0])}
                    />
                  </div>
                </div>
                <div className="modal-actions">
                  <button className="modal-btn secondary" onClick={closeCreateTripModal}>取消</button>
                  <button className="modal-btn primary" onClick={confirmCreateTrip}>确定创建</button>
                </div>
              </div>
            </div>
        )}

        {/* 时间冲突提示弹窗 */}
        {errorModalVisible && (
            <div className="modal-overlay show" onClick={closeErrorModal}>
              <div className="modal-content modal-w450">
                <h3 className="modal-title">...好像有哪里不对( ˙ε . )？</h3>
                <p className="modal-message">{errorModal.message}</p>
                <div className="modal-actions">
                  <button className="modal-btn secondary" onClick={closeErrorModal}>确定</button>
                </div>
              </div>
            </div>
        )}

        {/* 分享模态框 */}
        <ShareModal
            isOpen={isShareModalOpen}
            onClose={() => {
              setIsShareModalOpen(false);
              setCurrentSharingTripId(null);
              setShareTip('');
            }}
            onShare={handleShareComplete}
            itineraryId={currentSharingTripId}
            shareTip={shareTip}
        />
      </div>
  );
};

export default Manage; 