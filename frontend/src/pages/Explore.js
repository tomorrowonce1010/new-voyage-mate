import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Explore.css';

// 获取 API 基础 URL（根据环境变量）
const API_BASE = process.env.REACT_APP_API_BASE_URL;

const Explore = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const isSelectMode = location.state?.selectMode || false;
  const returnPath = location.state?.returnPath || '/group-travel';
  
  // 从sessionStorage恢复搜索状态，如果没有则使用默认值
  const getInitialSearchState = () => {
    try {
      const savedSearchText = sessionStorage.getItem('explore_searchText') || '';
      const savedActiveTags = JSON.parse(sessionStorage.getItem('explore_activeTags') || '[]');
      return { searchText: savedSearchText, activeTags: savedActiveTags };
    } catch (error) {
      console.error('恢复搜索状态失败:', error);
      return { searchText: '', activeTags: [] };
    }
  };

  const initialState = getInitialSearchState();
  const [searchText, setSearchText] = useState(initialState.searchText);
  const [debouncedSearchText, setDebouncedSearchText] = useState(initialState.searchText);
  const [activeTags, setActiveTags] = useState(initialState.activeTags);
  const [tags, setTags] = useState([]);
  const [destinations, setDestinations] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  // 保存搜索状态到sessionStorage
  const saveSearchState = useCallback((newSearchText, newActiveTags) => {
    try {
      sessionStorage.setItem('explore_searchText', newSearchText);
      sessionStorage.setItem('explore_activeTags', JSON.stringify(newActiveTags));
    } catch (error) {
      console.error('保存搜索状态失败:', error);
    }
  }, []);

  // 当搜索文本变化时，保存到sessionStorage
  useEffect(() => {
    saveSearchState(searchText, activeTags);
  }, [searchText, activeTags, saveSearchState]);

  // 对搜索文本进行防抖处理
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchText(searchText.trim());
    }, 300);

    return () => clearTimeout(timer);
  }, [searchText]);

  // 获取所有标签
  useEffect(() => {
    fetchTags();
  }, []);

  // 计算当前搜索模式和参数
  const searchParams = useMemo(() => {
    const hasSearchText = debouncedSearchText.length > 0;
    const hasTags = activeTags.length > 0;
    
    if (hasSearchText && hasTags) {
      console.log("start searching: semantic + tags combined");
      return { mode: 'semantic_combined', searchText: debouncedSearchText, tags: activeTags };
    } else if (hasSearchText) {
      console.log("start searching: semantic search");
      return { mode: 'semantic_search', searchText: debouncedSearchText, tags: [] };
    } else if (hasTags) {
      console.log("start searching: tags");
      return { mode: 'tags', searchText: '', tags: activeTags };
    } else {
      console.log("start searching: hot");
      return { mode: 'hot', searchText: '', tags: [] };
    }
  }, [debouncedSearchText, activeTags]);

  // 获取目的地数据的函数
  const fetchDestinations = useCallback(async () => {
    setLoading(true);
    try {
      let url = '';
      let isPaginatedResult = false;
      
      const { mode, searchText: searchKeyword, tags: searchTags } = searchParams;
      
      switch (mode) {
        case 'semantic_combined':
          // 语义搜索 + 标签组合（获取所有目的地进行标签过滤）
          const tagParams = searchTags.map(tag => `tags=${encodeURIComponent(tag)}`).join('&');
          url = `/api/destinations/semantic-search-by-tags?${tagParams}&keyword=${encodeURIComponent(searchKeyword)}&size=1000`;
          isPaginatedResult = false; // 语义搜索暂不支持分页，返回完整列表
          break;
          
        case 'semantic_search':
          // 纯语义搜索（获取所有目的地）
          url = `/api/destinations/semantic-search?keyword=${encodeURIComponent(searchKeyword)}&size=1000`;
          isPaginatedResult = false; // 语义搜索暂不支持分页，返回完整列表
          break;
          
        case 'tags':
          // 纯标签过滤（支持分页）
          const tagOnlyParams = searchTags.map(tag => `tags=${encodeURIComponent(tag)}`).join('&');
          url = `/api/destinations/filter-by-tags?${tagOnlyParams}&page=${currentPage}&size=8`;
          isPaginatedResult = true;
          break;
          
        case 'hot':
        default:
          // 热门目的地（支持分页）
          url = `/api/destinations/hot?page=${currentPage}&size=8`;
          isPaginatedResult = true;
          break;
      }

      const response = await fetch(url);
      
      if (response.ok) {
        let data = await response.json();
        
        if (isPaginatedResult && data.content) {
          // 分页数据
          setDestinations(data.content);
          setTotalPages(data.totalPages);
        } else if (!isPaginatedResult && Array.isArray(data)) {
          // 语义搜索返回的数组数据
          setDestinations(data);
          setTotalPages(1); // 语义搜索暂不支持分页，设为1页
        } else {
          // 兜底处理（理论上不应该执行到这里）
          const results = data.content || data;
          setDestinations(results);
          setTotalPages(1);
        }
      }
    } catch (error) {
      console.error('获取目的地数据失败:', error);
      setDestinations([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [searchParams, currentPage]);

  // 当搜索参数变化时，重置页码并触发搜索
  useEffect(() => {
    setCurrentPage(0);
  }, [searchParams.mode, searchParams.searchText, searchParams.tags]);

  // 当搜索参数或页码变化时，获取目的地数据
  useEffect(() => {
    fetchDestinations();
  }, [fetchDestinations]);

  const fetchTags = async () => {
    try {
      const response = await fetch('/api/destinations/tags');
      if (response.ok) {
        const tagList = await response.json();
        setTags(tagList);
      }
    } catch (error) {
      console.error('获取标签失败:', error);
    }
  };

  const handleTagClick = (tag) => {
    setActiveTags(prevTags => {
      if (prevTags.includes(tag)) {
        // 如果标签已选中，取消选择
        return prevTags.filter(t => t !== tag);
      } else {
        // 如果标签未选中，添加到选择列表
        return [...prevTags, tag];
      }
    });
  };

  const handleClearSearch = () => {
    setSearchText('');
    setDebouncedSearchText('');
    setActiveTags([]);
    // 清空sessionStorage中的搜索状态
    try {
      sessionStorage.removeItem('explore_searchText');
      sessionStorage.removeItem('explore_activeTags');
    } catch (error) {
      console.error('清空搜索状态失败:', error);
    }
  };



  const handleCardClick = (destination) => {
    if (isSelectMode) {
      // 选择模式：返回到创建组团页面并传递选中的目的地
      navigate(returnPath, { 
        state: { 
          selectedDestination: destination 
        } 
      });
    } else {
      // 普通模式：跳转到目的地详情页
      navigate(`/destination/${destination.id}`);
    }
  };

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  // 生成目的地图片URL（使用占位符图片）
  const getDestinationImageUrl = (destination) => {
    console.log('处理目的地图片URL:', destination.name, destination.imageUrl);
    
    if (destination.imageUrl) {
      // 如果是完整的HTTP/HTTPS URL，直接使用占位符（避免跨域问题）
      if (destination.imageUrl.startsWith('http://') || destination.imageUrl.startsWith('https://')) {
        console.log('网络图片URL，使用占位符:', destination.imageUrl);
        return getPlaceholderImageUrl(destination.name);
      }
      
      // 如果imageUrl以/开头，添加/api前缀
      if (destination.imageUrl.startsWith('/')) {
        const fullUrl = `/api${destination.imageUrl}`;
        console.log('生成的完整URL:', fullUrl);
        return fullUrl;
      }
      // 如果imageUrl不以/开头，直接添加/api/images/前缀
      const fullUrl = `/api/images/${destination.imageUrl}`;
      console.log('生成的完整URL:', fullUrl);
      return fullUrl;
    }
    // 如果没有图片，使用占位符
    console.log('使用占位符图片');
    return getPlaceholderImageUrl(destination.name);
  };

  // 生成占位符图片URL
  const getPlaceholderImageUrl = (name) => {
    const color = Math.floor(Math.random()*16777215).toString(16);
    return `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 300'><rect width='200' height='300' fill='%23${color}'/><text x='100' y='150' font-family='Arial' font-size='24' fill='white' text-anchor='middle' dy='0.3em'>${name}</text></svg>`;
  };

  // 添加图片加载错误处理
  const handleImageError = (event, destination) => {
    // 检查是否已经处理过错误，避免重复处理
    if (event.target.getAttribute('data-error-handled') === 'true') {
      return;
    }
    
    console.error('目的地图片加载失败:', {
      destination: destination.name,
      originalUrl: destination.imageUrl,
      attemptedUrl: event.target.src,
      error: event
    });
    
    // 如果加载失败，使用占位符图片
    event.target.src = getPlaceholderImageUrl(destination.name);
    
    // 添加一个标记，避免重复处理
    event.target.setAttribute('data-error-handled', 'true');
  };

  // 添加图片加载成功处理
  const handleImageLoad = (event, destination) => {
    console.log('目的地图片加载成功:', {
      destination: destination.name,
      url: event.target.src
    });
  };

  // 显示所有标签
  const getDisplayTags = () => {
    return tags;
  };

  return (
    <div className="explore-page">
      {/* 页面头部 */}
      <header className="page-header">
        <div className="modified-page-title">
          <h1>探索精彩世界</h1>
        </div>
        <div className="modified-page-subtitle">
          <p>发现你的下一个旅行目的地</p>
        </div>
      </header>

      {/* 筛选区域 */}
      <section className="filter-section">
        <div className="filter-row">
          <div className="filter-group" style={{ flexGrow: 1 }}>
            <label htmlFor="search" className="filter-label">搜索目的地或关键词</label>
            <div className="search-input-wrapper">
              <input
                type="text"
                id="search"
                className="search-input"
                placeholder="例如：上海, 海滩, 历史古迹..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />
              {(searchText || activeTags.length > 0) && (
                <button 
                  className="clear-search-btn"
                  onClick={handleClearSearch}
                  title="清空搜索和标签"
                >
                  ✕
                </button>
              )}
            </div>
          </div>
        </div>
        <div className="filter-row">
          <div className="filter-group">
            <label className="filter-label">
              热门标签
            </label>
            <div className="tags-container">
              {getDisplayTags().map(tag => (
                <span
                  key={tag}
                  className={`tag ${activeTags.includes(tag) ? 'active' : ''}`}
                  onClick={() => handleTagClick(tag)}
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* 目的地区域 */}
      <section className="destinations-section">
        <header className="page-header">
          <h2 className="page-title">
            {searchParams.mode === 'hot' ? 
              '近期热门目的地' : 
              searchParams.mode === 'semantic_search' ? 
                `语义搜索结果` : 
              searchParams.mode === 'semantic_combined' ? 
                `语义搜索+标签筛选结果` : 
                `目的地筛选结果`
            }
          </h2>
          {searchParams.mode === 'semantic_search' || searchParams.mode === 'semantic_combined' ? (
            <p className="explore-page-subtitle">
              基于语义理解搜索："{searchParams.searchText}" - 找到 {destinations.length} 个相关目的地
              {searchParams.mode === 'semantic_combined' && searchParams.tags.length > 0 && (
                <span>（已筛选标签：{searchParams.tags.join(', ')}）</span>
              )}
            </p>
          ) : (
            searchParams.mode === 'tags' && searchParams.searchText && (
              <p className="explore-page-subtitle">
                基于标签筛选："{searchParams.searchText}" - 找到 {destinations.length} 个相关目的地
              </p>
            )
          )}
        </header>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <div>
              {searchParams.mode === 'semantic_search' || searchParams.mode === 'semantic_combined' ? '正在使用AI语义搜索...' : '加载中...'}
            </div>
          </div>
        ) : (
          <>
            <div className="attractions-grid">
              {destinations.map(destination => (
                <div
                  key={destination.id}
                  className="attraction-card"
                  onClick={() => handleCardClick(destination)}
                >
                  <img
                    className="card-image"
                    src={getDestinationImageUrl(destination)}
                    alt={destination.name}
                    onError={(e) => handleImageError(e, destination)}
                    onLoad={(e) => handleImageLoad(e, destination)}
                  />
                  <div className="card-info">
                    <div className="card-header">
                      <h3 className="card-title">{destination.name}</h3>
                      <div className="card-tags">
                        <span className="card-tag">热度: {destination.joinCount || 0}</span>
                      </div>
                    </div>
                    <p className="card-description">
                      {destination.description || '暂无描述'}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            {/* 分页控件 */}
            {searchParams.mode !== 'semantic_search' && searchParams.mode !== 'semantic_combined' && totalPages > 1 && (
              <div className="pagination" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '20px 0', gap: '10px' }}>
                <button 
                  onClick={() => handlePageChange(currentPage - 1)} 
                  disabled={currentPage === 0}
                  style={{ padding: '8px 12px', border: '1px solid #ddd', borderRadius: '4px', background: currentPage === 0 ? '#f5f5f5' : '#fff' }}
                >
                  上一页
                </button>
                <span style={{ margin: '0 15px' }}>
                  第 {currentPage + 1} 页 / 共 {totalPages} 页
                </span>
                <button 
                  onClick={() => handlePageChange(currentPage + 1)} 
                  disabled={currentPage >= totalPages - 1}
                  style={{ padding: '8px 12px', border: '1px solid #ddd', borderRadius: '4px', background: currentPage >= totalPages - 1 ? '#f5f5f5' : '#fff' }}
                >
                  下一页
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
};

export default Explore; 