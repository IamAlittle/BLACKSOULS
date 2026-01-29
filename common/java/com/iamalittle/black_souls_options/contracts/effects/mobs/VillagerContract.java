package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.ContractDetector;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 村民契约效果 - 村庄英雄
 */
public class VillagerContract extends ContractEffect {
    private static final String EFFECT_ID = "villager_hero_of_the_village";
    private static final String DISPLAY_NAME = "村庄英雄";
    private static final String DESCRIPTION = "获得村庄英雄效果，与村民交易获得折扣";
    
    // 检查间隔（tick数，20 tick = 1秒）
    private static final int CHECK_INTERVAL = 200; // 每10秒检查一次
    
    // 上次检查时间记录
    private long lastCheckTime = 0;

    public VillagerContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            // 立即应用村庄英雄效果
            applyHeroOfTheVillageEffect(player);
            
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
            // 移除村庄英雄效果
            removeHeroOfTheVillageEffect(player);
            
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
        
        // 检查是否应该进行效果检查
        long currentTime = player.level().getGameTime();
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        
        lastCheckTime = currentTime;
        
        // 只有服务器端才处理效果逻辑
        if (player.level().isClientSide()) {
            return;
        }
        
        // 确保玩家持续拥有村庄英雄效果
        ensureHeroOfTheVillageEffect(player);
    }
    
    /**
     * 应用村庄英雄效果
     */
    private void applyHeroOfTheVillageEffect(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // 应用村庄英雄效果（等级1，持续10分钟）
        player.addEffect(new MobEffectInstance(
            MobEffects.HERO_OF_THE_VILLAGE, 
            -1, // 转换为tick数
            4,
            false, // 不显示粒子效果
            false
        ));
    }
    
    /**
     * 移除村庄英雄效果
     */
    private void removeHeroOfTheVillageEffect(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // 移除村庄英雄效果
        player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
    }
    
    /**
     * 确保玩家持续拥有村庄英雄效果
     */
    private void ensureHeroOfTheVillageEffect(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        
        // 检查玩家是否还有村庄英雄效果
        MobEffectInstance heroEffect = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        
        if (heroEffect == null || heroEffect.getDuration() < 100) { // 如果效果即将消失（少于5秒）
            // 重新应用村庄英雄效果
            applyHeroOfTheVillageEffect(player);
        }
    }
    
    /**
     * 检查玩家是否拥有村民契约效果
     */
    public static boolean hasVillagerContract(Player player) {
        return player != null && ContractDetector.hasContract(player, "minecraft:villager");
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§6村民契约效果："));
        details.add(Component.literal("§a- 获得村庄英雄效果"));
        return details;
    }

    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        // 不需要保存额外数据
        return nbt;
    }

    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        // 不需要加载额外数据
    }
}