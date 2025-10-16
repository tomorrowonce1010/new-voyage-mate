package com.se_07.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Python脚本执行工具类
 * 用于异步执行Python索引脚本
 */
@Component
public class PythonScriptExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonScriptExecutor.class);
    
    // Python环境路径
    private static final String PYTHON_PATH = "/root/voyagemate/new-voyage-mate/.venv/bin/python";
    // 脚本基础路径
    private static final String SCRIPT_BASE_PATH = "/root/voyagemate/new-voyage-mate/embedding-service";
    
    /**
     * 异步索引单个社区条目到Elasticsearch
     * @param communityEntryId 社区条目ID
     */
    public void indexCommunityEntryAsync(Long communityEntryId) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始异步索引社区条目 {} 到Elasticsearch", communityEntryId);
                
                String scriptPath = SCRIPT_BASE_PATH + "/index_single_community_entry.py";
                List<String> command = new ArrayList<>();
                command.add(PYTHON_PATH);
                command.add(scriptPath);
                command.add(String.valueOf(communityEntryId));
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);
                
                Process process = processBuilder.start();
                
                // 读取输出
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.debug("Python脚本输出: {}", line);
                    }
                }
                
                // 等待进程完成（最多30秒）
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    logger.error("Python脚本执行超时，已强制终止");
                    return;
                }
                
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    logger.info("✅ 社区条目 {} 成功索引到Elasticsearch", communityEntryId);
                } else {
                    logger.error("❌ 社区条目 {} 索引失败，退出码: {}, 输出: {}", 
                            communityEntryId, exitCode, output.toString());
                }
                
            } catch (Exception e) {
                logger.error("执行Python索引脚本时发生异常: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步索引单个用户（作者）到Elasticsearch
     * @param userId 用户ID
     */
    public void indexAuthorAsync(Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("开始异步索引用户（作者） {} 到Elasticsearch", userId);
                
                // 创建临时的单用户索引脚本
                String scriptPath = SCRIPT_BASE_PATH + "/index_single_author.py";
                List<String> command = new ArrayList<>();
                command.add(PYTHON_PATH);
                command.add(scriptPath);
                command.add(String.valueOf(userId));
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true);
                
                Process process = processBuilder.start();
                
                // 读取输出
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.debug("Python脚本输出: {}", line);
                    }
                }
                
                // 等待进程完成（最多30秒）
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    logger.error("Python脚本执行超时，已强制终止");
                    return;
                }
                
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    logger.info("✅ 用户（作者） {} 成功索引到Elasticsearch", userId);
                } else {
                    logger.error("❌ 用户（作者） {} 索引失败，退出码: {}, 输出: {}", 
                            userId, exitCode, output.toString());
                }
                
            } catch (Exception e) {
                logger.error("执行Python索引脚本时发生异常: {}", e.getMessage(), e);
            }
        });
    }
}

