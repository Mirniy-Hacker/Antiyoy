package yio.tro.antiyoy.gameplay.rules;

public class GameRules {

    public static final int MAX_FRACTIONS_QUANTITY = 11;
    public static final int NEUTRAL_FRACTION = 7;
    public static final int UNIT_MOVE_LIMIT = 4;

    public static final int PRICE_UNIT = 10;
    public static final int PRICE_TOWER = 15;
    public static final int PRICE_FARM = 12;
    public static final int PRICE_STRONG_TOWER = 35;

    public static final int PRICE_TREE = 10;
    public static final int FARM_INCOME = 4;
    public static final int TREE_CUT_REWARD = 3;

    public static final int TAX_TOWER = 1;
    public static final int TAX_STRONG_TOWER = 6;
    public static final int TAX_UNIT_GENERIC_1 = 2;
    public static final int TAX_UNIT_GENERIC_2 = 6;
    public static final int TAX_UNIT_GENERIC_3 = 18;
    public static final int TAX_UNIT_GENERIC_4 = 36;

    public static int fractionsQuantity = 5;
    public static boolean slayRules = false;
    public static boolean tutorialMode;
    public static boolean campaignMode;
    public static boolean inEditorMode;
    public static int difficulty;
    public static boolean aiOnlyMode;
    public static boolean replayMode;
    public static int editorChosenColor;
    public static boolean fogOfWarEnabled;
    public static boolean diplomacyEnabled;
    public static boolean userLevelMode;
    public static String ulKey;
    public static boolean editorFog;
    public static boolean editorDiplomacy;
    public static boolean editorColorFixApplied;
    public static int editorSlotNumber;
    public static int genProvinces;
    public static double treesSpawnChance;
    public static boolean diplomaticRelationsLocked;


    /**
     * Верхняя граница идентификаторов владельцев: размер массивов, которые
     * индексируются ownerId.
     *
     * Растёт при отделении государств. Отдельно от fractionsQuantity
     * сознательно: та задаёт число игроков в начале партии и в её смысле
     * не меняется, а владельцев по ходу партии становится больше.
     */
    public static int ownerLimit = MAX_FRACTIONS_QUANTITY;


    public static void ensureOwnerLimit(int ownerId) {
        if (ownerId < ownerLimit) return;

        ownerLimit = ownerId + 1;
    }


    public static void defaultValues() {
        ownerLimit = MAX_FRACTIONS_QUANTITY;
        tutorialMode = false;
        campaignMode = false;
        inEditorMode = false;
        aiOnlyMode = false;
        replayMode = false;
        fogOfWarEnabled = false;
        diplomacyEnabled = false;
        userLevelMode = false;
        editorChosenColor = 1;
        ulKey = null;
        editorFog = false;
        editorDiplomacy = false;
        editorColorFixApplied = false;
        diplomaticRelationsLocked = false;
        editorSlotNumber = -1;
        genProvinces = 0;
        treesSpawnChance = 0.1;
    }


    public static void setFractionsQuantity(int fractionsQuantity) {
        if (fractionsQuantity < 0) {
            fractionsQuantity = 0;
        }

        GameRules.fractionsQuantity = fractionsQuantity;
    }


    public static void setDifficulty(int difficulty) {
        GameRules.difficulty = difficulty;
    }


    public static void setSlayRules(boolean slay_rules) {
        GameRules.slayRules = slay_rules;
    }


    public static void setEditorChosenColor(int editorChosenColor) {
        GameRules.editorChosenColor = editorChosenColor;
    }


    public static void setFogOfWarEnabled(boolean fogOfWarEnabled) {
        GameRules.fogOfWarEnabled = fogOfWarEnabled;
    }


    public static void setDiplomacyEnabled(boolean diplomacyEnabled) {
        GameRules.diplomacyEnabled = diplomacyEnabled;
    }


    public static void setDiplomaticRelationsLocked(boolean diplomaticRelationsLocked) {
        GameRules.diplomaticRelationsLocked = diplomaticRelationsLocked;
    }


    // ==================================================================
    // Флаги мода. Спека, часть X: каждая механика включается отдельным
    // флагом, раздельно для обычного режима и slay. Старое поведение
    // остаётся доступным для сравнения, поэтому по умолчанию всё выключено.
    //
    // Флаги сознательно не сбрасываются в defaultValues(): это конфигурация
    // прогона, а не состояние матча, и загрузка уровня их не трогает.
    // Сохраняются отдельно, через GameSaver.
    // ==================================================================

    public static final int MODE_GENERIC = 0;
    public static final int MODE_SLAY = 1;
    public static final int MODES_QUANTITY = 2;

    /** Этап 2: потолок казны и утечка излишка. */
    public static boolean modTreasuryCap[] = new boolean[MODES_QUANTITY];

