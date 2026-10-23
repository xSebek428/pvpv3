package pl.pvpmod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Główna klasa moda (entrypoint "main").
 * Mod jest czysto kliencki, więc tu tylko logujemy start.
 * Cała logika renderowania jest w PvpModClient i PvpHud.
 */
public class PvpMod implements ModInitializer {
    public static final String MOD_ID = "pvpclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("PvP Client załadowany.");
    }
}
