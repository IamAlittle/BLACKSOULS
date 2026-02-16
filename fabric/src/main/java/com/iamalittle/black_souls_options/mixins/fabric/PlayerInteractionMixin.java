package com.iamalittle.black_souls_options.mixins.fabric;

import com.iamalittle.black_souls_options.contracts.effects.mobs.AxolotlContract;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player interaction mixin to prevent attacking and using items when feigning death
 */
@Mixin(Player.class)
public class PlayerInteractionMixin {

    /**
     * Prevent attacking when feigning death and add blaze contract ignite effect
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void preventAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否正在装死
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // 取消攻击动作
            ci.cancel();
            player.displayClientMessage(Component.translatable("black_souls_options.messages.feigning_death_cannot_attack"), true);
        }
    }



    /**
     * Prevent using items when feigning death
     */
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void preventItemUse(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否正在装死
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // 取消物品使用动作
            cir.setReturnValue(InteractionResult.PASS);
            player.displayClientMessage(Component.translatable("black_souls_options.messages.feigning_death_cannot_use_item"), true);
            return;
        }

        // 检查哞菇契约的右键点击玩家交互
        if (entity instanceof Player) {
            InteractionResult result = com.iamalittle.black_souls_options.contracts.effects.mobs.MooshroomContract.handlePlayerRightClick(player, entity, hand);
            if (result != InteractionResult.PASS) {
                cir.setReturnValue(result);
                return;
            }
        }
    }

    /**
     * Prevent eating food when feigning death
     */
    @Inject(method = "eat", at = @At("HEAD"), cancellable = true)
    private void preventEating(Level level, ItemStack itemStack, CallbackInfoReturnable<ItemStack> cir) {
        Player player = (Player) (Object) this;
        
        // 检查玩家是否正在装死
        if (AxolotlContract.isPlayerFeigningDeath(player)) {
            // 取消进食动作
            cir.setReturnValue(itemStack);
            player.displayClientMessage(Component.translatable("black_souls_options.messages.feigning_death_cannot_eat"), true);
        }

    }
}