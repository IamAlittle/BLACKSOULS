package com.iamalittle.black_souls_options;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import com.iamalittle.black_souls_options.controllers.TargetEntityScreen;
import com.iamalittle.black_souls_options.contracts.ContractSystem;
import com.iamalittle.black_souls_options.input.SpitAbilityKeyManager;
import com.iamalittle.black_souls_options.render.ContractTrackerRenderer;
import com.iamalittle.black_souls_options.sound.ModSounds;
import com.iamalittle.black_souls_options.wrappers.FabricEvents;
import com.iamalittle.black_souls_options.fabric.network.FabricContractNetwork;
import com.iamalittle.black_souls_options.utils.ItemCheckUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
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
		
		// 初始化Cloth Config
		com.iamalittle.black_souls_options.config.BlackSoulsClothConfig.init();
		
		// 初始化配置（保持向后兼容）
		BlackSoulsConfig.getInstance();
		
		// 初始化契约系统，启动定时更新器
		ContractSystem.getInstance();
		
		// 注册网络处理器
		FabricContractNetwork.initialize();
		
		// 注册自定义音效
		ModSounds.registerFabricSounds();

		// 注册吐口水能力按键
		KeyBindingHelper.registerKeyBinding(SpitAbilityKeyManager.SPIT_ABILITY_KEY);
		
		// 注册客户端tick事件，用于更新按键状态
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				SpitAbilityKeyManager.updateKeyState();
			}
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide) {
				// 检查玩家是否满足界面打开条件
				if (ItemCheckUtils.canOpenInterface(player)) {
					// 玩家满足条件，打开对话界面
					Minecraft.getInstance().setScreen(new TargetEntityScreen(entity));
				} else {
					// 玩家不满足条件，只在debug模式启用时显示提示信息
					if (BlackSoulsConfig.getInstance().isEnableDebugMode()) {
						String requirement = ItemCheckUtils.getRequirementDescription();
						player.displayClientMessage(Component.translatable("black_souls_options.messages.interface_requirement", requirement), true);
					}
				}

			}
			return InteractionResult.PASS;
		});

		logger.info("BlackSouls Options initialized");
	}
}