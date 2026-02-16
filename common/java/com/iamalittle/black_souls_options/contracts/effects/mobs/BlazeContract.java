package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 烈焰人契约效果 - 点燃
 * 玩家契约烈焰人后获得的能力：
 * 1. 攻击目标时使其被点燃
 * 2. 玩家在水中或下雨天会扣血
 */
public class BlazeContract extends ContractEffect {
    private static final String EFFECT_ID = "blaze_fire_bender";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.blaze.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.blaze.description";
    
    // 点燃持续时间（秒）
    private static final int BURN_DURATION = 5;
    
    // 扣血间隔（毫秒）
    private static final long DAMAGE_INTERVAL = 1000;
    
    // 扣血量
    private static final float DAMAGE_AMOUNT = 1.0f;
    
    // 上次扣血时间
    private long lastDamageTime;
    
    public BlazeContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
        this.lastDamageTime = 0;
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            blazeContractPlayers.add(player.getUUID());
            
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
            blazeContractPlayers.remove(player.getUUID());
            
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
        
        long currentTime = System.currentTimeMillis();
        
        // 检查玩家是否在水中或下雨天，若是则扣血
        if (shouldTakeDamage(player)) {
            if (currentTime - lastDamageTime >= DAMAGE_INTERVAL) {
                dealWaterDamage(player);
                lastDamageTime = currentTime;
            }
        }
    }
    
    /**
     * 检查玩家是否应该受到水/雨伤害
     */
    private boolean shouldTakeDamage(Player player) {
        Level level = player.level();
        
        // 检查是否在水中
        if (player.isInWaterOrRain()) {
            return true;
        }
        
        // 检查是否在下雨且暴露在雨中
        if (level.isRaining() && level.canSeeSky(player.blockPosition())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 对玩家造成水/雨伤害
     */
    private void dealWaterDamage(Player player) {
        // 对玩家造成伤害
        player.hurt(player.damageSources().onFire(), DAMAGE_AMOUNT);
        
        // 显示伤害粒子效果
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(),
                5, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    // 契约玩家集合（用于快速检查）
    private static final Set<UUID> blazeContractPlayers = new HashSet<>();
    
    /**
     * 点燃目标实体
     */
    public static void igniteTarget(Entity target, Player attacker) {
        if (target != null && attacker != null && attacker.isAlive() && hasBlazeContract(attacker)) {
            // 设置目标燃烧
            target.setSecondsOnFire(BURN_DURATION);
            
            // 显示火焰粒子效果
            if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.5, 0.5, 0.5, 0.2);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有烈焰人契约效果
     */
    public static boolean hasBlazeContract(Player player) {
        return player != null && blazeContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.blaze.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.blaze.ignite_effect").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.blaze.attack_ignite", BURN_DURATION).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.blaze.water_damage").withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        details.add(Component.translatable("black_souls_options.contracts.blaze.damage_amount", DAMAGE_AMOUNT).withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        return details;
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putLong("lastDamageTime", lastDamageTime);
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        if (nbt.contains("lastDamageTime")) {
            lastDamageTime = nbt.getLong("lastDamageTime");
        }
    }
}