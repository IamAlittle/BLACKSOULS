package com.iamalittle.black_souls_options.contracts.effects.mobs;

import com.iamalittle.black_souls_options.contracts.effects.ContractEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 嗅探兽契约效果 - 挖掘泥土有机率获得额外战利品
 * 玩家契约嗅探兽后，挖掘泥土、沙子、沙砾等方块时，有机率获得嗅探兽的找东西战利品表
 */
public class SnifferContract extends ContractEffect {
    private static final String EFFECT_ID = "sniffer_find_treasure";
    private static final String DISPLAY_NAME = "black_souls_options.contracts.sniffer.display_name";
    private static final String DESCRIPTION = "black_souls_options.contracts.sniffer.description";
    
    // 存储拥有嗅探兽契约的玩家UUID
    private static final List<UUID> snifferContractPlayers = new ArrayList<>();
    
    // 概率配置（0-100）
    private static final int TREASURE_CHANCE = 15; // 15%的几率获得战利品
    
    // 嗅探兽的战利品表路径
    private static final String SNIFFER_LOOT_TABLE = "minecraft:gameplay/sniffer_digging";
    
    // 泥土标签
    private static final TagKey<Block> DIRT_TAG = BlockTags.DIRT;

    public SnifferContract() {
        super(EFFECT_ID, DISPLAY_NAME, DESCRIPTION);
    }

    @Override
    protected void onActivate(Player player, boolean sendMessage) {
        if (player != null) {
            snifferContractPlayers.add(player.getUUID());
            
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
            snifferContractPlayers.remove(player.getUUID());
            
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
        // 不需要每tick执行操作，只在挖掘事件中处理
    }
    
    /**
     * 检查玩家是否拥有嗅探兽契约效果
     */
    public static boolean hasSnifferContract(Player player) {
        return player != null && snifferContractPlayers.contains(player.getUUID());
    }
    
    /**
     * 处理玩家挖掘方块事件，触发嗅探兽契约的战利品效果
     */
    public static void onPlayerBreakBlock(Player player, BlockPos pos, BlockState blockState) {
        if (player == null || pos == null || blockState == null || !hasSnifferContract(player)) {
            return; // 不处理
        }
        
        // 检查是否为泥土类方块
        if (isDirtBlock(blockState)) {
            // 有一定几率获得嗅探兽的战利品
            tryGenerateTreasure(player, pos, blockState);
        }
    }
    
    /**
     * 检查是否为泥土类方块
     */
    private static boolean isDirtBlock(BlockState blockState) {
        return blockState.is(DIRT_TAG);
    }
    
    /**
     * 尝试生成嗅探兽的战利品
     */
    private static void tryGenerateTreasure(Player player, BlockPos pos, BlockState blockState) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 使用Java的Random类
        Random random = new Random();
        
        // 检查是否触发战利品生成
        if (random.nextInt(100) < TREASURE_CHANCE) {
            // 只掉落瓶子草荚果和火把花种子
            List<ItemStack> lootItems = new ArrayList<>();
            
            // 随机选择掉落一种植物
            if (random.nextBoolean()) {
                // 掉落瓶子草荚果
                lootItems.add(new ItemStack(Blocks.PITCHER_PLANT));
            } else {
                // 掉落火把花种子
                lootItems.add(new ItemStack(Blocks.TORCHFLOWER));
            }
            
            // 生成并掉落战利品
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            for (ItemStack itemStack : lootItems) {
                if (!itemStack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(serverLevel, blockCenter.x, blockCenter.y, blockCenter.z, itemStack);
                    itemEntity.setDefaultPickUpDelay();
                    serverLevel.addFreshEntity(itemEntity);
                }
            }
        }
    }

    @Override
    public List<Component> getEffectDetails() {
        List<Component> details = new ArrayList<>();
        details.add(Component.translatable("black_souls_options.contracts.sniffer.effect_title").withStyle(style -> style.withColor(TextColor.parseColor("#55FFFF"))));
        details.add(Component.translatable("black_souls_options.contracts.sniffer.effect1", TREASURE_CHANCE).withStyle(style -> style.withColor(TextColor.parseColor("#55FF55"))));
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