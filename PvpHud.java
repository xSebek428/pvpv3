package pl.pvpmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rysuje Armor HUD i Potion HUD.
 *
 * Wydajność: metoda tylko CZYTA stan gracza po stronie klienta (brak pakietów,
 * brak zmian stanu gry), więc nie może powodować desyncu. Listy są
 * ponownie używane, żeby nie generować śmieci (GC) co klatkę.
 */
public final class PvpHud {
    private PvpHud() {}

    // Sloty wyświetlane w Armor HUD, od góry do dołu.
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    // Bufor na efekty - czyszczony co klatkę zamiast tworzyć nową listę.
    private static final List<StatusEffectInstance> EFFECT_BUFFER = new ArrayList<>();
    private static final Comparator<StatusEffectInstance> BY_DURATION_DESC =
            Comparator.comparingInt(StatusEffectInstance::getDuration).reversed();

    /** Wywoływane co klatkę z HudRenderCallback. */
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        // Nie rysuj, gdy nie ma gracza albo gracz wcisnął F1 (ukryty HUD).
        if (player == null || client.options.hudHidden) return;

        TextRenderer font = client.textRenderer;

        if (PvpConfig.ARMOR_HUD) renderArmorHud(context, font, player);
        if (PvpConfig.POTION_HUD) renderPotionHud(context, font, player);
    }

    // ------------------------------------------------------------------
    // ARMOR HUD
    // ------------------------------------------------------------------
    private static void renderArmorHud(DrawContext context, TextRenderer font, ClientPlayerEntity player) {
        final int rowHeight = 18; // 16 px ikona + 2 px odstępu

        // Liczymy ile slotów faktycznie rysujemy, żeby wyśrodkować HUD w pionie.
        int visible = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (PvpConfig.ARMOR_SHOW_EMPTY_SLOTS || !player.getEquippedStack(slot).isEmpty()) visible++;
        }
        if (visible == 0) return;

        int x = PvpConfig.ARMOR_HUD_X;
        int y = context.getScaledWindowHeight() / 2 - (visible * rowHeight) / 2 + PvpConfig.ARMOR_HUD_Y_OFFSET;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty() && !PvpConfig.ARMOR_SHOW_EMPTY_SLOTS) continue;

            if (!stack.isEmpty()) {
                context.drawItem(stack, x, y);                 // ikona przedmiotu
                context.drawStackOverlay(font, stack, x, y);   // pasek trwałości + licznik (np. strzały)

                if (stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    int left = max - stack.getDamage();
                    // Kolor tekstu: zielony (pełna trwałość) -> czerwony (prawie zniszczone).
                    float ratio = MathHelper.clamp((float) left / max, 0f, 1f);
                    int color = 0xFF000000 | MathHelper.hsvToRgb(ratio / 3f, 1f, 1f);
                    context.drawText(font, left + "/" + max, x + 20, y + 4, color, PvpConfig.TEXT_SHADOW);
                }
            }
            y += rowHeight;
        }
    }

    // ------------------------------------------------------------------
    // POTION HUD
    // ------------------------------------------------------------------
    private static void renderPotionHud(DrawContext context, TextRenderer font, ClientPlayerEntity player) {
        if (player.getStatusEffects().isEmpty()) return;

        EFFECT_BUFFER.clear();
        EFFECT_BUFFER.addAll(player.getStatusEffects());
        EFFECT_BUFFER.sort(BY_DURATION_DESC); // najdłużej trwające na górze

        int x = PvpConfig.POTION_HUD_X;
        int y = PvpConfig.POTION_HUD_Y;

        for (StatusEffectInstance effect : EFFECT_BUFFER) {
            // Nazwa efektu (tłumaczona na język gry) + poziom, np. "Szybkość II".
            String name = Text.translatable(effect.getTranslationKey()).getString();
            int amp = effect.getAmplifier();
            if (amp > 0) name += " " + (amp < 10 ? Text.translatable("enchantment.level." + (amp + 1)).getString() : String.valueOf(amp + 1));

            // Kolor nazwy = kolor mikstury z gry.
            int nameColor = 0xFF000000 | effect.getEffectType().value().getColor();

            // Czas trwania: mm:ss albo **:** dla efektów nieskończonych.
            String time;
            int timeColor = PvpConfig.POTION_TIME_COLOR;
            if (effect.isInfinite()) {
                time = "**:**";
            } else {
                int seconds = (effect.getDuration() + 19) / 20; // ticki -> sekundy (zaokrąglone w górę)
                time = (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
                if (seconds <= PvpConfig.POTION_LOW_TIME_SECONDS) timeColor = PvpConfig.POTION_LOW_TIME_COLOR;
            }

            context.drawText(font, name, x, y, nameColor, PvpConfig.TEXT_SHADOW);
            context.drawText(font, time, x + font.getWidth(name) + 6, y, timeColor, PvpConfig.TEXT_SHADOW);
            y += font.fontHeight + 2;
        }
    }
}
