package com.iamalittle.black_souls_options.contracts.effects;

import com.iamalittle.black_souls_options.contracts.effects.mobs.SilverfishContract;
import com.iamalittle.black_souls_options.contracts.effects.mobs.SnifferContract;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块破坏事件处理器，用于处理玩家挖掘方块时触发的契约效果
 * 包括蠹虫契约的秒破坏石头类方块效果
 */
public class BlockBreakEventHandler {
    
    /**
     * 处理玩家破坏方块事件（通用版本）
     * @param player 玩家
     * @param blockPos 方块位置
     * @param blockState 方块状态
     */
    public static void onPlayerBreakBlock(Player player, BlockPos blockPos, BlockState blockState) {
        if (player == null || blockPos == null || blockState == null || !player.isAlive()) {
            return;
        }
        
        // 检查并触发蠹虫契约的秒破坏石头类方块效果
        if (SilverfishContract.hasSilverfishContract(player)) {
            SilverfishContract.onPlayerBreakBlock(player, blockPos, blockState);
        }
        
        // 检查并触发嗅探兽契约的挖掘寻宝效果
        if (SnifferContract.hasSnifferContract(player)) {
            SnifferContract.onPlayerBreakBlock(player, blockPos, blockState);
        }
    }
    
    /**
     * 处理玩家开始挖掘方块事件（玩家按左键时立即调用）
     * @param player 玩家
     * @param blockPos 方块位置
     * @param blockState 方块状态
     * @return 是否阻止正常的挖掘过程
     */
    public static boolean onPlayerStartBreakBlock(Player player, BlockPos blockPos, BlockState blockState) {
        if (player == null || blockPos == null || blockState == null || !player.isAlive()) {
            return false;
        }
        
        // 检查并触发蠹虫契约的立即破坏石头类方块效果
        if (SilverfishContract.hasSilverfishContract(player)) {
            return SilverfishContract.onPlayerStartBreakBlock(player, blockPos, blockState);
        }
        
        return false;
    }
}