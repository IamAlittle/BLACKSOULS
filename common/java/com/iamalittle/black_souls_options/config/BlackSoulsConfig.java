package com.iamalittle.black_souls_options.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

/**
 * Black Souls Options 配置类
 * 作为Cloth Config系统的适配器，保持向后兼容性
 */
public class BlackSoulsConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static BlackSoulsConfig instance;
    
    private BlackSoulsConfig() {
        // 私有构造函数
    }

    public static BlackSoulsConfig getInstance() {
        if (instance == null) {
            instance = new BlackSoulsConfig();
        }
        return instance;
    }

    // Getter方法 - 委托给Cloth Config
    public int getContractCreationCost() {
        return BlackSoulsClothConfig.getInstance().contractCreationCost;
    }

    public boolean isEnableDebugMode() {
        return BlackSoulsClothConfig.getInstance().enableDebugMode;
    }

    public String getRequiredItemId() {
        // 保持向后兼容，将列表转换为逗号分隔的字符串
        return String.join(",", BlackSoulsClothConfig.getInstance().requiredItemIds);
    }
    
    public List<String> getRequiredItemIds() {
        return BlackSoulsClothConfig.getInstance().requiredItemIds;
    }

    public boolean isCheckHeldItem() {
        return BlackSoulsClothConfig.getInstance().checkHeldItem;
    }

    public boolean isCheckWornItem() {
        return BlackSoulsClothConfig.getInstance().checkWornItem;
    }

    public boolean isEnableInstantKill() {
        return BlackSoulsClothConfig.getInstance().enableInstantKill;
    }
    
    public boolean isEnableUncensoredMode() {
        return BlackSoulsClothConfig.getInstance().enableUncensoredMode;
    }

    // 重新加载配置
    public static void reload() {
        BlackSoulsClothConfig.reload();
    }

    // 调试信息输出方法
    // 只有在debug模式开启时才会输出调试信息
    public static void debug(String message) {
        if (getInstance().isEnableDebugMode()) {
            LOGGER.info("[BLACKSOULS DEBUG] " + message);
        }
    }

    // 调试信息输出方法（带前缀）
    public static void debug(String prefix, String message) {
        if (getInstance().isEnableDebugMode()) {
            LOGGER.info("[BLACKSOULS DEBUG] [" + prefix + "] " + message);
        }
    }

    // 错误信息输出方法（始终输出，不受debug模式影响）
    public static void error(String message) {
        LOGGER.error("[BLACKSOULS ERROR] " + message);
    }

    // 警告信息输出方法（始终输出，不受debug模式影响）
    public static void warn(String message) {
        LOGGER.warn("[BLACKSOULS WARN] " + message);
    }
}