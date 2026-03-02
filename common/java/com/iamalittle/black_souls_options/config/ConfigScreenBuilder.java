package com.iamalittle.black_souls_options.config;

import com.iamalittle.black_souls_options.ModMain;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import java.util.Arrays;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 配置界面构建器
 */
public class ConfigScreenBuilder {
    
    /**
     * 创建配置界面
     * @param parent 父界面
     * @return 配置界面
     */
    public static Screen createConfigScreen(Screen parent) {
        // 使用手动构建的配置界面，确保所有配置项放在一起
        return createManualConfigScreen(parent);
    }
    
    /**
     * 手动构建配置界面
     * @param parent 父界面
     * @return 配置界面
     */
    public static Screen createManualConfigScreen(Screen parent) {
        BlackSoulsClothConfig config = BlackSoulsClothConfig.getInstance();
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("black_souls_options.config.title"))
                .setSavingRunnable(BlackSoulsClothConfig::save)
                .setTransparentBackground(false);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // 主分类 - 所有配置项放在一起
        ConfigCategory mainCategory = builder.getOrCreateCategory(Component.translatable("black_souls_options.config.title"));
        
        // 契约创建成本
        mainCategory.addEntry(entryBuilder.startIntField(
                Component.translatable("black_souls_options.config.contract_creation_cost"),
                config.contractCreationCost
        ).setDefaultValue(100)
                .setTooltip(Component.translatable("black_souls_options.config.contract_creation_cost.tooltip"))
                .setSaveConsumer(newValue -> config.contractCreationCost = newValue)
                .build());
        
        // 添加所需物品ID列表配置项
        mainCategory.addEntry(entryBuilder.startStrList(
                Component.translatable("black_souls_options.config.required_item_ids"),
                config.requiredItemIds
        ).setDefaultValue(Arrays.asList("minecraft:iron_helmet"))
                .setTooltip(Component.translatable("black_souls_options.config.required_item_ids.tooltip"))
                .setSaveConsumer(newValue -> config.requiredItemIds = newValue)
                .build());
        
        // 检查手持物品
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("black_souls_options.config.check_held_item"),
                config.checkHeldItem
        ).setDefaultValue(true)
                .setTooltip(Component.translatable("black_souls_options.config.check_held_item.tooltip"))
                .setSaveConsumer(newValue -> config.checkHeldItem = newValue)
                .build());
        
        // 检查穿戴物品
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("black_souls_options.config.check_worn_item"),
                config.checkWornItem
        ).setDefaultValue(true)
                .setTooltip(Component.translatable("black_souls_options.config.check_worn_item.tooltip"))
                .setSaveConsumer(newValue -> config.checkWornItem = newValue)
                .build());
        
        // 启用瞬杀
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("black_souls_options.config.enable_instant_kill"),
                config.enableInstantKill
        ).setDefaultValue(false)
                .setTooltip(Component.translatable("black_souls_options.config.enable_instant_kill.tooltip"))
                .setSaveConsumer(newValue -> config.enableInstantKill = newValue)
                .build());
        
        // 调试模式
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("black_souls_options.config.enable_debug_mode"),
                config.enableDebugMode
        ).setDefaultValue(false)
                .setTooltip(Component.translatable("black_souls_options.config.enable_debug_mode.tooltip"))
                .setSaveConsumer(newValue -> config.enableDebugMode = newValue)
                .build());
        
        // 无和谐模式
        mainCategory.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("black_souls_options.config.enable_uncensored_mode"),
                config.enableUncensoredMode
        ).setDefaultValue(true)
                .setTooltip(Component.translatable("black_souls_options.config.enable_uncensored_mode.tooltip"))
                .setSaveConsumer(newValue -> config.enableUncensoredMode = newValue)
                .build());
        
        return builder.build();
    }
}