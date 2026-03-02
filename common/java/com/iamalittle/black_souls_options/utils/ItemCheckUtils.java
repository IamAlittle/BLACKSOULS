package com.iamalittle.black_souls_options.utils;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 物品检查工具类
 * 用于检查玩家是否持有或穿戴了配置中指定的物品
 */
public class ItemCheckUtils {
    
    /**
     * 检查玩家是否满足界面打开条件
     * @param player 玩家实体
     * @return 是否满足条件
     */
    public static boolean canOpenInterface(Player player) {
        // 强制重新加载配置以确保使用最新配置
        BlackSoulsConfig.reload();
        
        BlackSoulsConfig config = BlackSoulsConfig.getInstance();
        List<String> requiredItemIds = config.getRequiredItemIds();
        boolean checkHeldItem = config.isCheckHeldItem();
        boolean checkWornItem = config.isCheckWornItem();
        
        // 如果列表为空或包含"none"，则允许无物品打开
        if (requiredItemIds.isEmpty() || requiredItemIds.contains("none")) {
            BlackSoulsConfig.debug("ItemCheckUtils", "配置列表为空或包含'none'，允许无物品打开界面");
            return true;
        }
        
        BlackSoulsConfig.debug("ItemCheckUtils", "配置物品ID列表: " + requiredItemIds);
        BlackSoulsConfig.debug("ItemCheckUtils", "检查手持物品: " + checkHeldItem);
        BlackSoulsConfig.debug("ItemCheckUtils", "检查穿戴物品: " + checkWornItem);
        
        // 检查每个物品ID
        for (String itemId : requiredItemIds) {
            itemId = itemId.trim();
            if (itemId.isEmpty()) continue;
            
            // 获取配置的物品
            Item requiredItem = getItemById(itemId);
            if (requiredItem == null) {
                BlackSoulsConfig.debug("ItemCheckUtils", "物品不存在: " + itemId);
                // 如果物品不存在，继续检查下一个
                continue;
            }
            
            BlackSoulsConfig.debug("ItemCheckUtils", "检查物品: " + itemId);
            BlackSoulsConfig.debug("ItemCheckUtils", "获取到的物品对象: " + requiredItem);
            BlackSoulsConfig.debug("ItemCheckUtils", "物品注册表键: " + BuiltInRegistries.ITEM.getKey(requiredItem));
            
            // 检查手持物品
            if (checkHeldItem) {
                // 检查主手和副手
                Item mainHandItem = player.getMainHandItem().getItem();
                Item offHandItem = player.getOffhandItem().getItem();
                
                BlackSoulsConfig.debug("ItemCheckUtils", "主手物品: " + BuiltInRegistries.ITEM.getKey(mainHandItem));
                BlackSoulsConfig.debug("ItemCheckUtils", "副手物品: " + BuiltInRegistries.ITEM.getKey(offHandItem));
                BlackSoulsConfig.debug("ItemCheckUtils", "需要物品: " + BuiltInRegistries.ITEM.getKey(requiredItem));
                
                // 正确比较物品对象
                if (mainHandItem.equals(requiredItem) || offHandItem.equals(requiredItem)) {
                    BlackSoulsConfig.debug("ItemCheckUtils", "手持物品满足条件: " + itemId);
                    return true;
                } else {
                    BlackSoulsConfig.debug("ItemCheckUtils", "手持物品不匹配");
                }
            }
            
            // 检查穿戴物品
            if (checkWornItem) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.isArmor() || slot == EquipmentSlot.OFFHAND) {
                        ItemStack wornItem = player.getItemBySlot(slot);
                        if (wornItem.getItem().equals(requiredItem)) {
                            BlackSoulsConfig.debug("ItemCheckUtils", "穿戴物品满足条件: " + itemId + " 在槽位: " + slot);
                            return true;
                        }
                    }
                }
            }
        }
        
        BlackSoulsConfig.debug("ItemCheckUtils", "未找到满足条件的物品");
        return false;
    }
    
    /**
     * 检查玩家是否穿戴了配置中指定的头盔（兼容旧方法名）
     * @param player 玩家实体
     * @return 是否穿戴了指定头盔
     */
    public static boolean isWearingRequiredHelmet(Player player) {
        return canOpenInterface(player);
    }
    
    /**
     * 根据物品ID获取物品
     * @param itemId 物品ID（格式："namespace:item_name"）
     * @return 物品对象，如果不存在返回null
     */
    private static Item getItemById(String itemId) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation != null) {
                Item item = BuiltInRegistries.ITEM.get(resourceLocation);
                // 检查是否获取到了有效的物品（不是空气物品）
                if (item != null && !item.equals(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("minecraft:air")))) {
                    return item;
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
    }
    
    /**
     * 获取界面打开条件的描述信息
     * @return 描述信息
     */
    public static String getRequirementDescription() {
        BlackSoulsConfig config = BlackSoulsConfig.getInstance();
        List<String> requiredItemIds = config.getRequiredItemIds();
        boolean checkHeldItem = config.isCheckHeldItem();
        boolean checkWornItem = config.isCheckWornItem();
        
        if (requiredItemIds.isEmpty() || requiredItemIds.contains("none")) {
            return "无需任何物品";
        }
        
        StringBuilder description = new StringBuilder("需要");
        
        if (checkHeldItem && checkWornItem) {
            description.append("手持或穿戴");
        } else if (checkHeldItem) {
            description.append("手持");
        } else if (checkWornItem) {
            description.append("穿戴");
        }
        
        // 处理多个物品ID
        if (requiredItemIds.size() == 1) {
            description.append(" ").append(requiredItemIds.get(0).trim());
        } else {
            description.append("以下物品之一：");
            for (int i = 0; i < requiredItemIds.size(); i++) {
                if (i > 0) description.append("、");
                description.append(requiredItemIds.get(i).trim());
            }
        }
        
        return description.toString();
    }
}