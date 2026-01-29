package com.iamalittle.black_souls_options.hud;

import com.iamalittle.black_souls_options.contracts.ContractManagerHelper;
import com.iamalittle.black_souls_options.contracts.effects.mobs.EvokerContract;
import com.iamalittle.black_souls_options.contracts.GlobalContractManager;
import com.iamalittle.black_souls_options.contracts.ContractManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * 死亡图腾HUD渲染器
 * 在经验条中间上方显示不死图腾图标
 */
public class DeathTotemHud {
    
    private static final int ICON_SIZE = 16;
    
    /**
     * 渲染死亡图腾HUD
     */
    public static void render(GuiGraphics guiGraphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        
        // 检查玩家是否激活了唤魔者契约
        ContractManager contractManager = ContractManagerHelper.getAppropriateContractManager(player);
        if (contractManager == null) {
            return; // 没有契约管理器时不显示
        }
        
        // 检查是否有激活的唤魔者契约
        boolean hasActiveEvokerContract = contractManager.getAllContracts().stream()
            .anyMatch(contract -> "minecraft:evoker".equals(contract.getEntityType()) && 
                contract.getEffects().stream().anyMatch(effect -> effect.isActive()));
        
        if (!hasActiveEvokerContract) {
            return; // 没有激活的唤魔者契约时不显示
        }
        
        // 检查玩家背包中是否有不死图腾
        boolean hasTotemInInventory = EvokerContract.hasTotemInInventory(player);
        
        // 只有当玩家有图腾时才显示图标
        if (!hasTotemInInventory) {
            return;
        }
        
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // 计算HUD位置：经验条中间上方，血条和饥饿条之间
        int hudX = screenWidth / 2 - ICON_SIZE / 2; // 屏幕水平居中
        int hudY = screenHeight - 49; // 经验条上方位置
        
        // 渲染图腾图标
        renderTotemIcon(guiGraphics, hudX, hudY);
    }
    
    /**
     * 渲染不死图腾（使用原版纹理图片）
     */
    private static void renderTotemIcon(GuiGraphics guiGraphics, int x, int y) {
        // 使用原版不死图腾纹理路径
        ResourceLocation totemTexture = new ResourceLocation("textures/item/totem_of_undying.png");
        
        // 渲染纹理图片
        guiGraphics.blit(totemTexture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}