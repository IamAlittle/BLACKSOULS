package com.iamalittle.black_souls_options;

import com.iamalittle.black_souls_options.controllers.TargetEntityScreen;
import com.iamalittle.black_souls_options.contracts.ContractSystem;
import com.iamalittle.black_souls_options.input.ContractAbilityKeyManager;
import com.iamalittle.black_souls_options.render.ContractTrackerRenderer;
import com.iamalittle.black_souls_options.wrappers.FabricEvents;
import com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
		
		// 初始化契约系统，启动定时更新器
		ContractSystem.getInstance();
		
		// 注册网络处理器
		FabricContractNetwork.initialize();

		// 注册契约能力按键
		KeyBindingHelper.registerKeyBinding(ContractAbilityKeyManager.CONTRACT_ABILITY_KEY);
		
		// 注册客户端tick事件，用于更新按键状态
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				ContractAbilityKeyManager.updateKeyState();
			}
		});

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