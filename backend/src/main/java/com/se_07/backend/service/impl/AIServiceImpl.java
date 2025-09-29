package com.se_07.backend.service.impl;

import com.se_07.backend.dto.AIRecommendationRequest;
import com.se_07.backend.dto.AIRecommendationResponse;
import com.se_07.backend.service.AIService;
import com.se_07.backend.repository.DestinationRepository;
import com.se_07.backend.repository.AttractionRepository;
import com.se_07.backend.entity.Destination;
import com.se_07.backend.entity.Attraction;
import com.se_07.backend.service.SimpleRAGService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Random;

@Service
public class AIServiceImpl implements AIService {
    
    @Value("${deepseek.api.key:your-deepseek-api-key}")
    private String apiKey;
    
    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private DestinationRepository destinationRepository;
    
    @Autowired
    private AttractionRepository attractionRepository;

    @Autowired
    private SimpleRAGService simpleRAGService;
    
    public AIServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public AIRecommendationResponse generateProfileRecommendation(AIRecommendationRequest request) {
        try {
            // 1) 使用RAG服务根据用户偏好检索+生成答案
            String question = buildProfileQuestionForRAG(request);
            java.util.Map<String, Object> ragResult = simpleRAGService.askQuestion(question, 6);
            if (ragResult != null && Boolean.TRUE.equals(ragResult.get("success"))) {
                String answer = String.valueOf(ragResult.getOrDefault("answer", ""));
                if (answer != null && !answer.isBlank()) {
                    return AIRecommendationResponse.success(answer, "general");
                }
            }

            // 2) RAG失败则回退到直接LLM
            String prompt = buildProfilePrompt(request);
            String response = callDeepSeekAPI(prompt);
            return AIRecommendationResponse.success(response, "general");
        } catch (Exception e) {
            System.err.println("AI推荐生成失败: " + e.getMessage());
            // 返回模拟推荐作为降级方案
            return AIRecommendationResponse.success(generateFallbackProfileRecommendation(request), "general");
        }
    }
    
    @Override
    public AIRecommendationResponse generateItineraryPlan(AIRecommendationRequest request) {
        try {
            String prompt = buildItineraryPrompt(request);
            String response = callDeepSeekAPI(prompt);
            return AIRecommendationResponse.success(response, "itinerary");
        } catch (Exception e) {
            System.err.println("行程规划生成失败: " + e.getMessage());
            // 返回模拟行程作为降级方案
            return AIRecommendationResponse.success(generateFallbackItinerary(request), "itinerary");
        }
    }
    
