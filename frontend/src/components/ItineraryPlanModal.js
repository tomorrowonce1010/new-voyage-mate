import React, { useState } from 'react';
import './ItineraryPlanModal.css';

const ItineraryPlanModal = ({ isOpen, onClose, onConfirm, destination = "" }) => {
  const [formData, setFormData] = useState({
    days: 3,
    travelers: 2,
    budget: 5000
  });

  const [errors, setErrors] = useState({});

  if (!isOpen) return null;

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // 清除对应字段的错误
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: null
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.days || formData.days < 1 || formData.days > 30) {
      newErrors.days = "天数必须在1-30之间";
    }
    
    if (!formData.travelers || formData.travelers < 1 || formData.travelers > 10) {
      newErrors.travelers = "人数必须在1-10之间";
    }
    
    if (!formData.budget || formData.budget < 100 || formData.budget > 500000) {
      newErrors.budget = "预算必须在100-500000之间";
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleConfirm = () => {
    if (validateForm()) {
      onConfirm({
        destination,
        ...formData
      });
    }
  };

  const handleClose = () => {
    setFormData({
      days: 3,
      travelers: 2,
      budget: 5000
    });
    setErrors({});
    onClose();
  };

  return (
    <div className="itinerary-modal-overlay" onClick={handleClose}>
      <div className="itinerary-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="itinerary-modal-header">
          <h3 className="itinerary-modal-title">
            <span className="itinerary-icon">🗓️</span>
            定制 {destination} 行程计划
          </h3>
          <button className="itinerary-modal-close" onClick={handleClose}>
            ×
          </button>
        </div>
        
        <div className="itinerary-modal-body">
          <div className="form-section">
            <div className="form-group">
              <label className="form-label">
                <span className="label-icon">📅</span>
                旅行天数
              </label>
              <div className="input-group">
                <input
                  type="number"
                  value={formData.days}
                  onChange={(e) => handleInputChange('days', parseInt(e.target.value) || 0)}
                  className={`form-input ${errors.days ? 'error' : ''}`}
                  min="1"
                  max="30"
                  placeholder="请输入天数"
                />
                <span className="input-suffix">天</span>
              </div>
              {errors.days && <span className="error-message">{errors.days}</span>}
            </div>

            <div className="form-group">
              <label className="form-label">
                <span className="label-icon">👥</span>
                旅行人数
              </label>
              <div className="input-group">
                <input
                  type="number"
                  value={formData.travelers}
                  onChange={(e) => handleInputChange('travelers', parseInt(e.target.value) || 0)}
                  className={`form-input ${errors.travelers ? 'error' : ''}`}
                  min="1"
                  max="10"
                  placeholder="请输入人数"
                />
                <span className="input-suffix">人</span>
              </div>
              {errors.travelers && <span className="error-message">{errors.travelers}</span>}
            </div>

            <div className="form-group">
              <label className="form-label">
                <span className="label-icon">💰</span>
                预算范围
              </label>
              <div className="input-group">
                <input
                  type="number"
                  value={formData.budget}
                  onChange={(e) => handleInputChange('budget', parseInt(e.target.value) || 0)}
                  className={`form-input ${errors.budget ? 'error' : ''}`}
                  min="100"
                  max="500000"
                  step="100"
                  placeholder="请输入预算"
                />
                <span className="input-suffix">元</span>
              </div>
              {errors.budget && <span className="error-message">{errors.budget}</span>}
            </div>
          </div>
          
          <div className="plan-preview">
            <h4 className="preview-title">计划预览</h4>
            <div className="preview-content">
              <div className="preview-item">
                <span className="preview-label">目的地：</span>
                <span className="preview-value">{destination}</span>
              </div>
              <div className="preview-item">
                <span className="preview-label">行程：</span>
                <span className="preview-value">{formData.days}天{formData.days - 1}晚</span>
              </div>
              <div className="preview-item">
                <span className="preview-label">人数：</span>
                <span className="preview-value">{formData.travelers}人</span>
              </div>
              <div className="preview-item">
                <span className="preview-label">预算：</span>
                <span className="preview-value">¥{formData.budget}</span>
              </div>
              <div className="preview-item">
                <span className="preview-label">人均：</span>
                <span className="preview-value">¥{Math.round(formData.budget / formData.travelers)}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div className="itinerary-modal-footer">
          <button className="itinerary-btn itinerary-btn-secondary" onClick={handleClose}>
            取消
          </button>
          <button className="itinerary-btn itinerary-btn-primary" onClick={handleConfirm}>
            生成AI行程计划
          </button>
        </div>
      </div>
    </div>
  );
};

export default ItineraryPlanModal; 