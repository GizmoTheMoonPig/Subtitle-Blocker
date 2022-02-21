package com.gizmo.subtitleblocker;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(SubtitleBlocker.MOD_ID)
public class SubtitleBlocker {

	public static final Logger LOGGER = LogManager.getLogger("Subtitle Blocker");
	public static final String MOD_ID = "subtitleblocker";
	private static KeyMapping guiKey;

	public SubtitleBlocker() {
		MinecraftForge.EVENT_BUS.register(this);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupKeybind);

		//we're client only
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () ->
				new IExtensionPoint.DisplayTest(() -> "", (a, b) -> true));
	}

	public void setupKeybind(FMLClientSetupEvent event) {
		guiKey = new KeyMapping(
				"keybind.subtitleblocker.open_gui",
				KeyConflictContext.IN_GAME,
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_MINUS,
				"key.categories.misc");
		ClientRegistry.registerKeyBinding(getGuiKey());
	}

	@SubscribeEvent
	public void openGui(InputEvent.KeyInputEvent event) {
		if (getGuiKey().consumeClick() && Minecraft.getInstance().player != null) {
			if(!Minecraft.getInstance().options.showSubtitles) {
				Minecraft.getInstance().player.displayClientMessage(new TranslatableComponent("gui.subtitleblocker.subtitles_off").withStyle(ChatFormatting.RED), true);
			} else {
				SubtitleBlockerScreen.open();
			}
		}
	}

	@SubscribeEvent
	public void playerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
		if(event.getPlayer() != null) {
			try {
				BlockedSubtitleManager.load();
			} catch (NullPointerException e) {
				SubtitleBlocker.LOGGER.fatal("Couldn't load blocked subtitle list from event. Please report this! \nError: " + e);
			}
		}
	}

	public static KeyMapping getGuiKey() {
		return guiKey;
	}
}
