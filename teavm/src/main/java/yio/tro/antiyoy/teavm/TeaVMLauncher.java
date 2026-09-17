package yio.tro.antiyoy.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import org.teavm.jso.JSBody;
import yio.tro.antiyoy.PlatformType;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.gameplay.sim.SimConfig;

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

        // Отметки о ходе запуска выводятся прямо на страницу: консоли на
        // iOS нет, а застывшая заставка не отличается от падения.
        YioGdxGame.startupReporter = new WebStartupReporter();

        applyPlaytestParameter();

        WebApplicationConfiguration configuration = new WebApplicationConfiguration();

        configuration.canvasID = "canvas";

        // Ноль означает «занять всё окно».
        configuration.width = 0;
        configuration.height = 0;

        new WebApplication(new YioGdxGame(), configuration);
    }


    /**
     * ?turns=N разыгрывает партию сразу после запуска, ?scene=имя
     * открывает экран меню.
     *
     * Иначе до игрового поля надо дойти по меню, а снаружи — из браузерного
     * отладчика — это означает угадывание координат кнопок.
     */
    private static void applyPlaytestParameter() {
        String scene = readParameter("scene");
        if (scene != null && scene.length() > 0) {
            SimConfig.getInstance().startScene = scene;
        }

        int turns = readIntParameter("turns");
        if (turns <= 0) return;

        SimConfig.getInstance().playtestTurns = turns;
    }


    @JSBody(params = "name", script =
            "return new URLSearchParams(window.location.search).get(name);")
    private static native String readParameter(String name);


    @JSBody(params = "name", script =
            "var value = new URLSearchParams(window.location.search).get(name);" +
            "var number = parseInt(value, 10);" +
            "return isNaN(number) ? 0 : number;")
    private static native int readIntParameter(String name);
}
