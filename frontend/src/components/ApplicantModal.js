import React from 'react';
import './ApplicantModal.css';

const ApplicantModal = ({ isOpen, onClose, application, onApprove, onReject }) => {
  if (!isOpen || !application) return null;

  const { applicant, applicationMessage } = application;
  const preferences = applicant.travelPreferences || [];

  return (
    <div className="applicant-modal-overlay" onClick={onClose}>
      <div className="applicant-modal-content" onClick={e => e.stopPropagation()}>
        <div className="applicant-modal-header">
          <h3>申请人信息</h3>
          <button className="modal-close-btn" onClick={onClose}>×</button>
        </div>

        <div className="applicant-modal-body">
          <div className="applicant-basic">
            <img
              src={applicant.avatarUrl || "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23667eea'/%3E%3Ctext x='50' y='55' text-anchor='middle' font-family='Arial' font-size='20' fill='white'%3E申%3C/text%3E%3C/svg%3E"}
              alt="申请人头像"
              className="applicant-avatar-large"
            />
            <div className="applicant-info">
              <h4>{applicant.username}</h4>
              <p>{applicant.signature || '暂无签名'}</p>
            </div>
          </div>

          <div className="application-reason">
            <h4>申请理由</h4>
            <p>{applicationMessage}</p>
          </div>

          <div className="travel-preferences">
            <h4>旅行偏好</h4>
            <div className="preferences-list">
              {preferences.length > 0 ? (
                preferences.map((pref, index) => (
                  <span key={index} className="preference-tag">{pref}</span>
                ))
              ) : (
                <p className="no-preferences">暂无旅行偏好</p>
              )}
            </div>
          </div>
        </div>

        <div className="applicant-modal-actions">
          <button 
            className="btn-secondary" 
            onClick={onClose}
          >
            关闭
          </button>
          <button 
            className="btn-danger" 
            onClick={() => onReject(application.id)}
          >
            拒绝申请
          </button>
          <button 
            className="btn-success" 
            onClick={() => onApprove(application.id)}
          >
            同意申请
          </button>
        </div>
      </div>
    </div>
  );
};

export default ApplicantModal; 