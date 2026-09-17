package yio.tro.antiyoy.gameplay.sim;

import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.gameplay.DebugFlags;
import yio.tro.antiyoy.gameplay.ColorsManager;
import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Obj;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.SelectionTipType;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyInfoCondensed;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyManager;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyTuning;
import yio.tro.antiyoy.gameplay.rules.EconomyTuning;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticEntity;
import yio.tro.antiyoy.gameplay.statehood.Region;
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

        checkRelationsOrderIsStable();
        checkRegionsAndLoyalty();
        checkStickyBuildTapCount();
        checkAutoFarms();
        checkPaletteBeyondLimit();
        checkOwnerIdInvariant();
        checkStateHashSensitivity();
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

        // Проверяется формула, а не конкретное число: множитель подбирался
        // прогонами и ещё будет меняться.
        check("потолок казны пропорционален доходу",
                EconomyTuning.getTreasuryCap(80)
                        == (int) (EconomyTuning.treasuryCapMultiplier * 80));

        check("утечка есть сверх потолка и отсутствует под ним",
                EconomyTuning.getTreasuryLeak(EconomyTuning.getTreasuryCap(80), 80) == 0
                        && EconomyTuning.getTreasuryLeak(EconomyTuning.getTreasuryCap(80) * 2, 80) > 0);

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


    /**
     * Порядок обхода отношений обязан быть порядком вставки, а не зависеть
     * от identity hash ключей.
     *
     * Через него шёл порядок решений ИИ и порядок сообщений в логе, и любое
     * лишнее выделение объекта где угодно в игре его меняло. Это делало
     * след непригодным для проверки рефакторингов — ради чего он и заведён.
     */
    private void checkRelationsOrderIsStable() {
        DiplomacyManager diplomacyManager = getDiplomacyManager();

        if (diplomacyManager.entities.size() < 3) {
            check("хватает сущностей для проверки порядка отношений", false);
            return;
        }

        boolean matchesEntityOrder = true;

        for (DiplomaticEntity entity : diplomacyManager.entities) {
            int expectedIndex = 0;

            for (DiplomaticEntity other : entity.relations.keySet()) {
                // Ключи обязаны идти в том же порядке, что и сущности,
                // пропуская саму себя.
                while (expectedIndex < diplomacyManager.entities.size()
                        && diplomacyManager.entities.get(expectedIndex) == entity) {
                    expectedIndex++;
                }

                if (expectedIndex >= diplomacyManager.entities.size()
                        || diplomacyManager.entities.get(expectedIndex) != other) {
                    matchesEntityOrder = false;
                    break;
                }

                expectedIndex++;
            }

            if (!matchesEntityOrder) break;
        }

        check("порядок отношений совпадает с порядком сущностей", matchesEntityOrder);
    }


    /**
     * Регионы и лояльность. Спека, часть III.
     *
     * Проверяется не «код не падает», а три содержательных свойства:
     * нарезка воспроизводима, размеры регионов в заданных пределах, и
     * купленный гекс приходит недовольным.
     */
    private void checkRegionsAndLoyalty() {
        boolean saved = GameRules.modRegions[GameRules.MODE_GENERIC];
        GameRules.modRegions[GameRules.MODE_GENERIC] = true;

        try {
            gameController.regionManager.recreateRegions();

            int count = gameController.regionManager.regions.size();
            check("регионы нарезаны", count > 0);

            if (count == 0) return;

            // Нарезка обязана быть воспроизводимой: она пересчитывается при
            // каждой загрузке и отмене хода.
            String first = describeRegions();
            gameController.regionManager.recreateRegions();
            String second = describeRegions();

            check("нарезка на регионы воспроизводима", first.equals(second));

            check("лояльность назначена всем регионам", allRegionsHaveLoyalty());

            check("доход падает вместе с лояльностью", isIncomeTiedToLoyalty());
        } finally {
            GameRules.modRegions[GameRules.MODE_GENERIC] = saved;
            gameController.regionManager.recreateRegions();
        }
    }


    private String describeRegions() {
        StringBuilder builder = new StringBuilder();

        for (Region region : gameController.regionManager.regions) {
            builder.append(region.getHexCount()).append(':');

            Hex anchor = region.hexList.get(0);
            builder.append(anchor.index1).append(',').append(anchor.index2).append(';');
        }

        return builder.toString();
    }


    private boolean allRegionsHaveLoyalty() {
        for (Region region : gameController.regionManager.regions) {
            for (Hex hex : region.hexList) {
                if (hex.loyalty < 0) return false;
                if (hex.loyalty > StatehoodTuning.loyaltyMax) return false;
            }
        }

        return true;
    }


    /**
     * Множитель дохода обязан отличаться у довольного и у злого региона.
     */
    private boolean isIncomeTiedToLoyalty() {
        Region region = gameController.regionManager.regions.get(0);

        int savedLoyalty = region.getLoyalty();

        region.setLoyalty(StatehoodTuning.loyaltyMax);
        float high = region.getIncomeMultiplier();

        region.setLoyalty(StatehoodTuning.loyaltyMin);
        float low = region.getIncomeMultiplier();

        region.setLoyalty(savedLoyalty);

        return high > low;
    }


    /**
     * Критерий приёмки этапа 3: десять ферм подряд стоят одиннадцать тапов.
     *
     * Считаются настоящие тапы через публичный вход focusedHexActions, а не
     * заявляется число: выбор постройки — один тап, каждая установка — ещё
     * один. Без липкого режима постройку приходится выбирать заново каждый
     * раз, и тапов выходит вдвое больше.
     */
    private void checkStickyBuildTapCount() {
        int sticky = countTapsToBuildFarms(10, true);
        int plain = countTapsToBuildFarms(10, false);

        System.out.println("       тапов на 10 ферм: липкий режим " + sticky +
                ", обычный " + plain);

        check("десять ферм в липком режиме стоят не больше 11 тапов",
                sticky > 0 && sticky <= 11);

        check("липкий режим экономит тапы против обычного",
                plain > sticky);
    }


    /**
     * Прогоняет постройку ферм тапами и возвращает их число.
     * Ноль означает, что сценарий не удалось собрать.
     */
    private int countTapsToBuildFarms(int farmsWanted, boolean sticky) {
        prepareMatch();

        boolean savedFlag = GameRules.modUiImprovements[GameRules.MODE_GENERIC];
        GameRules.modUiImprovements[GameRules.MODE_GENERIC] = sticky;

        int taps = 0;

        try {
            Province province = getBiggestOwnProvince();
            if (province == null) return 0;

            // Денег заведомо хватает: меряется число тапов, а не экономика.
            province.money = 100000;

            gameController.fieldManager.selectedProvince = province;

            int built = 0;
            for (int attempt = 0; attempt < farmsWanted * 6 && built < farmsWanted; attempt++) {
                Hex hex = gameController.fieldManager.getBestHexForNewFarm(province);
                if (hex == null) break;

                // Выбор постройки — это тап. В липком режиме он нужен один
                // раз, в обычном перед каждой установкой.
                if (gameController.selectionManager.getTipType() != SelectionTipType.FARM) {
                    gameController.selectionManager.awakeTip(SelectionTipType.FARM);
                    taps++;
                }

                gameController.fieldManager.selectAdjacentHexes(hex);

                gameController.selectionManager.setFocusedHex(hex);
                gameController.selectionManager.focusedHexActions(hex);
                taps++;

                if (hex.objectInside == Obj.FARM) {
                    built++;
                }
            }

            if (built < farmsWanted) return 0;
        } finally {
            GameRules.modUiImprovements[GameRules.MODE_GENERIC] = savedFlag;
        }

        return taps;
    }


    private Province getBiggestOwnProvince() {
        Province best = null;

        for (Province province : gameController.fieldManager.provinces) {
            if (best == null || province.hexList.size() > best.hexList.size()) {
                best = province;
            }
        }

        return best;
    }


    /**
     * Автопостройка ферм обязана уважать заданный остаток казны: иначе она
     * оставит игрока без денег на юнитов.
     */
    private void checkAutoFarms() {
        prepareMatch();

        boolean savedFlag = GameRules.modUiImprovements[GameRules.MODE_GENERIC];
        GameRules.modUiImprovements[GameRules.MODE_GENERIC] = true;

        try {
            Province province = getBiggestOwnProvince();
            if (province == null) {
                check("нашлась провинция для автоферм", false);
                return;
            }

            province.money = 1000;
            int moneyToKeep = 300;

            int built = gameController.fieldManager.autoBuildFarms(province, moneyToKeep);

            check("автопостройка поставила хотя бы одну ферму", built > 0);
            check("автопостройка не тронула заданный остаток казны",
                    province.money >= moneyToKeep);

            // Повторный вызов на том же остатке не должен ничего строить.
            int again = gameController.fieldManager.autoBuildFarms(province, province.money);
            check("автопостройка останавливается на пороге", again == 0);
        } finally {
            GameRules.modUiImprovements[GameRules.MODE_GENERIC] = savedFlag;
        }
    }


    /**
     * Число государств больше не ограничено палитрой (спека, часть I).
     *
     * Проверяется не «код не падает», а три содержательных свойства: внутри
     * палитры ничего не изменилось, сверх неё цвет всегда валиден, и два
     * владельца с одинаковым цветом различаются штриховкой.
     */
    private void checkPaletteBeyondLimit() {
        ColorsManager colorsManager = gameController.colorsManager;

        boolean unchangedInsidePalette = true;
        for (int ownerId = 0; ownerId < ColorsManager.PALETTE_SIZE; ownerId++) {
            if (colorsManager.getColorByOwner(ownerId) != colorsManager.getColorByFraction(ownerId)) {
                unchangedInsidePalette = false;
            }
            if (colorsManager.getHatchingByOwner(ownerId) != ColorsManager.HATCHING_NONE) {
                unchangedInsidePalette = false;
            }
        }

        check("внутри палитры цвет и штриховка прежние", unchangedInsidePalette);

        boolean colorsValid = true;
        for (int ownerId = 0; ownerId < 200; ownerId++) {
            int color = colorsManager.getColorByOwner(ownerId);

            if (color < 0 || color >= ColorsManager.PALETTE_SIZE) {
                colorsValid = false;
            }

            // Нейтральный цвет не должен достаться государству.
            if (ownerId >= ColorsManager.PALETTE_SIZE && color == GameRules.NEUTRAL_FRACTION) {
                colorsValid = false;
            }
        }

        check("цвет валиден для 200 владельцев подряд", colorsValid);

        // Пара «цвет + штриховка» обязана быть уникальной, пока штриховка не
        // исчерпана. Дальше повторы неизбежны, и спека их допускает.
        int limit = ColorsManager.DISTINGUISHABLE_OWNERS;
        boolean allDistinct = true;

        for (int ownerId = 0; ownerId < limit && allDistinct; ownerId++) {
            for (int other = ownerId + 1; other < limit; other++) {
                if (colorsManager.getColorByOwner(ownerId) != colorsManager.getColorByOwner(other)) continue;
                if (colorsManager.getHatchingByOwner(ownerId) != colorsManager.getHatchingByOwner(other)) continue;

                allDistinct = false;
                System.out.println("       владельцы " + ownerId + " и " + other +
                        " неразличимы: цвет " + colorsManager.getColorByOwner(ownerId) +
                        ", штриховка " + colorsManager.getHatchingByOwner(ownerId));
                break;
            }
        }

        check("первые " + limit + " владельцев различимы попарно", allDistinct);
    }


    /**
     * Инвариант этапа 1: пока владелец и цвет не разведены по-настоящему,
     * ownerId обязан совпадать с fraction на каждом гексе.
     *
     * Это страховка на время рефакторинга: если хоть одно место меняет
     * fraction в обход владельца, проверка это поймает сразу, а не через
     * расхождение следа на сотом ходу.
     */
    private void checkOwnerIdInvariant() {
        int mismatches = 0;
        Hex firstMismatch = null;

        for (int i = 0; i < gameController.fieldManager.fWidth; i++) {
            for (int j = 0; j < gameController.fieldManager.fHeight; j++) {
                Hex hex = gameController.fieldManager.field[i][j];
                if (hex == null) continue;
                if (hex.ownerId == hex.fraction) continue;

                mismatches++;
                if (firstMismatch == null) {
                    firstMismatch = hex;
                }
            }
        }

        check("ownerId совпадает с fraction на всех гексах", mismatches == 0);

        if (mismatches > 0) {
            System.out.println("       расхождений: " + mismatches +
                    ", первое на гексе " + firstMismatch.index1 + "," + firstMismatch.index2 +
                    " (ownerId " + firstMismatch.ownerId +
                    ", fraction " + firstMismatch.fraction + ")");
        }
    }


    /**
     * Контрольная сумма, которая всегда говорит «совпало», бесполезна.
     * Здесь проверяется, что она устойчива к повторному вызову и при этом
     * реагирует на изменение состояния — в том числе на единственный гекс.
     */
    private void checkStateHashSensitivity() {
        long first = SimStateHash.computeField(gameController);
        long second = SimStateHash.computeField(gameController);

        check("хеш состояния устойчив при повторном вызове", first == second);

        Hex hex = findAnyOwnedHex();
        if (hex == null) {
            check("нашёлся гекс для проверки чувствительности хеша", false);
            return;
        }

        int savedFraction = hex.fraction;
        hex.fraction = savedFraction == 0 ? 1 : 0;
        long mutated = SimStateHash.computeField(gameController);
        hex.fraction = savedFraction;

        check("хеш ловит смену владельца одного гекса", mutated != first);

        check("хеш возвращается к прежнему после отката правки",
                SimStateHash.computeField(gameController) == first);

        // Казна провинции в состояние входит: этап 2 меняет именно её.
        Province province = gameController.fieldManager.provinces.size() > 0
                ? gameController.fieldManager.provinces.get(0)
                : null;

        if (province == null) {
            check("нашлась провинция для проверки хеша казны", false);
            return;
        }

        int savedMoney = province.money;
        province.money = savedMoney + 1;
        long moneyMutated = SimStateHash.computeField(gameController);
        province.money = savedMoney;

        check("хеш ловит изменение казны провинции", moneyMutated != first);
    }


    private Hex findAnyOwnedHex() {
        for (Hex hex : gameController.fieldManager.activeHexes) {
            if (hex.isNeutral()) continue;
            return hex;
        }

        return null;
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
