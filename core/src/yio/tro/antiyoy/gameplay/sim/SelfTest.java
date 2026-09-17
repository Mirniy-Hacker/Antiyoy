package yio.tro.antiyoy.gameplay.sim;

import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.gameplay.DebugFlags;
import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyInfoCondensed;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyManager;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyTuning;
import yio.tro.antiyoy.gameplay.rules.EconomyTuning;
import yio.tro.antiyoy.gameplay.statehood.StatehoodTuning;
import yio.tro.antiyoy.gameplay.loading.LoadingManager;
import yio.tro.antiyoy.gameplay.loading.LoadingParameters;
import yio.tro.antiyoy.gameplay.loading.LoadingType;
import yio.tro.antiyoy.gameplay.rules.GameRules;

/**
 * Самопроверка сериализации и флагов. Закрывает приёмку этапа 0 в части
 * «старые сохранения грузятся»: вместо утверждения — прогон.
 */
public class SelfTest {

    private final YioGdxGame yioGdxGame;
    private final GameController gameController;

    private int passed;
    private int failed;


    public SelfTest(YioGdxGame yioGdxGame) {
        this.yioGdxGame = yioGdxGame;
        this.gameController = yioGdxGame.gameController;
    }


    public boolean perform() {
        System.out.println("Self test started");

        DebugFlags.testMode = true;
        yioGdxGame.gamePaused = false;

        checkTuningInitialized();
        checkModFlagDefaults();
        checkModFlagModeSeparation();

        prepareMatch();

        checkVersionPrefix();
        checkRoundTrip();
        checkLegacySaveLoads();
        checkRestoreDoesNotInflateStatistics();

        DebugFlags.testMode = false;

        return report();
    }


    // ------------------------------------------------------------------
    // Флаги
    // ------------------------------------------------------------------

    /**
     * Классы Tuning инициализируются статическим блоком: явного места вызова
     * defaultValues() в движке нет, и без блока все числа были бы нулями.
     */
    private void checkTuningInitialized() {
        check("EconomyTuning инициализирован",
                EconomyTuning.treasuryCapMultiplier > 0
                        && EconomyTuning.treasuryLeakPerTurn > 0
                        && EconomyTuning.unitPriceGrowthPerUnit > 0);

        check("DiplomacyTuning инициализирован",
                DiplomacyTuning.trustStart > 0
                        && DiplomacyTuning.reputationStart > 0
                        && DiplomacyTuning.presets != null
                        && DiplomacyTuning.presets.length == DiplomacyTuning.PRESETS_QUANTITY);

        check("у каждого пресета характера шесть черт в пределах 0..1",
                arePresetsValid());

        check("StatehoodTuning инициализирован",
                StatehoodTuning.loyaltyStart > 0
                        && StatehoodTuning.priorityWeights != null
                        && StatehoodTuning.policyDotationShares != null
                        && StatehoodTuning.policyDotationShares.length == StatehoodTuning.POLICIES_QUANTITY);

        // Проверка формул, а не только присвоений.
        check("потолок казны считается по спеке: 20 * income",
                EconomyTuning.getTreasuryCap(80) == 1600);

        check("двенадцатый юнит примерно впятеро дороже первого",
                isTwelfthUnitAboutFiveTimes());

        check("множитель дохода от лояльности укладывается в 0.4..1.0",
                StatehoodTuning.getIncomeMultiplier(0) == 0.4f
                        && StatehoodTuning.getIncomeMultiplier(100) == 1.0f);
    }


    private boolean arePresetsValid() {
        for (float preset[] : DiplomacyTuning.presets) {
            if (preset == null) return false;
            if (preset.length != 6) return false;

            for (float trait : preset) {
                if (trait < 0f || trait > 1f) return false;
            }
        }

        return true;
    }


    private boolean isTwelfthUnitAboutFiveTimes() {
        int base = 10;
        int twelfth = EconomyTuning.getEscalatedUnitPrice(base, 11);
        float ratio = twelfth / (float) base;

        return ratio > 4.5f && ratio < 5.5f;
    }


    private void checkModFlagDefaults() {
        GameRules.defaultModFlags();

        boolean allOff = true;
        for (boolean flags[] : GameRules.getAllModFlags()) {
            if (flags[GameRules.MODE_GENERIC]) allOff = false;
            if (flags[GameRules.MODE_SLAY]) allOff = false;
        }

        check("флаги мода по умолчанию выключены", allOff);

        check("число ключей совпадает с числом флагов",
                GameRules.MOD_FLAG_KEYS.length == GameRules.getAllModFlags().length);
    }


    private void checkModFlagModeSeparation() {
        GameRules.defaultModFlags();

        GameRules.modTreasuryCap[GameRules.MODE_GENERIC] = true;
        GameRules.modTreasuryCap[GameRules.MODE_SLAY] = false;

        boolean savedSlayRules = GameRules.slayRules;

        GameRules.slayRules = false;
        boolean onInGeneric = GameRules.isTreasuryCapEnabled();

        GameRules.slayRules = true;
        boolean onInSlay = GameRules.isTreasuryCapEnabled();

        GameRules.slayRules = savedSlayRules;
        GameRules.defaultModFlags();

        check("флаг различает обычный режим и slay", onInGeneric && !onInSlay);
    }


    // ------------------------------------------------------------------
    // Сериализация дипломатии
    // ------------------------------------------------------------------

