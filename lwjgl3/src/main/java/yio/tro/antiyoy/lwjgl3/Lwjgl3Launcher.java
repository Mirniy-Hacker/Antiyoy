package yio.tro.antiyoy.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import yio.tro.antiyoy.PlatformType;
import yio.tro.antiyoy.YioGdxGame;

/**
 * Десктопная точка входа. Оригинал собирался через сгенерированный
 * DesktopLauncher, которого в репозитории yiotro/Antiyoy нет.
 */
public class Lwjgl3Launcher {

    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 960;


    public static void main(String[] args) {
        YioGdxGame.platformType = PlatformType.pc;

        new Lwjgl3Application(new YioGdxGame(), createConfiguration());
    }


    private static Lwjgl3ApplicationConfiguration createConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        configuration.setTitle("Antiyoy Mod");
        configuration.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        return configuration;
    }
}
