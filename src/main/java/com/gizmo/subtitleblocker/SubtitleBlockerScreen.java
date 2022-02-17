package com.gizmo.subtitleblocker;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubtitleBlockerScreen extends Screen {

	private float scrollOffs;
	private boolean scrolling;
	private EditBox search;
	private final int xSize = 199;
	private final int ySize = 156;
	private int leftPos;
	private int topPos;
	private List<ResourceLocation> subtitles = new ArrayList<>();
	private int startIndex;
	private static boolean blockedButton = false;
	private static final ResourceLocation TEXTURE = new ResourceLocation(SubtitleBlocker.MOD_ID, "textures/gui/subtitle_screen.png");

	protected SubtitleBlockerScreen() {
		super(TextComponent.EMPTY);
		Minecraft.getInstance().setScreen(this);
	}

	@Override
	protected void init() {
		super.init();
		blockedButton = false;
		this.leftPos = (this.width - xSize) / 2;
		this.topPos = (this.height - ySize) / 2;

		//allow holding keys, useful for the search bar
		Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(true);

		//init subtitles
		BlockedSubtitleManager.load();
		this.refreshSearchResults();

		//search bar!
		this.search = new EditBox(this.font, this.leftPos + 10, this.topPos + 9, 150, 9, new TranslatableComponent("test"));
		this.search.setMaxLength(70);
		this.search.setBordered(false);
		this.search.setVisible(true);
		this.search.setFocus(false);
		this.search.setTextColor(16777215);

		//button to view blocked subtitles
		this.addRenderableWidget(new BlockedSubtitlesButton(this.leftPos + 173, this.topPos + 4, button -> {
			blockedButton = !blockedButton;
			this.search.setValue(blockedButton ? new TranslatableComponent("gui.subtitleblocker.blocked_subtitles").getString() : "");
			this.refreshSearchResults();
			this.scrollOffs = 0.0F;
			this.startIndex = 0;
		}));
	}

	@Override
	public void tick() {
		super.tick();
		this.search.tick();
	}

	@Override
	public void render(@Nonnull PoseStack ms, int x, int y, float ticks) {
		this.renderBackground(ms);
		//screen render
		RenderSystem.setShaderTexture(0, TEXTURE);
		this.blit(ms, this.leftPos, this.topPos, 0, 0, this.xSize, this.ySize);

		//scroller render
		int k = (int) (109.0F * this.scrollOffs);
		this.blit(ms, this.leftPos + 177, this.topPos + 24 + k, 0, 156, 12, 15);

		//search bar render
		this.search.render(ms, x, y, ticks);

		//subtitle string renders
		this.renderSubtitles(ms, this.leftPos + 11, this.topPos + 26, this.startIndex + 7);

		super.render(ms, x, y, ticks);

		this.renderTooltip(ms, ItemStack.EMPTY, x, y);
	}

	private void renderSubtitles(PoseStack ms, int startX, int startY, int startIndex) {
		for (int i = this.startIndex; i < startIndex && i < this.subtitles.size(); ++i) {
			int j = i - this.startIndex;
			int y = startY + j * 18 + 2;
			WeighedSoundEvents sound = Minecraft.getInstance().getSoundManager().getSoundEvent(subtitles.get(i));
			if (sound != null) {
				//we remove sounds if they dont have a subtitle on init, so this should be ok
				if (Objects.requireNonNull(sound.getSubtitle()).getString().length() > 27) {
					//trim down sound names that are longer than 27 characters
					drawString(ms, this.font, sound.getSubtitle().getString().substring(0, 26) + "...", startX, y,
							BlockedSubtitleManager.blockedSubtitles.contains(new ResourceLocation(subtitles.get(i).toString())) ? 16733525 : 0xFFFFFF);
				} else {
					drawString(ms, this.font, sound.getSubtitle().getString(), startX, y,
							BlockedSubtitleManager.blockedSubtitles.contains(new ResourceLocation(subtitles.get(i).toString())) ? 16733525 : 0xFFFFFF);
				}
			}
		}

		if(subtitles.isEmpty() && blockedButton) {
			drawCenteredString(ms, this.font, new TranslatableComponent("gui.subtitleblocker.empty_list"), startX + 78, startY + 3 * 18 + 2, 0xFFFFFF);
		}
	}

	@Override
	protected void renderTooltip(@Nonnull PoseStack ms, @Nonnull ItemStack stack, int x, int y) {
		int topX = this.leftPos + 9;
		int topY = this.topPos + 23;
		int index = this.startIndex + 7;

		for (int l = this.startIndex; l < Math.min(subtitles.size(), index); ++l) {
			int selectedIndex = l - this.startIndex;
			double d0 = x - topX;
			double d1 = y - (double) (topY + selectedIndex * 18);
			if (d0 >= 0.0D && d1 >= 0.0D && d0 < 160.0D && d1 < 17.0D) {
				this.renderTooltip(ms, new TextComponent(subtitles.get(l).toString()), x, y);
			}
		}
	}

	private int getOffscreenRows() {
		return this.subtitles.size() - 7;
	}

	@Override
	public boolean charTyped(char character, int amount) {
		if (search != null && search.isFocused()) {
			String s = this.search.getValue();
			if (this.search.charTyped(character, amount)) {
				if (!Objects.equals(s, this.search.getValue())) {
					this.refreshSearchResults();
					this.scrollOffs = 0.0F;
					this.startIndex = 0;
				}
				return true;
			} else {
				return false;
			}
		}
		return super.charTyped(character, amount);
	}

	//is this the most efficient way to do this? probably not. But it works fine
	private void refreshSearchResults() {
		subtitles.clear();

		//add all sounds
		subtitles.addAll(Minecraft.getInstance().getSoundManager().getAvailableSounds());

		//remove sounds if they dont have a subtitle or its a null sound
		subtitles.removeIf(rl ->
				Minecraft.getInstance().getSoundManager().getSoundEvent(rl) == null ||
						Objects.requireNonNull(Minecraft.getInstance().getSoundManager().getSoundEvent(rl)).getSubtitle() == null);

		//remove duplicates, if any
		this.subtitles = subtitles.stream().distinct().collect(Collectors.toList());

		//sort alphabetically
		subtitles.sort((o1, o2) -> Objects.requireNonNull(Objects.requireNonNull(Minecraft.getInstance().getSoundManager().getSoundEvent(o1)).getSubtitle()).getString().compareToIgnoreCase(Objects.requireNonNull(Objects.requireNonNull(Minecraft.getInstance().getSoundManager().getSoundEvent(o2)).getSubtitle()).getString()));

		//remove ones that dont match the stuff typed, if anything
		if (!blockedButton && this.search != null && !this.search.getValue().isEmpty()) {
			subtitles.removeIf(rl -> !Objects.requireNonNull(Objects.requireNonNull(Minecraft.getInstance().getSoundManager().getSoundEvent(rl)).getSubtitle()).getString().toLowerCase(Locale.ROOT).contains(this.search.getValue().toLowerCase(Locale.ROOT)));
		}

		if(blockedButton) {
			subtitles.removeIf(rl -> !BlockedSubtitleManager.blockedSubtitles.contains(Objects.requireNonNull(Minecraft.getInstance().getSoundManager().getSoundEvent(rl)).getResourceLocation()));
		}
	}

	@Override
	public boolean keyPressed(int key, int value, int modifier) {
		if (!search.isFocused() && SubtitleBlocker.getGuiKey().matches(key, value)) {
			this.onClose();
			return true;
		}
		String s = this.search.getValue();
		if (this.search.keyPressed(key, value, modifier)) {
			if (!Objects.equals(s, this.search.getValue())) {
				this.refreshSearchResults();
			}

			return true;
		} else {
			return this.search.isFocused() && this.search.isVisible() && key != 256 || super.keyPressed(key, value, modifier);
		}
	}

	@Override
	public boolean mouseDragged(double p_231045_1_, double p_231045_3_, int p_231045_5_, double p_231045_6_, double p_231045_8_) {
		if (this.scrolling && this.subtitles.size() > 7) {
			int i = this.topPos + 24;
			int j = i + 125;
			this.scrollOffs = ((float) p_231045_3_ - (float) i - 7.5F) / ((float) (j - i) - 15.0F);
			this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
			this.startIndex = (int) ((double) (this.scrollOffs * (float) this.getOffscreenRows()) + 0.5D);
			return true;
		} else {
			return super.mouseDragged(p_231045_1_, p_231045_3_, p_231045_5_, p_231045_6_, p_231045_8_);
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double direction) {
		if (this.subtitles.size() > 7) {
			int i = this.getOffscreenRows();
			this.scrollOffs = (float) ((double) this.scrollOffs - direction / (double) i);
			this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
			this.startIndex = (int) ((double) (this.scrollOffs * (float) i) + 0.5D);
		}

		return true;
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		this.scrolling = false;
		int topX = this.leftPos + 9;
		int topY = this.topPos + 23;
		int index = this.startIndex + 7;

		for (int l = this.startIndex; l < Math.min(subtitles.size(), index); ++l) {
			int selectedIndex = l - this.startIndex;
			double d0 = x - topX;
			double d1 = y - (double) (topY + selectedIndex * 18);
			if (d0 >= 0.0D && d1 >= 0.0D && d0 < 160.0D && d1 < 17.0D) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				if (BlockedSubtitleManager.blockedSubtitles.contains(subtitles.get(l))) {
					BlockedSubtitleManager.blockedSubtitles.remove(subtitles.get(l));
				} else {
					BlockedSubtitleManager.blockedSubtitles.add(subtitles.get(l));
				}
				BlockedSubtitleManager.saveBlockedSubtitles();
				return true;
			}
		}

		topX = this.leftPos + 177;
		topY = this.topPos + 24;
		if (x >= (double) topX && x < (double) (topX + 12) && y >= (double) topY && y < (double) (topY + 125)) {
			this.scrolling = true;
		}

		topX = this.leftPos + 10;
		topY = this.topPos + 8;
		this.search.setFocus(x >= (double) topX && x < (double) (topX + 150) && y >= (double) topY && y < (double) (topY + 9) && !blockedButton);

		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		super.onClose();
		BlockedSubtitleManager.saveBlockedSubtitles();
	}

	//dumb
	public static void open() {
		new SubtitleBlockerScreen();
	}

	public static class BlockedSubtitlesButton extends Button {

		public BlockedSubtitlesButton(int x, int y, OnPress press) {
			super(x, y, 20, 18, TextComponent.EMPTY, press);
		}

		@Override
		public void renderButton(@Nonnull PoseStack ms, int x, int y, float partialTicks) {
			if(this.visible) {
				RenderSystem.setShaderTexture(0, SubtitleBlockerScreen.TEXTURE);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				this.isHovered = x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;

				int texX = 200;
				int texY = 0;

				if(this.isHovered) texY = (this.height + 1) * 2;

				if(SubtitleBlockerScreen.blockedButton) texY = this.height + 1;

				this.blit(ms, this.x, this.y, texX, texY, this.width, this.height);
			}
		}
	}
}
