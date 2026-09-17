package yio.tro.antiyoy.gameplay.sim;

import yio.tro.antiyoy.ai.Difficulty;
import yio.tro.antiyoy.gameplay.LevelSize;

/**
 * Параметры режима автоматчей. Заполняется из аргументов командной строки
 * в лаунчере, до старта игры.
 */
public class SimConfig {

    private static SimConfig instance;

    public boolean enabled;
    public boolean selfTest;

    /** Проверка воспроизводимости реплеев: число партий, 0 — выключено. */
    public int replayCheckMatches;
    public int matches;
    public long seed;
    public int fractionsQuantity;
    public int levelSize;
    public int difficulty;
    public boolean slayRules;
    public boolean diplomacy;
    public int maxTurns;
    public String outputPath;

    /**
     * Путь к по-ходовому следу состояния. Пустой — след не пишется.
     * Нужен приёмке этапа 1: обычный diff по этому файлу показывает не
     * «результаты разошлись», а номер хода, на котором они разошлись.
     */
    public String tracePath;

    /**
     * Путь для снимка игрового экрана. Пустой — снимок не делается.
     * Нужен, чтобы проверять картинку прогоном, а не глазами на телефоне.
     */
    public String screenshotPath;


    /**
     * Сколько ходов разыграть автозапуском партии. 0 — автозапуска нет.
     * В вебе задаётся параметром адреса ?turns=N.
     */
    public int playtestTurns;

    /**
     * Экран меню, который открыть сразу после запуска. Пустой — обычный
     * старт. Нужен, чтобы проверять новые экраны снимком, а не пальцем.
     */
    public String startScene;

    /**
     * Флаги мода заданы из командной строки. Настройки их тогда не трогают:
     * прогон обязан идти ровно с тем набором механик, который запросили,
     * а не с тем, что игрок оставил в меню.
     */
    public boolean modFlagsOverridden;


    private SimConfig() {
        defaultValues();
    }


    public static SimConfig getInstance() {
        if (instance == null) {
            instance = new SimConfig();
        }

        return instance;
    }


    public void defaultValues() {
        enabled = false;
        selfTest = false;
        replayCheckMatches = 0;
        matches = 100;
        seed = 0;
        fractionsQuantity = 5;
        levelSize = LevelSize.MEDIUM;
        difficulty = Difficulty.BALANCER;
        slayRules = false;
        diplomacy = true;
        maxTurns = 400;
        outputPath = "sim-results/sim.csv";
        tracePath = null;
        screenshotPath = null;
        playtestTurns = 0;
        startScene = null;
        modFlagsOverridden = false;
    }


    /**
     * Сид конкретного матча. Выводится из базового сида и номера матча, чтобы
     * прогон целиком воспроизводился по одному числу.
     */
    public long getMatchSeed(int matchIndex) {
        return seed * 1000003L + matchIndex;
    }


    /** Нужен ли автозапуск: партия, экран меню или и то и другое. */
    public boolean isPlaytestRequested() {
        return playtestTurns > 0 || startScene != null;
    }


    public String describe() {
        return "matches=" + matches +
                " seed=" + seed +
                " fractions=" + fractionsQuantity +
                " levelSize=" + levelSize +
                " difficulty=" + difficulty +
                " slay=" + slayRules +
                " diplomacy=" + diplomacy +
                " maxTurns=" + maxTurns;
    }
}
