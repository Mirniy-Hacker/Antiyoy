package yio.tro.antiyoy.teavm;

import org.teavm.jso.JSBody;
import yio.tro.antiyoy.YioGdxGame;

/**
 * Пишет ход запуска прямо на страницу.
 *
 * Консоли на iOS нет, а исключение внутри кадра гасит цикл отрисовки: на
 * экране застывает заставка, и падение неотличимо от зависания. Отметки по
 * шагам показывают, где именно всё встало.
 */
public class WebStartupReporter implements YioGdxGame.StartupReporter {

    @Override
    public void report(String message) {
        showOnPage(message);
    }


    /**
     * showError определена в index.html: её же использует перехват ошибок.
     */
    @JSBody(params = "text", script = "if (window.showError) window.showError(text); else console.log(text);")
    private static native void showOnPage(String text);
}
