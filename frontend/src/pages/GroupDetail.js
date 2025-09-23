import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ApplicantModal from '../components/ApplicantModal';
import './GroupDetail.css';

const GroupDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [loading, setLoading] = useState(true);
  const [applications, setApplications] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [isCreator, setIsCreator] = useState(false);
  const [applicationMessage, setApplicationMessage] = useState('');
  const [showApplicationModal, setShowApplicationModal] = useState(false);
  const [userApplicationStatus, setUserApplicationStatus] = useState(null);
  const [applicationsLoading, setApplicationsLoading] = useState(false);
  const [selectedPreferences, setSelectedPreferences] = useState([]);
  const [travelPreferences, setTravelPreferences] = useState([]);
  const [preferencesLoading, setPreferencesLoading] = useState(false);
  const [showApplicantModal, setShowApplicantModal] = useState(false);
  const [selectedApplication, setSelectedApplication] = useState(null);
  const [selectedApplicationStatus, setSelectedApplicationStatus] = useState('pending'); // 添加申请状态筛选
  const [groupItinerary, setGroupItinerary] = useState(null);
  const [itineraryLoading, setItineraryLoading] = useState(false);

  // 获取状态显示文本和样式
  const getStatusDisplay = (status, group) => {
    // 如果组团已满员，显示"已满员"状态
    if (group && group.currentMembers >= group.maxMembers && status === '招募中') {
      return { text: '已满员', class: 'full' };
    }
    
    const statusMap = {
      '招募中': { text: '招募中', class: 'recruiting' },
      '已满员': { text: '已满员', class: 'full' },
      '已出行': { text: '已出行', class: 'traveling' },
      '已结束': { text: '已结束', class: 'ended' },
      '已取消': { text: '已截止', class: 'cancelled' }
    };
    return statusMap[status] || { text: status, class: 'default' };
  };

  // 更新组团状态
  const updateGroupStatus = async (newStatus) => {
    try {
      const response = await fetch(`/api/group-travel/${id}/status`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus })
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setGroup(result.data);
          console.log('组团状态已更新为:', newStatus);
        }
      } else {
        console.error('状态更新失败:', await response.text());
      }
    } catch (error) {
      console.error('状态更新出错:', error);
    }
  };

  // 检查是否可以更新状态
  const canUpdateStatus = (currentStatus) => {
    return isCreator && (
      (currentStatus === '已满员') || 
      (currentStatus === '招募中')
    );
  };

  // 处理申请项点击事件
  const handleApplicationItemClick = (application) => {
    // 仅在待处理状态下弹出
    if (application.status === 'pending') {
      setSelectedApplication(application);
      setShowApplicantModal(true);
    }
  };

  // 关闭申请人模态框
  const handleApplicantModalClose = () => {
    setShowApplicantModal(false);
    setSelectedApplication(null);
  };

  // 在申请人模态框中同意申请
  const handleApplicantApprove = async (applicationId) => {
    await handleApplicationAction(applicationId, 'approve');
    handleApplicantModalClose();
    // 刷新组团详情数据
    loadGroupDetail();
  };

  // 在申请人模态框中拒绝申请
  const handleApplicantReject = async (applicationId) => {
    await handleApplicationAction(applicationId, 'reject');
    handleApplicantModalClose();
    // 刷新组团详情数据
    loadGroupDetail();
  };

  // 处理申请状态筛选
  const handleApplicationStatusFilter = (status) => {
    setSelectedApplicationStatus(status);
  };

  // 获取团队行程
  const loadGroupItinerary = async () => {
    // 移除发起人权限检查，所有团队成员都应该能看到团队行程
    setItineraryLoading(true);
    try {
      const response = await fetch(`/api/group-travel/${id}/itinerary`, {
        method: 'GET',
        credentials: 'include'
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setGroupItinerary(result.data);
        }
      } else {
        console.error('获取团队行程失败:', await response.text());
      }
    } catch (error) {
      console.error('获取团队行程出错:', error);
    } finally {
      setItineraryLoading(false);
    }
  };

  // 创建团队行程
  const handleCreateGroupItinerary = async () => {
    if (!group) return;
    
    setItineraryLoading(true);
    try {
      const itineraryData = {
        title: `${group.title} - 团队行程`,
        startDate: group.startDate,
        endDate: group.endDate,
        budget: group.estimatedBudget,
        travelerCount: group.maxMembers,
        permissionStatus: '所有人可见'
      };

      const response = await fetch(`/api/group-travel/${id}/itinerary`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(itineraryData)
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setGroupItinerary(result.data);
        }
      } else {
        const errorData = await response.json();
        alert('创建团队行程失败：' + (errorData.message || '未知错误'));
      }
    } catch (error) {
      console.error('创建团队行程出错:', error);
      alert('创建团队行程失败，请稍后重试');
    } finally {
      setItineraryLoading(false);
    }
  };

  // 查看团队行程
  const handleViewGroupItinerary = () => {
    if (groupItinerary) {
      // 添加来源参数，用于返回按钮
      navigate(`/edit-itinerary/${groupItinerary.id}?from=group&groupId=${id}`);
    }
  };

  // 根据状态筛选申请列表
  const getFilteredApplications = () => {
    if (!applications || applications.length === 0) return [];
    
    const statusMap = {
      'pending': '待审核',
      'approved': '已同意',
      'rejected': '已拒绝'
    };
    
    return applications.filter(application => application.status === statusMap[selectedApplicationStatus]);
  };

  // 获取各状态的申请数量
  const getApplicationStats = () => {
    if (!applications || applications.length === 0) {
      return { pending: 0, approved: 0, rejected: 0 };
    }
    
    const stats = {
      pending: applications.filter(app => app.status === '待审核').length,
      approved: applications.filter(app => app.status === '已同意').length,
      rejected: applications.filter(app => app.status === '已拒绝').length
    };
    
    return stats;
  };



  useEffect(() => {
    const loadData = async () => {
      await loadCurrentUser();
      await loadGroupDetail();
      await loadGroupItinerary();
    };
    loadData();
  }, [id]);

  useEffect(() => {
    // 当currentUser和group都存在时，检查是否为创建者并加载申请
    if (currentUser && group) {
      const isCreatorUser = group.creator.id === currentUser.id;
      console.log('当前用户ID:', currentUser.id, '组团创建者ID:', group.creator.id, '是否为创建者:', isCreatorUser);
      setIsCreator(isCreatorUser);
      if (isCreatorUser) {
        console.log('用户是创建者，开始加载申请列表');
        loadApplications();
      } else {
        // 只有在没有刚刚提交申请时才检查，避免覆盖刚设置的pending状态
        // 移除未使用的justSubmittedApplication
      }
    }
  }, [currentUser, group]);

  // 移除定时刷新，改为在需要时手动刷新

  const loadCurrentUser = async () => {
    try {
      const response = await fetch('/api/auth/status', {
        method: 'GET',
        credentials: 'include',
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setCurrentUser({ id: result.userId });
        }
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  const loadTravelPreferences = async () => {
    setPreferencesLoading(true);
    try {
      const response = await fetch('/api/tags', {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success && Array.isArray(result.data)) {
          setTravelPreferences(result.data);
        } else {
          setTravelPreferences([
            '自然风光', '历史足迹', '文化体验', '购物探店', '娱乐休闲',
            '冒险刺激', '摄影天堂', '艺术巡礼', '美食寻味', '户外徒步',
            '海岛度假', '心灵疗愈', '毕业旅行', '亲子游玩', '背包独行',
            '自驾路线', '网红热点', '小众秘境', '避暑胜地', '城市漫步',
            '田园民俗', '江南园林', '大漠风光', '温泉养生', '冰雪世界',
            '古迹村落', '考古博物', '极限运动', '浪漫之旅', '建筑奇观'
          ]);
        }
      }
    } catch (error) {
      console.error('获取标签失败:', error);
      setTravelPreferences([
        '自然风光', '历史足迹', '文化体验', '购物探店', '娱乐休闲',
        '冒险刺激', '摄影天堂', '艺术巡礼', '美食寻味', '户外徒步',
        '海岛度假', '心灵疗愈', '毕业旅行', '亲子游玩', '背包独行',
        '自驾路线', '网红热点', '小众秘境', '避暑胜地', '城市漫步',
        '田园民俗', '江南园林', '大漠风光', '温泉养生', '冰雪世界',
        '古迹村落', '考古博物', '极限运动', '浪漫之旅', '建筑奇观'
      ]);
    } finally {
      setPreferencesLoading(false);
    }
  };

  const loadGroupDetail = async () => {
    try {
      const response = await fetch(`/api/group-travel/${id}`, {
        method: 'GET',
        credentials: 'include',
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setGroup(result.data);
        }
      } else {
        alert('获取组团详情失败');
        navigate('/group-travel');
      }
    } catch (error) {
      console.error('获取组团详情失败:', error);
      alert('获取组团详情失败');
      navigate('/group-travel');
    } finally {
      setLoading(false);
    }
  };

  const loadApplications = async () => {
    console.log('开始加载申请列表...');
    setApplicationsLoading(true);
    try {
      const response = await fetch(`/api/group-travel/${id}/applications`, {
        method: 'GET',
        credentials: 'include',
      });

      console.log('申请列表响应状态:', response.status);
      if (response.ok) {
        const result = await response.json();
        console.log('申请列表结果:', result);
        if (result.success) {
          setApplications(result.data);
          const stats = result.data.reduce((acc, app) => {
            acc[app.status] = (acc[app.status] || 0) + 1;
            return acc;
          }, { pending: 0, approved: 0, rejected: 0 });
          console.log('申请统计:', stats);
          // 移除未使用的applicationStats
        }
      } else {
        console.error('申请列表请求失败:', response.status);
      }
    } catch (error) {
      console.error('获取申请列表失败:', error);
    } finally {
      setApplicationsLoading(false);
    }
  };

  const handleApplicationSubmit = async () => {
    if (!applicationMessage.trim()) {
      alert('请填写申请理由');
      return;
    }

    try {
      const response = await fetch(`/api/group-travel/${id}/apply`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationMessage: applicationMessage,
          preferences: selectedPreferences.join(',')
        })
      });

      if (response.ok) {
        setShowApplicationModal(false);
        setApplicationMessage('');
        setSelectedPreferences([]);
        setUserApplicationStatus('pending');
        // 移除未使用的justSubmittedApplication
        
        // 延迟刷新数据，并在一定时间后重置标记
        setTimeout(() => {
          loadGroupDetail();
          // 如果是创建者，也刷新申请列表
          if (isCreator) {
            loadApplications();
          }
          // 3秒后重置标记，允许后续的状态检查
          // 移除未使用的justSubmittedApplication
        }, 500);
      } else {
        const result = await response.json();
        alert(result.message || '申请失败');
      }
    } catch (error) {
      console.error('申请失败:', error);
      alert('申请失败，请稍后重试');
    }
  };

  const handlePreferenceToggle = (preference) => {
    setSelectedPreferences(prev => {
      const prefName = typeof preference === 'string' ? preference : preference.tag;
      if (prev.includes(prefName)) {
        return prev.filter(p => p !== prefName);
      } else {
        if (prev.length >= 8) {
          alert('最多只能选择8个偏好标签');
          return prev;
        }
        return [...prev, prefName];
      }
    });
  };

  const handleApplicationModalOpen = () => {
    setShowApplicationModal(true);
    loadTravelPreferences();
  };

  const handleApplicationModalClose = () => {
    setShowApplicationModal(false);
    setApplicationMessage('');
    setSelectedPreferences([]);
  };

  const checkUserApplicationStatus = async (groupData) => {
    if (!currentUser) return;

    try {
      const response = await fetch(`/api/group-travel/${id}/user-status`, {
        method: 'GET',
        credentials: 'include',
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setUserApplicationStatus(result.data || 'none');
        } else {
          setUserApplicationStatus('none');
        }
      } else {
        setUserApplicationStatus('none');
      }
    } catch (error) {
      console.error('获取用户状态失败:', error);
      setUserApplicationStatus('none');
    }
  };

  const handleApplicationAction = async (applicationId, action) => {
    try {
      const response = await fetch(
        `/api/group-travel/${id}/applications/${applicationId}/process?action=${action}`,
        {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          // 显示成功消息
          //alert(action === 'approve' ? '已同意申请' : '已拒绝申请');
          
          // 延迟刷新数据，确保后端数据已更新
          setTimeout(() => {
            // 重新加载申请列表
            loadApplications();
            // 刷新组团详情数据
            loadGroupDetail();
            // 重新检查用户状态（如果是申请人）
            if (!isCreator) {
              checkUserApplicationStatus(group);
            }
          }, 500);
        } else {
          throw new Error(result.message);
        }
      } else {
        throw new Error('处理申请失败');
      }
    } catch (error) {
      console.error('处理申请失败:', error);
      alert(error.message || '处理申请失败');
    }
  };



  const handleWithdrawApplication = async () => {
    if (!window.confirm('确定要撤销申请吗？')) {
      return;
    }

    try {
      const response = await fetch(`/api/group-travel/${group.id}/applications/withdraw`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '撤销失败');
      }

      alert('申请已撤销');
      // 更新用户申请状态
      setUserApplicationStatus('none');
      // 移除未使用的justSubmittedApplication
      // 刷新组团详情数据和申请列表
      loadGroupDetail();
      // 如果是创建者，也刷新申请列表
      if (isCreator) {
        loadApplications();
      }
    } catch (error) {
      console.error('撤销申请失败:', error);
      alert(`撤销申请失败: ${error.message}`);
    }
  };

  const handleLeaveGroup = async () => {
    if (!window.confirm('确定要退出团体吗？')) {
      return;
    }

    try {
      const response = await fetch(`/api/group-travel/${group.id}/leave`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || '退出失败');
      }

      alert('已退出团体');
      navigate('/group-travel');
    } catch (error) {
      console.error('退出团体失败:', error);
      alert(`退出团体失败: ${error.message}`);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN');
  };

  const formatBudget = (budget) => {
    if (!budget) return '待商议';
    return `¥${budget}`;
  };

  if (loading) {
    return (
        <div className="group-detail-container">
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>正在加载...</p>
          </div>
        </div>
    );
  }

  if (!group) {
    return (
        <div className="group-detail-container">
          <div className="error-container">
            <h3>组团不存在</h3>
            <button onClick={() => navigate('/group-travel')}>返回组团页面</button>
          </div>
        </div>
    );
  }

  return (
      <div className="group-detail-container">
        <div className="page-header">
          <a 
            href="#" 
            className="back-link"
            onClick={(e) => { e.preventDefault(); navigate('/group-travel'); }}
          >
          <span style={{ fontSize: '1.2rem' }}>←</span> 返回组团页面
          </a>
        </div>

        <div className="group-detail-content">
          <div className="group-main-info">
            <div className="group-header">
              <div className="group-avatar">
                {group.groupAvatarUrl ? (
                    <img src={group.groupAvatarUrl} alt="组团头像" />
                ) : (
                    <div className="default-avatar">🎒</div>
                )}
              </div>
              <div className="group-title-section">
                <h2>{group.title}</h2>
                <p className="destination">📍 {group.destination.name}</p>
                <div className="group-status">
                  <span className={`status-badge ${getStatusDisplay(group.status, group).class}`}>
                    {getStatusDisplay(group.status, group).text}
                  </span>
                  <span className="privacy-badge">{group.isPublic ? '公开' : '私密'}</span>
                  
                  
                  {/* 团队行程按钮 */}
                  {isCreator && (
                    !groupItinerary && (
                      <button
                        className="btn-primary"
                        onClick={handleCreateGroupItinerary}
                        disabled={group.status!='已满员' || itineraryLoading}
                      >
                      {itineraryLoading ? '创建中...' : '创建团队行程'}
                      </button>
                    )
                    
                  )}
                  {groupItinerary && (
                    <button
                        className="btn-secondary"
                        onClick={handleViewGroupItinerary}
                        disabled={itineraryLoading}
                    >
                      {itineraryLoading ? '加载中...' : '查看团队行程'}
                    </button>
                  )}

                </div>
              </div>
            </div>

            <div className="group-description">
              <h3>组团描述</h3>
              <p>{group.description || '暂无描述'}</p>
            </div>

            <div className="group-info-grid">
              <div className="info-item">
                <span className="info-label">旅行时间</span>
                <span className="info-value">
                {formatDate(group.startDate)} - {formatDate(group.endDate)}
              </span>
              </div>
              <div className="info-item">
                <span className="info-label">成员人数</span>
                <span className="info-value">{group.currentMembers}/{group.maxMembers}</span>
              </div>
              <div className="info-item">
                <span className="info-label">预算范围</span>
                <span className="info-value">{formatBudget(group.estimatedBudget)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">旅行类型</span>
                <span className="info-value">{group.groupType}</span>
              </div>
            </div>

            <div className="group-tags">
              <h3>旅行标签</h3>
              <div className="tags-list">
                {group.travelTags && group.travelTags.length > 0 ? (
                  group.travelTags.map((tag, index) => (
                    <span key={index} className="tag">{tag}</span>
                  ))
                ) : (
                  <p className="no-tags">暂无旅行标签</p>
                )}
              </div>
            </div>
          </div>

          <div className="group-sidebar">
            <div className="creator-info">
              <h3>组团发起人</h3>
              <div className="creator-card">
                <img
                    src={group.creator.avatarUrl || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23667eea'/%3E%3Ctext x='50' y='55' text-anchor='middle' font-family='Arial' font-size='30' fill='white'%3E创%3C/text%3E%3C/svg%3E"}
                    alt="创建者头像"
                    className="creator-avatar"
                />
                <div className="creator-details">
                  <h4>{group.creator.username}</h4>
                </div>
              </div>
              
              {/* 团队行程按钮 - 所有成员都能看到 */}
              {(userApplicationStatus === 'member' || isCreator) && (
                <div className="itinerary-actions">
                  {!groupItinerary && !isCreator && (
                    <p className="no-itinerary">发起人还未创建团队行程</p>
                  )}
                </div>
              )}
            </div>

            <div className="group-members">
              <h3>组团成员 ({group.currentMembers}人)</h3>
              <div className="members-list">
                {group.members && group.members.map((member, index) => (
                    <div key={index} className="member-item">
                      <img
                          src={member.avatarUrl || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23667eea'/%3E%3Ctext x='50' y='55' text-anchor='middle' font-family='Arial' font-size='20' fill='white'%3E成%3C/text%3E%3C/svg%3E"}
                          alt="成员头像"
                          className="member-avatar"
                      />
                      <span className="member-name">{member.username}</span>
                    </div>
                ))}
              </div>
            </div>

            {!isCreator && (group.status === '招募中' || (group.currentMembers >= group.maxMembers)) && (
                <div className="join-actions">
                  {userApplicationStatus === 'member' ? (
                      <div className="status-display">
                        <span className="status-badge member">✓ 您已加入该旅行团</span>
                        <button
                            className="btn-secondary"
                            onClick={handleLeaveGroup}
                        >
                          退出团体
                        </button>
                      </div>
                  ) : userApplicationStatus === 'pending' ? (
                      <div className="status-display">
                        <span className="status-badge pending">⏳ 申请正在处理</span>
                      </div>
                  ) : userApplicationStatus === 'rejected' ? (
                      <div className="status-display">
                        <span className="status-badge rejected">❌ 申请已被拒绝</span>
                      </div>
                  ) : group.currentMembers < group.maxMembers ? (
                      <button
                          className="btn-primary"
                          onClick={handleApplicationModalOpen}
                      >
                        申请加入 ({(group.maxMembers - group.currentMembers)}个名额)
                      </button>
                  ) : (
                      <div className="status-display">
                        <span className="status-badge full">😔 组团已满员</span>
                      </div>
                  )}
                </div>
            )}

            {!isCreator && (group.status === '已满员' || group.status === '已结束') && userApplicationStatus === 'member' && (
                <div className="member-actions">
                  <button
                      className="btn-secondary"
                      onClick={handleLeaveGroup}
                  >
                    退出团体
                  </button>
                </div>
            )}

            {isCreator && (
                <div className="applications-section">
                  <div className="applications-header">
                    <div className="applications-title-row">
                      <h3>申请管理</h3>
                      <button
                          className="refresh-btn"
                          onClick={loadApplications}
                          disabled={applicationsLoading}
                      >
                        {applicationsLoading ? '刷新中...' : '刷新申请'}
                      </button>
                    </div>
                    <div className="application-stats">
                      <span 
                        className={`stat-item pending ${selectedApplicationStatus === 'pending' ? 'active' : ''}`}
                        onClick={() => handleApplicationStatusFilter('pending')}
                      >
                        待处理: {getApplicationStats().pending}
                      </span>
                      <span 
                        className={`stat-item approved ${selectedApplicationStatus === 'approved' ? 'active' : ''}`}
                        onClick={() => handleApplicationStatusFilter('approved')}
                      >
                        已同意: {getApplicationStats().approved}
                      </span>
                      <span 
                        className={`stat-item rejected ${selectedApplicationStatus === 'rejected' ? 'active' : ''}`}
                        onClick={() => handleApplicationStatusFilter('rejected')}
                      >
                        已拒绝: {getApplicationStats().rejected}
                      </span>
                    </div>
                  </div>

                  {applicationsLoading ? (
                      <div className="loading-container">
                        <div className="loading-spinner"></div>
                        <p>正在加载申请...</p>
                      </div>
                  ) : getFilteredApplications().length === 0 ? (
                      <div className="no-applications">
                        <p>
                          {selectedApplicationStatus === 'pending' && '暂无待处理申请'}
                          {selectedApplicationStatus === 'approved' && '暂无已同意申请'}
                          {selectedApplicationStatus === 'rejected' && '暂无已拒绝申请'}
                        </p>
                      </div>
                  ) : (
                      <div className="applications-list">
                        {getFilteredApplications().map((application) => (
                            <div 
                              key={application.id} 
                              className="application-item"
                              onClick={() => handleApplicationItemClick(application)}
                              style={{ cursor: application.status === 'pending' ? 'pointer' : 'default' }}
                            >
                              <div className="applicant-info">
                                <img
                                    src={application.avatarUrl || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23667eea'/%3E%3Ctext x='50' y='55' text-anchor='middle' font-family='Arial' font-size='20' fill='white'%3E申%3C/text%3E%3C/svg%3E"}
                                    alt="申请人头像"
                                    className="applicant-avatar"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      navigate(`/profile/${application.applicantId}`);
                                    }}
                                />
                                <div className="applicant-details">
                                  <h4 
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      navigate(`/profile/${application.applicantId}`);
                                    }} 
                                    className="applicant-name"
                                  >
                                    {application.applicantName}
                                  </h4>
                                  <p className="application-message">{application.message}</p>
                                  <div className="application-meta">
                                    <span className="application-time" style={{fontSize: '12px'}}>
                                      申请时间：
                                      {formatDate(application.applyDate)}</span>
                                    {application.status !== 'pending' && application.processDate && (
                                        <span className="process-time" style={{fontSize: '10px'}}>
                                          处理时间：{formatDate(application.processDate)}
                                        </span>
                                    )}
                                  </div>
                                </div>
                              </div>
                              {application.status === '待审核' && (
                                  <div className="application-actions">
                                    <button
                                        className="btn-success"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          handleApplicationAction(application.id, 'approve');
                                        }}
                                    >
                                      同意申请
                                    </button>
                                    <button
                                        className="btn-danger"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          handleApplicationAction(application.id, 'reject');
                                        }}
                                    >
                                      拒绝申请
                                    </button>
                                  </div>
                              )}
                            </div>
                        ))}
                      </div>
                  )}
                </div>
            )}
          </div>
        </div>

        {/* 申请人详情模态框 */}
        <ApplicantModal
          isOpen={showApplicantModal}
          onClose={handleApplicantModalClose}
          application={selectedApplication}
          onApprove={handleApplicantApprove}
          onReject={handleApplicantReject}
        />

        {showApplicationModal && (
            <div className="groupdetail-modal-overlay">
              <div className="modal-content">
                <div className="modal-header">
                  <h3>申请加入组团</h3>
                  <button
                      className="modal-close"
                      onClick={handleApplicationModalClose}
                  >
                    ×
                  </button>
                </div>
                <div className="modal-body">
                  <div className="group-preview">
                    <div className="preview-header">
                      <div className="preview-avatar">
                        {group.groupAvatarUrl ? (
                            <img src={group.groupAvatarUrl} alt="组团头像" />
                        ) : (
                            <div className="default-avatar">🎒</div>
                        )}
                      </div>
                      <div className="preview-info">
                        <h4>{group.title}</h4>
                        <p>📍 {group.destination.name}</p>
                        <p>👤 发起人：{group.creator.username}</p>
                      </div>
                    </div>
                  </div>

                  <div className="form-group">
                    <label>申请理由 <span className="required">*</span></label>
                    <p className="field-desc">请简单介绍一下自己，说明为什么想加入这个组团</p>
                    <textarea
                        value={applicationMessage}
                        onChange={(e) => setApplicationMessage(e.target.value)}
                        placeholder="例如：我是一个热爱旅行的90后，喜欢摄影和美食，希望能找到志同道合的旅友一起探索..."
                        rows={5}
                        maxLength={300}
                    />
                    <span className="char-count">{applicationMessage.length}/300</span>
                  </div>

                </div>
                <div className="modal-footer">
                  <button
                      className="btn-secondary"
                      onClick={handleApplicationModalClose}
                  >
                    取消
                  </button>
                  <button
                      className="btn-primary"
                      onClick={handleApplicationSubmit}
                  >
                    提交申请
                  </button>
                </div>
              </div>
            </div>
        )}
      </div>
  );
};

export default GroupDetail;