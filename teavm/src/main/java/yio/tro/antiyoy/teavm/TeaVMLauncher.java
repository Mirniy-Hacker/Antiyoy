package yio.tro.antiyoy.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import yio.tro.antiyoy.PlatformType;
import yio.tro.antiyoy.YioGdxGame;

/**
 * Веб-точка входа. Собирается в JS или WASM и открывается в Safari на
 * телефоне, ставится на домашний экран как PWA — без подписи, AltStore и
 * семидневного срока жизни сборки.
 */
public class TeaVMLauncher {

    public static void main(String[] args) {
        // Телефон, а не десктоп: от этого зависит раскладка интерфейса и
        // обработка касаний. Веб-сборка живёт именно на телефоне.
        YioGdxGame.platformType = PlatformType.ios;

        WebApplicationConfiguration configuration = new WebApplicationConfiguration();

        configuration.canvasID = "canvas";

        // Ноль означает «занять всё окно».
        configuration.width = 0;
        configuration.height = 0;

        new WebApplication(new YioGdxGame(), configuration);
    }
}
