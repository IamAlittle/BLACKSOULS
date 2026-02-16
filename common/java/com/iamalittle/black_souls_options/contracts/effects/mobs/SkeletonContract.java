package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 骷髅契约效果 - 无限箭矢
 * 玩家契约骷髅后，在生存模式下也能像创造模式一样拉弓射箭，不需要消耗箭矢
 */
public class SkeletonContract extends ContractEffect {
    private static final String EFFECT_ID = "skeleton_infinite_arrows";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.skeleton.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.skeleton.description";
    
    // 存储拥有骷髅契约的玩家UUID
    private static final List<UUID> skeletonContractPlayers = new ArrayList<>();

    public SkeletonContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            skeletonContractPlayers.add(player.getUUID());
            
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
            skeletonContractPlayers.remove(player.getUUID());
            
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
        // 不需要每tick执行操作，只在需要检查玩家状态时使用
    }
    
    /**
     * 检查玩家是否拥有骷髅契约效果
     */
    public static boolean hasSkeletonContract(Player player) {
        return player != null && skeletonContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 检查玩家是否可以使用无限箭矢
     */
    public static boolean canUseInfiniteArrows(Player player) {
        if (player == null) return false;
        
        // 如果是创造模式，直接返回true
        if (player.getAbilities().instabuild) {
            return true;
        }
        
        // 检查是否有骷髅契约
        return hasSkeletonContract(player);
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.skeleton.effect_title")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.skeleton.effect1")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
        details.add(Component.translatable("black_souls_options.contracts.skeleton.effect2")
                .withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
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