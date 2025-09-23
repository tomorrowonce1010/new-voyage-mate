import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import PreferenceModal from '../components/PreferenceModal';
import './GroupTravel.css';

const GroupTravel = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('discover');
  const [groups, setGroups] = useState([]);
  const [myGroups, setMyGroups] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showPreferenceModal, setShowPreferenceModal] = useState(false);
  
  // 搜索相关状态
  const [searchText, setSearchText] = useState('');
  const [searchType, setSearchType] = useState('groupName'); // groupName, creator, destination
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [showSearchPanel, setShowSearchPanel] = useState(false);

  useEffect(() => {
    loadGroups();
  }, [activeTab, searchText, searchType, startDate, endDate]);

  const loadGroups = async () => {
    setLoading(true);
    try {
      switch (activeTab) {
        case 'discover':
          await loadPublicGroups();
          break;
        case 'recommendations':
          await loadRecommendations();
          break;
        case 'my-groups':
          await loadMyGroups();
          break;
        default:
          break;
      }
    } catch (error) {
      console.error('加载组团数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadPublicGroups = async () => {
    try {
      // 构建搜索参数
      const params = new URLSearchParams();
      if (searchText.trim()) {
        params.append('searchText', searchText.trim());
        params.append('searchType', searchType);
      }
      if (startDate) {
        params.append('startDate', startDate);
      }
      if (endDate) {
        params.append('endDate', endDate);
      }

      // 如果有搜索参数，使用搜索接口，否则使用普通的public接口
      const url = (searchText.trim() || startDate || endDate) ? 
        `/api/group-travel/public/search?${params.toString()}` :
        '/api/group-travel/public';
      
      const response = await fetch(url, {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setGroups(result.data);
        }
      }
    } catch (error) {
      console.error('获取公开组团失败:', error);
    }
  };

  const loadRecommendations = async (preferences = null) => {
    // 如果没有传入偏好且用户点击了智能推荐tab，显示偏好选择弹框
    if (!preferences && activeTab === 'recommendations') {
      setShowPreferenceModal(true);
      return;
    }

    try {
      let url = '/api/group-travel/recommendations';
      let options = {
        method: 'GET',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      };

      // 如果有偏好参数，使用POST请求发送偏好
      if (preferences && preferences.length > 0) {
        url = '/api/group-travel/recommendations-by-preferences';
        options = {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ preferences })
        };
      }

      const response = await fetch(url, options);

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          setRecommendations(result.data);
        }
      }
    } catch (error) {
      console.error('获取推荐组团失败:', error);
    }
  };

  const loadMyGroups = async () => {
    try {
      const [createdResponse, joinedResponse] = await Promise.all([
        fetch('/api/group-travel/my-created', {
          method: 'GET',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
        }),
        fetch('/api/group-travel/my-joined', {
          method: 'GET',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
        })
      ]);

      const createdResult = await createdResponse.json();
      const joinedResult = await joinedResponse.json();

      if (createdResult.success && joinedResult.success) {
        // 创建一个Map来避免重复，以组团ID为key
        const groupsMap = new Map();
        
        // 先添加创建的组团（优先级更高）
        createdResult.data.forEach(group => {
          groupsMap.set(group.id, { ...group, role: 'creator' });
        });
        
        // 再添加参与的组团，但不覆盖已存在的（避免重复）
        joinedResult.data.forEach(group => {
          if (!groupsMap.has(group.id)) {
            groupsMap.set(group.id, { ...group, role: 'member' });
          }
        });
        
        // 转换为数组
        setMyGroups(Array.from(groupsMap.values()));
      }
    } catch (error) {
      console.error('获取我的组团失败:', error);
    }
  };

  const handleCreateGroup = () => {
    navigate('/create-group');
  };

  const handleJoinGroup = async (groupId) => {
    try {
      const response = await fetch(`/api/group-travel/${groupId}/apply`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationMessage: '希望能加入这个组团！'
        })
      });

      if (response.ok) {
        loadGroups(); // 刷新数据
      } else {
        const result = await response.json();
        alert(result.message || '申请失败');
      }
    } catch (error) {
      console.error('申请加入组团失败:', error);
      alert('申请失败，请稍后重试');
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  };

  const formatBudget = (budget) => {
    if (!budget) return '待商议';
    return `¥${budget}`;
  };

  const getCompatibilityColor = (score) => {
    if (score >= 80) return '#4CAF50';
    if (score >= 60) return '#FF9800';
    return '#F44336';
  };

  // 获取状态显示文本和样式
  const getStatusDisplay = (status, group) => {
    // 如果组团已满员，强制显示"已满员"状态
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

  const handlePreferenceConfirm = async (selectedPreferences) => {
    setShowPreferenceModal(false);
    setLoading(true);
    try {
      await loadRecommendations(selectedPreferences);
    } catch (error) {
      console.error('获取推荐失败:', error);
      alert('获取推荐失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handlePreferenceClose = () => {
    setShowPreferenceModal(false);
    // 如果用户取消了偏好选择，切换回发现组团tab
    if (activeTab === 'recommendations') {
      setActiveTab('discover');
    }
  };

  // 搜索相关方法
  const handleSearch = () => {
    if (activeTab === 'discover') {
      loadPublicGroups();
    }
  };

  const handleClearSearch = () => {
    setSearchText('');
    setSearchType('groupName');
    setStartDate('');
    setEndDate('');
    if (activeTab === 'discover') {
      loadPublicGroups();
    }
  };

  const toggleSearchPanel = () => {
    setShowSearchPanel(!showSearchPanel);
  };

  const renderGroupCard = (group, showCompatibility = false) => (
    <div key={group.id} className="group-card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/group-detail/${group.id}`)}>
      

      <div className="group-dates">
        <div className="date-range">
          <span className="date-label">开始</span>
          <span className="date-value">{formatDate(group.startDate)}</span>
        </div>
        <div className="date-range">
          <span className="date-label">结束</span>
          <span className="date-value">{formatDate(group.endDate)}</span>
        </div>
      </div>

      <h3 className="group-title">{group.title}</h3>
      <div className="row" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <div className="group-destination">📍 {group.destination.name}</div>
        <div className="group-status-container">
          <div className={`group-status ${getStatusDisplay(group.status, group).class}`}>
            {getStatusDisplay(group.status, group).text}
          </div>
        </div>
      </div>
      
      <div className="group-content">
        <p className="group-description">{group.description || '暂无描述'}</p>
        
        <div className="group-tags">
          {group.travelTags && group.travelTags.length > 0 ? (
            group.travelTags.map((tag, index) => (
              <span key={index} className="tag">{tag}</span>
            ))
          ) : (
            <span className="no-tags">暂无标签</span>
          )}
        </div>
        
        <div className="group-meta">
          <span className="group-members">👥 {group.currentMembers}/{group.maxMembers}人</span>
          <span className="group-type">🎯 {group.groupType}</span>
          <span className="group-budget">💰 {formatBudget(group.estimatedBudget)}</span>
        </div>

        <div className="group-creator">
          <img 
            src={group.creator.avatarUrl || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23667eea'/%3E%3Ctext x='50' y='55' text-anchor='middle' font-family='Arial' font-size='30' fill='white'%3E创%3C/text%3E%3C/svg%3E"} 
            alt="创建者头像" 
            className="creator-avatar"
            style={{ width: '24px', height: '24px', borderRadius: '50%', marginRight: '8px' }}
          />
          <span className="creator-name">{group.creator.username}</span>
        </div>

        <div className="group-action-hint fs-12" style={{ color: '#666', marginTop: '8px' }}>
          点击查看组团详情 👁️
        </div>
      </div>
    </div>
  );

  return (
    <div className="group-travel-container">
      <div className="page-header">
        <h1 className="group-travel-page-title">智能组团</h1>
        <p className="group-travel-page-subtitle">找到志同道合的旅友，开启精彩的团队旅行</p>
      </div>

      <div className="action-bar">
        <div className="tab-nav">
          <button 
            className={`tab-btn ${activeTab === 'discover' ? 'active' : ''}`}
            onClick={() => setActiveTab('discover')}
          >
            <span className="tab-icon">🔍</span>
            发现组团
          </button>
          <button 
            className={`tab-btn ${activeTab === 'recommendations' ? 'active' : ''}`}
            onClick={() => setActiveTab('recommendations')}
          >
            <span className="tab-icon">⭐</span>
            智能推荐
          </button>
          <button 
            className={`tab-btn ${activeTab === 'my-groups' ? 'active' : ''}`}
            onClick={() => setActiveTab('my-groups')}
          >
            <span className="tab-icon">👥</span>
            我的组团
          </button>
        </div>
        
        <div className="action-buttons">
          {activeTab === 'discover' && (
            <button className="search-toggle-btn" onClick={toggleSearchPanel}>
              <span className="btn-icon">🔎</span>
              {showSearchPanel ? '收起搜索' : '搜索'}
            </button>
          )}
          <button className="create-group-btn" onClick={handleCreateGroup}>
            <span className="btn-icon">➕</span>
            创建组团
          </button>
        </div>
      </div>

      {/* 搜索面板 */}
      {activeTab === 'discover' && showSearchPanel && (
        <div className="search-panel">
          <div className="search-row">
            <div className="search-input-group">
              <input
                type="text"
                placeholder="输入搜索关键词..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                className="search-input"
              />
              <select
                value={searchType}
                onChange={(e) => setSearchType(e.target.value)}
                className="search-type-select"
              >
                <option value="groupName">团名</option>
                <option value="creator">发起人</option>
                <option value="destination">目的地</option>
              </select>
            </div>
            <div className="date-input-group">
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="date-input"
                placeholder="开始日期"
              />
              <span className="date-separator">至</span>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="date-input"
                placeholder="结束日期"
              />
            </div>
          </div>
          <div className="search-actions">
            <button className="search-btn" onClick={handleSearch}>
              🔍 搜索
            </button>
            <button className="clear-btn" onClick={handleClearSearch}>
              🗑️ 清空
            </button>
          </div>
        </div>
      )}

      <div className="group-content-area">
        {loading ? (
          <div className="loading-container">
            <div className="loading-spinner"></div>
            <p>正在加载...</p>
          </div>
        ) : (
          <div className="groups-grid">
            {activeTab === 'discover' && groups.map(group => renderGroupCard(group))}
            {activeTab === 'recommendations' && recommendations.map(group => renderGroupCard(group, true))}
            {activeTab === 'my-groups' && myGroups.map(group => renderGroupCard(group))}
            
            {((activeTab === 'discover' && groups.length === 0) ||
              (activeTab === 'recommendations' && recommendations.length === 0) ||
              (activeTab === 'my-groups' && myGroups.length === 0)) && (
              <div className="empty-state">
                <div className="empty-icon">
                  {activeTab === 'discover' && '🔍'}
                  {activeTab === 'recommendations' && '⭐'}
                  {activeTab === 'my-groups' && '👥'}
                </div>
                <h3>
                  {activeTab === 'discover' && '暂无公开组团'}
                  {activeTab === 'recommendations' && '暂无推荐组团'}
                  {activeTab === 'my-groups' && '暂无参与的组团'}
                </h3>
                <p>
                  {activeTab === 'discover' && '目前还没有公开的组团，快来创建第一个吧！'}
                  {activeTab === 'recommendations' && '完善您的旅行偏好，获取更精准的推荐'}
                  {activeTab === 'my-groups' && '您还没有创建或加入任何组团'}
                </p>
                {activeTab !== 'my-groups' && (
                  <button className="empty-action-btn" onClick={handleCreateGroup}>
                    创建组团
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* 偏好选择弹框 */}
      <PreferenceModal
        isOpen={showPreferenceModal}
        onClose={handlePreferenceClose}
        onConfirm={handlePreferenceConfirm}
      />
    </div>
  );
};

export default GroupTravel; 