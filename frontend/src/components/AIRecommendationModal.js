import React, { useState } from 'react';
import MarkdownRenderer from './MarkdownRenderer';
import './AIRecommendationModal.css';

const AIRecommendationModal = ({ isOpen, onClose, content, title = "AI智能推荐", isLoading = false }) => {
  const [importing, setImporting] = useState(false);
  const [copying, setCopying] = useState(false);
  const [showJson, setShowJson] = useState(false);

  if (!isOpen) return null;

  // 判断是否为个人档案推荐
  const isProfileRecommendation = title === 'AI个人档案分析推荐';

  // 判断是否为行程规划
  const isItineraryPlan = title.includes('AI定制行程');

  // 解析AI输出的JSON内容
  const parseAIContent = (content) => {
    try {
      console.log('原始AI内容:', content);

      // 如果content已经是对象，直接返回
      if (typeof content === 'object' && content !== null) {
        console.log('内容已经是对象格式:', content);
        return content;
      }

      // 确保content是字符串
      const contentStr = String(content);

      // 尝试从markdown内容中提取JSON
      const jsonMatch = contentStr.match(/```json\s*([\s\S]*?)\s*```/);
      if (jsonMatch) {
        const jsonData = JSON.parse(jsonMatch[1]);
        console.log('从markdown提取的JSON:', jsonData);
        return jsonData;
      }

      // 尝试直接解析整个字符串作为JSON
      try {
        const jsonData = JSON.parse(contentStr);
        console.log('直接解析整个内容为JSON:', jsonData);
        return jsonData;
      } catch (parseError) {
        console.log('无法直接解析为JSON，尝试提取JSON片段');
      }

      // 如果不是完整的JSON，尝试提取JSON片段
      const jsonStart = contentStr.indexOf('{');
      const jsonEnd = contentStr.lastIndexOf('}') + 1;
      if (jsonStart !== -1 && jsonEnd > jsonStart) {
        const jsonStr = contentStr.substring(jsonStart, jsonEnd);
        const jsonData = JSON.parse(jsonStr);
        console.log('从内容片段解析的JSON:', jsonData);
        return jsonData;
      }

      throw new Error('未找到有效的JSON格式');
    } catch (error) {
      console.error('解析AI内容失败:', error);
      throw new Error('AI输出格式不正确，无法导入行程');
    }
  };

  // 注意：原来的searchAttractionId和createItineraryWithActivities函数已经被移除
  // 现在使用新的后端API /api/itineraries/import-ai 来一次性处理所有导入逻辑

  // 处理导入行程 - 使用新的一键导入API
  const handleImportItinerary = async () => {
    if (!content) {
      alert('没有可导入的AI推荐');
      return;
    }

    try {
      console.log('开始处理导入行程');
      setImporting(true);

      // 解析AI内容
      const parsedData = parseAIContent(content);
      console.log('解析后的AI数据:', parsedData);

      // 验证数据
      if (!parsedData.plan || !Array.isArray(parsedData.plan)) {
        throw new Error('AI数据格式不正确，缺少行程计划');
      }

      // 直接调用新的一键导入API
      const response = await fetch('/api/itineraries/import-ai', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(parsedData)
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || `导入失败: ${response.status}`);
      }

      const itinerary = await response.json();
      console.log('行程导入成功:', itinerary);

      // 关闭弹窗
      onClose();
      setImporting(false);

      // 显示成功消息
      alert(`行程"${itinerary.title}"导入成功！`);


    } catch (error) {
      console.error('处理导入失败:', error);
      setImporting(false);
      alert('导入失败: ' + error.message);
    }
  };

  // 复制到剪切板功能
  const handleCopyToClipboard = async () => {
    if (!content) {
      alert('没有可复制的内容');
      return;
    }

    try {
      setCopying(true);

      // 如果content是对象，转换为字符串
      const textContent = typeof content === 'object' ? JSON.stringify(content, null, 2) : content;

      // 使用现代的clipboard API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(textContent);
      } else {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = textContent;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
      }

      alert('内容已复制到剪切板');
    } catch (error) {
      console.error('复制失败:', error);
      alert('复制失败，请手动复制');
    } finally {
      setCopying(false);
    }
  };

  // 注意：原来的确认日期函数和日期选择弹窗已经被移除
  // 现在直接在handleImportItinerary中处理所有导入逻辑

  return (
    <div className="ai-modal-overlay" onClick={onClose}>
      <div className="ai-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ai-modal-header">
          <h2>{title}</h2>
          <button className="close-button" onClick={onClose}>×</button>
        </div>
        
        <div className="ai-modal-content">
          {isLoading ? (
            <div className="loading">AI正在生成推荐...</div>
          ) : (
            <>
              
                {isProfileRecommendation && (
                  <>
                  <MarkdownRenderer content={content} />
                  <div className="ai-modal-actions">
                    <button
                      className="copy-button"
                      onClick={handleCopyToClipboard}
                      disabled={copying}
                    >
                      {copying ? '复制中...' : '复制到剪切板'}
                    </button>
                  </div>
                  </>
                )}
                {isItineraryPlan && (
                  <>
                    <p style={{ textAlign: 'center', marginBottom: '10px' }}>智能行程已经生成♪～(´ε｀　)</p>
                    {showJson && (
                      <pre className="json-content">
                        {JSON.stringify(parseAIContent(content), null, 2)}
                      </pre>
                    )}
                    
                    <div className="ai-modal-actions">
                    <button
                      className="view-json-button"
                      onClick={() => setShowJson(!showJson)}
                    >
                      {showJson ? '隐藏JSON' : '查看JSON'}
                    </button>
                    
                    <button
                    className="import-button"
                    onClick={handleImportItinerary}
                    disabled={importing}
                  >
                    {importing ? '导入中...' : '一键导入行程'}
                  </button>
                  </div>
                  </>
                )}
              
              
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default AIRecommendationModal; 