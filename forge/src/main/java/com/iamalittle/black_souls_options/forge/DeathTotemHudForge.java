package com.iamalittle.black_souls_options.forge;

import com.iamalittle.black_souls_options.hud.DeathTotemHud;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge版本的死亡图腾HUD注册器
 */
@Mod.EventBusSubscriber(modid = "black_souls_options", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathTotemHudForge {
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        // 在GUI渲染之前渲染HUD，尝试避免被聊天框遮挡
        DeathTotemHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}