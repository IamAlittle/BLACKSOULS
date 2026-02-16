package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.ContractDetector;
import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 蝌蚪契约效果 - 攻击时询问目标是否为妈妈
 * 玩家契约蝌蚪后获得的能力：
 * 1. 攻击时会自动在公屏聊天中询问目标是不是自己妈妈
 */
public class TadpoleContract extends ContractEffect {
    private static final String EFFECT_ID = "tadpole_mom_question";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.tadpole.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.tadpole.description";

    public TadpoleContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
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
            // 停用效果时发送消息
            String entityName = effectData.getString("contractEntityName");
            if (entityName.isEmpty()) {
                entityName = displayName; // 回退到效果名称
            }
            sendDeactivationMessage(player, entityName);
        }
    }

    @Override
    protected void onTick(Player player) {
        // 不需要每tick执行操作，通过事件监听器处理攻击事件
    }
    
    /**
     * 检查玩家是否拥有蝌蚪契约效果
     */
    public static boolean hasTadpoleContract(Player player) {
        return player != null && ContractDetector.hasContract(player, "minecraft:tadpole");
    }
    
    /**
     * 处理玩家攻击事件
     * 当玩家攻击时，在公屏聊天中询问目标是不是妈妈
     */
    public static void askIfTargetIsMom(Entity target, Player player, DamageSource damageSource) {
        if (hasTadpoleContract(player) && target instanceof LivingEntity) {
            // 检查是否为普通攻击（近战攻击）
            if (damageSource != null && !isMeleeAttack(damageSource)) {
                return; // 非近战攻击不触发
            }
            
            // 获取目标名称
            String targetName = target.getName().getString();
            
            // 使用玩家聊天消息的标准格式：<玩家名> 消息内容
            String message = String.format("%s，你是我的妈妈吗？", targetName);
            
            // 发送到所有在线玩家，显示为标准的玩家聊天消息
            player.level().players().forEach(p -> {
                // 使用标准的玩家聊天消息格式，显示为 <玩家名> 消息内容
                p.sendSystemMessage(Component.literal("<" + player.getScoreboardName() + "> " + message));
            });
            
            // 给玩家自己发送确认消息
            player.sendSystemMessage(Component.literal("§6[蝌蚪契约] 你正在寻找妈妈..."));
        }
    }
    
    /**
     * 检查是否为近战攻击
     */
    private static boolean isMeleeAttack(DamageSource damageSource) {
        // 近战攻击的判断标准
        return damageSource.is(DamageTypes.PLAYER_ATTACK) || 
               damageSource.is(DamageTypes.MOB_ATTACK) ||
               damageSource.getMsgId().equals("player") ||
               damageSource.getMsgId().equals("mob");
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.tadpole.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.tadpole.effect1").withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
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