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
 * 岩浆怪契约效果 - 免疫火焰伤害
 * 玩家契约岩浆怪后获得的能力：
 * 1. 完全免疫所有火焰伤害（包括火、熔岩、火焰弹等）
 * 2. 模仿岩浆怪在熔岩中生存的特性
 */
public class MagmaCubeContract extends ContractEffect {
    private static final String EFFECT_ID = "magma_cube_fire_immunity";
    private static final String DISPLAY_NAME = "岩浆怪";
    private static final String DESCRIPTION = "免疫火焰伤害";

    // 岩浆怪契约玩家集合
    private static final Set<UUID> magmaCubeContractPlayers = new HashSet<>();

    public MagmaCubeContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            magmaCubeContractPlayers.add(player.getUUID());

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
            magmaCubeContractPlayers.remove(player.getUUID());

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
        // 岩浆怪契约不需要每tick更新，伤害免疫在受伤事件中处理
    }

    /**
     * 检查玩家是否应该免疫火焰伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean shouldImmuneFireDamage(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }

        // 检查玩家是否拥有岩浆怪契约效果
        if (!hasMagmaCubeContract(player)) {
            return false;
        }

        // 检查伤害来源是否为火焰伤害
        return isFireDamage(damageSource);
    }

    /**
     * 处理玩家受到伤害事件，检查是否需要免疫火焰伤害
     * 这个方法需要在伤害事件处理器中调用
     */
    public static boolean onPlayerHurt(Player player, DamageSource damageSource) {
        if (player == null || damageSource == null) {
            return false;
        }

        // 检查玩家是否拥有岩浆怪契约效果
        if (!hasMagmaCubeContract(player)) {
            return false;
        }

        // 检查伤害来源是否为火焰伤害
        if (isFireDamage(damageSource)) {
            return true; // 表示伤害被免疫
        }

        return false; // 不免疫伤害
    }

    /**
     * 检查伤害来源是否为火焰伤害
     */
    private static boolean isFireDamage(DamageSource damageSource) {
        if (damageSource == null) {
            return false;
        }

        // 检查是否为火焰伤害（包括火、熔岩、火焰弹等）
        return damageSource.is(DamageTypeTags.IS_FIRE) ||
               damageSource.is(DamageTypes.IN_FIRE) ||
               damageSource.is(DamageTypes.ON_FIRE) ||
               damageSource.is(DamageTypes.LAVA) ||
               damageSource.is(DamageTypes.HOT_FLOOR) ||
               damageSource.getMsgId().contains("fire") ||
               damageSource.getMsgId().contains("lava") ||
               damageSource.getMsgId().contains("burn");
    }

    /**
     * 检查玩家是否拥有岩浆怪契约效果
     */
    public static boolean hasMagmaCubeContract(Player player) {
        return player != null && magmaCubeContractPlayers.contains(player.getUUID());
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.literal("§c岩浆怪契约效果："));
        details.add(Component.literal("§7- 完全免疫所有火焰伤害"));
        details.add(Component.literal("§7- 免疫火、熔岩、火焰弹等火焰伤害"));
        details.add(Component.literal("§7- 模仿岩浆怪在熔岩中生存的特性"));
        return details;
    }
}