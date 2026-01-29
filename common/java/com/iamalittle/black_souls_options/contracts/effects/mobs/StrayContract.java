package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 流浪者契约效果 - 迟缓之箭
 * 玩家契约流浪者后获得的能力：
 * 1. 射出的箭矢会变成迟缓之箭（tipped_arrow）
 * 2. 迟缓效果持续5秒
 */
public class StrayContract extends ContractEffect {
    private static final String EFFECT_ID = "stray_slowness_arrows";
    private static final String DISPLAY_NAME = "迟缓之箭";
    private static final String DESCRIPTION = "射出的箭矢会变成迟缓之箭";
    
    // 存储拥有流浪者契约的玩家UUID
    private static final Set<UUID> strayContractPlayers = new HashSet<>();

    public StrayContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            strayContractPlayers.add(player.getUUID());
            
            // 使用契约目标名称发送消息（仅在需要时发送）
            if (sendMessage) {
                String entityName = effectData.getString("contractEntityName");
                if (entityName.isEmpty()) {
                    entityName = displayName; // 回退到效果名称
                }
                sendActivationMessage(player, entityName);
            }
        }
    }

    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
            strayContractPlayers.remove(player.getUUID());
            
            // 停用效果时发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }

    @Override
    protected void onTick(Player player) {
        // 不需要每tick执行操作，只在需要检查玩家状态时使用
    }
    
    /**
     * 检查玩家是否拥有流浪者契约效果
     */
    public static boolean hasStrayContract(Player player) {
        return player != null && strayContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 获取玩家射出的箭矢类型（根据优先级系统）
     * 优先级：背包里其他类型的箭 > 流浪者契约的迟缓之箭 > 骷髅契约的箭 > 背包里普通的箭
     */
    public static ItemStack getStrayArrowStack(Player player, ItemStack originalArrow) {
        if (hasStrayContract(player)) {
            // 创建迟缓之箭
            ItemStack slownessArrow = new ItemStack(Items.TIPPED_ARROW);
            
            // 设置迟缓效果（5秒，等级I）
            slownessArrow.getOrCreateTag().putString("Potion", "minecraft:slowness");
            
            return slownessArrow;
        }
        
        return originalArrow;
    }
    
    /**
     * 检查玩家是否应该使用迟缓之箭
     */
    public static boolean shouldUseSlownessArrows(Player player) {
        return hasStrayContract(player);
    }
    
    /**
     * 检查玩家是否有背包里的特殊箭矢（药水箭、光灵箭等）
     */
    public static boolean hasSpecialArrowsInInventory(Player player) {
        if (player == null) return false;
        
        // 检查玩家背包中是否有特殊箭矢
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && isSpecialArrow(stack)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否为特殊箭矢（非普通箭矢）
     */
    public static boolean isSpecialArrow(ItemStack arrowStack) {
        if (arrowStack.isEmpty()) return false;
        
        // 特殊箭矢类型：药水箭、光灵箭等
        return arrowStack.getItem() == Items.TIPPED_ARROW || 
               arrowStack.getItem() == Items.SPECTRAL_ARROW;
    }
    
    /**
     * 获取玩家背包中的特殊箭矢（如果有）
     */
    public static ItemStack getSpecialArrowFromInventory(Player player) {
        if (player == null) return ItemStack.EMPTY;
        
        // 优先查找玩家背包中的特殊箭矢
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && isSpecialArrow(stack)) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6流浪者迟缓之箭效果："));
        details.add(Component.literal("§a射出的箭矢会变成迟缓之箭"));
        details.add(Component.literal("§a迟缓效果持续5秒"));
        details.add(Component.literal("§7使被击中的目标移动速度降低"));
        return details;
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 不需要保存额外数据
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 不需要加载额外数据
    }
}