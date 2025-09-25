import React, { useState } from 'react';
import './Login.css';

const Login = ({ onLogin }) => {
  const [activeForm, setActiveForm] = useState('login');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '', show: false });
  const [formData, setFormData] = useState({
    login: { email: '', password: '' },
    register: { username: '', email: '', password: '', confirmPassword: '', agreeTerms: false }
  });

  // 显示消息
  const showMessage = (text, type) => {
    setMessage({ text, type, show: true });
    if (type === 'info') {
      setTimeout(() => setMessage({ text: '', type: '', show: false }), 3000);
    }
  };

  // 隐藏消息
  const hideMessages = () => {
    setMessage({ text: '', type: '', show: false });
  };

  // 表单切换
  const switchForm = (formType) => {
    hideMessages();
    setActiveForm(formType);
  };

  // 设置加载状态
  const setButtonLoading = (isLoading) => {
    setLoading(isLoading);
  };

  // 处理输入变化
  const handleInputChange = (formType, field, value) => {
    setFormData(prev => ({
      ...prev,
      [formType]: {
        ...prev[formType],
        [field]: value
      }
    }));
  };

  // 登录处理
  const handleLogin = async (event) => {
    event.preventDefault();
    
    const { email, password } = formData.login;
    
    if (email && password) {
      setButtonLoading(true);
      showMessage('正在验证登录信息...', 'success');
      
      try {
        const response = await fetch('/api/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include', // 重要：携带cookies以保存session
          body: JSON.stringify({
            email: email,
            password: password
          })
        });
        
        // 检查响应状态
        if (!response.ok) {
          // 检查响应内容类型
          const contentType = response.headers.get('content-type');
          if (contentType && contentType.includes('application/json')) {
            const errorResult = await response.json();
            throw new Error(errorResult.message || `HTTP ${response.status}: ${response.statusText}`);
          } else {
            // 如果不是JSON，可能是HTML错误页面
            const text = await response.text();
            console.error('服务器返回非JSON响应:', text.substring(0, 200));
            throw new Error(`服务器错误 (${response.status}): 请检查后端服务是否正常运行`);
          }
        }
        
        // 检查响应内容类型
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
          const text = await response.text();
          console.error('服务器返回非JSON响应:', text.substring(0, 200));
          throw new Error('服务器返回了非JSON响应，请检查后端服务配置');
        }
        
        const result = await response.json();
        
        setButtonLoading(false);
        
        if (result.success) {
          showMessage('登录成功！正在跳转到主页面...', 'success');
          
          // 清除中转站数据
          sessionStorage.removeItem('transferStationItems');
          sessionStorage.removeItem('transferStationAttractions');
          sessionStorage.removeItem('pendingAttractionToAdd');
          localStorage.removeItem('pendingAttractionToAdd'); // 也清除旧的localStorage数据
          localStorage.removeItem('transferStationAttractions'); // 也清除旧的localStorage数据
          
          // 调用父组件的登录回调，让App.js处理路由跳转
          if (onLogin) {
            onLogin(result);
          }
        } else {
          showMessage(result.message || '登录失败', 'error');
        }
      } catch (error) {
        setButtonLoading(false);
        
        // 根据错误类型显示不同的消息
        if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
          showMessage('无法连接到服务器，请检查网络连接和后端服务是否启动', 'error');
        } else if (error.message.includes('服务器错误') || error.message.includes('非JSON响应')) {
          showMessage(error.message, 'error');
        } else {
          showMessage('网络错误，请稍后重试', 'error');
        }
        
        console.error('登录错误:', error);
      }
    } else {
      showMessage('请填写完整的登录信息', 'error');
    }
  };



  // 注册处理
  const handleRegister = async (event) => {
    event.preventDefault();
    
    const { username, email, password, confirmPassword, agreeTerms } = formData.register;
    
    // 表单验证
    if (!username || !email || !password || !confirmPassword) {
      showMessage('请填写完整的注册信息', 'error');
      return;
    }
    
    if (password !== confirmPassword) {
      showMessage('两次输入的密码不一致', 'error');
      return;
    }
    
    if (password.length < 6) {
      showMessage('密码长度至少需要6位', 'error');
      return;
    }
    
    if (!agreeTerms) {
      showMessage('请同意服务条款和隐私政策', 'error');
      return;
    }
    
    setButtonLoading(true);
    showMessage('正在创建您的账户...', 'success');
    
    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // 重要：携带cookies以保存session
        body: JSON.stringify({
          username: username,
          email: email,
          password: password,
          confirmPassword: confirmPassword
        })
      });
      
      // 检查响应状态
      if (!response.ok) {
        // 检查响应内容类型
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          const errorResult = await response.json();
          throw new Error(errorResult.message || `HTTP ${response.status}: ${response.statusText}`);
        } else {
          // 如果不是JSON，可能是HTML错误页面
          const text = await response.text();
          console.error('服务器返回非JSON响应:', text.substring(0, 200));
          throw new Error(`服务器错误 (${response.status}): 请检查后端服务是否正常运行`);
        }
      }
      
      // 检查响应内容类型
      const contentType = response.headers.get('content-type');
      if (!contentType || !contentType.includes('application/json')) {
        const text = await response.text();
        console.error('服务器返回非JSON响应:', text.substring(0, 200));
        throw new Error('服务器返回了非JSON响应，请检查后端服务配置');
      }
      
      const result = await response.json();
      
      setButtonLoading(false);
      
      if (result.success) {
        showMessage('注册成功！您现在可以登录了', 'success');
        
        setTimeout(() => {
          switchForm('login');
          // 清空注册表单
          setFormData(prev => ({
            ...prev,
            register: { username: '', email: '', password: '', confirmPassword: '', agreeTerms: false }
          }));
        }, 2000);
      } else {
        showMessage(result.message || '注册失败', 'error');
      }
    } catch (error) {
      setButtonLoading(false);
      
      // 根据错误类型显示不同的消息
      if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
        showMessage('无法连接到服务器，请检查网络连接和后端服务是否启动', 'error');
      } else if (error.message.includes('服务器错误') || error.message.includes('非JSON响应')) {
        showMessage(error.message, 'error');
      } else {
        showMessage('网络错误，请稍后重试', 'error');
      }
      
      console.error('注册错误:', error);
    }
  };

  return (
    <div className="login-page">
      {/* 粒子背景 */}
      <div className="particles">
        <div className="particle"></div>
        <div className="particle"></div>
        <div className="particle"></div>
        <div className="particle"></div>
        <div className="particle"></div>
        <div className="particle"></div>
        <div className="particle"></div>
        <div className="particle"></div>
      </div>

      <div className="container">
        <div className="welcome-section">
          <div className="travel-icon">✈️</div>
          <h1>Voyage Mate</h1>
          
          <div className="features-list">
            <div className="feature-item">
              <span className="feature-icon">🌍</span>
              <span>多元目的地推荐</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">🖊️</span>
              <span>个性化行程管理</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">👥</span>
              <span>旅行者交流社区</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">💡</span>
              <span>AI智能旅行规划</span>
            </div>
          </div>
        </div>

        <div className="form-section">
          <div className="form-container">
            <div className="form-header">
              <h2 className="form-title">
                {activeForm === 'login' ? 'Welcome' : 'Sign Up'}
              </h2>
              <p className="form-subtitle">
                {activeForm === 'login' ? '开始您的智能旅行体验' : '加入Voyage Mate，开启智能旅行'}
              </p>
            </div>

            <div className="form-toggle">
              <button 
                className={`toggle-btn ${activeForm === 'login' ? 'active' : ''}`}
                onClick={() => switchForm('login')}
              >
                <span>登录</span>
              </button>
              <button 
                className={`toggle-btn ${activeForm === 'register' ? 'active' : ''}`}
                onClick={() => switchForm('register')}
              >
                <span>注册</span>
              </button>
            </div>

            {/* 消息显示 */}
            {message.show && (
              <div className={`message ${message.type === 'success' ? 'success-message' : 'error-message'}`}>
                {message.text}
              </div>
            )}

            {/* 登录表单 */}
            <form 
              className={`form ${activeForm === 'login' ? 'active' : ''}`} 
              onSubmit={handleLogin}
            >
              <div className="input-group">
                <label htmlFor="login-email">E-mail</label>
                <div className="input-wrapper">
                  <input
                    type="email"
                    id="login-email"
                    name="email"
                    required
                    placeholder="请输入您的邮箱地址"
                    value={formData.login.email}
                    onChange={(e) => handleInputChange('login', 'email', e.target.value)}
                  />
                </div>
              </div>
              
              <div className="input-group">
                <label htmlFor="login-password">Password</label>
                <div className="input-wrapper">
                  <input
                    type="password"
                    id="login-password"
                    name="password"
                    required
                    placeholder="请输入您的密码"
                    value={formData.login.password}
                    onChange={(e) => handleInputChange('login', 'password', e.target.value)}
                  />
                </div>
              </div>

              <button type="submit" className={`submit-btn ${loading && activeForm === 'login' ? 'loading' : ''}`}>
                <span className="btn-text">登录</span>
                <div className="loading-spinner"></div>
              </button>

              <div className="form-footer">
                <a href="#" onClick={(e) => { e.preventDefault(); showMessage('找回密码功能正在开发中', 'info'); }}>
                  忘记密码？
                </a>
              </div>
            </form>

            {/* 注册表单 */}
            <form 
              className={`form ${activeForm === 'register' ? 'active' : ''}`} 
              onSubmit={handleRegister}
            >
              <div className="input-group">
                <label htmlFor="register-username">Username</label>
                <div className="input-wrapper">
                  <input
                    type="text"
                    id="register-username"
                    name="username"
                    required
                    placeholder="请输入您的用户名"
                    value={formData.register.username}
                    onChange={(e) => handleInputChange('register', 'username', e.target.value)}
                  />
                </div>
              </div>

              <div className="input-group">
                <label htmlFor="register-email">E-mail</label>
                <div className="input-wrapper">
                  <input
                    type="email"
                    id="register-email"
                    name="email"
                    required
                    placeholder="请输入您的邮箱地址"
                    value={formData.register.email}
                    onChange={(e) => handleInputChange('register', 'email', e.target.value)}
                  />
                </div>
              </div>
              
              <div className="input-group">
                <label htmlFor="register-password">Password</label>
                <div className="input-wrapper">
                  <input
                    type="password"
                    id="register-password"
                    name="password"
                    required
                    placeholder="请输入密码（至少6位）"
                    minLength="6"
                    value={formData.register.password}
                    onChange={(e) => handleInputChange('register', 'password', e.target.value)}
                  />
                </div>
              </div>

              <div className="input-group">
                <label htmlFor="confirm-password">Confirm Password</label>
                <div className="input-wrapper">
                  <input
                    type="password"
                    id="confirm-password"
                    name="confirmPassword"
                    required
                    placeholder="请再次输入密码"
                    value={formData.register.confirmPassword}
                    onChange={(e) => handleInputChange('register', 'confirmPassword', e.target.value)}
                  />
                </div>
              </div>

              <div className="checkbox-group">
                <div className="custom-checkbox">
                  <input
                    type="checkbox"
                    id="agree-terms"
                    name="agreeTerms"
                    required
                    checked={formData.register.agreeTerms}
                    onChange={(e) => handleInputChange('register', 'agreeTerms', e.target.checked)}
                  />
                  <span className="checkmark"></span>
                </div>
                <label htmlFor="agree-terms">
                  我已阅读并同意{' '}
                  <a href="#" onClick={(e) => { e.preventDefault(); showMessage('服务条款正在完善中', 'info'); }}>
                    服务条款
                  </a>
                  {' '}和{' '}
                  <a href="#" onClick={(e) => { e.preventDefault(); showMessage('隐私政策正在完善中', 'info'); }}>
                    隐私政策
                  </a>
                </label>
              </div>

              <button type="submit" className={`submit-btn ${loading && activeForm === 'register' ? 'loading' : ''}`}>
                <span className="btn-text">创建账户</span>
                <div className="loading-spinner"></div>
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login; 