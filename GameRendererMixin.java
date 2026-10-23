package pl.pvpmod.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.pvpmod.PvpConfig;

/**
 * No Hurt Cam - wyłącza przechylanie/trzęsienie kamery po otrzymaniu obrażeń.
 * To czysto wizualna zmiana po stronie klienta; nie wpływa na rzeczywiste obrażenia ani serwer.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void pvpclient$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (PvpConfig.NO_HURT_CAM) ci.cancel();
    }
}
