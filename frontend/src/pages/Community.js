import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Community.css';

const Community = () => {
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState('');
  const [currentTrips, setCurrentTrips] = useState([]);
  const [activeFilters, setActiveFilters] = useState([]);
  const [activeHotTags, setActiveHotTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [allTags, setAllTags] = useState([]);
  const [popularTags, setPopularTags] = useState([]);
  const [popularAuthors, setPopularAuthors] = useState([]);
  const [activeSearchTags, setActiveSearchTags] = useState([]);
  const [searchMode, setSearchMode] = useState('semantic'); // 'semantic' | 'shareCode' | 'destination' | 'author'
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchLoading, setSearchLoading] = useState(false);
  const [sortBy, setSortBy] = useState('time'); // 'time' | 'popularity'
  const [isSearching, setIsSearching] = useState(false); // 是否正在搜索状态

  // 获取公共社区条目
  const fetchPublicCommunityEntries = async (sortType = sortBy, page = 0) => {
    try {
      setLoading(true);
      const response = await fetch(`/api/community/public/sorted?sortBy=${sortType}&page=${page}&size=10`, {
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        // 转换数据格式以匹配原有的tripsData结构
        const transformedData = data.content.map(entry => ({
          id: entry.itinerary.id,
          entryId: entry.id,
          title: entry.itinerary.title,
          description: entry.description || '暂无描述',
          duration: calculateDuration(entry.itinerary.startDate, entry.itinerary.endDate),
          author: entry.itinerary.user?.username || '未知用户',
          authorId: entry.itinerary.user?.id, // 添加用户ID，用于跳转
          authorAvatar: entry.itinerary.user?.username?.charAt(0) || '?',
          views: entry.viewCount || 0,
          category: 'public',
          tags: (entry.tags && entry.tags.length > 0) ? entry.tags : ['公共分享'],
          emoji: getDestinationEmoji(entry.itinerary.destination),
          shareCode: entry.shareCode,
          coverImageUrl: entry.itinerary.coverImageUrl ? `/api/static${entry.itinerary.coverImageUrl}` : null,
          startDate: entry.itinerary.startDate,
          endDate: entry.itinerary.endDate,
          publishedAt: entry.createdAt,
          destination: entry.itinerary.destination
        }));
        setCurrentTrips(transformedData);
        setCurrentPage(data.currentPage);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      } else {
        setError('获取社区数据失败');
      }
    } catch (error) {
      console.error('获取社区数据失败:', error);
      setError('网络错误，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 计算行程天数
  const calculateDuration = (startDate, endDate) => {
    if (!startDate || !endDate) return '未知天数';
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return `${diffDays}天`;
  };

  // 根据目的地获取emoji
  const getDestinationEmoji = (destination) => {
    if (!destination) return '🌍';
    const dest = destination.toLowerCase();
    if (dest.includes('日本') || dest.includes('东京') || dest.includes('京都')) return '🇯🇵';
    if (dest.includes('北京')) return '🏛️';
    if (dest.includes('杭州')) return '🌸';
    if (dest.includes('成都')) return '🐼';
    if (dest.includes('西安')) return '🏺';
    if (dest.includes('云南')) return '🏔️';
    if (dest.includes('广州') || dest.includes('深圳')) return '🏙️';
    if (dest.includes('福建')) return '🏮';
    return '🌍';
  };

  // 将后台返回的社区条目转换为前端展示所需格式（支持分享码搜索返回单条数据）
  const transformCommunityEntry = (entry) => ({
    id: entry.itinerary.id,
    entryId: entry.id,
    title: entry.itinerary.title,
    description: entry.description || '暂无描述',
    duration: calculateDuration(entry.itinerary.startDate, entry.itinerary.endDate),
    author: entry.itinerary.user?.username || '未知用户',
    authorId: entry.itinerary.user?.id,
    authorAvatar: entry.itinerary.user?.username?.charAt(0) || '?',
    views: entry.viewCount || 0,
    category: 'public',
    tags: (entry.tags && entry.tags.length > 0) ? entry.tags : ['公共分享'],
    emoji: getDestinationEmoji(entry.itinerary.destination),
    shareCode: entry.shareCode,
    coverImageUrl: entry.itinerary.coverImageUrl ? `/api/static${entry.itinerary.coverImageUrl}` : null,
    startDate: entry.itinerary.startDate,
    endDate: entry.itinerary.endDate,
    publishedAt: entry.createdAt,
    destination: entry.itinerary.destination
  });

  // 获取辅助数据：所有标签、热门标签、热门作者
  const fetchAuxiliaryData = async () => {
    try {
      // 所有标签（30 条）
      const tagsRes = await fetch('/api/itineraries/tags', { credentials: 'include' });
      if (tagsRes.ok) {
        const t = await tagsRes.json();
        setAllTags(Array.isArray(t) ? t.map(tag=>tag.tag) : []);
      }
      // 热门标签
      const hotRes = await fetch('/api/community/popular/tags?limit=20');
      if (hotRes.ok) {
        const pt = await hotRes.json();
        setPopularTags(pt.map(p=>p.tag));
      }
      // 热门作者
      const authorRes = await fetch('/api/community/popular/authors?limit=5');
      if (authorRes.ok) {
        const pa = await authorRes.json();
        setPopularAuthors(pa);
      }
    } catch(err) {
      console.error('获取标签/作者失败', err);
    }
  };

  // 页面加载动画和初始化数据
  useEffect(() => {
    const timer = setTimeout(() => {
      const pageContent = document.querySelector('.page-content');
      if (pageContent) {
        pageContent.classList.add('loaded');
      }
    }, 100);

    // 获取社区数据
    fetchPublicCommunityEntries();
    fetchAuxiliaryData();

    return () => clearTimeout(timer);
  }, []);

  // 搜索功能（支持多种搜索模式）
  const handleSearch = async (searchTerm, currentTags = null) => {
    const term = searchTerm.trim();
    // 使用传入的标签状态，如果没有传入则使用当前状态
    const tagsToUse = currentTags !== null ? currentTags : activeSearchTags;

    // 如果搜索文本为空且没有标签，返回默认的社区条目展示结果
    if (!term && tagsToUse.length === 0) {
      setIsSearching(false);
      await fetchPublicCommunityEntries(sortBy, 0);
      return;
    }

    setIsSearching(true);
    setSearchLoading(true);

    try {
      if (searchMode === 'shareCode') {
        // 分享码搜索模式
        const res = await fetch(`/api/community/share/${encodeURIComponent(term)}`, { credentials: 'include' });
        if (res.ok) {
          const entry = await res.json();
          setCurrentTrips([transformCommunityEntry(entry)]);
          setCurrentPage(0);
          setTotalPages(1);
          setTotalElements(1);
        } else {
          setCurrentTrips([]);
          setCurrentPage(0);
          setTotalPages(0);
          setTotalElements(0);
        }
      } else if (searchMode === 'destination') {
        // 目的地关键词搜索模式
        const url = `/api/community/search/destination?destination=${encodeURIComponent(term)}&page=${currentPage}&size=10`;
        const searchRes = await fetch(url, { credentials: 'include' });
        if (searchRes.ok) {
          const data = await searchRes.json();
          const transformedData = data.content.map(entry => transformCommunityEntry(entry));
          setCurrentTrips(transformedData);
          setCurrentPage(data.currentPage);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        } else {
          setCurrentTrips([]);
          setCurrentPage(0);
          setTotalPages(0);
          setTotalElements(0);
        }
      } else if (searchMode === 'author') {
        // 作者语义搜索模式
        const url = `/api/community/semantic/search/authors?q=${encodeURIComponent(term)}&page=${currentPage}&size=10`;
        const searchRes = await fetch(url, { credentials: 'include' });
        if (searchRes.ok) {
          const data = await searchRes.json();
          // 作者搜索现在返回的是这些作者的公开行程
          const transformedData = data.content.map(entry => transformCommunityEntry(entry));
          setCurrentTrips(transformedData);
          setCurrentPage(data.currentPage);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        } else {
          setCurrentTrips([]);
          setCurrentPage(0);
          setTotalPages(0);
          setTotalElements(0);
        }
      } else {
        // 语义搜索模式
        if (!term && tagsToUse.length > 0) {
          // 仅标签筛选
          setIsSearching(true);
          let url = `/api/community/search/tags?page=${currentPage}&size=10`;
          tagsToUse.forEach(tag => {
            url += `&tags=${encodeURIComponent(tag)}`;
          });
          const res = await fetch(url, { credentials: 'include' });
          if (res.ok) {
            const data = await res.json();
            setCurrentTrips(data.content.map(transformCommunityEntry));
            setCurrentPage(data.currentPage);
            setTotalPages(data.totalPages);
            setTotalElements(data.totalElements);
          } else {
            setCurrentTrips([]);
            setCurrentPage(0);
            setTotalPages(0);
            setTotalElements(0);
          }
          setSearchLoading(false);
          return;
        }
        // 原有语义/组合搜索逻辑
        let url = `/api/community/semantic/search?q=${encodeURIComponent(term)}&page=${currentPage}&size=10`;
        if (tagsToUse.length > 0) {
          url = `/api/community/semantic/search/tags?q=${encodeURIComponent(term)}&page=${currentPage}&size=10`;
          tagsToUse.forEach(tag => {
            url += `&tags=${encodeURIComponent(tag)}`;
          });
        }
        const searchRes = await fetch(url, { credentials: 'include' });
        if (searchRes.ok) {
          const searchData = await searchRes.json();
          const transformedData = searchData.content.map(entry => transformCommunityEntry(entry));
          setCurrentTrips(transformedData);
          setCurrentPage(searchData.currentPage);
          setTotalPages(searchData.totalPages);
          setTotalElements(searchData.totalElements);
        } else {
          // 如果语义搜索失败，回退到传统搜索
          const fallbackRes = await fetch(`/api/community/search?q=${encodeURIComponent(term)}`, { 
            credentials: 'include' 
          });
          if (fallbackRes.ok) {
            const fallbackData = await fallbackRes.json();
            const transformedData = fallbackData.map(entry => transformCommunityEntry(entry));
            setCurrentTrips(transformedData);
            setCurrentPage(0);
            setTotalPages(1);
            setTotalElements(transformedData.length);
          } else {
            setCurrentTrips([]);
            setCurrentPage(0);
            setTotalPages(0);
            setTotalElements(0);
          }
        }
      }
    } catch (e) {
      console.error('搜索失败:', e);
      setCurrentTrips([]);
      setCurrentPage(0);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setSearchLoading(false);
    }
  };

  // 筛选功能 - 基于目的地和内容筛选
  const handleFilter = (filter) => {
    setActiveFilters(filter === 'all' ? [] : [filter]);
    setSearchInput(''); // 清空搜索框

    // 重新获取数据并应用筛选
    fetchPublicCommunityEntries().then(() => {
      if (filter === 'all') {
        // 显示所有数据
        return;
      }

      // 根据筛选器过滤数据
      setCurrentTrips(prevTrips =>
          prevTrips.filter(trip => {
            const dest = trip.destination.toLowerCase();
            switch (filter) {
              case 'domestic':
                return !dest.includes('日本') && !dest.includes('国外');
              case 'international':
                return dest.includes('日本') || dest.includes('国外');
              case 'weekend':
                return parseInt(trip.duration) <= 3;
              case 'family':
                return trip.description.toLowerCase().includes('家庭') ||
                    trip.description.toLowerCase().includes('亲子');
              case 'photography':
                return trip.description.toLowerCase().includes('摄影') ||
                    trip.description.toLowerCase().includes('拍照');
              default:
                return true;
            }
          })
      );
    });
  };

  // 查看行程详情
  const viewTrip = (trip) => {
    // 增加社区条目查看次数
    if (trip.entryId) {
      fetch(`/api/community/${trip.entryId}/view`, {
        method: 'POST',
        credentials: 'include'
      }).catch(()=>{});
    }
    navigate(`/view-itinerary/${trip.id}?from=community`);
  };

  // 热门标签点击事件 - 支持切换
  const handleTagClick = (tagText) => {
    const cleanTag = tagText.replace('#', '');

    let newActiveHotTags;
    if (activeHotTags.includes(cleanTag)) {
      // 如果已激活，则取消激活
      newActiveHotTags = activeHotTags.filter(tag => tag !== cleanTag);
    } else {
      // 如果未激活，则激活
      newActiveHotTags = [...activeHotTags, cleanTag];
    }

    setActiveHotTags(newActiveHotTags);

    // 使用后端API进行标签搜索
    if (newActiveHotTags.length === 0) {
      // 如果没有选中标签，重新获取所有数据
      setIsSearching(false);
      fetchPublicCommunityEntries(sortBy, 0);
      setSearchInput('');
      return;
    }

    // 调用后端标签搜索API
    const searchByTags = async () => {
      setIsSearching(true);
      setSearchLoading(true);
      try {
        let url = `/api/community/search/tags?page=0&size=10`;
        newActiveHotTags.forEach(tag => {
          url += `&tags=${encodeURIComponent(tag)}`;
        });
        
        const res = await fetch(url, { credentials: 'include' });
        if (res.ok) {
          const data = await res.json();
          setCurrentTrips(data.content.map(transformCommunityEntry));
          setCurrentPage(data.currentPage);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        } else {
          setCurrentTrips([]);
          setCurrentPage(0);
          setTotalPages(0);
          setTotalElements(0);
        }
      } catch (e) {
        console.error('标签搜索失败:', e);
        setCurrentTrips([]);
        setCurrentPage(0);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        setSearchLoading(false);
      }
    };

    searchByTags();
    // 不在搜索框显示标签文本
    // setSearchInput(newActiveHotTags.join(' '));

    // 清空筛选器状态
    setActiveFilters([]);
  };

  // 格式化日期
  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const m = (d.getMonth()+1).toString().padStart(2,'0');
    const day = d.getDate().toString().padStart(2,'0');
    return `${d.getFullYear()}-${m}-${day}`;
  };

  const toggleSearchTag = (tag) => {
    setActiveSearchTags(prevTags => {
      let newTags;
      if (prevTags.includes(tag)) {
        newTags = prevTags.filter(t => t !== tag);
      } else {
        newTags = [...prevTags, tag];
      }
      
      // 直接使用新的标签状态进行搜索
      handleSearch(searchInput, newTags);
      
      return newTags;
    });
  };

  // 切换搜索模式
  const toggleSearchMode = () => {
    const modes = ['semantic', 'destination', 'author', 'shareCode'];
    const currentIndex = modes.indexOf(searchMode);
    const nextIndex = (currentIndex + 1) % modes.length;
    setSearchMode(modes[nextIndex]);
    // 重置所有搜索相关状态
    setSearchInput('');
    setActiveSearchTags([]);
    setActiveHotTags([]); // 清空热门标签选择
    setCurrentPage(0);
    setTotalPages(0);
    setTotalElements(0);
    setIsSearching(false); // 重置搜索状态
    // 重新获取默认数据
    fetchPublicCommunityEntries(sortBy, 0);
  };
  
  // 获取搜索模式显示名称
  const getSearchModeName = () => {
    switch (searchMode) {
      case 'semantic': return '语义搜索';
      case 'destination': return '目的地搜索';
      case 'author': return '作者搜索';
      case 'shareCode': return '分享码搜索';
      default: return '语义搜索';
    }
  };
  
  // 获取搜索模式图标
  const getSearchModeIcon = () => {
    switch (searchMode) {
      case 'semantic': return '🧠';
      case 'destination': return '🌍';
      case 'author': return '👤';
      case 'shareCode': return '🔗';
      default: return '🧠';
    }
  };
  
  // 切换排序方式
  const handleSortChange = async (newSortBy) => {
    setSortBy(newSortBy);
    if (!isSearching) {
      // 如果不在搜索状态，重新获取默认数据
      await fetchPublicCommunityEntries(newSortBy, 0);
    }
  };
  
  // 页面变化处理
  const handlePageChange = async (newPage) => {
    if (newPage < 0 || newPage >= totalPages) return;
    
    setCurrentPage(newPage);
    
    if (isSearching) {
      // 如果在搜索状态，重新执行搜索
      await handleSearch(searchInput, activeSearchTags);
    } else {
      // 如果不在搜索状态，获取默认数据
      await fetchPublicCommunityEntries(sortBy, newPage);
    }
  };
  
  // 获取搜索占位符文本
  const getSearchPlaceholder = () => {
    switch (searchMode) {
      case 'semantic': return "语义搜索行程名称、描述、作者、目的地...";
      case 'destination': return "输入目的地关键词，如：北京、上海、杭州...";
      case 'author': return "输入用户名...";
      case 'shareCode': return "输入分享码搜索特定行程...";
      default: return "语义搜索行程名称、描述、作者、目的地...";
    }
  };



  // 跳转到用户主页
  const goToUserHomepage = (authorId, event) => {
    event.stopPropagation(); // 阻止事件冒泡，避免触发查看行程
    if (authorId) {
      navigate(`/user-homepage/${authorId}?from=community`);
    }
  };

  return (
      <div className="community-container">
        <div className="page-content">
          {/* 页面头部 */}
          <header className="page-header">
            <h1 className="community-page-title">发现精彩行程</h1>
            <p className="community-page-subtitle">解锁无限旅行可能</p>
          </header>

          {/* 搜索区域 */}
          <section className="search-section">
            <div className="search-container">
              <input
                  type="text"
                  className="search-input"
                  placeholder={getSearchPlaceholder()}
                  value={searchInput}
                  onChange={(e) => {
                    setSearchInput(e.target.value);
                    if (searchMode === 'shareCode') {
                      // 分享码搜索模式下，延迟搜索
                      setTimeout(() => handleSearch(e.target.value, activeSearchTags), 500);
                    } else {
                      handleSearch(e.target.value, activeSearchTags);
                    }
                  }}
              />
              <button 
                className="search-mode-toggle"
                onClick={toggleSearchMode}
                title={`当前模式：${getSearchModeName()}，点击切换`}
              >
                {getSearchModeIcon()}
              </button>
            </div>
            <div className="search-mode-info">
              <span className="search-mode-label">{getSearchModeName()}</span>
            </div>
            {searchMode === 'semantic' && (
              <div className="search-filters">
                <div className="tags-container" style={{marginTop:'12px'}}>
                  {allTags.map((tag,idx)=>(
                      <span key={idx} className={`tag ${activeSearchTags.includes(tag)?'active':''}`} onClick={()=>toggleSearchTag(tag)}>
                  #{tag}
                </span>
                  ))}
                </div>
              </div>
            )}

            
            {searchLoading && (
              <div className="search-loading">
                <span>搜索中...</span>
              </div>
            )}
            {totalElements > 0 && (
              <div className="search-results-info">
                {isSearching && (
                  <div className="current-search-info">
                    <span className="search-status-label">当前搜索：</span>
                    {searchInput.trim() && (
                      <span className="search-text-display">
                        <span className="search-text-label">文本：</span>
                        <span className="search-text-value">"{searchInput}"</span>
                      </span>
                    )}
                    {(activeSearchTags.length > 0 || activeHotTags.length > 0) && (
                      <span className="search-tags-display">
                        <span className="search-tags-label">标签：</span>
                        {activeSearchTags.map((tag, index) => (
                          <span key={index} className="search-tag-item">
                            #{tag}
                          </span>
                        ))}
                        {activeHotTags.map((tag, index) => (
                          <span key={index} className="search-tag-item">
                            #{tag}
                          </span>
                        ))}
                      </span>
                    )}
                  </div>
                )}
                <div className="results-count">找到 {totalElements} 个结果</div>
                {totalPages > 1 && (
                  <div className="pagination">
                    <button 
                      onClick={() => handlePageChange(currentPage - 1)}
                      disabled={currentPage === 0}
                      className="page-btn"
                    >
                      上一页
                    </button>
                    <span className="page-info">
                      {currentPage + 1} / {totalPages}
                    </span>
                    <button 
                      onClick={() => handlePageChange(currentPage + 1)}
                      disabled={currentPage >= totalPages - 1}
                      className="page-btn"
                    >
                      下一页
                    </button>
                  </div>
                )}
              </div>
            )}
          </section>

          <div className="community-content">
            {/* 推荐行程 */}
            <section className="recommended-trips">
              <div className="section-header">
                <h3 className="section-title">
                  {isSearching ? '搜索结果' : '推荐行程'}
                </h3>
                {!isSearching && (
                  <div className="sort-options">
                    <span className="sort-label">排序：</span>
                    <button 
                      className={`sort-btn ${sortBy === 'time' ? 'active' : ''}`}
                      onClick={() => handleSortChange('time')}
                    >
                      最新
                    </button>
                    <button 
                      className={`sort-btn ${sortBy === 'popularity' ? 'active' : ''}`}
                      onClick={() => handleSortChange('popularity')}
                    >
                      最热
                    </button>
                  </div>
                )}
              </div>

              {loading && (
                  <div className="loading-container">
                    <p>正在加载社区行程...</p>
                  </div>
              )}

              {error && (
                  <div className="error-container">
                    <p>{error}</p>
                    <button onClick={fetchPublicCommunityEntries} className="retry-button">
                      重试
                    </button>
                  </div>
              )}

              {!loading && !error && (
                  <div className="community-trip-grid">
                    {currentTrips.length === 0 ? (
                        <div className="no-trips-message">
                          <p>{isSearching ? '暂无搜索结果' : '暂无公共行程分享'}</p>
                        </div>
                    ) : (
                        currentTrips.map(trip => (
                            <div
                                key={trip.id}
                                className="community-trip-card"
                                onClick={() => viewTrip(trip)}
                            >
                              <div className="community-trip-image">
                                {trip.coverImageUrl ? (
                                    <img src={trip.coverImageUrl} alt={trip.title} />
                                ) : (
                                    <span>{trip.emoji}</span>
                                )}
                                <div className="trip-duration">{trip.duration}</div>
                                {trip.startDate && trip.endDate && (
                                    <div className="trip-date-range" style={{position:'absolute',top:'8px',left:'8px',background:'rgba(0,0,0,0.6)',color:'#fff',padding:'2px 6px',borderRadius:'4px',fontSize:'12px'}}>
                                      {formatDate(trip.startDate)} ~ {formatDate(trip.endDate)}
                                    </div>
                                )}
                              </div>
                              <div className="community-trip-content">
                                <h4 className="community-trip-title">{trip.title}</h4>
                                <p className="community-trip-description">{trip.description}</p>
                                <div className="trip-tags">
                                  {trip.tags && trip.tags.slice(0,5).map((t,i)=>(
                                      <span key={i} className="tag-item">{t}</span>
                                  ))}
                                </div>
                                {trip.author && (
                                      <div 
                                        className="trip-author clickable-author" 
                                        onClick={(e) => goToUserHomepage(trip.authorId, e)}
                                        title={`查看 ${trip.author} 的主页`}
                                      >
                                        <div className="author-avatar">{trip.authorAvatar}</div>
                                        <span>{trip.author}</span>
                                      </div>
                                    )}
                                <div className="trip-meta">
                                  {trip.publishedAt && (
                                      <div style={{fontSize:'12px',color:'#888',marginBottom:'4px'}}>发布于 {formatDate(trip.publishedAt)}</div>
                                  )}
                                  <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
                                    
                                    <div className="trip-stats">
                                      <div className="stat-item">
                                        <span>👁️</span>
                                        <span>{trip.views}</span>
                                      </div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                        ))
                    )}
                  </div>
              )}
            </section>

            {/* 侧边栏内容 */}
            <aside className="sidebar-content">
              {/* 热门标签 */}
              <section className="popular-tags">
                <h3 className="section-title">热门标签</h3>
                <div className="tags-container">
                  {popularTags.map((tag,idx)=>(
                      <span key={idx} className={`tag ${activeHotTags.includes(tag)?'active':''}`} onClick={()=>handleTagClick(tag)}>#{tag}</span>
                  ))}
                </div>
              </section>

                        {/* 热门作者 */}
          <section className="popular-authors">
            <h3 className="section-title">热门作者</h3>
            <div className="author-list">
              {popularAuthors.map((au,idx)=>(
                <div 
                  key={au.userId} 
                  className="author-item clickable-author" 
                  onClick={() => goToUserHomepage(au.userId, {stopPropagation: () => {}})}
                  title={`查看 ${au.username} 的主页`}
                >
                  <div className="author-large-avatar">{au.username.charAt(0)}</div>
                  <div className="author-info">
                    <div className="author-name">{au.username}</div>
                    <div className="author-trips">总浏览 {au.totalViews}</div>
                  </div>
                </div>
              ))}
            </div>
          </section>
            </aside>
          </div>
        </div>
      </div>
  );
};

export default Community; 