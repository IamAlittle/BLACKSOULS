package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
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
 * 骆驼契约效果 - 仙人掌食用
 * 玩家契约骆驼后获得的能力：
 * 1. 手持仙人掌右键可以触发食用动画
 * 2. 动画结束后恢复2点生命值和2点饥饿值
 */
public class CamelContract extends ContractEffect {
    private static final String EFFECT_ID = "camel_cactus_eat";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.camel.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.camel.description";
    
    // 恢复生命值
    private static final float HEAL_AMOUNT = 2.0f;
    
    // 恢复饥饿值
    private static final int FOOD_AMOUNT = 2;
    
    // 骆驼契约玩家集合
    private static final Set<UUID> camelContractPlayers = new HashSet<>();
    
    public CamelContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            camelContractPlayers.add(player.getUUID());
            
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
            camelContractPlayers.remove(player.getUUID());
            
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
        // 骆驼契约不需要每tick更新
    }
    
    /**
     * 检查玩家是否拥有骆驼契约效果
     */
    public static boolean hasCamelContract(Player player) {
        return player != null && camelContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 处理仙人掌食用效果
     * 在仙人掌食用动画结束后调用
     */
    public static void onCactusEatComplete(Player player) {
        if (player != null && hasCamelContract(player)) {
            // 恢复生命值
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(HEAL_AMOUNT);
            }
            
            // 恢复饥饿值
            FoodData foodData = player.getFoodData();
            if (foodData.getFoodLevel() < 20) {
                foodData.setFoodLevel(Math.min(20, foodData.getFoodLevel() + FOOD_AMOUNT));
            }
            
            // 显示恢复效果粒子
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    player.getX(), player.getY() + 1, player.getZ(),
                    5, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.camel.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.camel.cactus_eat").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.camel.right_click_trigger").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.camel.heal_amount", HEAL_AMOUNT).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.camel.food_amount", FOOD_AMOUNT).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
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