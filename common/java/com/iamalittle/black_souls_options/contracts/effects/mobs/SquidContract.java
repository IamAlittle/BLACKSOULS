package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 鱿鱼契约效果 - 墨汁防御
 * 玩家契约鱿鱼后获得的能力：
 * 1. 被攻击时自动给攻击者失明效果
 * 2. 模仿鱿鱼的墨汁防御机制
 */
public class SquidContract extends ContractEffect {
    private static final String EFFECT_ID = "squid_blindness_defense";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.squid.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.squid.description";
    
    // 失明持续时间（秒）
    private static final int BLINDNESS_DURATION = 5;
    
    // 失明等级（0为基础等级）
    private static final int BLINDNESS_AMPLIFIER = 0;
    
    // 鱿鱼契约玩家集合
    private static final Set<UUID> squidContractPlayers = new HashSet<>();
    
    public SquidContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            squidContractPlayers.add(player.getUUID());
            
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
            squidContractPlayers.remove(player.getUUID());
            
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
        // 鱿鱼契约不需要每tick更新，防御逻辑在被攻击时处理
    }
    
    /**
     * 处理玩家被攻击事件
     * 这个方法需要在伤害事件处理器中调用
     */
    public static void onPlayerHurt(Player player, Entity attacker) {
        if (player == null || attacker == null || !player.isAlive() || !hasSquidContract(player)) {
            return;
        }
        
        // 检查攻击者是否为生物实体
        if (attacker instanceof LivingEntity livingAttacker) {
            // 施加失明效果
            MobEffectInstance blindnessEffect = new MobEffectInstance(
                MobEffects.BLINDNESS, 
                BLINDNESS_DURATION * 20, // 转换为tick数
                BLINDNESS_AMPLIFIER,
                false, // 不显示粒子效果
                true   // 显示图标
            );
            
            livingAttacker.addEffect(blindnessEffect);
            
            // 显示墨汁粒子效果
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                    15, 0.8, 0.8, 0.8, 0.2);
            }
            
            // 播放墨汁声音效果
            if (player.level() instanceof net.minecraft.server.level.ServerLevel) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.SQUID_SQUIRT, 
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有鱿鱼契约效果
     */
    public static boolean hasSquidContract(Player player) {
        return player != null && squidContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.squid.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.squid.effect_subtitle").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.squid.effect1").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.squid.effect2", BLINDNESS_DURATION).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.squid.effect3").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}