import React from 'react';
import MarkdownRenderer from './MarkdownRenderer';
import './UserGuideModal.css';

const UserGuideModal = ({ isVisible, onClose }) => {
  if (!isVisible) return null;

  const guideContent = `

## 🔍Explore - 探索精彩世界
- **不必担心“景点荒”**——收录热门旅游城市及万余条旅游景点，进入目的地城市的详情页面后，
  我们为您提供景点检索、地图浏览、天气信息查询等功能。
  您可以在这里将感兴趣的景点拖入左下角“中转站”区域内。
## 📝Manage - 管理您的行程
- **运筹帷幄**——您可以自由创建、编辑、删除行程，查看行程可视化地图、规划景点间交通路线、
  记录行程的预算及出行人数等其他信息。
- **多样的选择**——您可以将中转站内的景点加入行程，也可以直接输入名称来检索目标景点。
- **分享你的旅行故事**——创建好行程后，您可以设置行程的公开状态，将行程分享到社区。
## 🎒Group Travel - 智能组团
- **寻找志同道合的旅伴**——您可以发起、发现、搜索、申请组团，和旅行偏好一致的其他Voyage Mate用户成为“旅行搭子”。
## 👥Community - 发现
- **“你有一本书，我也有一本书，那么我们就有两本书了”**——您可以浏览其他用户分享的行程详情，获得特别的旅行灵感。
- **成为社区明星吧**——浏览量排行榜前列的作者将展示在页面右侧，您也有可能成为他人的追随对象。
## 👤Profile - 个人档案
- **个性化档案**——您可以编辑个人资料、设置旅行偏好与特殊需求、和AI智能助手交流以获得个性化旅行建议。
- **懒人的选择**——在页面下方添加期望目的地后，可以让AI智能助手针对该目的地，帮您生成独一无二的行程计划。
- **您的Milestone**——您还可以添加个人足迹、生成与查看旅行报告。
## 💬Chat - 与其他用户交流
- **尽情私联！**——添加、删除好友，创建、退出、解散群组，并与好友或群组进行实时聊天交流。
## 更多
- 如使用过程中遇到其他疑问，请通过邮件联系tomorrowonce@sjtu.edu.cn或zyqzyq-041230@sjtu.edu.cn。

`;

  return (
    <div className="modal-overlay">
      <div className="user-guide-modal">
        <div className="guide-modal-header">
          <h2>Voyage Mate使用说明书</h2>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>
        <div className="guide-modal-content">
          <MarkdownRenderer content={guideContent} />
        </div>
      </div>
    </div>
  );
};

export default UserGuideModal;