package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 牛的契约效果 - 自动清除所有buff
 * 玩家契约牛后可以自动清除身上所有正面和负面效果
 */
public class CowContract extends ContractEffect {
    private static final String EFFECT_ID = "cow_clear_buffs";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.cow.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.cow.description";
    
    // 清除间隔（毫秒）
    private long clearInterval = 1000; // 默认5秒清除一次
    
    public CowContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 激活时立即清除一次所有效果
            clearAllEffects(player);
            
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
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        // 定期清除所有效果
        clearAllEffects(player);
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.cow.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.cow.clear_negative").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.cow.clear_all").withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        return details;
    }
    
    /**
     * 清除玩家身上所有效果
     */
    private void clearAllEffects(Player player) {
        // 获取玩家当前所有效果
        Iterable<MobEffectInstance> activeEffects = player.getActiveEffects();
        
        // 创建要移除的效果列表
        List<MobEffect> effectsToRemove = new ArrayList<>();
        
        // 遍历所有效果并标记要移除的
        for (MobEffectInstance effect : activeEffects) {
            MobEffect effectType = effect.getEffect();
            // 移除所有效果，包括正面和负面
            effectsToRemove.add(effectType);
        }
        
        // 移除标记的效果
        for (MobEffect effect : effectsToRemove) {
            player.removeEffect(effect);
        }
    }
    
    /**
     * 重写tick间隔，设置为清除间隔
     */
    @Override
    protected long getTickInterval() {
        return clearInterval;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putLong("clearInterval", clearInterval);
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("clearInterval")) {
            clearInterval = nbt.getLong("clearInterval");
        }
    }
}