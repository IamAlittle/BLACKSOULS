package com.iamalittle.black_souls_options.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Black Souls Options 配置类
 * 采用类似QuantumLotusConfig的TOML解析方式
 */
public class BlackSoulsConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_FILE_NAME = "black_souls_options.toml";
    private static final Path CONFIG_PATH = getConfigPath();
    private static BlackSoulsConfig instance;
    
    // 配置字段
    private int contractCreationCost;
    private boolean enableDebugMode;
    private String requiredItemId;
    private boolean checkHeldItem;
    private boolean checkWornItem;
    private boolean enableInstantKill; // 启用秒杀模式，杀害选项直接秒杀目标

    private BlackSoulsConfig(int contractCreationCost, boolean enableDebugMode, String requiredItemId, boolean checkHeldItem, boolean checkWornItem, boolean enableInstantKill) {
        this.contractCreationCost = contractCreationCost;
        this.enableDebugMode = enableDebugMode;
        this.requiredItemId = requiredItemId;
        this.checkHeldItem = checkHeldItem;
        this.checkWornItem = checkWornItem;
        this.enableInstantKill = enableInstantKill;
    }

    public static BlackSoulsConfig getInstance() {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }

    private static BlackSoulsConfig loadConfig() {
        try {
            if (!CONFIG_PATH.toFile().exists()) {
                createDefaultConfig();
            }
            
            String content = Files.readString(CONFIG_PATH);
            return parseToml(content);
        } catch (IOException e) {
            createDefaultConfig();
            return new BlackSoulsConfig(1000, false, "minecraft:iron_helmet", true, true, false);
        }
    }

    private static BlackSoulsConfig parseToml(String content) {
        // 解析TOML配置
        int contractCreationCost = parseInt(content, "contract_creation_cost", 100);
        boolean enableDebugMode = parseBoolean(content, "debug_mode", false);
        String requiredItemId = parseString(content, "required_item_id", "minecraft:iron_helmet");
        boolean checkHeldItem = parseBoolean(content, "check_held_item", true);
        boolean checkWornItem = parseBoolean(content, "check_worn_item", true);
        boolean enableInstantKill = parseBoolean(content, "enable_instant_kill", false);

        return new BlackSoulsConfig(contractCreationCost, enableDebugMode, requiredItemId, checkHeldItem, checkWornItem, enableInstantKill);
    }

    private static void createDefaultConfig() {
        try {
            Path configDir = CONFIG_PATH.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            String defaultConfig = "# Black Souls Options Configuration\n" +
                    "#\n" +
                    "# Contract Settings\n" +
                    "contract_creation_cost = 100\n" +
                    "\n" +
                    "# Interface Access Settings\n" +
                    "# 设置打开界面所需的物品ID(逗号隔开)\n" +
                    "required_item_id = [\"minecraft:iron_helmet\"]\n" +
                    "# 是否检查手持物品\n" +
                    "check_held_item = true\n" +
                    "# 是否检查穿戴物品\n" +
                    "check_worn_item = true\n" +
                    "\n" +
                    "# Kill Attack Settings\n" +
                    "# 是否启用秒杀模式，杀害选项直接秒杀目标\n" +
                    "enable_instant_kill = false\n" +
                    "\n" +
                    "# Debug Settings\n" +
                    "debug_mode = false\n";
            
            Files.writeString(CONFIG_PATH, defaultConfig);
        } catch (IOException e) {
            // 静默处理错误
        }
    }

    // 解析方法
    private static int parseInt(String content, String key, int defaultValue) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(key + " = ") || line.trim().startsWith(key + "=")) {
                String value = line.split("=")[1].trim();
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private static boolean parseBoolean(String content, String key, boolean defaultValue) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(key + " = ") || line.trim().startsWith(key + "=")) {
                String value = line.split("=")[1].trim();
                return Boolean.parseBoolean(value);
            }
        }
        return defaultValue;
    }

    private static String parseString(String content, String key, String defaultValue) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith(key + " = ") || line.trim().startsWith(key + "=")) {
                String value = line.split("=")[1].trim();
                
                // 处理数组格式 ["item1", "item2", "item3"]
                if (value.startsWith("[") && value.endsWith("]")) {
                    value = value.substring(1, value.length() - 1).trim();
                    // 移除引号并处理逗号分隔
                    String[] items = value.split(",");
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < items.length; i++) {
                        String item = items[i].trim();
                        if (item.startsWith("\"") && item.endsWith("\"")) {
                            item = item.substring(1, item.length() - 1);
                        }
                        if (!item.isEmpty()) {
                            if (result.length() > 0) result.append(",");
                            result.append(item);
                        }
                    }
                    return result.toString();
                }
                
                // 处理单个字符串格式
                // 移除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return defaultValue;
    }

    private static Path getConfigPath() {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.gameDirectory != null) {
                return minecraft.gameDirectory.toPath().resolve("config").resolve("black_souls_options").resolve(CONFIG_FILE_NAME);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return Paths.get("config").resolve("black_souls_options").resolve(CONFIG_FILE_NAME);
    }

    // Getter方法
    public int getContractCreationCost() {
        return contractCreationCost;
    }

    public boolean isEnableDebugMode() {
        return enableDebugMode;
    }

    public String getRequiredItemId() {
        return requiredItemId;
    }

    public boolean isCheckHeldItem() {
        return checkHeldItem;
    }

    public boolean isCheckWornItem() {
        return checkWornItem;
    }

    public boolean isEnableInstantKill() {
        return enableInstantKill;
    }

    // 重新加载配置
    public static void reload() {
        instance = loadConfig();
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