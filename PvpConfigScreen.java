package pl.pvpmod;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu moda w stylu "Mod Menu": lewy pasek z zakładkami + strona po prawej.
 * Wszystko rysowane ręcznie (DrawContext), więc łatwo zmienić wygląd.
 *
 * Zakładki: Gracze | Ustawienia | Mody | Profil
 * Jak dodać nową opcję: dopisz wiersz w metodzie odpowiedniej strony (pageSettings / pageModules).
 */
public class PvpConfigScreen extends Screen {
    // Rozmiar panelu w pikselach GUI.
    private static final int W = 330, H = 220, SIDE = 78;
    private static final String[] TABS = {"Gracze", "Ustawienia", "Mody", "Profil"};
    private static final String[] MODULES = {"Armor HUD", "Potion HUD", "Własny celownik", "No Hurt Cam"};
    private static final int[] ACCENTS = {0x3B82F6, 0x22C55E, 0xEF4444, 0xA855F7, 0xF97316, 0xEC4899};
    private static final int GREEN = 0xFF22C55E;

    /** Paleta kolorów motywu. */
    private record Theme(int panel, int side, int row, int rowHover, int control, int text, int sub, int line) {}
    private static final Theme[] THEMES = {
            // Ciemny
            new Theme(0xFF1E1E1E, 0xFF141414, 0xFF2A2A2A, 0xFF343434, 0xFF191919, 0xFFFFFFFF, 0xFFAAAAAA, 0xFF3A3A3A),
            // Jasny
            new Theme(0xFFE6E6E6, 0xFFCFCFCF, 0xFFF5F5F5, 0xFFFFFFFF, 0xFFBDBDBD, 0xFF111111, 0xFF555555, 0xFFB0B0B0),
            // Podstawowy
            new Theme(0xFF3B3B3B, 0xFF2B2B2B, 0xFF4A4A4A, 0xFF555555, 0xFF222222, 0xFFFFFFFF, 0xFFBBBBBB, 0xFF555555)
    };

    /** Obszar klikalny zapamiętany podczas rysowania klatki. */
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {}

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private int tab = 1;               // aktualna zakładka
    private int scroll = 0;            // przewinięcie strony
    private int contentHeight = 0;     // wysokość zawartości strony
    private int listening = -1;        // indeks modułu, któremu ustawiamy bind (-1 = nic)

    // Obszary ustawiane w render():
    private int cx, cx2, vy1, vy2;     // zawartość: lewa/prawa krawędź, góra/dół widocznego okna
    private TextRenderer font;
    private Theme t;

