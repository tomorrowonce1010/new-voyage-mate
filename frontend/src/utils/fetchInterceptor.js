/*
  fetchInterceptor.js
  安装全局 fetch 拦截器：当后端返回 401（未登录或登录过期）时，若当前页面不是登录页，则自动跳转到 /login
*/

let interceptorInstalled = false;

export function installFetchInterceptor() {
  if (typeof window === 'undefined') return; // SSR 安全检查
  if (interceptorInstalled) return; // 只安装一次

  interceptorInstalled = true;

  const originalFetch = window.fetch.bind(window);

  window.fetch = async (...args) => {
    const response = await originalFetch(...args);

    try {
      if (response.status === 401) {
        const currentPath = window.location.pathname;
        // 避免在登录页面上造成跳转循环
        if (!currentPath.startsWith('/login')) {
          // 使用完整刷新以清理可能残留的状态
          window.location.href = '/login';
        }
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error('fetch interceptor error', e);
    }

    return response;
  };
} 