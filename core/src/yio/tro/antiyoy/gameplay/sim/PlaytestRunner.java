package yio.tro.antiyoy.gameplay.sim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.gameplay.DebugFlags;
import yio.tro.antiyoy.gameplay.Province;

/**
 * Автозапуск партии ради проверки картинки.
 *
 * Появился после того, как с телефона пришло «иконок не видно»: автоматчи
 * отрисовку не трогают вовсе и такую поломку пропускают, а проверять глазами
 * на устройстве — самый долгий цикл из возможных.
 *
 * Партия разыгрывается в тестовом режиме (без отрисовки), затем режим
 * снимается, и кадры идут обычным путём игры — видно ровно то, что увидел бы
 * игрок. На десктопе кадр дополнительно сохраняется в PNG; в вебе снимок
 * делает браузер снаружи, поэтому путь к файлу там не задаётся.
 */
public class PlaytestRunner {

    /**
     * Сколько кадров дать игре после розыгрыша партии.
     *
     * Кэш фона применяется не в том же кадре, появление элементов меню
     * анимировано: снимок с первого же кадра застаёт пустой экран. Партия
     * к этому моменту заморожена, так что лишние кадры ничего не меняют.
     */
    private static final int FRAMES_BEFORE_SHOT = 40;

    private final YioGdxGame yioGdxGame;
    private final SimConfig config;

    private boolean matchReady;
    private int framesRendered;


    public PlaytestRunner(YioGdxGame yioGdxGame) {
        this.yioGdxGame = yioGdxGame;
        this.config = SimConfig.getInstance();
    }


    public boolean isMatchReady() {
        return matchReady;
    }


    public void prepareMatch() {
        matchReady = true;

        if (config.startScene != null) {
            openScene(config.startScene);
            return;
        }

        launchAndPlay();
    }


    /**
     * Открывает экран меню по имени. Проверять новый экран снимком
     * дешевле, чем доходить до него тапами на устройстве.
     */
    private void openScene(String name) {
        if (name.equals("mod")) {
            yio.tro.antiyoy.menu.scenes.Scenes.sceneModOptions.create();
            return;
        }

        if (name.equals("settings")) {
            yio.tro.antiyoy.menu.scenes.Scenes.sceneSettings.create();
            return;
        }

        System.out.println("Неизвестный экран: " + name);
    }


    private void launchAndPlay() {

        long matchSeed = config.getMatchSeed(0);

        DebugFlags.testMode = true;
        yioGdxGame.gamePaused = false;

        SimRunner.seedRandomSources(yioGdxGame.gameController, matchSeed);
        SimRunner.launchMatch(config, matchSeed);

        playTurns();

        DebugFlags.testMode = false;

        // Поле уже разыграно, но кэш фона в тестовом режиме не строился.
        yioGdxGame.gameView.updateCacheLevelTextures();

        // Появление поля анимировано, а снимок нужен сразу.
        yioGdxGame.gameView.appearFactor.setValues(1, 0);

        // Партия замораживается: иначе ИИ успевает доиграть её за те кадры,
        // что идут до снимка, и на экране оказывается окно победителя.
        yioGdxGame.setGamePaused(true);

        describeField();
    }


    private void playTurns() {
        int turns = config.playtestTurns;

        // Страховка от партии, которая не двигает ходы.
        int moveLimit = turns * 20 + 100;

        while (yioGdxGame.gameController.matchStatistics.turnsMade < turns) {
            yioGdxGame.gameController.move();

            moveLimit--;
            if (moveLimit <= 0) break;
            if (DebugFlags.testWinner != -1) break;
        }
    }


    /**
     * Сводка по полю одной строкой.
     *
     * Снимок показывает, что на экране, но не отвечает, пусто на нём из-за
     * отрисовки или из-за того, что показывать нечего. Одни и те же числа с
     * десктопа и из браузера сразу говорят, где именно расхождение.
     */
    private void describeField() {
        System.out.println("Ходов " + yioGdxGame.gameController.matchStatistics.turnsMade
                + ", гексов " + yioGdxGame.gameController.fieldManager.activeHexes.size()
                + ", провинций " + yioGdxGame.gameController.fieldManager.provinces.size()
                + ", объектов " + yioGdxGame.gameController.fieldManager.solidObjects.size()
                + ", воинов " + yioGdxGame.gameController.unitList.size()
                + ", казна " + describeMoney());

        YioGdxGame.reportStartup("ДИАГНОСТИКА поле: гексов "
                + yioGdxGame.gameController.fieldManager.activeHexes.size()
                + ", объектов " + yioGdxGame.gameController.fieldManager.solidObjects.size()
                + ", размер гекса " + yioGdxGame.gameView.hexViewSize
                + ", качество " + yioGdxGame.gameView.currentZoomQuality);
    }


    private String describeMoney() {
        StringBuilder builder = new StringBuilder();

        for (Province province : yioGdxGame.gameController.fieldManager.provinces) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(province.money);
        }

        return builder.toString();
    }


    /**
     * Вызывается в конце кадра, когда экран уже нарисован целиком.
     */
    public void afterFrame() {
        if (config.screenshotPath == null) return;

        framesRendered++;
        if (framesRendered < FRAMES_BEFORE_SHOT) return;

        writeScreenshot();

        Gdx.app.exit();
    }


    private void writeScreenshot() {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();

        Pixmap raw = Pixmap.createFromFrameBuffer(0, 0, width, height);
        Pixmap flipped = flipVertically(raw);
        raw.dispose();

        PixmapIO.writePNG(Gdx.files.local(config.screenshotPath), flipped);
        flipped.dispose();

        System.out.println("Снимок экрана: " + config.screenshotPath
                + " (" + width + "x" + height + ")");
    }


    /**
     * Чтение из буфера кадра идёт снизу вверх, а PNG пишется сверху вниз.
     */
    private Pixmap flipVertically(Pixmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        Pixmap result = new Pixmap(width, height, source.getFormat());
        result.setBlending(Pixmap.Blending.None);

        for (int y = 0; y < height; y++) {
            result.drawPixmap(source, 0, y, 0, height - 1 - y, width, 1);
        }

        return result;
    }
}