    public PvpConfigScreen(Screen parent) {
        super(Text.literal("PvP Client"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false; // gra działa dalej w tle
    }

    // ------------------------------------------------------------------
    // RYSOWANIE
    // ------------------------------------------------------------------
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        font = this.textRenderer;
        t = THEMES[Math.floorMod(PvpConfig.THEME, THEMES.length)];
        hits.clear();

        c.fill(0, 0, width, height, 0x88000000); // przyciemnione tło

        int px = (width - W) / 2, py = (height - H) / 2;
        rect(c, px, py, px + W, py + H, t.panel());

        // --- pasek boczny ---
        int sx = px + 6, sy = py + 6, sh = H - 12;
        rect(c, sx, sy, sx + SIDE, sy + sh, t.side());
        c.drawText(font, "Mod Menu", sx + 10, sy + 10, accent(), PvpConfig.TEXT_SHADOW);

        for (int i = 0; i < TABS.length; i++) {
            int ty = sy + 30 + i * 24;
            boolean sel = i == tab;
            boolean hov = mx >= sx + 4 && mx < sx + SIDE - 4 && my >= ty && my < ty + 20;
            if (sel) rect(c, sx + 4, ty, sx + SIDE - 4, ty + 20, darken(accent(), 0.6f));
            else if (hov) rect(c, sx + 4, ty, sx + SIDE - 4, ty + 20, t.row());
            c.drawText(font, TABS[i], sx + 12, ty + 6, t.text(), PvpConfig.TEXT_SHADOW);
            final int idx = i;
            addHit(sx + 4, ty, sx + SIDE - 4, ty + 20, () -> { tab = idx; scroll = 0; listening = -1; }, false);
        }

        // --- zawartość ---
        cx = sx + SIDE + 8;
        cx2 = px + W - 10;
        int cy = py + 10;
        c.drawText(font, TABS[tab], cx, cy, accent(), PvpConfig.TEXT_SHADOW);
        c.fill(cx, cy + 12, cx2, cy + 13, t.line());
        vy1 = cy + 18;
        vy2 = py + H - 8;

        int maxScroll = Math.max(0, contentHeight - (vy2 - vy1));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        c.enableScissor(cx, vy1, cx2, vy2); // przycinanie do widocznego okna
        int endY = switch (tab) {
            case 0 -> pageUsers(c, mx, my);
            case 1 -> pageSettings(c, mx, my);
            case 2 -> pageModules(c, mx, my);
            default -> pageProfile(c, mx, my);
        };
        c.disableScissor();
        contentHeight = endY + scroll - vy1;
    }

    // ------------------------------------------------------------------
    // STRONY
    // ------------------------------------------------------------------
    private int pageUsers(DrawContext c, int mx, int my) {
        int y = vy1 - scroll;
        List<AbstractClientPlayerEntity> players = client.world == null ? List.of() : client.world.getPlayers();
        if (players.isEmpty()) {
            c.drawText(font, "Brak graczy w pobliżu.", cx + 2, y + 4, t.sub(), PvpConfig.TEXT_SHADOW);
            return y + 16;
        }
        for (AbstractClientPlayerEntity p : players) {
            rowBg(c, y, 20, mx, my);
            PlayerSkinDrawer.draw(c, p.getSkinTextures(), cx + 4, y + 2, 16); // głowa gracza
            c.drawText(font, p.getName().getString(), cx + 26, y + 6, t.text(), PvpConfig.TEXT_SHADOW);
            String d = (client.player == p) ? "Ty" : (int) p.distanceTo(client.player) + " m";
            c.drawText(font, d, cx2 - 6 - font.getWidth(d), y + 6, t.sub(), PvpConfig.TEXT_SHADOW);
            y += 23;
        }
        return y;
    }

    private int pageSettings(DrawContext c, int mx, int my) {
        int y = vy1 - scroll;
        final int h = 22;

        // Kolor GUI - kliknięcie przełącza na następny kolor z listy ACCENTS.
        rowBg(c, y, h, mx, my);
        c.drawText(font, "Kolor GUI", cx + 6, y + 7, t.text(), PvpConfig.TEXT_SHADOW);
        String hex = String.format("#%06X", PvpConfig.GUI_COLOR);
        int swx = cx2 - 8 - 30;
        rect(c, swx, y + 5, swx + 30, y + 17, accent());
        c.drawText(font, hex, swx - 6 - font.getWidth(hex), y + 7, t.sub(), PvpConfig.TEXT_SHADOW);
        addHit(cx, y, cx2, y + h, () -> {
            int next = 0;
            for (int i = 0; i < ACCENTS.length; i++) if (ACCENTS[i] == PvpConfig.GUI_COLOR) next = (i + 1) % ACCENTS.length;
            PvpConfig.GUI_COLOR = ACCENTS[next];
        }, true);
        y += h + 3;

        y = toggleRow(c, mx, my, y, "Zaokrąglone ramki", PvpConfig.ROUNDED, () -> PvpConfig.ROUNDED = !PvpConfig.ROUNDED);
        y = toggleRow(c, mx, my, y, "Dźwięki menu", PvpConfig.MENU_SOUNDS, () -> PvpConfig.MENU_SOUNDS = !PvpConfig.MENU_SOUNDS);
        y = toggleRow(c, mx, my, y, "Cień tekstu w menu i HUD", PvpConfig.TEXT_SHADOW, () -> PvpConfig.TEXT_SHADOW = !PvpConfig.TEXT_SHADOW);

        // Motyw: trzy przyciski (od prawej).
        rowBg(c, y, h, mx, my);
        c.drawText(font, "Motyw", cx + 6, y + 7, t.text(), PvpConfig.TEXT_SHADOW);
        String[] names = {"Ciemny", "Jasny", "Podstawowy"};
        int bx = cx2 - 6;
        for (int i = names.length - 1; i >= 0; i--) {
            int bw = font.getWidth(names[i]) + 12;
            bx -= bw;
            boolean sel = PvpConfig.THEME == i;
            rect(c, bx, y + 4, bx + bw, y + h - 4, sel ? GREEN : t.control());
            c.drawText(font, names[i], bx + 6, y + 7, sel ? 0xFFFFFFFF : t.text(), PvpConfig.TEXT_SHADOW);
            final int idx = i;
            addHit(bx, y + 4, bx + bw, y + h - 4, () -> PvpConfig.THEME = idx, true);
            bx -= 4;
        }
        y += h + 6;

        // Bindy modułów.
        c.drawText(font, "Bindy modułów (ESC = wyczyść)", cx + 2, y, t.sub(), PvpConfig.TEXT_SHADOW);
        y += 12;
        for (int i = 0; i < MODULES.length; i++) {
            rowBg(c, y, 20, mx, my);
            c.drawText(font, MODULES[i], cx + 6, y + 6, t.text(), PvpConfig.TEXT_SHADOW);
            bindButton(c, cx2 - 6 - 70, y + 3, 70, i);
            y += 23;
        }
        return y;
    }

    private int pageModules(DrawContext c, int mx, int my) {
        int y = vy1 - scroll;
        for (int i = 0; i < MODULES.length; i++) {
            final int idx = i;
            rowBg(c, y, 22, mx, my);
            c.drawText(font, MODULES[i], cx + 6, y + 7, t.text(), PvpConfig.TEXT_SHADOW);
            boolean on = PvpConfig.isModule(i);
            // przełącznik po prawej, bind po lewej od niego
            addHit(cx, y, cx2, y + 22, () -> PvpConfig.setModule(idx, !PvpConfig.isModule(idx)), true);
            drawSwitch(c, cx2 - 8 - 26, y + 5, on);
            bindButton(c, cx2 - 8 - 26 - 6 - 60, y + 4, 60, i);
            y += 25;
        }
        c.drawText(font, "Kliknij pole z klawiszem, potem naciśnij klawisz.", cx + 2, y + 2, t.sub(), PvpConfig.TEXT_SHADOW);
        return y + 14;
    }

    private int pageProfile(DrawContext c, int mx, int my) {
        int y = vy1 - scroll;
        if (client.player != null) {
            PlayerSkinDrawer.draw(c, client.player.getSkinTextures(), cx + 6, y + 4, 32);
        }
        c.drawText(font, client.getSession().getUsername(), cx + 46, y + 8, t.text(), PvpConfig.TEXT_SHADOW);
        c.drawText(font, "Minecraft 1.21.4 (Fabric)", cx + 46, y + 22, t.sub(), PvpConfig.TEXT_SHADOW);
        y += 46;
        int active = 0;
        for (int i = 0; i < MODULES.length; i++) if (PvpConfig.isModule(i)) active++;
        rowBg(c, y, 22, mx, my);
        c.drawText(font, "Aktywne moduły", cx + 6, y + 7, t.text(), PvpConfig.TEXT_SHADOW);
        String s = active + "/" + MODULES.length;
        c.drawText(font, s, cx2 - 8 - font.getWidth(s), y + 7, t.sub(), PvpConfig.TEXT_SHADOW);
        return y + 25;
    }

    // ------------------------------------------------------------------
    // ELEMENTY
    // ------------------------------------------------------------------
    private int toggleRow(DrawContext c, int mx, int my, int y, String label, boolean on, Runnable click) {
        rowBg(c, y, 22, mx, my);
        c.drawText(font, label, cx + 6, y + 7, t.text(), PvpConfig.TEXT_SHADOW);
        drawSwitch(c, cx2 - 8 - 26, y + 5, on);
        addHit(cx, y, cx2, y + 22, click, true);
        return y + 25;
    }

    /** Przełącznik 26x12: zielony = włączony. */
    private void drawSwitch(DrawContext c, int x, int y, boolean on) {
        rect(c, x, y, x + 26, y + 12, on ? GREEN : 0xFF666666);
        int kx = on ? x + 14 : x + 2;
        c.fill(kx, y + 2, kx + 10, y + 10, 0xFFFFFFFF);
    }

    /** Pole z klawiszem; kliknięcie = nasłuchiwanie nowego klawisza. */
    private void bindButton(DrawContext c, int x, int y, int w, int idx) {
        boolean listen = listening == idx;
        rect(c, x, y, x + w, y + 14, listen ? accent() : t.control());
        String s = listen ? "..." : keyName(PvpConfig.BINDS[idx]);
        c.drawText(font, s, x + (w - font.getWidth(s)) / 2, y + 3, listen ? 0xFFFFFFFF : t.text(), PvpConfig.TEXT_SHADOW);
        addHit(x, y, x + w, y + 14, () -> listening = (listening == idx ? -1 : idx), true);
    }

    private static String keyName(int key) {
        if (key < 0) return "—";
        return InputUtil.fromKeyCode(key, -1).getLocalizedText().getString();
    }

    private void rowBg(DrawContext c, int y, int h, int mx, int my) {
        boolean hov = mx >= cx && mx < cx2 && my >= Math.max(y, vy1) && my < Math.min(y + h, vy2);
        rect(c, cx, y, cx2, y + h, hov ? t.rowHover() : t.row());
    }

    /** Prostokąt; przy włączonych zaokrągleniach ścina rogi o 1 px. */
    private static void rect(DrawContext c, int x1, int y1, int x2, int y2, int color) {
        if (PvpConfig.ROUNDED && x2 - x1 > 4 && y2 - y1 > 4) {
            c.fill(x1 + 1, y1, x2 - 1, y2, color);
            c.fill(x1, y1 + 1, x2, y2 - 1, color);
        } else {
            c.fill(x1, y1, x2, y2, color);
        }
    }

    private static int accent() {
        return 0xFF000000 | PvpConfig.GUI_COLOR;
    }

    private static int darken(int argb, float f) {
        int r = (int) (((argb >> 16) & 255) * f), g = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void addHit(int x1, int y1, int x2, int y2, Runnable action, boolean scrollable) {
        if (scrollable) { // obetnij do widocznego okna
            y1 = Math.max(y1, vy1);
            y2 = Math.min(y2, vy2);
            if (y1 >= y2) return;
        }
        hits.add(new Hit(x1, y1, x2, y2, action));
    }

    private void playClick() {
        if (PvpConfig.MENU_SOUNDS) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    // ------------------------------------------------------------------
    // WEJŚCIE
    // ------------------------------------------------------------------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // od końca - elementy narysowane później (np. przyciski bindów) mają pierwszeństwo
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (mouseX >= h.x1() && mouseX < h.x2() && mouseY >= h.y1() && mouseY < h.y2()) {
                    h.action().run();
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, contentHeight - (vy2 - vy1));
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 12));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ustawianie bindu: ESC czyści, inny klawisz ustawia.
        if (listening >= 0) {
            PvpConfig.BINDS[listening] = (keyCode == GLFW.GLFW_KEY_ESCAPE) ? -1 : keyCode;
            listening = -1;
            PvpConfig.save();
            return true;
        }
        // Klawisz menu (X) zamyka menu.
        if (PvpModClient.openMenuKey != null && PvpModClient.openMenuKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        PvpConfig.save();
        this.client.setScreen(parent);
    }
}
