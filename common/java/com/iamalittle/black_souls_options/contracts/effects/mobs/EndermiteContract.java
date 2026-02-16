package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import java.util.*;

/**
 * 末影螨契约效果 - 激怒末影人
 * 玩家契约末影螨后获得的能力：
 * 1. 让附近的末影人主动攻击玩家
 * 2. 效果范围与末影螨相同
 */
public class EndermiteContract extends ContractEffect {
    private static final String EFFECT_ID = "endermite_anger_enderman";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.endermite.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.endermite.description";
    
    // 激怒效果范围（格）
    private static final int ANGER_RANGE = 16;
    
    // 检查间隔（游戏刻）
    private static final int CHECK_INTERVAL = 20; // 1秒
    
    // 末影螨契约玩家集合
    private static final Set<UUID> endermiteContractPlayers = new HashSet<>();
    
    // 记录上次检查时间
    private static final Map<UUID, Integer> lastCheckTimeMap = new HashMap<>();
    
    public EndermiteContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }
    
    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            endermiteContractPlayers.add(player.getUUID());
            lastCheckTimeMap.put(player.getUUID(), 0);
            
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
            endermiteContractPlayers.remove(player.getUUID());
            lastCheckTimeMap.remove(player.getUUID());
            
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
        if (player != null && player.isAlive() && hasEndermiteContract(player)) {
            // 只在服务端执行
            if (!player.level().isClientSide()) {
                // 检查是否到了检查时间
                int currentTime = (int) (player.level().getGameTime() % CHECK_INTERVAL);
                int lastCheckTime = lastCheckTimeMap.getOrDefault(player.getUUID(), 0);
                
                if (currentTime != lastCheckTime) {
                    lastCheckTimeMap.put(player.getUUID(), currentTime);
                    
                    // 激怒附近的末影人
                    angerNearbyEndermen(player);
                }
            }
        }
    }
    
    /**
     * 激怒玩家附近的末影人
     */
    private static void angerNearbyEndermen(Player player) {
        // 获取玩家附近的末影人
        List<EnderMan> nearbyEndermen = player.level().getEntitiesOfClass(
            EnderMan.class, 
            player.getBoundingBox().inflate(ANGER_RANGE)
        );
        
        for (EnderMan enderman : nearbyEndermen) {
            // 检查末影人是否已经激怒
            if (!enderman.isAngry()) {
                // 激怒末影人，目标设为玩家
                enderman.setTarget(player);
            }
        }
    }
    
    /**
     * 检查玩家是否拥有末影螨契约效果
     */
    public static boolean hasEndermiteContract(Player player) {
        return player != null && endermiteContractPlayers.contains(player.getUUID());
    }
    
    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.endermite.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.endermite.effect1").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.endermite.effect2", ANGER_RANGE).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.endermite.effect3").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.endermite.warning").withStyle(style -> style.withColor(TextColor.parseColor("#FF5555"))));
        return details;
    }
}