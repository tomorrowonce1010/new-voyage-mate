import React, { useState } from 'react';
import './FootprintModal.css';

const FootprintModal = ({ 
  isOpen, 
  onClose, 
  onSubmit 
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    startDate: '',
    endDate: ''
  });

  const [errors, setErrors] = useState({});

  const validateForm = () => {
    const newErrors = {};
    if (!formData.name.trim()) {
      newErrors.name = '请输入城市名称';
    }
    if (!formData.startDate) {
      newErrors.startDate = '请选择出行开始日期';
    }
    if (!formData.endDate) {
      newErrors.endDate = '请选择出行结束日期';
    }
    if (formData.startDate && formData.endDate && formData.startDate > formData.endDate) {
      newErrors.endDate = '结束日期不能早于开始日期';
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
      startDate: '',
      endDate: ''
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
    <div className="footprint-modal-overlay" onClick={handleClose}>
      <div className="footprint-modal" onClick={e => e.stopPropagation()}>
        <div className="footprint-modal-header">
          <h2 className="footprint-modal-title">📍 添加足迹</h2>
          <button className="close-btn" onClick={handleClose}>×</button>
        </div>
        <form className="footprint-modal-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">🏙️ 城市名称 *</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="例如：北京、上海、杭州..."
            />
            {errors.name && <span className="error-message">{errors.name}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="description">📝 城市描述（可选）</label>
            <input
              type="text"
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="例如：古都风韵 • 历史文化、海滨度假 • 美食天堂..."
            />
          </div>

          <div className="form-group">
            <label htmlFor="startDate">📅 出行开始日期 *</label>
            <input
              type="date"
              id="startDate"
              name="startDate"
              value={formData.startDate}
              onChange={handleChange}
            />
            {errors.startDate && <span className="error-message">{errors.startDate}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="endDate">📅 出行结束日期 *</label>
            <input
              type="date"
              id="endDate"
              name="endDate"
              value={formData.endDate}
              onChange={handleChange}
            />
            {errors.endDate && <span className="error-message">{errors.endDate}</span>}
          </div>

          <div className="footprint-modal-actions">
            <button 
              type="button" 
              className="modal-btn modal-btn-cancel"
              onClick={handleClose}
            >
              ❌ 取消
            </button>
            <button 
              type="submit" 
              className="modal-btn modal-btn-submit"
            >
              ✅ 确认添加
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default FootprintModal; 