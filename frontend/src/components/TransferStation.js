import React, { useState, useEffect } from 'react';
import './TransferStation.css';

const TransferStation = ({ isVisible, onToggle, onAddToItinerary }) => {
  const [attractions, setAttractions] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOverItem, setDragOverItem] = useState(null);

  // 从localStorage加载中转站数据
  useEffect(() => {
    const savedAttractions = localStorage.getItem('transferStationAttractions');
    if (savedAttractions) {
      try {
        setAttractions(JSON.parse(savedAttractions));
      } catch (error) {
        console.error('解析中转站数据失败:', error);
        setAttractions([]);
      }
    }
  }, []);



  // 保存中转站数据到localStorage
  const saveAttractions = (newAttractions) => {
    setAttractions(newAttractions);
    localStorage.setItem('transferStationAttractions', JSON.stringify(newAttractions));
  };



  // 添加景点到中转站
  const addAttraction = (attraction) => {
    // 检查是否已存在
    const exists = attractions.find(item => item.id === attraction.id);
    if (!exists) {
      const newAttractions = [...attractions, {
        id: attraction.id,
        name: attraction.name,
        description: attraction.description || '',
        category: attraction.category || '',
        imageUrl: attraction.imageUrl || '',
        joinCount: attraction.joinCount || 0,
        longitude: attraction.longitude,
        latitude: attraction.latitude,
        destinationId: attraction.destinationId,
        destinationName: attraction.destinationName,
        addedAt: new Date().toISOString()
      }];
      saveAttractions(newAttractions);
    }
  };

  // 从中转站移除景点
  const removeAttraction = (attractionId) => {
    const newAttractions = attractions.filter(item => item.id !== attractionId);
    saveAttractions(newAttractions);
  };

  // 清空中转站
  const clearAll = () => {
    if (attractions.length > 0 && window.confirm('确定要清空中转站吗？')) {
      saveAttractions([]);
    }
  };



  // 处理拖拽开始
  const handleDragStart = (e, attraction) => {
    setIsDragging(true);
    e.dataTransfer.setData('application/json', JSON.stringify(attraction));
    e.dataTransfer.effectAllowed = 'move';
  };

  // 处理拖拽结束
  const handleDragEnd = () => {
    setIsDragging(false);
    setDragOverItem(null);
  };

  // 处理拖拽悬停
  const handleDragOver = (e, attractionId) => {
    e.preventDefault();
    setDragOverItem(attractionId);
  };

  // 处理拖拽离开
  const handleDragLeave = () => {
    setDragOverItem(null);
  };

  // 处理拖拽放置
  const handleDrop = (e, targetAttractionId) => {
    e.preventDefault();
    setDragOverItem(null);
    
    try {
      const draggedAttraction = JSON.parse(e.dataTransfer.getData('application/json'));
      
      // 重新排序景点
      const draggedIndex = attractions.findIndex(item => item.id === draggedAttraction.id);
      const targetIndex = attractions.findIndex(item => item.id === targetAttractionId);
      
      if (draggedIndex !== -1 && targetIndex !== -1 && draggedIndex !== targetIndex) {
        const newAttractions = [...attractions];
        const [draggedItem] = newAttractions.splice(draggedIndex, 1);
        newAttractions.splice(targetIndex, 0, draggedItem);
        saveAttractions(newAttractions);
      }
    } catch (error) {
      console.error('处理拖拽放置失败:', error);
    }
  };

  // 处理中转站区域拖拽放置（添加新景点）
  const handleTransferStationDrop = (e) => {
    e.preventDefault();
    setDragOverItem(null);
    
    try {
      const attraction = JSON.parse(e.dataTransfer.getData('application/json'));
      addAttraction(attraction);
    } catch (error) {
      console.error('添加景点到中转站失败:', error);
    }
  };

  // 处理中转站区域拖拽悬停
  const handleTransferStationDragOver = (e) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
  };

  // 生成景点图片URL
  const getAttractionImageUrl = (attraction) => {
    if (attraction.imageUrl) {
      if (attraction.imageUrl.startsWith('http://') || attraction.imageUrl.startsWith('https://')) {
        return attraction.imageUrl;
      }
      if (attraction.imageUrl.startsWith('/')) {
        return `/api${attraction.imageUrl}`;
      }
      return `/api/images/${attraction.imageUrl}`;
    }
    // 使用占位符图片
    const color = Math.floor(Math.random()*16777215).toString(16);
    return `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 150'><rect width='200' height='150' fill='%23${color}'/><text x='100' y='75' font-family='Arial' font-size='16' fill='white' text-anchor='middle' dy='0.3em'>${attraction.name}</text></svg>`;
  };

  // 处理图片加载错误
  const handleImageError = (event, attraction) => {
    const color = Math.floor(Math.random()*16777215).toString(16);
    event.target.src = `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 150'><rect width='200' height='150' fill='%23${color}'/><text x='100' y='75' font-family='Arial' font-size='16' fill='white' text-anchor='middle' dy='0.3em'>${attraction.name}</text></svg>`;
  };

  // 暴露添加景点方法给父组件
  useEffect(() => {
    if (window.addToTransferStation) {
      window.addToTransferStation = addAttraction;
    } else {
      window.addToTransferStation = addAttraction;
    }
  }, []);



  if (!isVisible) {
    return (
      <div className="transfer-station-toggle" onClick={onToggle}>
        <span className="toggle-icon">📦</span>
        <span className="toggle-text">中转站</span>
        {attractions.length > 0 && (
          <span className="attraction-count">{attractions.length}</span>
        )}
      </div>
    );
  }

    return (
    <div className="transfer-station-container">
      <div className="transfer-station-header">
        <div className="header-left">
          <span className="header-icon">📦</span>
          <span className="header-title">中转站</span>
          {attractions.length > 0 && (
            <span className="attraction-count">{attractions.length}</span>
          )}
        </div>
        <div className="header-actions">
          <button 
            className="clear-btn" 
            onClick={clearAll}
            title="清空中转站"
          >
            🗑️
          </button>
          <button 
            className="close-btn" 
            onClick={onToggle}
            title="关闭中转站"
          >
            ✕
          </button>
        </div>
      </div>

      <div 
        className={`transfer-station-content ${attractions.length === 0 ? 'empty' : ''}`}
        onDrop={handleTransferStationDrop}
        onDragOver={handleTransferStationDragOver}
        onDragLeave={() => setDragOverItem(null)}
      >
        {attractions.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📦</div>
            <div className="empty-text">中转站为空</div>
            <div className="empty-hint">从目的地详情页拖拽景点到这里</div>
          </div>
        ) : (
          <div className="attractions-list">
            {attractions.map((attraction, index) => (
              <div
                key={attraction.id}
                className={`attraction-item ${dragOverItem === attraction.id ? 'drag-over' : ''}`}
                draggable
                onDragStart={(e) => handleDragStart(e, attraction)}
                onDragEnd={handleDragEnd}
                onDragOver={(e) => handleDragOver(e, attraction.id)}
                onDragLeave={handleDragLeave}
                onDrop={(e) => handleDrop(e, attraction.id)}
              >
                <div className="attraction-image">
                  <img
                    src={getAttractionImageUrl(attraction)}
                    alt={attraction.name}
                    onError={(e) => handleImageError(e, attraction)}
                  />
                </div>
                <div className="attraction-info">
                  <div className="attraction-name">{attraction.name}</div>
                  <div className="attraction-category">{attraction.category}</div>
                  <div className="attraction-destination">{attraction.destinationName}</div>
                  <div className="attraction-description">
                    {attraction.description.length > 50 
                      ? `${attraction.description.substring(0, 50)}...` 
                      : attraction.description}
                  </div>
                </div>
                <div className="attraction-actions">
                  <button
                    className="add-to-itinerary-btn"
                    onClick={() => onAddToItinerary && onAddToItinerary(attraction)}
                    title="添加到行程"
                  >
                    ➕
                  </button>
                  <button
                    className="remove-btn"
                    onClick={() => removeAttraction(attraction.id)}
                    title="从中转站移除"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TransferStation; 