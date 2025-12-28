package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;

/**
 * 末影人契约效果 - 免疫弹射物，但接触水会掉血
 * 玩家契约末影人后获得的能力：
 * 1. 免疫所有弹射物伤害
 * 2. 接触水时会受到伤害（类似末影人怕水的特性）
 */
public class EnderManContract extends ContractEffect {
    private static final String EFFECT_ID = "enderman_projectile_immunity";
    private static final String DISPLAY_NAME = "末影之躯";
    private static final String DESCRIPTION = "免疫弹射物，但接触水会掉血";
    
    // 水伤害间隔（毫秒）
    private static final long WATER_DAMAGE_INTERVAL = 1000; // 1秒
    
    // 水伤害量
    private static final float WATER_DAMAGE_AMOUNT = 1.0F; // 半颗心
    
    // 末影人契约玩家集合
    private static final Set<UUID> enderManContractPlayers = new HashSet<>();
    
    // 上次水伤害时间记录
    private long lastWaterDamageTime = 0;
    
    public EnderManContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            enderManContractPlayers.add(player.getUUID());
            
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
            enderManContractPlayers.remove(player.getUUID());
            
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
        if (player != null && player.isAlive()) {
            // 检查玩家是否在水中，如果在水中则受到伤害
            checkWaterDamage(player);
        }
    }
    
    /**
     * 检查玩家是否在水中，如果在水中则受到伤害（模仿末影人怕水的特性）
     */
    private void checkWaterDamage(Player player) {
        if (player != null && player.isAlive() && player.isInWater()) {
            long currentTime = System.currentTimeMillis();
            
            // 检查是否达到水伤害间隔
            if (currentTime - lastWaterDamageTime >= WATER_DAMAGE_INTERVAL) {
                // 对玩家造成水伤害
                player.hurt(player.damageSources().drown(), WATER_DAMAGE_AMOUNT);
                
                // 更新上次伤害时间
                lastWaterDamageTime = currentTime;
            }
        }
    }
    
    /**
     * 检查玩家是否应该免疫伤害（模仿末影人的弹射物免疫）
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean shouldImmuneDamage(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }
        
        // 检查玩家是否拥有末影人契约效果
        if (!hasEnderManContract(player)) {
            return false;
        }
        
        // 检查伤害来源是否为弹射物（模仿末影人的弹射物免疫）
        return damageSource.is(DamageTypeTags.IS_PROJECTILE);
    }
    
    /**
     * 处理玩家受到伤害事件，检查是否需要免疫弹射物伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean onPlayerHurt(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }
        
        // 检查玩家是否拥有末影人契约效果
        if (!hasEnderManContract(player)) {
            return false;
        }
        
        // 检查伤害来源是否为弹射物（模仿末影人的弹射物免疫）
        if (damageSource.is(DamageTypeTags.IS_PROJECTILE)) {
            return true; // 表示伤害被免疫
        }
        
        return false; // 不免疫伤害
    }
    
    /**
     * 检查玩家是否拥有末影人契约效果
     */
    public static boolean hasEnderManContract(Player player) {
        return player != null && enderManContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§5末影人契约效果："));
        details.add(Component.literal("§7- 免疫所有弹射物伤害"));
        details.add(Component.literal("§7- 接触水时会受到伤害（每1秒造成半颗心伤害）"));
        details.add(Component.literal("§7- 模仿末影人的特性：怕水但免疫弹射物"));
        return details;
    }
}