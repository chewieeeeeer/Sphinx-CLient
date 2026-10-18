package dev.sphinx.client.mixin;

import dev.sphinx.client.SphinxClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps the vanilla title screen for the Sphinx menu wherever the game opens it (startup, disconnect, ...). */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen sphinx$replaceTitleScreen(Screen screen) {
        // Exact class match: don't touch subclasses added by other mods.
        if (screen != null && screen.getClass() == TitleScreen.class) {
            return new SphinxClient.SphinxTitleScreen();
        }
        return screen;
    }
}
