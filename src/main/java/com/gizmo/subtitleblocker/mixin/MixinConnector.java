package com.gizmo.subtitleblocker.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

@SuppressWarnings("unused")
public class MixinConnector implements IMixinConnector {
	@Override
	public void connect() {
		Mixins.addConfiguration("subtitleblocker.mixins.json");
	}
}