    /** Этап 2: удорожание найма внутри хода. */
    public static boolean modUnitPriceGrowth[] = new boolean[MODES_QUANTITY];

    /** Этап 4: нарезка на регионы и лояльность. */
    public static boolean modRegions[] = new boolean[MODES_QUANTITY];

    /** Этап 5: волнения, отделение, новые государства. */
    public static boolean modSecession[] = new boolean[MODES_QUANTITY];

    /** Этап 6: мнение, доверие, репутация, пороги вместо рандома. */
    public static boolean modOpinion[] = new boolean[MODES_QUANTITY];

    /** Этап 7: осмысленные сделки по земле. */
    public static boolean modLandDeals[] = new boolean[MODES_QUANTITY];

    /** Этап 8: характеры. */
    public static boolean modPersonalities[] = new boolean[MODES_QUANTITY];

    /** Этап 9: новые дипломатические действия. */
    public static boolean modDiplomaticActions[] = new boolean[MODES_QUANTITY];

    /** Этап 10: координация ИИ в коалиции. */
    public static boolean modAiCoordination[] = new boolean[MODES_QUANTITY];

    /** Этап 3: липкий режим постройки, долгий тап, автофермы. */
    public static boolean modUiImprovements[] = new boolean[MODES_QUANTITY];

    /** Ключи сохранения, в том же порядке, что и getAllModFlags(). */
    public static final String MOD_FLAG_KEYS[] = {
            "mod_treasury_cap",
            "mod_unit_price_growth",
            "mod_regions",
            "mod_secession",
            "mod_opinion",
            "mod_land_deals",
            "mod_personalities",
            "mod_diplomatic_actions",
            "mod_ai_coordination",
            "mod_ui_improvements",
    };


    /**
     * Механики, за которыми уже есть реализация.
     *
     * Остальные ключи заведены заранее: они попадают в сейв и
     * принимаются ключом --mod, но пока ничего не меняют, и в меню
     * им не место — переключатель без последствий хуже его отсутствия.
     *
     * Отделение реализовано, но скрыто намеренно: отделившиеся
     * государства не получают хода и ломают определение победителя.
     * Ход партии перебирает фракции, а не живых владельцев.
     *
     * Порядок обязан совпадать с MOD_FLAG_KEYS.
     */
    public static final boolean MOD_FLAG_READY[] = {
            true,   // mod_treasury_cap
            true,   // mod_unit_price_growth
            true,   // mod_regions
            false,  // mod_secession — ломает определение победителя
            false,  // mod_opinion — этап 6
            false,  // mod_land_deals — этап 7
            false,  // mod_personalities — этап 8
            false,  // mod_diplomatic_actions — этап 9
            false,  // mod_ai_coordination — этап 10
            true,   // mod_ui_improvements
    };


    public static int getCurrentModeIndex() {
        return slayRules ? MODE_SLAY : MODE_GENERIC;
    }


    private static boolean isEnabled(boolean flags[]) {
        return flags[getCurrentModeIndex()];
    }


    public static boolean isTreasuryCapEnabled() {
        return isEnabled(modTreasuryCap);
    }


    public static boolean isUnitPriceGrowthEnabled() {
        return isEnabled(modUnitPriceGrowth);
    }


    public static boolean areRegionsEnabled() {
        return isEnabled(modRegions);
    }


    public static boolean isSecessionEnabled() {
        return isEnabled(modSecession);
    }


    public static boolean isOpinionEnabled() {
        return isEnabled(modOpinion);
    }


    public static boolean areLandDealsEnabled() {
        return isEnabled(modLandDeals);
    }


    public static boolean arePersonalitiesEnabled() {
        return isEnabled(modPersonalities);
    }


    public static boolean areDiplomaticActionsEnabled() {
        return isEnabled(modDiplomaticActions);
    }


    public static boolean isAiCoordinationEnabled() {
        return isEnabled(modAiCoordination);
    }


    public static boolean areUiImprovementsEnabled() {
        return isEnabled(modUiImprovements);
    }


    /**
     * Порядок обязан совпадать с MOD_FLAG_KEYS: по нему идёт сохранение.
     */
    public static boolean[][] getAllModFlags() {
        return new boolean[][]{
                modTreasuryCap,
                modUnitPriceGrowth,
                modRegions,
                modSecession,
                modOpinion,
                modLandDeals,
                modPersonalities,
                modDiplomaticActions,
                modAiCoordination,
                modUiImprovements,
        };
    }


    /**
     * Все механики мода разом, для обоих режимов. Нужно прогонам автоматчей
     * и отладке.
     */
    public static void setAllModFlags(boolean value) {
        for (boolean flags[] : getAllModFlags()) {
            flags[MODE_GENERIC] = value;
            flags[MODE_SLAY] = value;
        }
    }


    public static void defaultModFlags() {
        setAllModFlags(false);
    }
}
