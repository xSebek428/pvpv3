package pl.pvpmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * USTAWIENIA MODA.
 * - Włączniki, wygląd menu i bindy zmieniasz w grze (klawisz X -> menu).
 *   Zapisują się w config/pvpclient.properties.
 * - Pozycje HUD i kolory celownika edytujesz tutaj w kodzie (kolory 0xAARRGGBB).
 */
public final class PvpConfig {
    private PvpConfig() {}

    // ---- Moduły (zmieniane z menu / bindów) ----
    public static boolean ARMOR_HUD = true;
    public static boolean POTION_HUD = true;
    public static boolean CUSTOM_CROSSHAIR = true;
    public static boolean NO_HURT_CAM = true;

    // ---- Wygląd menu ----
    public static int GUI_COLOR = 0x3B82F6;   // kolor akcentu (RRGGBB)
    public static boolean ROUNDED = true;     // zaokrąglone ramki
    public static boolean MENU_SOUNDS = true; // dźwięki kliknięć w menu
    public static boolean TEXT_SHADOW = true; // cień tekstu w menu i HUD
    public static int THEME = 2;              // 0 = Ciemny, 1 = Jasny, 2 = Podstawowy

    // ---- Bindy modułów (kody GLFW, -1 = brak) w kolejności: Armor, Potion, Celownik, NoHurtCam ----
    public static final int[] BINDS = {-1, -1, -1, -1};

    // ---- Armor HUD ----
    public static final int ARMOR_HUD_X = 4;
    public static final int ARMOR_HUD_Y_OFFSET = -50;   // ujemne = wyżej, dodatnie = niżej od środka
    public static final boolean ARMOR_SHOW_EMPTY_SLOTS = false;

    // ---- Potion HUD ----
    public static final int POTION_HUD_X = 4;
    public static final int POTION_HUD_Y = 4;
    public static final int POTION_LOW_TIME_SECONDS = 10;
    public static final int POTION_LOW_TIME_COLOR = 0xFFFF5555;
    public static final int POTION_TIME_COLOR = 0xFFFFFFFF;

    // ---- Celownik (plusik) ----
    public static final int CROSSHAIR_COLOR = 0xFFFFFFFF;
    public static final int CROSSHAIR_ARM = 4;
    public static final int CROSSHAIR_THICKNESS = 1;
    public static final boolean CROSSHAIR_OUTLINE = true;
    public static final int CROSSHAIR_OUTLINE_COLOR = 0xFF000000;

    // ---- Dostęp do modułów po indeksie (0..3) ----
    public static boolean isModule(int i) {
        return switch (i) {
            case 0 -> ARMOR_HUD;
            case 1 -> POTION_HUD;
            case 2 -> CUSTOM_CROSSHAIR;
            default -> NO_HURT_CAM;
        };
    }

    public static void setModule(int i, boolean v) {
        switch (i) {
            case 0 -> ARMOR_HUD = v;
            case 1 -> POTION_HUD = v;
            case 2 -> CUSTOM_CROSSHAIR = v;
            default -> NO_HURT_CAM = v;
        }
    }

    // ---- Zapis / odczyt ----
    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pvpclient.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Properties p = new Properties();
            p.load(r);
            ARMOR_HUD = Boolean.parseBoolean(p.getProperty("armorHud", "true"));
            POTION_HUD = Boolean.parseBoolean(p.getProperty("potionHud", "true"));
            CUSTOM_CROSSHAIR = Boolean.parseBoolean(p.getProperty("customCrosshair", "true"));
            NO_HURT_CAM = Boolean.parseBoolean(p.getProperty("noHurtCam", "true"));
            ROUNDED = Boolean.parseBoolean(p.getProperty("rounded", "true"));
            MENU_SOUNDS = Boolean.parseBoolean(p.getProperty("menuSounds", "true"));
            TEXT_SHADOW = Boolean.parseBoolean(p.getProperty("textShadow", "true"));
            GUI_COLOR = Integer.parseInt(p.getProperty("guiColor", "3B82F6"), 16);
            THEME = Integer.parseInt(p.getProperty("theme", "2"));
            for (int i = 0; i < BINDS.length; i++) {
                BINDS[i] = Integer.parseInt(p.getProperty("bind" + i, "-1"));
            }
        } catch (IOException | NumberFormatException e) {
            PvpMod.LOGGER.warn("Nie udało się wczytać configu", e);
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("armorHud", String.valueOf(ARMOR_HUD));
        p.setProperty("potionHud", String.valueOf(POTION_HUD));
        p.setProperty("customCrosshair", String.valueOf(CUSTOM_CROSSHAIR));
        p.setProperty("noHurtCam", String.valueOf(NO_HURT_CAM));
        p.setProperty("rounded", String.valueOf(ROUNDED));
        p.setProperty("menuSounds", String.valueOf(MENU_SOUNDS));
        p.setProperty("textShadow", String.valueOf(TEXT_SHADOW));
        p.setProperty("guiColor", String.format("%06X", GUI_COLOR));
        p.setProperty("theme", String.valueOf(THEME));
        for (int i = 0; i < BINDS.length; i++) p.setProperty("bind" + i, String.valueOf(BINDS[i]));
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "PvP Client");
        } catch (IOException e) {
            PvpMod.LOGGER.warn("Nie udało się zapisać configu", e);
        }
    }
}
