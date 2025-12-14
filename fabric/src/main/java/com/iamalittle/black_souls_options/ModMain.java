package com.iamalittle.black_souls_options;

import com.iamalittle.black_souls_options.controllers.TargetEntityScreen;
import com.iamalittle.black_souls_options.render.ContractTrackerRenderer;
import com.iamalittle.black_souls_options.wrappers.FabricEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class ModMain implements ClientModInitializer {

	public static final String MODID = "black_souls_options";

	private final Logger logger = LogManager.getLogger(ModMain.class);
	private static boolean escapeKeyPressed = false;

	@Override
	public void onInitializeClient() {
		FabricEvents.setup();
		ContractTrackerRenderer.setup();

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide) {
					// 检查玩家是否戴着铁头盔
					if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.IRON_HELMET) {
						// 玩家戴着铁头盔，打开对话界面
						Minecraft.getInstance().setScreen(new TargetEntityScreen(entity));
					} else {
						// 玩家没有戴铁头盔，显示提示信息
						player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c需要戴着铁头盔才能与实体互动"), true);
					}

			}
			return InteractionResult.PASS;
		});
		
		logger.info("GalGame related mod initialized");
	}
}