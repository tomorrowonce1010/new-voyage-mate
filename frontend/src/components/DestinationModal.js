import React, { useState } from 'react';
import './DestinationModal.css';

const DestinationModal = ({ 
  isOpen, 
  onClose, 
  onSubmit, 
  type = 'history' // 'history' 或 'wishlist'
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    notes: '',
    visitYearMonth: '',
    days: ''
  });

  const [errors, setErrors] = useState({});

  const validateForm = () => {
    const newErrors = {};
    if (!formData.name.trim()) {
      newErrors.name = '请输入目的地名称';
    }
    if (!formData.description.trim()) {
      newErrors.description = '请输入目的地描述';
    }
    if (type === 'history') {
      if (!formData.visitYearMonth.trim()) {
        newErrors.visitYearMonth = '请选择游玩年月';
      }
      if (!formData.days.trim() || isNaN(formData.days) || parseInt(formData.days) <= 0) {
        newErrors.days = '请输入有效的游玩天数';
      }
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit(formData);
      handleClose();
    }
  };

  const handleClose = () => {
    setFormData({
      name: '',
      description: '',
      notes: '',
      visitYearMonth: '',
      days: ''
    });
    setErrors({});
    onClose();
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // 清除对应字段的错误
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="destination-modal-overlay" onClick={handleClose}>
      <div className="destination-modal" onClick={e => e.stopPropagation()}>
        <div className="destination-modal-header">
          <h2 className="destination-modal-title">
            {type === 'history' ? '添加历史目的地' : '添加期望目的地'}
          </h2>
        </div>
        <form className="destination-modal-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">目的地名称</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="例如：北京"
            />
            {errors.name && <span className="error-message">{errors.name}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="description">目的地描述</label>
            <input
              type="text"
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="例如：古都风韵 • 历史文化"
            />
            {errors.description && <span className="error-message">{errors.description}</span>}
          </div>

          {type === 'history' && (
            <>
              <div className="form-group">
                <label htmlFor="visitYearMonth">游玩年月</label>
                <input
                  type="month"
                  id="visitYearMonth"
                  name="visitYearMonth"
                  value={formData.visitYearMonth}
                  onChange={handleChange}
                />
                {errors.visitYearMonth && <span className="error-message">{errors.visitYearMonth}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="days">游玩天数</label>
                <input
                  type="number"
                  id="days"
                  name="days"
                  value={formData.days}
                  onChange={handleChange}
                  min="1"
                  placeholder="请输入游玩天数"
                />
                {errors.days && <span className="error-message">{errors.days}</span>}
              </div>
            </>
          )}

          <div className="form-group">
            <label htmlFor="notes">备注（可选）</label>
            <textarea
              id="notes"
              name="notes"
              value={formData.notes}
              onChange={handleChange}
              placeholder="添加一些备注信息..."
            />
          </div>

          <div className="destination-modal-actions">
            <button 
              type="button" 
              className="modal-btn modal-btn-cancel"
              onClick={handleClose}
            >
              取消
            </button>
            <button 
              type="submit" 
              className="modal-btn modal-btn-submit"
            >
              确认添加
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default DestinationModal; 