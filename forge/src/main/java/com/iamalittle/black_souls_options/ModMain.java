package com.iamalittle.black_souls_options;

import com.iamalittle.black_souls_options.config.BlackSoulsConfig;
import com.iamalittle.black_souls_options.controllers.TargetEntityScreen;
import com.iamalittle.black_souls_options.contracts.ContractSystem;
import com.iamalittle.black_souls_options.input.SpitAbilityKeyManager;
import com.iamalittle.black_souls_options.render.ContractTrackerRenderer;
import com.iamalittle.black_souls_options.sound.ModSounds;
import com.iamalittle.black_souls_options.utils.ItemCheckUtils;
import com.iamalittle.black_souls_options.wrappers.ForgeEvents;
import com.iamalittle.black_souls_options.forge.ContractEventsForge;
import com.iamalittle.black_souls_options.forge.network.ForgeContractNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ModMain.MODID)
public class ModMain {

	public static final String MODID = "black_souls_options";

	private final Logger logger = LogManager.getLogger(ModMain.class);

	public ModMain() {
		ForgeEvents.setup();
		logger.info("BlackSouls Options initialized");
	}

	@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEvents {

		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event) {
			ContractTrackerRenderer.setup();
			
			// 初始化TOML配置文件
			BlackSoulsConfig.getInstance();
			
			// 初始化契约系统，启动定时更新器
			ContractSystem.getInstance();
			
			// 注册网络处理器
			ForgeContractNetwork.initialize();

		}

		@SubscribeEvent
		public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
			event.register(SpitAbilityKeyManager.SPIT_ABILITY_KEY);
		}
	}
	
	@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
	public static class ForgeClientEvents {
		
		@SubscribeEvent
		public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
			if (event.getEntity().level().isClientSide) {
					if (ItemCheckUtils.canOpenInterface(event.getEntity())) {
						Minecraft.getInstance().setScreen(new TargetEntityScreen(event.getTarget()));
					} else {
						// 玩家不满足条件，只在debug模式启用时显示提示信息
						if (BlackSoulsConfig.getInstance().isEnableDebugMode()) {
							String requirement = ItemCheckUtils.getRequirementDescription();
							event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("black_souls_options.messages.interface_requirement", requirement), true);
						}
					}
				}
		}
		
		@SubscribeEvent
		public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
			if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
				Minecraft minecraft = Minecraft.getInstance();
				if (minecraft.player != null) {
					SpitAbilityKeyManager.updateKeyState();
				}
			}
		}
	}
}