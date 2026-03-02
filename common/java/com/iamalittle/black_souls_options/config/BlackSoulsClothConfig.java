package com.iamalittle.black_souls_options.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

/**
 * Black Souls Options Cloth Config配置类
 */
@Config(name = "black_souls_options")
public class BlackSoulsClothConfig implements ConfigData {
    
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
    public int contractCreationCost = 100;
    
    @ConfigEntry.Gui.CollapsibleObject
    public List<String> requiredItemIds = new ArrayList<>(Arrays.asList("minecraft:iron_helmet"));
    
    public boolean checkHeldItem = true;
    
    public boolean checkWornItem = true;
    
    public boolean enableInstantKill = false;
    
    public boolean enableDebugMode = false;
    
    public boolean enableUncensoredMode = true;
    
    /**
     * 初始化AutoConfig
     */
    public static void init() {
        AutoConfig.register(BlackSoulsClothConfig.class, Toml4jConfigSerializer::new);
    }
    
    /**
     * 获取配置实例
     */
    public static BlackSoulsClothConfig getInstance() {
        return AutoConfig.getConfigHolder(BlackSoulsClothConfig.class).getConfig();
    }
    
    /**
     * 保存配置
     */
    public static void save() {
        AutoConfig.getConfigHolder(BlackSoulsClothConfig.class).save();
    }
    
    /**
     * 重新加载配置
     */
    public static void reload() {
        AutoConfig.getConfigHolder(BlackSoulsClothConfig.class).load();
    }
}