package pl.pvpmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

/**
 * Entrypoint "client": wczytuje config, rejestruje HUD, klawisz menu (X)
 * oraz obsługę bindów poszczególnych modułów.
 */
public class PvpModClient implements ClientModInitializer {
    // Klawisz otwierający menu - domyślnie X (zmienisz w Sterowaniu).
    public static KeyBinding openMenuKey;

    // Poprzedni stan klawiszy bindów (żeby reagować raz na wciśnięcie).
    private static final boolean[] prev = new boolean[PvpConfig.BINDS.length];

    @Override
    public void onInitializeClient() {
        PvpConfig.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.pvpclient"));

        HudRenderCallback.EVENT.register(PvpHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new PvpConfigScreen(null));
            }

            // Bindy modułów działają tylko gdy nie ma otwartego okna (czat, ekwipunek, menu).
            if (client.currentScreen != null || client.getWindow() == null) {
                Arrays.fill(prev, false);
                return;
            }
            long handle = client.getWindow().getHandle();
            for (int i = 0; i < PvpConfig.BINDS.length; i++) {
                int key = PvpConfig.BINDS[i];
                boolean down = key >= 0 && InputUtil.isKeyPressed(handle, key);
                if (down && !prev[i]) {
                    PvpConfig.setModule(i, !PvpConfig.isModule(i));
                    PvpConfig.save();
                }
                prev[i] = down;
            }
        });
    }
}
