package com.iamalittle.black_souls_options.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 文本读取器 - 负责从资源文件中读取多段文本
 * 支持逐字显示效果和随机文本选择
 */
public class TextReader {
    private static final Random RANDOM = new Random();
    
    /**
     * 根据语言和文件名读取文本文件
     * @param fileName 文件名（不含扩展名）
     * @return 文本段落列表
     */
    public static List<String> readTexts(String fileName) {
        List<String> texts = new ArrayList<>();
        
        // 获取当前语言
        String language = getCurrentLanguage();
        
        // 构建资源路径
        String resourcePath = "texts/" + language + "/" + fileName + ".txt";
        ResourceLocation resourceLocation = new ResourceLocation("black_souls_options", resourcePath);
        
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(resourceLocation).orElse(null);
            
            if (resource != null) {
                try (InputStream inputStream = resource.open()) {
                    String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    texts = parseTexts(content);
                }
            }
        } catch (IOException e) {
            // 如果指定语言文件不存在，尝试读取默认语言（英语）
            if (!language.equals("en_us")) {
                texts = readDefaultTexts(fileName);
            }
        }
        
        return texts;
    }
    
    /**
     * 读取默认语言（英语）的文本文件
     */
    private static List<String> readDefaultTexts(String fileName) {
        List<String> texts = new ArrayList<>();
        String resourcePath = "texts/en_us/" + fileName + ".txt";
        ResourceLocation resourceLocation = new ResourceLocation("black_souls_options", resourcePath);
        
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(resourceLocation).orElse(null);
            
            if (resource != null) {
                try (InputStream inputStream = resource.open()) {
                    String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    texts = parseTexts(content);
                }
            }
        } catch (IOException e) {
            // 默认文件也不存在，返回空列表
        }
        
        return texts;
    }
    
    /**
     * 解析文本内容，按段落分割
     */
    private static List<String> parseTexts(String content) {
        List<String> texts = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return texts;
        }
        
        // 标准化换行符：将所有类型的换行符转换为\n
        // 首先将\r\n转换为\n
        String normalizedContent = content.replace("\r\n", "\n");
        // 然后处理单独的\r

        normalizedContent = normalizedContent.replace("\r", "\n");
        
        // 按空行分割段落
        String[] paragraphs = normalizedContent.split("\\n\\s*\\n");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                // 保留段落内部的换行符，不做额外处理
                texts.add(trimmed);
            }
        }
        
        return texts;
    }
    
    /**
     * 获取当前游戏语言
     */
    private static String getCurrentLanguage() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null && minecraft.options.languageCode != null) {
            return minecraft.options.languageCode;
        }
        return "en_us"; // 默认英语
    }
    
    /**
     * 从文本列表中随机选择一段文本
     */
    public static String getRandomText(String fileName) {
        List<String> texts = readTexts(fileName);
        if (texts.isEmpty()) {
            return ""; // 返回空字符串表示没有文本
        }
        
        return texts.get(RANDOM.nextInt(texts.size()));
    }
    
    /**
     * 获取所有文本段落（用于调试或特定用途）
     */
    public static List<String> getAllTexts(String fileName) {
        return readTexts(fileName);
    }
}