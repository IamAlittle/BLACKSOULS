package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * 唤魔者契约效果 - 不死图腾
 * 玩家契约唤魔者后可以获得背包不死图腾效果
 * 死亡时检查背包中是否有不死图腾，有则触发效果并消耗一个图腾
 */
public class EvokerContract extends ContractEffect {
    private static final String EFFECT_ID = "evoker_death_totem";
    private static final String DISPLAY_NAME = "唤魔者不死图腾";
    private static final String DESCRIPTION = "死亡时检查背包中的不死图腾并触发效果";

    public static final Set<UUID> evokerDeathTotemEffectSet = new HashSet<>();

    public EvokerContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null && sendMessage) {
            // 使用契约目标名称发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendActivationMessage(player, entityName);
        }
    }
    
    @Override
    protected void onDeactivate(Player player) {
        if (player != null) {
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
        // 不需要每tick更新，只在死亡时检查
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§b不死图腾效果："));
        details.add(Component.literal("§7死亡时检查背包中是否有不死图腾"));
        details.add(Component.literal("§7有则触发效果并消耗一个图腾"));
        details.add(Component.literal("§7无需手持图腾"));
        return details;
    }
    
    /**
     * 检查玩家背包中是否有不死图腾
     */
    public static boolean hasTotemInInventory(Player player) {
        if (player == null) {
            return false;
        }
        
        // 检查玩家背包中是否有不死图腾
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 从玩家背包中消耗一个不死图腾
     */
    public static boolean consumeTotemFromInventory(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        // 查找并消耗一个不死图腾
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                stack.shrink(1); // 消耗一个图腾
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 触发不死图腾效果（死亡时调用）
     */
    public static boolean triggerTotemEffect(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        // 检查背包中是否有不死图腾
        if (!hasTotemInInventory(player)) {
            return false;
        }
        
        // 消耗一个不死图腾
        if (!consumeTotemFromInventory(player)) {
            return false;
        }
        
        // 触发不死图腾效果
        applyTotemEffects(player);
        
        // 发送消息
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("§6唤魔者契约：背包中的不死图腾已触发！"));
        }
        
        return true;
    }
    
    /**
     * 应用不死图腾效果
     */
    private static void applyTotemEffects(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // 播放音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // 清除负面效果
        player.removeAllEffects();

        // 设置生命值
        player.setHealth(1.0F);

        // 应用效果
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
    }
    public static boolean hasevokerContract(Player player) {
        return player != null && evokerDeathTotemEffectSet.contains(player.getUUID());
    }
}