import AMapLoader from '@amap/amap-jsapi-loader';

// 设置高德地图安全密钥
window._AMapSecurityConfig = {
    securityJsCode: '48a09726706b392b4c3c8907ac8cff2b',
};

class AMapManager {
    constructor() {
        this.AMap = null;
        this.loading = false;
        this.loadPromise = null;
    }

    /**
     * 加载高德地图API
     * @param {Array} plugins 需要加载的插件
     * @returns {Promise} 返回AMap实例
     */
    async load(plugins = []) {
        // 如果已经加载完成，直接返回
        if (this.AMap) {
            return this.AMap;
        }

        // 如果正在加载，返回加载中的Promise
        if (this.loading && this.loadPromise) {
            return this.loadPromise;
        }

        // 开始加载
        this.loading = true;
        this.loadPromise = AMapLoader.load({
            key: '0a98c59a1aac46b8430bed72a75cff36',
            version: '2.0',
            plugins: plugins
        }).then((AMap) => {
            this.AMap = AMap;
            this.loading = false;
            return AMap;
        }).catch((error) => {
            this.loading = false;
            this.loadPromise = null;
            console.error('高德地图API加载失败:', error);
            throw error;
        });

        return this.loadPromise;
    }

    /**
     * 获取已加载的AMap实例
     * @returns {Object|null} AMap实例
     */
    getAMap() {
        return this.AMap;
    }

    /**
     * 检查是否已加载
     * @returns {boolean}
     */
    isLoaded() {
        return !!this.AMap;
    }

    /**
     * 重置状态（用于测试或重新加载）
     */
    reset() {
        this.AMap = null;
        this.loading = false;
        this.loadPromise = null;
    }

    /**
     * 创建地图实例
     * @param {string} container 容器ID
     * @param {Object} options 地图配置选项
     * @returns {Promise} 返回地图实例
     */
    async createMap(container, options = {}) {
        const AMap = await this.load();
        return new AMap.Map(container, {
            viewMode: '2D',
            zoom: 8,
            center: [116.397428, 39.90923],
            ...options
        });
    }

    /**
     * 路径规划
     * @param {Object} params 路径规划参数
     * @returns {Promise} 返回路径规划结果
     */
    async routePlanning(params) {
        const { from, to, mode } = params;
        const AMap = await this.load(['AMap.Driving', 'AMap.Transfer', 'AMap.Walking', 'AMap.Riding']);

        return new Promise((resolve) => {
            if (mode === '驾车') {
                const driving = new AMap.Driving({ policy: 0 });
                driving.search(from, to, (status, result) => {
                    let duration = null;
                    if (status === 'complete' && result && result.routes && result.routes.length > 0) {
                        duration = result.routes[0].duration;
                    }
                    resolve(duration);
                });
            } else {
                let planner;
                if (mode === '公交' || mode === '公共交通') {
                    planner = new AMap.Transfer({ city: '上海' });
                } else if (mode === '步行') {
                    planner = new AMap.Walking();
                } else if (mode === '骑行' || mode === '骑乘') {
                    planner = new AMap.Riding();
                } else {
                    resolve(null);
                    return;
                }

                planner.search(from, to, (status, result) => {
                    let duration = null;
                    if (status === 'complete') {
                        if (result.routes && result.routes.length > 0) {
                            duration = result.routes[0].duration;
                        } else if (result.plans && result.plans.length > 0) {
                            duration = result.plans[0].duration;
                        }
                    }
                    resolve(duration);
                });
            }
        });
    }

    /**
     * 地点搜索
     * @param {string} keyword 搜索关键词
     * @returns {Promise} 返回搜索结果
     */
    async searchPlaces(keyword) {
        const AMap = await this.load(['AMap.PlaceSearch', 'AMap.Geocoder']);
        
        return new Promise((resolve) => {
            const placeSearch = new AMap.PlaceSearch({
                pageSize: 15,
                pageIndex: 1,
                city: '全国'
            });
            
            const geocoder = new AMap.Geocoder();
            
            placeSearch.search(keyword, (status, result) => {
                if (status === 'complete' && result && result.poiList && result.poiList.pois.length > 0) {
                    const poisWithCity = result.poiList.pois.map(poi => {
                        return new Promise((resolvePoi) => {
                            geocoder.getAddress(poi.location, (status, result) => {
                                let city = '';
                                
                                if (status === 'complete' && result && result.regeocode) {
                                    const addressComponent = result.regeocode.addressComponent;
                                    if (addressComponent.city) {
                                        city = addressComponent.city.replace(/(市|盟|自治州)$/, '');
                                    } else if (addressComponent.province) {
                                        city = addressComponent.province.replace(/(省|市|盟|自治州)$/, '');
                                    }
                                }
                                
                                if (!city) {
                                    if (poi.cityname) {
                                        city = poi.cityname.replace(/(市|盟|自治州)$/, '');
                                    } else if (poi.city) {
                                        city = poi.city.replace(/(市|盟|自治州)$/, '');
                                    } else if (poi.adname) {
                                        city = poi.adname.replace(/(市|盟|自治州)$/, '');
                                    }
                                }
                                
                                resolvePoi({
                                    id: poi.id,
                                    name: poi.name,
                                    address: poi.address,
                                    location: poi.location,
                                    longitude: poi.location.lng,
                                    latitude: poi.location.lat,
                                    type: poi.type,
                                    tel: poi.tel,
                                    city: city || '上海',
                                    description: poi.address || ''
                                });
                            });
                        });
                    });
                    
                    Promise.all(poisWithCity).then(pois => {
                        resolve(pois);
                    });
                } else {
                    resolve([]);
                }
            });
        });
    }

    /**
     * 天气查询
     * @param {string} city 城市名称
     * @returns {Promise} 返回天气信息
     */
    async getWeather(city) {
        const AMap = await this.load(['AMap.Weather']);
        
        return new Promise((resolve) => {
            const weather = new AMap.Weather();
            const result = {
                live: null,
                forecast: []
            };

            // 实时天气
            weather.getLive(city, (err, data) => {
                if (!err && data) {
                    result.live = data;
                }
                
                // 预报天气
                weather.getForecast(city, (err, data) => {
                    if (!err && data && data.forecasts) {
                        result.forecast = data.forecasts;
                    }
                    resolve(result);
                });
            });
        });
    }
}

// 创建单例实例
const amapManager = new AMapManager();

export default amapManager; 