package pl.pvpmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.pvpmod.PvpConfig;

/**
 * Podmienia vanilla celownik na statyczny plusik.
 * Wstrzykujemy się na początek renderCrosshair i anulujemy oryginał (cancel),
 * dzięki czemu znika też animowany wskaźnik ataku wokół celownika.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void pvpclient$customCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!PvpConfig.CUSTOM_CROSSHAIR) return; // wyłączone w configu -> działa vanilla

        ci.cancel(); // nie rysuj vanilla celownika

        // Celownik tylko w pierwszej osobie (jak w vanilla).
        if (!MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) return;

        int t = PvpConfig.CROSSHAIR_THICKNESS;
        int arm = PvpConfig.CROSSHAIR_ARM;
        int cx = (context.getScaledWindowWidth() - t) / 2;
        int cy = (context.getScaledWindowHeight() - t) / 2;

        // Kontur (rysowany najpierw, o 1 px większy).
        if (PvpConfig.CROSSHAIR_OUTLINE) {
            int c = PvpConfig.CROSSHAIR_OUTLINE_COLOR;
            context.fill(cx - arm - 1, cy - 1, cx + t + arm + 1, cy + t + 1, c);   // poziomy
            context.fill(cx - 1, cy - arm - 1, cx + t + 1, cy + t + arm + 1, c);   // pionowy
        }

        // Właściwy plusik.
        int color = PvpConfig.CROSSHAIR_COLOR;
        context.fill(cx - arm, cy, cx + t + arm, cy + t, color);   // linia pozioma
        context.fill(cx, cy - arm, cx + t, cy + t + arm, color);   // linia pionowa
    }
}
