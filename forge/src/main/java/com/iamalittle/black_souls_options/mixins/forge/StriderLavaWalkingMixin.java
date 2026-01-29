package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.StriderContract;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 赤足兽契约Mixin - 岩浆行走
 * 修改岩浆的碰撞形状，让拥有赤足兽契约的玩家可以在岩浆上行走
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class StriderLavaWalkingMixin {
    @Shadow public abstract FluidState getFluidState();

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    public void shape(BlockGetter p_60743_, BlockPos p_60744_, CollisionContext p_60745_, CallbackInfoReturnable<VoxelShape> cir) {
        // 检查是否为流体方块
        if (getFluidState().isEmpty()) return;
        
        Fluid fluid = getFluidState().getType();
        
        // 只处理岩浆
        if (fluid.equals(Fluids.LAVA) || fluid.equals(Fluids.FLOWING_LAVA)) {
            // 从CollisionContext获取相关实体，支持服务器和客户端
            // 检查CollisionContext是否包含实体信息
            if (p_60745_ instanceof net.minecraft.world.phys.shapes.EntityCollisionContext) {
                net.minecraft.world.phys.shapes.EntityCollisionContext entityContext = (net.minecraft.world.phys.shapes.EntityCollisionContext) p_60745_;
                // 获取实体
                net.minecraft.world.entity.Entity entity = entityContext.getEntity();
                
                if (entity instanceof Player player) {
                    // 检查玩家是否拥有赤足兽契约
                    if (StriderContract.hasStriderContract(player)) {
                        // 计算玩家脚部位置与岩浆方块的相对位置
                        // 只在玩家脚部接近岩浆方块顶部时才将其设为固体
                        // 这样玩家可以在岩浆表面行走，但不会在岩浆内部被卡住
                        double playerFootY = player.getY();
                        double blockTopY = p_60744_.getY() + 1.0;
                        
                        // 当玩家脚部距离岩浆方块顶部小于0.5格时，将岩浆设为固体
                        if (blockTopY - playerFootY < 0.5) {
                            cir.setReturnValue(Shapes.block());
                            cir.cancel();
                        }
                    }
                }
            }
        }
    }
}