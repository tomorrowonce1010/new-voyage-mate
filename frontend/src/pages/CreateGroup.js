import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './CreateGroup.css';

const CreateGroup = () => {
  const navigate = useNavigate();
  const location = useLocation();
  
  // 从location state获取预选的目的地
  const preSelectedDestination = location.state?.selectedDestination;
  
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    destination: preSelectedDestination || null,
    startDate: '',
    endDate: '',
    estimatedBudget: '',
    maxMembers: '',
    groupType: '自由行',
    privacy: 'public'
  });
  
  const [travelTags, setTravelTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [destinations, setDestinations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);

  // 获取旅行标签
  useEffect(() => {
    fetchTravelTags();
    if (!preSelectedDestination) {
      fetchPopularDestinations();
    }
  }, []);

  const fetchTravelTags = async () => {
    try {
      const response = await fetch('/api/destinations/tags');
      if (response.ok) {
        const tags = await response.json();
        setTravelTags(tags);
      }
    } catch (error) {
      console.error('获取旅行标签失败:', error);
    }
  };

  const fetchPopularDestinations = async () => {
    try {
      const response = await fetch('/api/destinations/hot?page=0&size=20');
      if (response.ok) {
        const data = await response.json();
        setDestinations(data.content || []);
      }
    } catch (error) {
      console.error('获取热门目的地失败:', error);
    }
  };

  const searchDestinations = async (keyword) => {
    if (!keyword.trim()) {
      fetchPopularDestinations();
      return;
    }
    
    setLoading(true);
    try {
      const response = await fetch(`/api/destinations/search?keyword=${encodeURIComponent(keyword)}&page=0&size=20`);
      if (response.ok) {
        const data = await response.json();
        setDestinations(data.content || []);
      }
    } catch (error) {
      console.error('搜索目的地失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDestinationSearch = (value) => {
    const timer = setTimeout(() => {
      searchDestinations(value);
    }, 300);
    
    return () => clearTimeout(timer);
  };

  const handleDestinationSelect = (destination) => {
    setFormData({ ...formData, destination });
  };

  const handleTagToggle = (tag) => {
    setSelectedTags(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.destination) {
      alert('请选择目的地');
      return;
    }
    
    if (!formData.title.trim()) {
      alert('请输入组团标题');
      return;
    }
    
    if (!formData.startDate || !formData.endDate) {
      alert('请选择出行日期');
      return;
    }
    
    if (new Date(formData.startDate) >= new Date(formData.endDate)) {
      alert('结束日期必须晚于开始日期');
      return;
    }
    
    if (!formData.maxMembers || formData.maxMembers < 2 || formData.maxMembers > 10) {
      alert('最大成员数必须在2-10人之间');
      return;
    }

    setSubmitLoading(true);
    try {
      const requestData = {
        title: formData.title,
        description: formData.description,
        destinationId: formData.destination.id,
        startDate: formData.startDate,
        endDate: formData.endDate,
        estimatedBudget: formData.estimatedBudget ? parseFloat(formData.estimatedBudget) : null,
        maxMembers: parseInt(formData.maxMembers),
        groupType: formData.groupType,
        isPublic: formData.privacy === 'public',
        travelTags: selectedTags
      };

      const response = await fetch('/api/group-travel', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData)
      });

      const result = await response.json();
      
      if (response.ok && result.success) {
        navigate('/group-travel');
      } else {
        alert(result.message || '创建失败，请稍后重试');
      }
    } catch (error) {
      console.error('创建组团失败:', error);
      alert('创建失败，请稍后重试');
    } finally {
      setSubmitLoading(false);
    }
  };

  const getDestinationImageUrl = (destination) => {
    return destination.imageUrl || `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 120'><rect width='200' height='120' fill='%23${Math.floor(Math.random()*16777215).toString(16)}'/><text x='100' y='60' font-family='Arial' font-size='16' fill='white' text-anchor='middle' dy='0.3em'>${destination.name}</text></svg>`;
  };

  return (
    <div className="create-group-container">
      <div className="page-header">
      <a 
        href="#" 
        className="back-link"
        onClick={(e) => { e.preventDefault(); navigate('/group-travel'); }}
      >
        <span style={{ fontSize: '1.2rem' }}>←</span> 返回组团页面
      </a>
      </div>

      <form className="create-group-form" onSubmit={handleSubmit}>
        {/* 目的地选择 */}
        <div className="create-group-form-section" style={{height: '200px'}}>
          <h3 className="section-title">📍 选择目的地</h3>
          {formData.destination ? (
            <div className="selected-destination">
              <div className="destination-info">
                <h4>{formData.destination.name}</h4>
                <p>{formData.destination.description || '暂无描述'}</p>
              </div>
              <button 
                type="button" 
                className="change-destination-btn"
                onClick={() => {
                  // setShowDestinationSelector(true); // This state is removed
                  navigate('/explore', { state: { selectMode: true, returnPath: '/create-group' } });
                }}
              >
                更换
              </button>
            </div>
          ) : (
            <button 
              type="button" 
              className="select-destination-btn"
              onClick={() => {
                // setShowDestinationSelector(true); // This state is removed
                navigate('/explore', { state: { selectMode: true, returnPath: '/create-group' } });
              }}
            >
              点击选择目的地
            </button>
          )}
        </div>

        {/* 基本信息 */}
        <div className="create-group-form-section">
          <h3 className="section-title">📝 基本信息</h3>
          <div className="form-row">
            <div className="form-group">
              <label>组团标题 *</label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) => setFormData({...formData, title: e.target.value})}
                placeholder="给您的组团起个吸引人的标题"
                maxLength={50}
                required
              />
            </div>
          </div>
          
          <div className="form-group">
            <label>组团描述</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({...formData, description: e.target.value})}
              placeholder="描述一下您的旅行计划和期望..."
              rows={4}
              maxLength={500}
            />
          </div>
        </div>

        {/* 旅行时间 */}
        <div className="create-group-form-section">
          <h3 className="section-title">📅 旅行时间</h3>
          <div className="form-row">
            <div className="form-group">
              <label>开始日期 *</label>
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => setFormData({...formData, startDate: e.target.value})}
                min={new Date().toISOString().split('T')[0]}
                required
              />
            </div>
            <div className="form-group">
              <label>结束日期 *</label>
              <input
                type="date"
                value={formData.endDate}
                onChange={(e) => setFormData({...formData, endDate: e.target.value})}
                min={formData.startDate || new Date().toISOString().split('T')[0]}
                required
              />
            </div>
          </div>
        </div>

        {/* 组团设置 */}
        <div className="create-group-form-section">
          <h3 className="section-title">👥 组团设置</h3>
          <div className="form-row">
            <div className="form-group">
              <label>最大成员数 *</label>
              <input
                type="number"
                value={formData.maxMembers}
                onChange={(e) => setFormData({...formData, maxMembers: e.target.value})}
                placeholder="请输入最大成员数（建议小于10人）"
                min={2}
                max={10}
                required
              />
              <small className="form-hint">建议最大成员数不超过10人，便于协调和管理</small>
            </div>
            <div className="form-group">
              <label>旅行类型</label>
              <select
                value={formData.groupType}
                onChange={(e) => setFormData({...formData, groupType: e.target.value})}
              >
                <option value="自由行">自由行</option>
                <option value="半自助">半自助</option>
                <option value="深度游">深度游</option>
              </select>
            </div>
            <div className="form-group">
              <label>预算范围 (人均/元)</label>
              <input
                type="number"
                value={formData.estimatedBudget}
                onChange={(e) => setFormData({...formData, estimatedBudget: e.target.value})}
                placeholder="预估人均预算（可留空）"
                min={0}
              />
            </div>
          </div>
        </div>

        {/* 旅行标签 */}
        <div className="create-group-form-section">
          <h3 className="section-title">🏷️ 旅行标签</h3>
          <p className="section-description">选择符合您旅行风格的标签，帮助找到合适的旅友</p>
          <div className="tags-container">
            {travelTags.map(tag => (
              <span
                key={tag}
                className={`tag-item ${selectedTags.includes(tag) ? 'selected' : ''}`}
                onClick={() => handleTagToggle(tag)}
              >
                {tag}
              </span>
            ))}
          </div>
        </div>

        {/* 提交按钮 */}
        <div className="form-actions">
          <button type="button" className="cancel-btn" onClick={() => navigate('/group-travel')}>
            取消
          </button>
          <button type="submit" className="submit-btn" disabled={submitLoading}>
            {submitLoading ? '创建中...' : '创建组团'}
          </button>
        </div>
      </form>

      {/* 目的地选择模态框 */}
      {/* This block is removed as showDestinationSelector state is removed */}
    </div>
  );
};

export default CreateGroup; 