package com.gizmo.subtitleblocker.mixin;

import com.gizmo.subtitleblocker.BlockedSubtitleManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayGuiMixin {

	@Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
	//remove a subtitle from appearing if its blocked
	public void cancelSubtitleIfDisabled(SoundInstance sound, WeighedSoundEvents accessor, CallbackInfo ci) {
		if(BlockedSubtitleManager.blockedSubtitles.stream().anyMatch(rl ->
				accessor.getSubtitle() != null &&
				accessor.getResourceLocation().equals(rl))) {
			ci.cancel();
		}
	}
}
