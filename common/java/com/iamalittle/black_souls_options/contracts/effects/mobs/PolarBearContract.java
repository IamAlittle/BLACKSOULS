package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import java.util.*;

/**
 * 北极熊契约效果 - 免疫细雪冻伤伤害
 * 玩家契约北极熊后获得的能力：
 * 1. 完全免疫细雪的冻伤伤害
 * 2. 在细雪中不会受到伤害和减速效果
 */
public class PolarBearContract extends ContractEffect {
    private static final String EFFECT_ID = "polar_bear_freeze_immunity";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.polar_bear.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.polar_bear.description";

    // 北极熊契约玩家集合
    private static final Set<UUID> polarBearContractPlayers = new HashSet<>();

    public PolarBearContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            polarBearContractPlayers.add(player.getUUID());

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
            polarBearContractPlayers.remove(player.getUUID());

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
        // 北极熊契约不需要每tick更新，伤害免疫在受伤事件中处理
    }

    /**
     * 检查玩家是否应该免疫细雪冻伤伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean shouldImmuneFreezeDamage(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }

        // 检查玩家是否拥有北极熊契约效果
        if (!hasPolarBearContract(player)) {
            return false;
        }

        // 检查伤害来源是否为细雪冻伤伤害
        return isFreezeDamage(damageSource);
    }

    /**
     * 处理玩家受到伤害事件，检查是否需要免疫细雪冻伤伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean onPlayerHurt(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }

        // 检查玩家是否拥有北极熊契约效果
        if (!hasPolarBearContract(player)) {
            return false;
        }

        // 检查伤害来源是否为细雪冻伤伤害
        if (isFreezeDamage(damageSource)) {
            return true; // 表示伤害被免疫
        }

        return false; // 不免疫伤害
    }

    /**
     * 检查伤害来源是否为细雪冻伤伤害
     */
    private static boolean isFreezeDamage(DamageSource damageSource) {
        if (damageSource == null) {
            return false;
        }

        // 检查是否为冻伤伤害（细雪伤害）
        // 在Minecraft中，细雪造成的伤害类型通常是冻伤伤害
        return damageSource.is(DamageTypes.FREEZE) ||
               damageSource.getMsgId().contains("freeze") ||
               damageSource.getMsgId().contains("powder_snow");
    }

    /**
     * 检查玩家是否拥有北极熊契约效果
     */
    public static boolean hasPolarBearContract(Player player) {
        return player != null && polarBearContractPlayers.contains(player.getUUID());
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.polar_bear.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.polar_bear.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.polar_bear.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        return details;
    }
}