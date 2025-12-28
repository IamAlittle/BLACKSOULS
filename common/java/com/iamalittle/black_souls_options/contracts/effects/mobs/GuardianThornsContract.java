package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 守卫者反伤契约效果 - 被攻击时对攻击者造成反伤
 * 玩家被攻击时会对攻击你的目标造成2点反伤伤害
 * 模仿守卫者的荆棘反伤机制
 */
public class GuardianThornsContract extends ContractEffect {
    private static final String EFFECT_ID = "guardian_thorns";
    private static final String DISPLAY_NAME = "反伤";
    private static final String DESCRIPTION = "被攻击时对攻击者造成2点反伤伤害";
    
    // 反伤参数
    private static final float THORNS_DAMAGE = 2.0f; // 反伤伤害值
    
    // 存储拥有反伤契约的玩家
    private static final Set<UUID> thornsContractPlayers = new HashSet<>();
    
    public GuardianThornsContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            thornsContractPlayers.add(player.getUUID());
            
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
            thornsContractPlayers.remove(player.getUUID());
            
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
        // 守卫者反伤契约不需要tick逻辑，反伤在受伤时触发
    }
    /**
     * 处理玩家受伤事件
     * @param player 受伤的玩家
     * @param damageSource 伤害来源
     * @param damageAmount 伤害值
     * @return 是否触发了反伤效果
     */
    public static boolean onPlayerHurt(Player player, DamageSource damageSource, float damageAmount) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        
        // 获取攻击者
        Entity attacker = damageSource.getEntity();
        if (attacker == null || attacker == player) {
            return false;
        }
        
        // 检查玩家是否拥有守卫者契约
        if (!hasGuardianThornsContract(player)) {
            return false;
        }
        
        // 对攻击者造成荆棘伤害
        attacker.hurt(player.damageSources().thorns(player), THORNS_DAMAGE);
        
        // 播放守卫者受伤音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.GUARDIAN_HURT, SoundSource.HOSTILE, 1.0F, 1.0F);
        return true;
    }
    
    /**
     * 检查玩家是否拥有守卫者反伤契约
     */
    public static boolean hasGuardianThornsContract(Player player) {
        return player != null && thornsContractPlayers.contains(player.getUUID());
    }
    
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        // 读取额外的保存数据（如果需要）
    }
    
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        // 保存额外的数据（如果需要）
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6守卫者反伤契约效果："));
        details.add(Component.literal("§7- 被攻击时对攻击者造成2点反伤伤害"));

        return details;
    }
}