import React, { useState, useEffect } from 'react';
import './PreferenceModal.css';

const PreferenceModal = ({ isOpen, onClose, onConfirm }) => {
  const [selectedTags, setSelectedTags] = useState([]);
  const [loading, setLoading] = useState(false);
  const [travelPreferences, setTravelPreferences] = useState([]);
  const [tagsLoading, setTagsLoading] = useState(true);

  // 从后端获取真实的旅行偏好标签
  useEffect(() => {
    if (isOpen) {
      loadTravelTags();
    }
  }, [isOpen]);

  const loadTravelTags = async () => {
    try {
      setTagsLoading(true);
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
          // 如果后端没有数据，使用默认标签
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
      // 使用默认标签
      setTravelPreferences([
        '自然风光', '历史足迹', '文化体验', '购物探店', '娱乐休闲',
        '冒险刺激', '摄影天堂', '艺术巡礼', '美食寻味', '户外徒步',
        '海岛度假', '心灵疗愈', '毕业旅行', '亲子游玩', '背包独行',
        '自驾路线', '网红热点', '小众秘境', '避暑胜地', '城市漫步',
        '田园民俗', '江南园林', '大漠风光', '温泉养生', '冰雪世界',
        '古迹村落', '考古博物', '极限运动', '浪漫之旅', '建筑奇观'
      ]);
    } finally {
      setTagsLoading(false);
    }
  };

  const handleTagToggle = (tag) => {
    setSelectedTags(prev => {
      if (prev.includes(tag)) {
        return prev.filter(t => t !== tag);
      } else {
        // 最多选择10个标签
        if (prev.length >= 10) {
          alert('最多只能选择10个偏好标签');
          return prev;
        }
        return [...prev, tag];
      }
    });
  };

  const handleConfirm = async () => {
    if (selectedTags.length === 0) {
      alert('请至少选择一个偏好标签');
      return;
    }

    setLoading(true);
    try {
      await onConfirm(selectedTags);
    } catch (error) {
      console.error('获取推荐失败:', error);
      alert('获取推荐失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="preference-modal-overlay">
      <div className="preference-modal-content">
        <div className="preference-modal-header">
          <h3>选择您的旅行偏好</h3>
          <p className="preference-subtitle">
            选择符合您兴趣的标签，我们将为您推荐最合适的组团 
            <span className="selection-count">({selectedTags.length}/10)</span>
          </p>
          <button 
            className="preference-modal-close"
            onClick={handleClose}
            disabled={loading}
          >
            ×
          </button>
        </div>
        
        <div className="preference-modal-body">
          {tagsLoading ? (
            <div className="tags-loading">
              <div className="loading-spinner-small"></div>
              <p>正在加载标签...</p>
            </div>
          ) : (
            <div className="preference-categories">
              <div className="preference-category">
                <h4>🎯 旅行偏好标签</h4>
                <p className="category-desc">从以下标签中选择您的旅行偏好 (最多选择10个)</p>
                <div className="preference-tags">
                  {travelPreferences.map((tag, index) => (
                    <span
                      key={typeof tag === 'string' ? tag : tag.tag || index}
                      className={`preference-tag ${selectedTags.includes(typeof tag === 'string' ? tag : tag.tag) ? 'selected' : ''}`}
                      onClick={() => handleTagToggle(typeof tag === 'string' ? tag : tag.tag)}
                    >
                      {typeof tag === 'string' ? tag : tag.tag}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
          
          {selectedTags.length > 0 && (
            <div className="selected-preferences">
              <h4>已选择的偏好：</h4>
              <div className="selected-tags">
                {selectedTags.map(tag => (
                  <span key={tag} className="selected-tag">
                    {tag}
                    <button 
                      className="remove-tag"
                      onClick={() => handleTagToggle(tag)}
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
        
        <div className="preference-modal-footer">
          <button 
            className="btn-secondary"
            onClick={handleClose}
            disabled={loading}
          >
            取消
          </button>
          <button 
            className="btn-primary"
            onClick={handleConfirm}
            disabled={loading || selectedTags.length === 0}
          >
            {loading ? (
              <>
                <span className="loading-spinner-small"></span>
                获取推荐中...
              </>
            ) : (
              `获取推荐 (${selectedTags.length}个偏好)`
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PreferenceModal; 