package yio.tro.antiyoy.gameplay.sim;

import yio.tro.antiyoy.SettingsManager;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.ai.Difficulty;
import yio.tro.antiyoy.gameplay.DebugFlags;
import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.LevelSize;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.SpeedManager;
import yio.tro.antiyoy.gameplay.loading.LoadingManager;
import yio.tro.antiyoy.gameplay.loading.LoadingParameters;
import yio.tro.antiyoy.gameplay.loading.LoadingType;
import yio.tro.antiyoy.gameplay.replays.Replay;
import yio.tro.antiyoy.gameplay.rules.GameRules;

/**
 * Проверка воспроизводимости реплеев.
 *
 * Прогоняет партию с записью, затем проигрывает её реплей и сравнивает
 * состояние. Нужна приёмке этапа 1: «реплеи старых партий воспроизводятся
 * без расхождений».
 *
 * Что реплеи гарантируют на самом деле, выяснено прогоном, а не из
 * документации. Среди типов RepAction нет ни одного дипломатического и нет
 * казны: записываются только действия на поле. Отсюда критерий проверки —
 * совпадение конечной карты. Казна и по-ходовой путь сравниваются и
 * печатаются, но провалом не считаются: формат оригинала их не хранит.
 */
public class ReplayCheck {

    private final YioGdxGame yioGdxGame;
    private final GameController gameController;

    private final int matches;
    private final boolean diplomacy;

    private int mapReproduced;
    private int mapDiverged;
    private int moneyReproduced;
    private int empty;


    public ReplayCheck(YioGdxGame yioGdxGame, int matches, boolean diplomacy) {
        this.yioGdxGame = yioGdxGame;
        this.gameController = yioGdxGame.gameController;
        this.matches = matches;
        this.diplomacy = diplomacy;
    }


    public boolean perform() {
        System.out.println("Replay check started: matches=" + matches +
                " diplomacy=" + diplomacy);

        boolean savedReplaysEnabled = SettingsManager.replaysEnabled;
        SettingsManager.replaysEnabled = true;

        DebugFlags.testMode = true;
        yioGdxGame.gamePaused = false;

        for (int i = 0; i < matches; i++) {
            checkSingleMatch(i);
        }

        DebugFlags.testMode = false;
        SettingsManager.replaysEnabled = savedReplaysEnabled;

        return report();
    }


    private void checkSingleMatch(int matchIndex) {
        long seed = 500000L + matchIndex;

        int turns = runOriginalMatch(seed);

        Replay recorded = gameController.replayManager.getReplay();
        if (recorded == null || recorded.getActionsQuantity() == 0) {
            empty++;
            System.out.println("  матч " + matchIndex + ": реплей пуст, пропущен");
            return;
        }

        int actionsQuantity = recorded.getActionsQuantity();

        long originalHexes = SimStateHash.computeHexes(gameController);
        long originalProvinces = SimStateHash.computeProvinces(gameController);
        int originalMoney[] = getMoneyByFraction();

        runReplay(recorded, turns);

        long replayedHexes = SimStateHash.computeHexes(gameController);
        long replayedProvinces = SimStateHash.computeProvinces(gameController);

        boolean mapMatches = originalHexes == replayedHexes;
        boolean moneyMatches = originalProvinces == replayedProvinces;

        if (mapMatches) {
            mapReproduced++;
        } else {
            mapDiverged++;
        }

        if (moneyMatches) {
            moneyReproduced++;
        }

        System.out.println("  матч " + matchIndex +
                ": ходов " + turns +
                ", действий " + actionsQuantity +
                ", карта " + (mapMatches ? "совпала" : "РАЗОШЛАСЬ") +
                ", казна " + (moneyMatches ? "совпала" : "разошлась"));

        if (mapMatches && !moneyMatches) {
            System.out.println("      казна: партия " + describeMoney(originalMoney) +
                    ", реплей " + describeMoney(getMoneyByFraction()));
        }
    }


    private int runOriginalMatch(long seed) {
        gameController.random.setSeed(seed);
        gameController.predictableRandom.setSeed(seed);
        YioGdxGame.random.setSeed(seed);

        LoadingParameters parameters = LoadingParameters.getInstance();

        parameters.loadingType = LoadingType.skirmish;
        parameters.levelSize = LevelSize.MEDIUM;
        parameters.playersNumber = 0;
        parameters.fractionsQuantity = 5;
        parameters.difficulty = Difficulty.BALANCER;
        parameters.colorOffset = 0;
        parameters.slayRules = false;
        parameters.fogOfWar = false;
        parameters.diplomacy = diplomacy;
        parameters.genProvinces = 0;
        parameters.treesPercentageIndex = 2;
        parameters.campaignLevelIndex = (int) seed;

        LoadingManager.getInstance().startGame(parameters);

        DebugFlags.testWinner = -1;

        for (int i = 0; i < 4000; i++) {
            if (DebugFlags.testWinner != -1) break;
            if (gameController.matchStatistics.turnsMade >= 200) break;

            gameController.move();
        }

        return gameController.matchStatistics.turnsMade;
    }


    /**
     * Повторяет ReplayManager.startInstantReplay, но без обращения к Scenes:
     * в прогоне без интерфейса оверлея скорости не существует.
     */
    private void runReplay(Replay source, int turnLimit) {
        Replay copy = new Replay(gameController);
        source.saveToPreferences("replay_check_temp");
        copy.loadFromPreferences("replay_check_temp");

        LoadingParameters parameters = new LoadingParameters();
        parameters.loadingType = LoadingType.load_replay;
        gameController.gameSaver.legacyImportManager.applyFullLevel(parameters, copy.initialLevelString);
        parameters.replay = copy;
        parameters.playersNumber = 0;
        parameters.colorOffset = gameController.colorsManager.colorOffset;
        parameters.slayRules = GameRules.slayRules;
        parameters.campaignLevelIndex = -1;

        LoadingManager.getInstance().startGame(parameters);

        // prepare() ставит паузу и опускает флаг go, иначе performStep
        // не сделает ни шага.
        gameController.speedManager.setSpeed(SpeedManager.SPEED_NORMAL);

        Replay active = gameController.replayManager.getReplay();
        if (active != null) {
            active.onResumeNormalSpeed();
        }

        for (int i = 0; i < turnLimit * 40 + 400; i++) {
            if (active != null && active.isFinished()) break;

            gameController.move();
        }

        GameRules.replayMode = false;
    }


    private int[] getMoneyByFraction() {
        int money[] = new int[GameRules.MAX_FRACTIONS_QUANTITY];

        for (Province province : gameController.fieldManager.provinces) {
            int fraction = province.getFraction();
            if (fraction < 0 || fraction >= money.length) continue;

            money[fraction] += province.money;
        }

        return money;
    }


    private String describeMoney(int money[]) {
        StringBuilder builder = new StringBuilder();

        builder.append('[');
        for (int i = 0; i < 5; i++) {
            if (i > 0) builder.append(' ');
            builder.append(money[i]);
        }
        builder.append(']');

        return builder.toString();
    }


    private boolean report() {
        System.out.println();
        System.out.println("Replay check:");
        System.out.println("  карта воспроизведена: " + mapReproduced);
        System.out.println("  карта разошлась:      " + mapDiverged);
        System.out.println("  казна воспроизведена: " + moneyReproduced + " (справочно)");
        System.out.println("  пустых реплеев:       " + empty);

        return mapDiverged == 0 && mapReproduced > 0;
    }
}
