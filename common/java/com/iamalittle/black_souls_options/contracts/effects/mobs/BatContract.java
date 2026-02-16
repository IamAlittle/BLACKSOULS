package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * 蝙蝠契约效果 - 永久夜视
 * 玩家契约蝙蝠后，获得永久夜视效果，效果会独立清除
 */
public class BatContract extends ContractEffect {
    private static final String EFFECT_ID = "bat_night_vision";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.bat.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.bat.description";
    
    // 夜视效果管理
    private MobEffectInstance batNightVisionEffect = null; // 蝙蝠契约给予的夜视效果实例
    private static final Set<UUID> nightVisionPlayers = new HashSet<>(); // 拥有蝙蝠夜视效果的玩家集合
    
    public BatContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 给予玩家永久夜视效果并保存效果实例
            batNightVisionEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false);
            player.addEffect(batNightVisionEffect);
            nightVisionPlayers.add(player.getUUID());
            
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
            // 移除蝙蝠契约给予的夜视效果，保留其他来源的效果
            if (batNightVisionEffect != null) {
                // 使用安全的方法移除特定效果实例
                removeSpecificEffect(player, batNightVisionEffect);
                batNightVisionEffect = null;
            }
            nightVisionPlayers.remove(player.getUUID());
            
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
        if (player == null || !player.isAlive() || player.level() == null) return;

        applyNightVisionEffect(player);

    }
    
    /**
     * 应用夜视效果给玩家
     */
    private void applyNightVisionEffect(Player player) {
        if (player != null && player.isAlive()) {
            // 创建新的夜视效果实例，持续时间为30秒，但会定期刷新
            MobEffectInstance nightVision = new MobEffectInstance(
                MobEffects.NIGHT_VISION, 
                -1,  // 持续时间
                0,   // 等级0
                false, // 不显示粒子
                false,  // 显示图标
                false
            );
            
            // 应用效果到玩家
            player.addEffect(nightVision);
        }
    }
    
    /**
     * 安全地移除特定的效果实例
     */
    private void removeSpecificEffect(Player player, MobEffectInstance effectToRemove) {
        // 获取玩家当前的所有效果
        Collection<MobEffectInstance> activeEffects = player.getActiveEffects();
        
        // 遍历效果列表，只移除与指定效果匹配的实例
        for (MobEffectInstance effect : activeEffects) {
            if (effect.getEffect() == effectToRemove.getEffect() && 
                effect.getAmplifier() == effectToRemove.getAmplifier() &&
                effect.getDuration() == effectToRemove.getDuration()) {
                // 找到匹配的效果，移除它
                player.removeEffect(effect.getEffect());
                break;
            }
        }
    }
    
    /**
     * 检查玩家是否拥有蝙蝠夜视效果（供Mixin使用）
     */
    public static boolean hasBatNightVision(Player player) {
        return nightVisionPlayers.contains(player.getUUID());
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.bat.effect_title").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.bat.night_vision_effect").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.bat.permanent_effect").withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.parseColor("#55FF55"))));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putBoolean("hasNightVision", batNightVisionEffect != null);
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("hasNightVision") && nbt.getBoolean("hasNightVision")) {
            // 标记为有夜视效果，实际效果实例会在onActivate中重新创建
        }
    }
}