    private String callDeepSeekAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.set("messages", messages);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        
        // 解析响应
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.getBody());
        return responseJson.get("choices").get(0).get("message").get("content").asText();
    }
    
    private String buildProfilePrompt(AIRecommendationRequest request) {
        // 从数据库获取可用的城市信息（不再获取景点信息）
        List<Destination> availableDestinations = destinationRepository.findAll();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为一名专业的旅行顾问，请根据以下用户档案信息，为用户生成个性化的旅行目的地推荐：\n\n");
        
        if (request.getTravelPreferences() != null && !request.getTravelPreferences().isEmpty()) {
            prompt.append("用户旅行偏好：").append(String.join("、", request.getTravelPreferences())).append("\n");
        }
        
        if (request.getSpecialNeeds() != null && !request.getSpecialNeeds().isEmpty()) {
            prompt.append("特殊需求：").append(String.join("、", request.getSpecialNeeds())).append("\n");
        }
        
        if (request.getNaturalLanguageDescription() != null && !request.getNaturalLanguageDescription().trim().isEmpty()) {
            prompt.append("用户描述：").append(request.getNaturalLanguageDescription()).append("\n");
        }
        
        if (request.getHistoricalDestinations() != null && !request.getHistoricalDestinations().isEmpty()) {
            prompt.append("历史目的地：").append(String.join("、", request.getHistoricalDestinations())).append("\n");
        }
        
        if (request.getWishlistDestinations() != null && !request.getWishlistDestinations().isEmpty()) {
            prompt.append("期望目的地：").append(String.join("、", request.getWishlistDestinations())).append("\n");
        }
        
        // 添加数据库中可用的城市信息
        prompt.append("\n可推荐的城市范围（请仅从这些城市中选择推荐）：\n");
        availableDestinations.forEach(dest -> {
            prompt.append("- ").append(dest.getName()).append("：").append(dest.getDescription()).append("\n");
        });
        
        prompt.append("\n请生成：\n");
        prompt.append("1. 3-5个推荐城市（仅从上述可推荐城市范围中选择），包含推荐理由\n");
        prompt.append("2. 每个城市的特色和亮点\n");
        prompt.append("3. 旅行注意事项和建议\n");
        prompt.append("4. 最佳出行时间建议\n\n");
        prompt.append("请用温馨友好的语调，内容要具体实用，字数控制在800字以内。");
        
        return prompt.toString();
    }

    /**
     * 构造给RAG服务的提问，将用户偏好和候选目的地提示给RAG。
     */
    private String buildProfileQuestionForRAG(AIRecommendationRequest request) {
        StringBuilder q = new StringBuilder();
        q.append("根据以下用户档案，为其推荐3-5个中国城市（只从常见热门城市中选择），并说明推荐理由、特色亮点和最佳出行时间。\n\n");

        if (request.getTravelPreferences() != null && !request.getTravelPreferences().isEmpty()) {
            q.append("用户旅行偏好：").append(String.join("、", request.getTravelPreferences())).append("\n");
        }
        if (request.getSpecialNeeds() != null && !request.getSpecialNeeds().isEmpty()) {
            q.append("特殊需求：").append(String.join("、", request.getSpecialNeeds())).append("\n");
        }
        if (request.getNaturalLanguageDescription() != null && !request.getNaturalLanguageDescription().trim().isEmpty()) {
            q.append("用户描述：").append(request.getNaturalLanguageDescription()).append("\n");
        }
        if (request.getHistoricalDestinations() != null && !request.getHistoricalDestinations().isEmpty()) {
            q.append("历史目的地：").append(String.join("、", request.getHistoricalDestinations())).append("\n");
        }
        if (request.getWishlistDestinations() != null && !request.getWishlistDestinations().isEmpty()) {
            q.append("期望目的地：").append(String.join("、", request.getWishlistDestinations())).append("\n");
        }

        q.append("\n输出格式：\n");
        q.append("- 每个城市用一段话描述，包含：推荐理由、特色亮点、最佳出行时间；\n");
        q.append("- 语气友好，字数合计800字以内；\n");
        q.append("- 若资料不足请直接跳过，不要编造。\n");
        return q.toString();
    }
    
    private String buildItineraryPrompt(AIRecommendationRequest request) {
        // 从数据库获取目的地相关的景点信息
        List<Attraction> destinationAttractions = attractionRepository.findByDestinationName(request.getDestination());
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为专业旅行规划师，请为用户制定详细的旅行行程，并以JSON格式输出：\n\n");
        
        prompt.append("目的地：").append(request.getDestination()).append("\n");
        prompt.append("旅行天数：").append(request.getDays()).append("天\n");
        prompt.append("旅行人数：").append(request.getTravelers()).append("人\n");
        
        if (request.getBudget() != null) {
            prompt.append("预算：¥").append(request.getBudget().intValue()).append("\n");
        }
        
        if (request.getTravelPreferences() != null && !request.getTravelPreferences().isEmpty()) {
            prompt.append("旅行偏好：").append(String.join("、", request.getTravelPreferences())).append("\n");
        }
        
        if (request.getSpecialNeeds() != null && !request.getSpecialNeeds().isEmpty()) {
            prompt.append("特殊需求：").append(String.join("、", request.getSpecialNeeds())).append("\n");
        }
        
        // 添加该目的地可用的景点信息
        if (!destinationAttractions.isEmpty()) {
            prompt.append("\n").append(request.getDestination()).append("可游览的景点（请仅从这些景点中安排行程）：\n");
            destinationAttractions.forEach(attraction -> {
                prompt.append("- ").append(attraction.getName())
                      .append("：").append(attraction.getDescription())
                      .append("（建议游览时间：2-3小时）\n");
            });
        }
        
        prompt.append("\n请严格按照以下JSON格式输出行程规划：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"行程标题\",\n");
        prompt.append("  \"days\": ").append(request.getDays()).append(",\n");
        prompt.append("  \"travelers\": ").append(request.getTravelers()).append(",\n");
        prompt.append("  \"budget\": ").append(request.getBudget() != null ? request.getBudget().intValue() : 5000).append(",\n");
        prompt.append("  \"plan\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"day\": 1,\n");
        prompt.append("      \"activities\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"name\": \"景点名称\",\n");
        prompt.append("          \"startTime\": \"09:00\",\n");
        prompt.append("          \"endTime\": \"11:00\",\n");
        prompt.append("          \"transportMode\": \"步行\",\n");
        prompt.append("          \"description\": \"景点描述\"\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("1. 交通方式只能是：步行、骑行、公共交通、开车 四种之一\n");
        prompt.append("2. 时间格式为24小时制，如：09:00、14:30\n");
        prompt.append("3. 每天安排2-4个景点，时间安排合理\n");
        prompt.append("4. 景点名称必须从上述可游览景点列表中选择\n");
        prompt.append("5. 确保JSON格式正确，可以被解析\n");
        prompt.append("6. 行程安排要符合用户偏好和预算\n");
        
        return prompt.toString();
    }
    
    private String generateFallbackProfileRecommendation(AIRecommendationRequest request) {
        StringBuilder recommendation = new StringBuilder();
        recommendation.append("🎯 基于您的个人档案，为您推荐以下目的地：\n\n");
        
        // 从数据库获取推荐数据
        List<Destination> destinations = destinationRepository.findAll();
        List<String> preferences = request.getTravelPreferences();
        
        if (destinations.isEmpty()) {
            // 数据库为空时的默认推荐
            recommendation.append("🏞️ **张家界** - 壮美的自然奇观\n");
            recommendation.append("推荐理由：世界自然遗产，奇峰异石，云雾缭绕\n");
            recommendation.append("最佳时间：4-6月、9-11月\n\n");
        } else {
            // 基于用户偏好和数据库数据生成推荐
            Random random = new Random();
            List<Destination> shuffledDestinations = destinations.stream()
                    .collect(Collectors.toList());
            java.util.Collections.shuffle(shuffledDestinations, random);
            
            int count = 0;
            for (Destination dest : shuffledDestinations) {
                if (count >= 3) break; // 最多推荐3个城市
                
                recommendation.append("🏛️ **").append(dest.getName()).append("** - ")
                             .append(dest.getDescription()).append("\n");
                
                recommendation.append("推荐理由：").append(dest.getDescription()).append("\n");
                recommendation.append("最佳时间：四季皆宜").append("\n\n");
                count++;
            }
        }
        
        recommendation.append("💡 **贴心提示：**\n");
        recommendation.append("• 建议提前1-2个月预订机票和住宿\n");
        recommendation.append("• 关注当地天气变化，准备合适衣物\n");
        recommendation.append("• 下载离线地图，确保网络畅通\n");
        recommendation.append("• 购买旅行保险，保障出行安全");
        
        return recommendation.toString();
    }
    
    private String generateFallbackItinerary(AIRecommendationRequest request) {
        StringBuilder itinerary = new StringBuilder();
        String destination = request.getDestination();
        Integer days = request.getDays();
        
        // 从数据库获取该目的地的景点
        List<Attraction> attractions = attractionRepository.findByDestinationName(destination);
        
        // 构建JSON格式的行程
        itinerary.append("```json\n");
        itinerary.append("{\n");
        itinerary.append("  \"title\": \"").append(destination).append(" ").append(days).append("日游智能行程\",\n");
        itinerary.append("  \"days\": ").append(days).append(",\n");
        itinerary.append("  \"travelers\": ").append(request.getTravelers()).append(",\n");
        itinerary.append("  \"budget\": ").append(request.getBudget() != null ? request.getBudget().intValue() : 5000).append(",\n");
        itinerary.append("  \"plan\": [\n");
        
        for (int day = 1; day <= days; day++) {
            itinerary.append("    {\n");
            itinerary.append("      \"day\": ").append(day).append(",\n");
            itinerary.append("      \"activities\": [\n");
            
            if (day == 1) {
                // 第一天
                if (!attractions.isEmpty()) {
                    Attraction firstAttraction = attractions.get(0);
                    itinerary.append("        {\n");
                    itinerary.append("          \"name\": \"").append(firstAttraction.getName()).append("\",\n");
                    itinerary.append("          \"startTime\": \"09:00\",\n");
                    itinerary.append("          \"endTime\": \"11:00\",\n");
                    itinerary.append("          \"transportMode\": \"步行\",\n");
                    itinerary.append("          \"description\": \"").append(firstAttraction.getDescription()).append("\"\n");
                    itinerary.append("        },\n");
                }
                
                itinerary.append("        {\n");
                itinerary.append("          \"name\": \"市中心漫步\",\n");
                itinerary.append("          \"startTime\": \"14:00\",\n");
                itinerary.append("          \"endTime\": \"16:00\",\n");
                itinerary.append("          \"transportMode\": \"步行\",\n");
                itinerary.append("          \"description\": \"感受城市氛围，体验当地文化\"\n");
                itinerary.append("        }\n");
            } else if (day == days) {
                // 最后一天
                itinerary.append("        {\n");
                itinerary.append("          \"name\": \"购买纪念品\",\n");
                itinerary.append("          \"startTime\": \"09:00\",\n");
                itinerary.append("          \"endTime\": \"11:00\",\n");
                itinerary.append("          \"transportMode\": \"步行\",\n");
                itinerary.append("          \"description\": \"最后一次游览，购买纪念品\"\n");
                itinerary.append("        }\n");
            } else {
                // 中间几天
                if (attractions.size() > day - 1) {
                    Attraction dayAttraction = attractions.get(day - 1);
                    itinerary.append("        {\n");
                    itinerary.append("          \"name\": \"").append(dayAttraction.getName()).append("\",\n");
                    itinerary.append("          \"startTime\": \"09:00\",\n");
                    itinerary.append("          \"endTime\": \"11:00\",\n");
                    itinerary.append("          \"transportMode\": \"步行\",\n");
                    itinerary.append("          \"description\": \"").append(dayAttraction.getDescription()).append("\"\n");
                    itinerary.append("        },\n");
                }
                
                itinerary.append("        {\n");
                itinerary.append("          \"name\": \"深度游览特色景点\",\n");
                itinerary.append("          \"startTime\": \"14:00\",\n");
                itinerary.append("          \"endTime\": \"16:00\",\n");
                itinerary.append("          \"transportMode\": \"公共交通\",\n");
                itinerary.append("          \"description\": \"体验当地特色活动\"\n");
                itinerary.append("        }\n");
            }
            
            itinerary.append("      ]\n");
            if (day < days) {
                itinerary.append("    },\n");
            } else {
                itinerary.append("    }\n");
            }
        }
        
        itinerary.append("  ]\n");
        itinerary.append("}\n");
        itinerary.append("```\n\n");
        
        itinerary.append("💡 **实用贴士：**\n");
        itinerary.append("• 建议穿着舒适的鞋子\n");
        itinerary.append("• 随身携带雨具和防晒用品\n");
        itinerary.append("• 保持手机电量充足\n");
        itinerary.append("• 尊重当地风俗习惯");
        
        return itinerary.toString();
    }
} 