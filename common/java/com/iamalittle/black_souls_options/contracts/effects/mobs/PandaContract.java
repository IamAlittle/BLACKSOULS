package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 熊猫契约效果 - 竹子快速食用
 * 玩家契约熊猫后获得的能力：
 * 1. 手持竹子右键可以快速触发食用动画
 * 2. 动画结束后快速恢复大量饥饿值
 */
public class PandaContract extends ContractEffect {
    private static final String EFFECT_ID = "panda_bamboo_eat";
    private static final String DISPLAY_NAME = "熊猫";
    private static final String DESCRIPTION = "可以快速食用竹子恢复饥饿值";
    
    // 恢复饥饿值（比骆驼契约更快）
    private static final int FOOD_AMOUNT = 1;
    
    // 熊猫契约玩家集合
    private static final Set<UUID> pandaContractPlayers = new HashSet<>();
    
    public PandaContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            pandaContractPlayers.add(player.getUUID());
            
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
            pandaContractPlayers.remove(player.getUUID());
            
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onTick(Player player) {
        // 熊猫契约不需要每tick更新
    }
    
    /**
     * 检查玩家是否拥有熊猫契约效果
     */
    public static boolean hasPandaContract(Player player) {
        return player != null && pandaContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 处理竹子食用效果
     * 在竹子食用动画结束后调用
     */
    public static void onBambooEatComplete(Player player) {
        if (player != null && hasPandaContract(player)) {
            // 恢复饥饿值（比骆驼契约更快）
            FoodData foodData = player.getFoodData();
            if (foodData.getFoodLevel() < 20) {
                foodData.setFoodLevel(Math.min(20, foodData.getFoodLevel() + FOOD_AMOUNT));
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);

        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6熊猫契约效果："));
        details.add(Component.literal("§7竹子快速食用"));
        details.add(Component.literal("§7- 竹子右键触发快速食用"));
        details.add(Component.literal("§7- 恢复1点饱食度"));

        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        return super.saveToNBT();
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
    }
}