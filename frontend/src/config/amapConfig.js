/**
 * 高德地图配置
 * 从环境变量中读取API密钥，避免硬编码在代码中
 */

const amapConfig = {
    // 高德地图 API Key
    key: process.env.REACT_APP_AMAP_KEY || '',
    // 高德地图安全密钥
    securityJsCode: process.env.REACT_APP_AMAP_SECURITY_CODE || '',
    // API版本
    version: '2.0',
};

// 开发环境下的警告
if (process.env.NODE_ENV === 'development') {
    if (!amapConfig.key) {
        console.warn('⚠️ 高德地图 API Key 未配置，请在 .env 文件中设置 REACT_APP_AMAP_KEY');
    }
    if (!amapConfig.securityJsCode) {
        console.warn('⚠️ 高德地图安全密钥未配置，请在 .env 文件中设置 REACT_APP_AMAP_SECURITY_CODE');
    }
}

export default amapConfig;

