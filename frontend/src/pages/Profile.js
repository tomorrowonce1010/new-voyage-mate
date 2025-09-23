import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AIRecommendationModal from '../components/AIRecommendationModal';
import ItineraryPlanModal from '../components/ItineraryPlanModal';
import DestinationModal from '../components/DestinationModal';
import FootprintModal from '../components/FootprintModal';
import './Profile.css';

const Profile = ({ user, onLogout, onUpdateUser }) => {
  const navigate = useNavigate();
  
  // 用户信息状态
  const [userProfile, setUserProfile] = useState({
    id: null,
    username: '',
    email: '',
    avatarUrl: '',
    birthday: '',
    signature: '',
    bio: '',
    specialRequirementsDescription: '',
    travelPreferences: [],
    specialRequirements: [],
    historyDestinations: [],
    wishlistDestinations: []
  });
  
  const [travelPreferences, setTravelPreferences] = useState([]);
  const [selectedSpecialNeeds, setSelectedSpecialNeeds] = useState([]);
  const [sectionsExpanded, setSectionsExpanded] = useState({
    destinations: false,
    wishlist: false
  });
  const [activeMenus, setActiveMenus] = useState({});
  const [preferenceText, setPreferenceText] = useState('');
  const [specialRequirementsText, setSpecialRequirementsText] = useState('');
  const [aiRecommendation, setAiRecommendation] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  
  // AI功能相关状态
  const [showAIModal, setShowAIModal] = useState(false);
  const [showItineraryModal, setShowItineraryModal] = useState(false);
  const [aiModalContent, setAiModalContent] = useState('');
  const [aiModalTitle, setAiModalTitle] = useState('AI智能推荐');
  const [isAILoading, setIsAILoading] = useState(false);
  const [selectedDestination, setSelectedDestination] = useState('');
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [showWishlistModal, setShowWishlistModal] = useState(false);
  const [showDeleteConfirmModal, setShowDeleteConfirmModal] = useState(false);
  const [deleteConfirmData, setDeleteConfirmData] = useState(null);
  const [deleteSuccessMessage, setDeleteSuccessMessage] = useState('');
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('');

  // 足迹相关状态
  const [showFootprintModal, setShowFootprintModal] = useState(false);
  const [showAddFootprintModal, setShowAddFootprintModal] = useState(false);

  // 期望目的地弹窗相关状态
  const [showWishlistViewModal, setShowWishlistViewModal] = useState(false);

  // 关闭所有菜单的函数
  const closeAllMenus = () => {
    setActiveMenus({});
  };

  const handleAvatarClick = () => {
    // 创建一个隐藏的文件输入元素
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = uploadAvatar;
    input.click();
  };

  // 上传头像
  const uploadAvatar = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    // 验证文件类型
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(file.type)) {
      alert('请上传 JPG、PNG 或 GIF 格式的图片');
      return;
    }

    // 验证文件大小（限制为2MB）
    if (file.size > 2 * 1024 * 1024) {
      alert('图片文件大小不能超过2MB');
      return;
    }

    const formData = new FormData();
    formData.append('avatar', file);

    try {
      const response = await fetch('http://localhost:8080/api/users/avatar', {
        method: 'POST',
        credentials: 'include',
        body: formData
      });

      if (response.ok) {
        const data = await response.json();

        // 更新本地状态
        setUserProfile(prev => ({ ...prev, avatarUrl: data.avatarUrl }));

        // 同步更新App.js中的用户状态，以更新右上角用户名显示
        if (onUpdateUser) {
          onUpdateUser({ avatarUrl: data.avatarUrl });
        }

        alert('头像上传成功！');
      } else {
        console.error('上传失败，状态码:', response.status);
        try {
          const errorData = await response.json();
          console.error('错误详情:', errorData);
          alert(errorData.message || '上传头像失败，请重试');
        } catch (parseError) {
          console.error('解析错误响应失败:', parseError);
          alert('上传头像失败，服务器响应异常');
        }
      }
    } catch (error) {
      console.error('上传头像网络错误:', error);
      alert('上传头像失败，网络连接异常');
    }

    // 清空input
    if (event.target) {
      event.target.value = '';
    }
  };

  // 获取用户档案数据
  const fetchUserProfile = async () => {
    try {
      setIsLoading(true);
      const response = await fetch('http://localhost:8080/api/users/profile', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        if (response.status === 401) {
          navigate('/login');
          return;
        }
        throw new Error('获取用户档案失败');
      }

      const data = await response.json();
      setUserProfile(data);
      setTravelPreferences(data.travelPreferences || []);
      setSelectedSpecialNeeds(data.specialRequirements || []);
      setSpecialRequirementsText(data.specialRequirementsDescription || '');
    } catch (error) {
      console.error('获取用户档案失败:', error);
      // 设置默认值
      setUserProfile({
        ...userProfile,
        username: '用户',
        email: 'user@example.com',
        specialRequirementsDescription: '',
        travelPreferences: [],
        specialRequirements: [],
        historyDestinations: [],
        wishlistDestinations: []
      });
      setSpecialRequirementsText('');
    } finally {
      setIsLoading(false);
    }
  };

  // 更新用户档案
  const updateUserProfile = async (updates) => {
    try {
      const response = await fetch('http://localhost:8080/api/users/profile', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(updates),
      });

      if (!response.ok) {
        throw new Error('更新用户档案失败');
      }

      const data = await response.json();
      setUserProfile(data);
      return data;
    } catch (error) {
      console.error('更新用户档案失败:', error);
      throw error;
    }
  };

  // 更新用户偏好
  const updateUserPreferences = async (preferences) => {
    try {
      const response = await fetch('http://localhost:8080/api/users/preferences', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          travelPreferences: preferences.travelPreferences,
          specialRequirements: preferences.specialRequirements,
          specialRequirementsDescription: preferences.specialRequirementsDescription,
        }),
      });

      if (!response.ok) {
        throw new Error('更新用户偏好失败');
      }

      const data = await response.json();
      setUserProfile(data);
      return data;
    } catch (error) {
      console.error('更新用户偏好失败:', error);
      throw error;
    }
  };

  // 初始化时获取用户档案
  useEffect(() => {
    fetchUserProfile();
  }, []);

  // 点击外部关闭菜单
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest('.wishlist-actions')) {
        closeAllMenus();
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  // 切换偏好标签
  const toggleTag = async (tagId) => {
    // 更新本地状态
    const newTravelPreferences = travelPreferences.map(pref => {
      if (pref.tagId === tagId) {
        return { ...pref, selected: !pref.selected };
      }
      return pref;
    });
    
    setTravelPreferences(newTravelPreferences);
    
    try {
      // 构建JSON格式的偏好数据
      const preferencesJson = {};
      newTravelPreferences.forEach(pref => {
        preferencesJson[pref.tagId.toString()] = pref.selected ? 1 : 0;
      });
      
      await updateUserPreferences({
        travelPreferences: JSON.stringify(preferencesJson),
        specialRequirements: JSON.stringify(selectedSpecialNeeds),
        specialRequirementsDescription: userProfile.specialRequirementsDescription,
      });
    } catch (error) {
      // 如果更新失败，回滚状态
      setTravelPreferences(travelPreferences);
      alert('更新偏好失败，请重试');
    }
  };

  // 更新特殊需求描述
  const updateSpecialRequirementsDescription = async (description) => {
    try {
      const preferencesJson = {};
      travelPreferences.forEach(pref => {
        preferencesJson[pref.tagId.toString()] = pref.selected ? 1 : 0;
      });

      await updateUserPreferences({
        travelPreferences: JSON.stringify(preferencesJson),
        specialRequirements: JSON.stringify(selectedSpecialNeeds),
        specialRequirementsDescription: description,
      });
    } catch (error) {
      console.error('更新特殊需求描述失败:', error);
      throw error;
    }
  };

  // 切换特殊需求标签
  const toggleSpecialTag = async (tag) => {
    const newSpecialNeeds = selectedSpecialNeeds.includes(tag) 
      ? selectedSpecialNeeds.filter(t => t !== tag)
      : [...selectedSpecialNeeds, tag];
    
    setSelectedSpecialNeeds(newSpecialNeeds);
    
    try {
      // 构建JSON格式的偏好数据
      const preferencesJson = {};
      travelPreferences.forEach(pref => {
        preferencesJson[pref.tagId.toString()] = pref.selected ? 1 : 0;
      });
      
      await updateUserPreferences({
        travelPreferences: JSON.stringify(preferencesJson),
        specialRequirements: JSON.stringify(newSpecialNeeds),
        specialRequirementsDescription: userProfile.specialRequirementsDescription,
      });
    } catch (error) {
      // 如果更新失败，回滚状态
      setSelectedSpecialNeeds(selectedSpecialNeeds);
      alert('更新特殊需求失败，请重试');
    }
  };

  // 切换区域展开/折叠
  const toggleSection = (section) => {
    setSectionsExpanded(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  // 编辑字段功能
  const editField = async (element, type) => {
    // 检查元素是否存在
    if (!element) {
      console.error('editField: element is null');
      return;
    }

    // 检查元素是否有textContent属性
    if (!element.textContent) {
      console.error('editField: element does not have textContent');
      return;
    }

    const currentText = element.textContent.replace('✏️', '').trim();

    // 检查父元素是否存在
    if (!element.parentElement) {
      console.error('editField: element parentElement is null');
      return;
    }

    const labelElement = element.parentElement.querySelector('.detail-label');
    if (!labelElement) {
      console.error('editField: detail-label not found');
      return;
    }

    const fieldName = labelElement.textContent;
    const fieldKey = element.getAttribute('data-field');
    
    // 创建输入框进行原地编辑
    let inputElement;
    if (type === 'textarea') {
      inputElement = document.createElement('textarea');
      inputElement.style.width = '100%';
      inputElement.style.minHeight = '60px';
      inputElement.style.resize = 'vertical';
    } else {
      inputElement = document.createElement('input');
      if (type === 'date') {
        inputElement.type = 'date';
        // 将日期格式转换为YYYY-MM-DD
        if (currentText && currentText !== '未设置') {
          inputElement.value = currentText;
        }
      } else {
        inputElement.type = 'text';
      }
    }

    if (type !== 'date') {
      inputElement.value = currentText === '未设置' ? '' : currentText;
    }
    
    inputElement.style.border = '1px solid #ddd';
    inputElement.style.padding = '8px';
    inputElement.style.borderRadius = '4px';
    inputElement.style.fontSize = '14px';
    inputElement.style.fontFamily = 'inherit';

    // 保存原始内容
    const originalContent = element.innerHTML;

    // 替换为输入框
    element.innerHTML = '';
    element.appendChild(inputElement);

    // 创建按钮容器
    const buttonContainer = document.createElement('div');
    buttonContainer.style.marginTop = '8px';
    buttonContainer.style.display = 'flex';
    buttonContainer.style.gap = '8px';

    const saveButton = document.createElement('button');
    saveButton.textContent = '保存';
    saveButton.style.padding = '4px 12px';
    saveButton.style.backgroundColor = '#93CCC3FF';
    saveButton.style.color = 'white';
    saveButton.style.border = 'none';
    saveButton.style.borderRadius = '4px';
    saveButton.style.cursor = 'pointer';

    const cancelButton = document.createElement('button');
    cancelButton.textContent = '取消';
    cancelButton.style.padding = '4px 12px';
    cancelButton.style.backgroundColor = '#ccc';
    cancelButton.style.color = 'black';
    cancelButton.style.border = 'none';
    cancelButton.style.borderRadius = '4px';
    cancelButton.style.cursor = 'pointer';

    buttonContainer.appendChild(saveButton);
    buttonContainer.appendChild(cancelButton);
    element.appendChild(buttonContainer);

    // 聚焦到输入框
    inputElement.focus();
    if (type === 'text' || type === 'textarea') {
      inputElement.select();
    }

    // 保存功能
    const saveChanges = async () => {
      const newValue = inputElement.value.trim();

      if (newValue !== currentText && (newValue !== '' || currentText !== '未设置')) {
        try {
          let updateData = {};

          if (fieldKey === 'username') {
            updateData.username = newValue;
          } else if (fieldKey === 'birthday') {
            updateData.birthday = newValue;
          } else if (fieldKey === 'signature') {
            updateData.signature = newValue;
          } else if (fieldKey === 'bio') {
            updateData.bio = newValue;
          } else if (fieldKey === 'specialRequirements') {
            // 更新特殊需求描述
            const preferencesJson = {};
            travelPreferences.forEach(pref => {
              preferencesJson[pref.tagId.toString()] = pref.selected ? 1 : 0;
            });

            await updateUserPreferences({
              travelPreferences: JSON.stringify(preferencesJson),
              specialRequirements: JSON.stringify(selectedSpecialNeeds),
              specialRequirementsDescription: newValue,
            });

            element.innerHTML = (newValue || '未设置') + '<span class="edit-icon">✏️</span>';
            return;
          }

          if (Object.keys(updateData).length > 0) {
            await updateUserProfile(updateData);
            element.innerHTML = (newValue || '未设置') + '<span class="edit-icon">✏️</span>';

            // 如果更新的是用户名，需要更新页面其他地方显示的用户名
            if (fieldKey === 'username') {
              setUserProfile(prev => ({ ...prev, username: newValue }));
              // 同步更新App.js中的用户状态，以更新右上角用户名显示
              if (onUpdateUser) {
                onUpdateUser({ username: newValue });
              }
            }
          }
        } catch (error) {
          alert('更新失败，请重试');
          console.error('更新字段失败:', error);
          element.innerHTML = originalContent;
        }
      } else {
        // 没有改变，恢复原内容
        element.innerHTML = originalContent;
      }
    };

    // 取消功能
    const cancelChanges = () => {
      element.innerHTML = originalContent;
    };

    // 绑定事件
    saveButton.onclick = saveChanges;
    cancelButton.onclick = cancelChanges;

    // 按Enter保存（对于文本输入框）
    if (type !== 'textarea') {
      inputElement.onkeydown = (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          saveChanges();
        } else if (e.key === 'Escape') {
          e.preventDefault();
          cancelChanges();
        }
      };
    } else {
      // 对于textarea，Ctrl+Enter保存
      inputElement.onkeydown = (e) => {
        if (e.key === 'Enter' && e.ctrlKey) {
          e.preventDefault();
          saveChanges();
        } else if (e.key === 'Escape') {
          e.preventDefault();
          cancelChanges();
        }
      };
    }
  };

  // 切换愿望目的地菜单
  const toggleWishlistMenu = (itemId) => {
    setActiveMenus(prev => ({
      ...prev,
      [itemId]: !prev[itemId]
    }));
  };

  // 创建旅行计划
  const createTravelPlan = (city) => {
    closeAllMenus();
    // 跳转到manage页面并触发创建新行程弹窗
    navigate('/manage?action=create&city=' + encodeURIComponent(city));
  };

  // 获取AI建议
  const getAIRecommendation = (city) => {
    setSelectedDestination(city);
    setShowItineraryModal(true);
    closeAllMenus();
  };

  // 处理行程规划确认
  const handleItineraryPlanConfirm = async (planData) => {
    setShowItineraryModal(false);
    setIsAILoading(true);
    setShowAIModal(true);
    setAiModalTitle(`${planData.destination} ${planData.days}日游 AI定制行程`);
    
    try {
      const selectedTags = travelPreferences.filter(pref => pref.selected).map(pref => pref.tagName);
      const requestData = {
        destination: planData.destination,
        days: planData.days,
        travelers: planData.travelers,
        budget: planData.budget,
        travelPreferences: selectedTags,
        specialNeeds: selectedSpecialNeeds,
        requestType: 'itinerary'
      };
      
      const response = await callAIAPI('/ai/itinerary-plan', requestData);
      
      if (response.success) {
        setAiModalContent(response.content);
      } else {
        setAiModalContent('生成行程失败：' + response.errorMessage);
      }
    } catch (error) {
      // 降级到本地行程生成
      const fallbackItinerary = generateFallbackItinerary(planData);
      setAiModalContent(fallbackItinerary);
    } finally {
      setIsAILoading(false);
    }
  };

  // 本地降级行程生成
  const generateFallbackItinerary = (planData) => {
    const { destination, days, travelers, budget } = planData;
    let itinerary = `📍 ${destination} ${days}日游详细行程\n\n`;
    
    for (let day = 1; day <= days; day++) {
      itinerary += `📅 **第${day}天**\n\n`;
      
      if (day === 1) {
        itinerary += `🌅 **上午 (9:00-12:00)**\n`;
        itinerary += `• 抵达${destination}，前往酒店办理入住\n`;
        itinerary += `• 市中心漫步，感受城市氛围\n\n`;
        
        itinerary += `🍽️ **中午 (12:00-14:00)**\n`;
        itinerary += `• 品尝当地特色午餐\n`;
        itinerary += `• 推荐餐厅：当地知名老字号\n\n`;
        
        itinerary += `🌆 **下午 (14:00-18:00)**\n`;
        itinerary += `• 参观主要景点（根据偏好安排）\n`;
        itinerary += `• 体验当地文化活动\n\n`;
        
        itinerary += `🌃 **晚上 (18:00-22:00)**\n`;
        itinerary += `• 夜游著名景点或商业街\n`;
        itinerary += `• 品尝夜市小吃\n\n`;
      } else if (day === days) {
        itinerary += `🌅 **上午 (9:00-12:00)**\n`;
        itinerary += `• 最后一次游览，购买纪念品\n`;
        itinerary += `• 整理行李，准备返程\n\n`;
        
        itinerary += `🛫 **中午后**\n`;
        itinerary += `• 前往机场/车站\n`;
        itinerary += `• 结束愉快的${destination}之旅\n\n`;
      } else {
        itinerary += `🌅 **上午 (9:00-12:00)**\n`;
        itinerary += `• 深度游览特色景点\n`;
        itinerary += `• 体验当地特色活动\n\n`;
        
        itinerary += `🍽️ **中午 (12:00-14:00)**\n`;
        itinerary += `• 当地特色美食体验\n\n`;
        
        itinerary += `🌆 **下午 (14:00-18:00)**\n`;
        itinerary += `• 继续景点游览\n`;
        itinerary += `• 文化体验或休闲活动\n\n`;
        
        itinerary += `🌃 **晚上 (18:00-22:00)**\n`;
        itinerary += `• 当地夜生活体验\n`;
        itinerary += `• 特色晚餐\n\n`;
      }
      
      itinerary += `💰 **预估花费：¥${Math.round(budget / days)}/天**\n\n`;
      itinerary += `---\n\n`;
    }
    
    itinerary += `👥 **${travelers}人出行建议：**\n`;
    itinerary += `• 建议穿着舒适的鞋子\n`;
    itinerary += `• 随身携带雨具和防晒用品\n`;
    itinerary += `• 保持手机电量充足\n`;
    itinerary += `• 尊重当地风俗习惯\n`;
    itinerary += `• 总预算：¥${budget}，人均：¥${Math.round(budget / travelers)}`;
    
    return itinerary;
  };

  // 删除愿望目的地
  const removeWishlist = async (destinationId, city) => {
    setShowDeleteConfirmModal(true);
    setDeleteConfirmData({ destinationId, city });
  };

  // 确认删除愿望目的地
  const confirmDeleteWishlist = async () => {
    if (!deleteConfirmData) return;

    const { destinationId, city } = deleteConfirmData;

    try {
      const response = await fetch(`http://localhost:8080/api/users/destinations/wishlist/${destinationId}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '删除失败');
      }

      // 直接更新本地状态，避免重新获取整个用户档案
      setUserProfile(prev => ({
        ...prev,
        wishlistDestinations: prev.wishlistDestinations.filter(
          dest => dest.destinationId !== destinationId
        )
      }));

      setDeleteSuccessMessage(`${city} 已从愿望清单中删除`);

      // 3秒后自动隐藏成功消息
      setTimeout(() => {
        setDeleteSuccessMessage('');
      }, 3000);
    } catch (error) {
      console.error('删除愿望目的地失败:', error);
      setDeleteErrorMessage('删除失败：' + error.message);

      // 3秒后自动隐藏错误消息
      setTimeout(() => {
        setDeleteErrorMessage('');
      }, 3000);
    } finally {
      setShowDeleteConfirmModal(false);
      setDeleteConfirmData(null);
      closeAllMenus();
    }
  };

  // 取消删除
  const cancelDeleteWishlist = () => {
    setShowDeleteConfirmModal(false);
    setDeleteConfirmData(null);
  };

  // 添加目的地
  const addDestination = async (formData) => {
    try {
      const response = await fetch('http://localhost:8080/api/users/destinations/history', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: formData.name,
          description: `历史目的地：${formData.name}`,
          days: parseInt(formData.days),
          notes: formData.notes,
          startDate: formData.startDate,
          endDate: formData.endDate,
        }),
      });

      if (!response.ok) {
        throw new Error('添加失败');
      }

      // 直接更新本地状态，避免重新获取整个用户档案
      const newHistoryItem = {
        destinationId: Date.now(), // 临时ID，实际应该从后端返回
        name: formData.name,
        description: `历史目的地：${formData.name}`,
        imageUrl: null,
        days: parseInt(formData.days),
        notes: formData.notes,
        startDate: formData.startDate,
        endDate: formData.endDate
      };

      setUserProfile(prev => ({
        ...prev,
        historyDestinations: [...prev.historyDestinations, newHistoryItem]
      }));

      setShowHistoryModal(false);
      setDeleteSuccessMessage(`已添加历史目的地: ${formData.name}`);

      // 3秒后自动隐藏消息
      setTimeout(() => {
        setDeleteSuccessMessage('');
      }, 3000);
    } catch (error) {
      console.error('添加历史目的地失败:', error);
      setDeleteErrorMessage('添加失败，请重试');

      // 3秒后自动隐藏错误消息
      setTimeout(() => {
        setDeleteErrorMessage('');
      }, 3000);
    }
  };

  // 添加期望目的地
  const addWishlist = async (formData) => {
    try {
      const response = await fetch('http://localhost:8080/api/users/destinations/wishlist', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: formData.name,
          description: '',
          notes: formData.notes,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '添加失败');
      }

      const responseData = await response.json();

      // 直接更新本地状态，避免重新获取整个用户档案
      const newWishlistItem = {
        destinationId: Date.now(), // 临时ID，实际应该从后端返回
        name: formData.name,
        description: '',
        imageUrl: null,
        notes: formData.notes
      };

      setUserProfile(prev => ({
        ...prev,
        wishlistDestinations: [...prev.wishlistDestinations, newWishlistItem]
      }));

      setShowWishlistModal(false);
      setDeleteSuccessMessage(`已添加期望目的地: ${formData.name}`);

      // 3秒后自动隐藏消息
      setTimeout(() => {
        setDeleteSuccessMessage('');
      }, 3000);
    } catch (error) {
      console.error('添加期望目的地失败:', error);
      const errorMessage = error.message.includes('已在您的列表中') ? error.message : '添加失败，请重试';
      setDeleteErrorMessage(errorMessage);

      // 3秒后自动隐藏错误消息
      setTimeout(() => {
        setDeleteErrorMessage('');
      }, 3000);
    }
  };

  // 退出登录
  const logout = async () => {
    if (window.confirm('确定要退出登录吗？')) {
      try {
        // 调用后端登出接口
        await fetch('http://localhost:8080/api/auth/logout', {
          method: 'POST',
          credentials: 'include',
        });
      } catch (error) {
        console.error('登出请求失败:', error);
      } finally {
        // 调用父组件的登出处理函数（清除前端状态）
        if (onLogout) {
          onLogout();
        }
        // 导航到登录页面
        navigate('/login');
      }
    }
  };

  // API调用函数
  const callAIAPI = async (url, requestData) => {
    try {
      const response = await fetch(`http://localhost:8080/api${url}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(requestData)
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('API调用失败:', error);
      throw error;
    }
  };

  // 生成AI推荐
  const generateAIRecommendation = async () => {
    setIsAILoading(true);
    setShowAIModal(true);
    setAiModalTitle('AI个人档案分析推荐');
    
    try {
      const selectedTags = travelPreferences.filter(pref => pref.selected).map(pref => pref.tagName);
      const requestData = {
        travelPreferences: selectedTags,
        specialNeeds: selectedSpecialNeeds,
        naturalLanguageDescription: preferenceText || '',
        historicalDestinations: userProfile.historyDestinations?.map(dest => dest.name) || [],
        wishlistDestinations: userProfile.wishlistDestinations?.map(dest => dest.name) || [],
        requestType: 'general'
      };
      
      const response = await callAIAPI('/ai/profile-recommendation', requestData);
      
      if (response.success) {
        setAiModalContent(response.content);
      } else {
        setAiModalContent('生成推荐失败：' + response.errorMessage);
      }
    } catch (error) {
      // 降级到本地推荐
      const fallbackRecommendations = [
        "🎯 基于您的个人档案，为您推荐以下目的地：\n\n🏞️ **张家界** - 壮美的自然奇观\n推荐理由：世界自然遗产，奇峰异石，云雾缭绕\n最佳时间：4-6月、9-11月\n\n🏛️ **西安** - 千年古都风华\n推荐理由：十三朝古都，兵马俑、古城墙等历史遗迹丰富\n最佳时间：3-5月、9-11月\n\n🍜 **成都** - 美食天堂\n推荐理由：川菜发源地，火锅、串串、小吃应有尽有\n最佳时间：3-6月、9-11月\n\n💡 **贴心提示：**\n• 建议提前1-2个月预订机票和住宿\n• 关注当地天气变化，准备合适衣物\n• 下载离线地图，确保网络畅通\n• 购买旅行保险，保障出行安全",
        
        "📊 **AI个性化分析报告**\n\n🔍 您的旅行画像：\n• 偏好类型：" + (travelPreferences.filter(p => p.selected).map(p => p.tagName).slice(0,3).join('、') || '综合型') + "旅行者\n• 出行风格：深度体验，注重品质\n• 特殊需求：" + (selectedSpecialNeeds.join('、') || '无特殊需求') + "\n\n🎯 为您推荐：\n1. 青海湖环线 - 自然风光+户外徒步\n2. 西藏拉萨 - 历史足迹+心灵体验\n3. 新疆喀纳斯 - 小众秘境+摄影天堂\n\n🗓️ 建议行程天数：5-7天\n💰 预算范围：¥8,000-12,000\n\n📋 个性化建议：\n根据您的偏好标签，建议选择包含" + (travelPreferences.filter(p => p.selected).map(p => p.tagName)[0] || '自然风光') + "元素的目的地，这样的行程会更符合您的兴趣！"
      ];
      
      const randomRecommendation = fallbackRecommendations[Math.floor(Math.random() * fallbackRecommendations.length)];
      setAiModalContent(randomRecommendation);
    } finally {
      setIsAILoading(false);
    }
  };

  // 足迹相关函数
  const handleFootprintClick = () => {
    setShowFootprintModal(true);
  };

  const handleFootprintModalClose = () => {
    setShowFootprintModal(false);
  };

  const handleManualAddFootprint = () => {
    setShowFootprintModal(false);
    setShowAddFootprintModal(true); // 使用新的足迹添加弹框
  };

  const handleGenerateTravelReport = () => {
    setShowFootprintModal(false);
    // 跳转到旅行报告页面
    navigate('/travel-report');
  };

  // 添加足迹函数
  const addFootprint = async (formData) => {
    try {
      // 计算游玩天数
      const startDate = new Date(formData.startDate);
      const endDate = new Date(formData.endDate);
      const days = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;

      const requestData = {
        name: formData.name,
        description: formData.description || '',
        startDate: formData.startDate,
        endDate: formData.endDate,
        days: days
      };

      const response = await fetch('http://localhost:8080/api/users/destinations/history', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      });

      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          alert('足迹添加成功！');
          // 重新获取用户档案数据以更新显示
          await fetchUserProfile();
        } else {
          alert(data.message || '添加足迹失败');
        }
      } else {
        const errorData = await response.json();
        alert(errorData.message || '添加足迹失败');
      }
    } catch (error) {
      console.error('添加足迹失败:', error);
      alert('添加足迹失败，请重试');
    }
  };

  // 处理期望目的地弹窗
  const handleWishlistViewClick = () => {
    setShowWishlistViewModal(true);
  };

  const handleWishlistViewModalClose = () => {
    setShowWishlistViewModal(false);
  };

  if (isLoading) {
    return (
      <div className="profile-container">
        <div className="loading-container">
          <p>加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      {/* 页面头部 */}
      <header className="page-header">
        <h1 className="profile-page-title">个人档案</h1>
        <p className="profile-page-subtitle">关于您的旅行者画像</p>
      </header>

      {/* 个人信息和偏好区域 */}
      <div className="profile-main-section">
        {/* 个人信息 */}
        <div className="profile-info-section">
          <div className="profile-header">
            <div className="profile-avatar" onClick={handleAvatarClick}>
              {userProfile.avatarUrl ? (<img
                 src={ `/api${userProfile.avatarUrl}`}
                 alt="用户头像"
                 className="avatar-image"
               />):(<div className="avatar-auto-generated">
                {userProfile.username ? userProfile.username.charAt(0) : '用'}
              </div>
               )}
               <div className="avatar-overlay">
                 <span>点击上传头像</span>
               </div>
             </div>
            <div className="profile-basic">
              <div className="name-and-logout">
                <h2>{userProfile.username || '用户'}</h2>
                <button className="logout-btn" onClick={logout}>
                  退出登录
                </button>
              </div>
              <p className="user-title">{userProfile.signature || '探索世界的脚步'}</p>
            </div>
          </div>
          
          <div className="profile-details">
            <div className="detail-item">
              <span className="detail-label">用户名</span>
              <span className="detail-value editable" onClick={(e) => editField(e.currentTarget, 'text')} data-field="username">
                {userProfile.username || '用户'}
                <span className="edit-icon">✏️</span>
              </span>
            </div>
            <div className="detail-item">
              <span className="detail-label">邮箱</span>
              <span className="detail-value">
                {userProfile.email || 'user@example.com'}
              </span>
            </div>
            <div className="detail-item">
              <span className="detail-label">生日</span>
              <span className="detail-value editable" onClick={(e) => editField(e.currentTarget, 'date')} data-field="birthday">
                {userProfile.birthday || '未设置'}
                <span className="edit-icon">✏️</span>
              </span>
            </div>
            <div className="detail-item">
              <span className="detail-label">个性签名</span>
              <span className="detail-value editable" onClick={(e) => editField(e.currentTarget, 'text')} data-field="signature">
                {userProfile.signature || '未设置'}
                <span className="edit-icon">✏️</span>
              </span>
            </div>
            <div className="detail-item bio-item">
              <span className="detail-label">个人简介</span>
              <p className="detail-value bio-text editable" onClick={(e) => editField(e.currentTarget, 'textarea')} data-field="bio">
                {userProfile.bio || '未设置'}
                <span className="edit-icon">✏️</span>
              </p>
            </div>
            <div className="detail-item special-needs-item">
              <span className="detail-label">特殊需求</span>
              <div className="special-needs-tags">
                {['母婴室', '无障碍', '婴幼儿设施', '残疾人友好', '老年人友好', '医疗急救'].map(tag => (
                  <span 
                    key={tag}
                    className={`special-tag ${selectedSpecialNeeds.includes(tag) ? 'active' : ''}`}
                    onClick={() => toggleSpecialTag(tag)}
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>
            <div className="detail-item special-requirements-item">
              <span className="detail-label">特殊需求描述</span>
              <div className="special-requirements-input-section">
                <textarea
                  className="special-requirements-textarea"
                  placeholder="请详细描述您的特殊需求，如无障碍设施、母婴室、医疗急救等..."
                  rows="3"
                  value={specialRequirementsText}
                  onChange={(e) => setSpecialRequirementsText(e.target.value)}
                  onBlur={() => updateSpecialRequirementsDescription(specialRequirementsText)}
                />
              </div>
            </div>
          </div>
        </div>

        {/* 分隔线 */}
        <div className="section-divider"></div>

        {/* 偏好标签 */}
        <div className="preference-section">
          <h3 className="section-title">
            <i>🏷️</i>
            旅行偏好
          </h3>
          <div className="preference-tags">
            {travelPreferences.map(preference => (
              <span 
                key={preference.tagId}
                className={`tag ${preference.selected ? 'active' : ''}`}
                onClick={() => toggleTag(preference.tagId)}
              >
                {preference.tagName}
              </span>
            ))}
          </div>
          <div className="preference-input-section">
            <label htmlFor="preference-textarea" className="preference-input-label">
              您这次有怎样的旅行需求？(●´∀｀●)
            </label>
            <textarea 
              id="preference-textarea" 
              className="preference-textarea" 
              placeholder="例如：我最近有点累(-ωก̀ )，这个假期想去海边放松一下..." 
              rows="3"
              value={preferenceText}
              onChange={(e) => setPreferenceText(e.target.value)}
            />
            <button 
              className="filter-btn ai-recommend-btn" 
              onClick={generateAIRecommendation}
            >
              根据个人档案生成AI个性化建议
            </button>

          </div>
        </div>
      </div>

      {/* 我的足迹和期望目的地按钮 */}
      <div className="main-buttons-container">
        <button className="wishlist-btn" onClick={handleWishlistViewClick}>
          <i>⭐</i>
          期望目的地
        </button>
        <button className="footprint-btn" onClick={handleFootprintClick}>
          <i>👣</i>
          我的足迹
        </button>
      </div>

      {/* 足迹选项弹框 */}
      {showFootprintModal && (
        <div className="profile-modal-overlay" onClick={handleFootprintModalClose}>
          <div className="modal-content footprint-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>我的足迹</h3>
              <button className="close-btn" onClick={handleFootprintModalClose}>×</button>
            </div>
            <div className="modal-body">
              <div className="footprint-options">
                <div className="footprint-option" onClick={handleManualAddFootprint}>
                  <div className="option-icon">📍</div>
                  <div className="option-content">
                    <h4>手动添加足迹</h4>
                    <p>记录您去过的地方</p>
                  </div>
                  <div className="option-arrow">→</div>
                </div>
                <div className="footprint-option" onClick={handleGenerateTravelReport}>
                  <div className="option-icon">📊</div>
                  <div className="option-content">
                    <h4>生成旅行报告</h4>
                    <p>查看您的旅行统计数据</p>
                  </div>
                  <div className="option-arrow">→</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 期望目的地弹窗 */}
      {showWishlistViewModal && (
        <div className="profile-modal-overlay" onClick={handleWishlistViewModalClose}>
          <div className="modal-content wishlist-view-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>期望目的地</h3>
              <div className="header-actions">
                <button className="add-destination-btn" onClick={() => {
                  setShowWishlistViewModal(false);
                  setShowWishlistModal(true);
                }}>
                  ➕
                </button>
                <button className="close-btn" onClick={handleWishlistViewModalClose}>×</button>
              </div>
            </div>
            <div className="modal-body">
              <div className="wishlist-list">
                {userProfile.wishlistDestinations && userProfile.wishlistDestinations.length > 0 ? (
                  userProfile.wishlistDestinations.map((destination, index) => (
                    <div key={destination.destinationId || index} className="wishlist-list-item">
                      <div className="destination-info">
                        <h4>{destination.name}</h4>
                        {destination.notes && <p className="destination-notes">{destination.notes}</p>}
                      </div>
                      <div className="destination-actions">
                        <button className="action-btn" onClick={() => toggleWishlistMenu(destination.destinationId)}>⋯</button>
                        <div className={`action-menu ${activeMenus[destination.destinationId] ? 'show' : ''}`}>
                          <div className="action-item" onClick={() => {
                            setShowWishlistViewModal(false);
                            createTravelPlan(destination.name);
                          }}>
                            <i>📅</i>
                            <span>创建旅行计划</span>
                          </div>
                          <div className="action-item" onClick={() => {
                            setShowWishlistViewModal(false);
                            getAIRecommendation(destination.name);
                          }}>
                            <i>🤖</i>
                            <span>AI智能个性化建议</span>
                          </div>
                          <div className="menu-divider"></div>
                          <div className="action-item" onClick={() => {
                            setShowWishlistViewModal(false);
                            removeWishlist(destination.destinationId, destination.name);
                          }}>
                            <i>🗑️</i>
                            <span>删除</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="empty-wishlist">
                    <div className="empty-icon">⭐</div>
                    <p>您还没有添加期望目的地</p>
                    <button className="add-first-btn" onClick={() => {
                      setShowWishlistViewModal(false);
                      setShowWishlistModal(true);
                    }}>
                      添加第一个目的地
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* AI推荐弹框 */}
      <AIRecommendationModal
        isOpen={showAIModal}
        onClose={() => setShowAIModal(false)}
        content={aiModalContent}
        title={aiModalTitle}
        isLoading={isAILoading}
      />

      {/* 行程规划配置弹框 */}
      <ItineraryPlanModal
        isOpen={showItineraryModal}
        onClose={() => setShowItineraryModal(false)}
        onConfirm={handleItineraryPlanConfirm}
        destination={selectedDestination}
      />

      {/* 添加历史目的地弹窗 */}
      <DestinationModal
        isOpen={showHistoryModal}
        onClose={() => setShowHistoryModal(false)}
        onSubmit={addDestination}
        type="history"
      />

      {/* 添加期望目的地弹窗 */}
      <DestinationModal
        isOpen={showWishlistModal}
        onClose={() => setShowWishlistModal(false)}
        onSubmit={addWishlist}
        type="wishlist"
      />

      {/* 添加足迹弹窗 */}
      <FootprintModal
        isOpen={showAddFootprintModal}
        onClose={() => setShowAddFootprintModal(false)}
        onSubmit={addFootprint}
      />

      {/* 删除确认弹窗 */}
      {showDeleteConfirmModal && deleteConfirmData && (
        <div className="profile-modal-overlay" onClick={cancelDeleteWishlist}>
          <div className="modal-content delete-confirm-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>确认删除</h3>
              <button className="close-btn" onClick={cancelDeleteWishlist}>×</button>
            </div>
            <div className="modal-body">
              <div className="delete-confirm-icon">🗑️</div>
              <p>确定要删除 <strong>{deleteConfirmData.city}</strong> 吗？</p>
              <p className="delete-warning">此操作无法撤销</p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={cancelDeleteWishlist}>
                取消
              </button>
              <button className="btn btn-danger" onClick={confirmDeleteWishlist}>
                确认删除
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 成功/错误消息提示 */}
      {deleteSuccessMessage && (
        <div className="message-toast success-toast">
          <div className="toast-content">
            <span className="toast-icon">✅</span>
            <span>{deleteSuccessMessage}</span>
            <button className="toast-close" onClick={() => setDeleteSuccessMessage('')}>×</button>
          </div>
        </div>
      )}

      {deleteErrorMessage && (
        <div className="message-toast error-toast">
          <div className="toast-content">
            <span className="toast-icon">❌</span>
            <span>{deleteErrorMessage}</span>
            <button className="toast-close" onClick={() => setDeleteErrorMessage('')}>×</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profile; 