package com.iamalittle.black_souls_options.mixins.fabric;

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
        // Check if player is feigning death on both client and server
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // Only show message on server side to avoid duplicate messages
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§c装死时不能使用物品！"), true);
            }
            // Cancel the item use and return the original item stack to ensure client-server synchronization
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(interactionHand)));
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
            // Only show message on server side to avoid duplicate messages
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("§c装死时不能使用物品！"), true);
            }
            // Cancel the block interaction
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}