    private void prepareMatch() {
        LoadingParameters parameters = LoadingParameters.getInstance();

        parameters.loadingType = LoadingType.skirmish;
        parameters.levelSize = yio.tro.antiyoy.gameplay.LevelSize.MEDIUM;
        parameters.playersNumber = 0;
        parameters.fractionsQuantity = 5;
        parameters.difficulty = yio.tro.antiyoy.ai.Difficulty.BALANCER;
        parameters.colorOffset = 0;
        parameters.slayRules = false;
        parameters.fogOfWar = false;
        parameters.diplomacy = true;
        parameters.genProvinces = 0;
        parameters.treesPercentageIndex = 2;
        parameters.campaignLevelIndex = 7;

        gameController.random.setSeed(7);
        gameController.predictableRandom.setSeed(7);
        YioGdxGame.random.setSeed(7);

        LoadingManager.getInstance().startGame(parameters);

        // Несколько ходов, чтобы появились реальные отношения и контракты:
        // проверять сериализацию на пустом состоянии бессмысленно.
        DebugFlags.testWinner = -1;
        for (int i = 0; i < 400 && DebugFlags.testWinner == -1; i++) {
            gameController.move();
            if (gameController.matchStatistics.turnsMade >= 25) break;
        }

        check("в матче появились контракты",
                gameController.fieldManager.diplomacyManager.contracts.size() > 0);
    }


    private DiplomacyManager getDiplomacyManager() {
        return gameController.fieldManager.diplomacyManager;
    }


    private String encodeCurrent() {
        DiplomacyInfoCondensed instance = DiplomacyInfoCondensed.getInstance();
        instance.update(getDiplomacyManager());

        return instance.getFull();
    }


    private void checkVersionPrefix() {
        String encoded = encodeCurrent();

        check("строка дипломатии пишется с префиксом v2/",
                encoded != null && encoded.startsWith(DiplomacyInfoCondensed.VERSION_PREFIX));
    }


    private void checkRoundTrip() {
        String original = encodeCurrent();

        DiplomacyInfoCondensed instance = DiplomacyInfoCondensed.getInstance();
        instance.setFull(original);
        instance.apply(getDiplomacyManager());

        check("прочитанная строка опознана как v2",
                instance.getLoadedVersion() == DiplomacyInfoCondensed.VERSION_CURRENT);

        String reencoded = encodeCurrent();

        boolean identical = original.equals(reencoded);
        check("v2 переживает полный цикл запись-чтение-запись", identical);

        if (!identical) {
            showDifference(original, reencoded);
        }
    }


    /**
     * Показывает, какая именно секция строки разъехалась.
     */
    private void showDifference(String expected, String actual) {
        String expectedSections[] = expected.split("#");
        String actualSections[] = actual.split("#");

        System.out.println("       секций было " + expectedSections.length +
                ", стало " + actualSections.length);

        int count = Math.min(expectedSections.length, actualSections.length);
        for (int i = 0; i < count; i++) {
            if (expectedSections[i].equals(actualSections[i])) continue;

            System.out.println("       секция " + i + " разошлась:");
            System.out.println("         было:  " + shorten(expectedSections[i]));
            System.out.println("         стало: " + shorten(actualSections[i]));
        }
    }


    private String shorten(String source) {
        if (source.length() <= 220) return source;

        return source.substring(0, 220) + "...(" + source.length() + ")";
    }


    /**
     * Главная проверка приёмки: строка без префикса — это в точности формат
     * сейвов оригинальной игры.
     */
    private void checkLegacySaveLoads() {
        String v2 = encodeCurrent();
        String legacy = v2.substring(DiplomacyInfoCondensed.VERSION_PREFIX.length());

        DiplomacyInfoCondensed instance = DiplomacyInfoCondensed.getInstance();
        instance.setFull(legacy);

        boolean loaded;
        try {
            instance.apply(getDiplomacyManager());
            loaded = true;
        } catch (Exception exception) {
            loaded = false;
            System.out.println("    исключение: " + exception);
        }

        check("сейв без префикса грузится без исключения", loaded);

        check("сейв без префикса опознан как legacy",
                instance.getLoadedVersion() == DiplomacyInfoCondensed.VERSION_LEGACY);

        check("состояние из legacy-сейва совпадает с исходным",
                encodeCurrent().equals(v2));
    }


    /**
     * Восстановление не должно накручивать счётчики: setRelation и
     * addContract вызываются заново, а статистика в LevelSnapshot
     * восстанавливается раньше дипломатии.
     */
    private void checkRestoreDoesNotInflateStatistics() {
        int warsBefore = gameController.matchStatistics.warsDeclared;
        int contractsBefore = gameController.matchStatistics.contractsSigned;

        String encoded = encodeCurrent();

        DiplomacyInfoCondensed instance = DiplomacyInfoCondensed.getInstance();
        instance.setFull(encoded);
        instance.apply(getDiplomacyManager());

        check("восстановление не накручивает счётчик войн",
                gameController.matchStatistics.warsDeclared == warsBefore);

        check("восстановление не накручивает счётчик сделок",
                gameController.matchStatistics.contractsSigned == contractsBefore);
    }


    // ------------------------------------------------------------------

    private void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  OK   " + name);
        } else {
            failed++;
            System.out.println("  FAIL " + name);
        }
    }


    private boolean report() {
        System.out.println();
        System.out.println("Self test: " + passed + " passed, " + failed + " failed");

        return failed == 0;
    }
}
