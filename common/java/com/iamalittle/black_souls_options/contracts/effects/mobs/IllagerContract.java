package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.TextColor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 掠夺者契约效果 - 时刻获得不祥之兆效果
 * 玩家与掠夺者建立契约后获得的能力：
 * 1. 持续获得不祥之兆效果（Bad Omen）
 * 2. 效果等级为1，持续时间为无限
 * 3. 允许玩家随时触发袭击事件
 */
public class IllagerContract extends ContractEffect {
    private static final String EFFECT_ID = "illager_bad_omen";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.illager.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.illager.description";

    // 掠夺者契约玩家集合
    private static final Set<UUID> illagerContractPlayers = new HashSet<>();
    
    // 不祥之兆效果实例
    private MobEffectInstance badOmenEffect;
    
    public IllagerContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            illagerContractPlayers.add(player.getUUID());

            // 施加不祥之兆效果
            applyBadOmenEffect(player);
            
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
            illagerContractPlayers.remove(player.getUUID());

            // 移除不祥之兆效果
            removeBadOmenEffect(player);
            
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
        // 每tick检查并维持不祥之兆效果
        if (player == null || !player.isAlive() || player.level() == null) return;
        
        if (hasIllagerContract(player)) {
            // 确保不祥之兆效果持续存在
            maintainBadOmenEffect(player);
        }
    }
    
    /**
     * 施加不祥之兆效果
     */
    private void applyBadOmenEffect(Player player) {
        if (player.level().isClientSide()) {
            return; // 只在服务端执行
        }
        
        // 创建不祥之兆效果实例（等级1，无限持续时间）
        badOmenEffect = new MobEffectInstance(MobEffects.BAD_OMEN, -1, 0, false, false, true);
        player.addEffect(badOmenEffect);
    }
    
    /**
     * 移除不祥之兆效果
     */
    private void removeBadOmenEffect(Player player) {
        if (player.level().isClientSide()) {
            return; // 只在服务端执行
        }
        
        // 安全地移除特定效果实例
        if (badOmenEffect != null) {
            removeSpecificEffect(player, badOmenEffect);
            badOmenEffect = null;
        }
    }
    
    /**
     * 维持不祥之兆效果
     */
    private void maintainBadOmenEffect(Player player) {
        if (player.level().isClientSide()) {
            return; // 只在服务端执行
        }
        
        // 检查玩家是否还有不祥之兆效果
        if (!player.hasEffect(MobEffects.BAD_OMEN)) {
            // 如果效果消失，重新施加
            applyBadOmenEffect(player);
        }
    }
    
    /**
     * 安全地移除特定的效果实例
     */
    private void removeSpecificEffect(Player player, MobEffectInstance effectToRemove) {
        // 获取玩家当前的所有效果
        var activeEffects = player.getActiveEffects();
        
        // 遍历效果列表，只移除与指定效果匹配的实例
        for (var effect : activeEffects) {
            if (effect.getEffect() == effectToRemove.getEffect() && 
                effect.getAmplifier() == effectToRemove.getAmplifier()) {
                player.removeEffect(effectToRemove.getEffect());
                break;
            }
        }
    }
    
    /**
     * 检查玩家是否拥有掠夺者契约效果
     */
    public static boolean hasIllagerContract(Player player) {
        return player != null && illagerContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.illager.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.illager.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.illager.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.illager.effect3")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 不需要保存额外的数据
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 不需要加载额外的数据
    }
}