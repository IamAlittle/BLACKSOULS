package com.iamalittle.black_souls_options.mixins.forge;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemStack mixin to prevent item usage when player is feigning death
 */
@Mixin(ItemStack.class)
public class MixinItemStack {

    /**
     * Prevent using items when feigning death
     * This method intercepts the use method to prevent item usage
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventUse(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        // 检查玩家是否正在装死（客户端和服务器端都检查以确保同步）
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // 只在服务器端显示消息，避免重复显示
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("black_souls_options.messages.feigning_death_cannot_use_item"), true);
            }
            // 取消物品使用并返回原始物品栈，确保客户端-服务器同步
            ItemStack originalStack = player.getItemInHand(interactionHand);
            cir.setReturnValue(InteractionResultHolder.fail(originalStack));
        }
    }

    /**
     * Prevent using items on blocks when feigning death
     * This method intercepts the useOn method to prevent block interactions
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void preventUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null && AxolotlContract.isPlayerFeigningDeath(player)) {
            // 只在服务器端显示消息，避免重复显示
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("black_souls_options.messages.feigning_death_cannot_use_item"), true);
            }
            // 取消物品使用并显示消息
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}