package yio.tro.antiyoy.gameplay.diplomacy;

/**
 * Числа дипломатии. Спека, части IV, V, VI, VII, VIII.
 *
 * Значения не final: смысл класса в том, чтобы их можно было крутить на
 * прогонах автоматчей. Магических чисел в самой логике быть не должно
 * (CLAUDE.md, правило 4).
 */
public class DiplomacyTuning {

    // ------------------------------------------------------------------
    // 4.1 Мнение и доверие
    // ------------------------------------------------------------------

    public static int opinionMin;
    public static int opinionMax;
    public static int opinionStart;

    public static int trustMin;
    public static int trustMax;
    public static int trustStart;

    /** Общая граница, помноженная на превосходство в силе. Пересчёт каждый ход. */
    public static int modBorderPressureMax;

    /** Ты лидер по силе. Пересчёт каждый ход. */
    public static int modPowerLeaderMax;

    /** Воюешь с их другом. */
    public static int modWarWithFriend;
    public static float modWarWithFriendDecay;

    /** Воюешь с их врагом. */
    public static int modWarWithEnemy;
    public static float modWarWithEnemyDecay;

    /** Подарок деньгами: +1 за GIFT_MONEY_PER_POINT монет. */
    public static int modGiftMoneyPerPoint;
    public static int modGiftMax;
    public static float modGiftDecay;

    /** Контракт исполнен до конца. */
    public static int modContractFulfilledOpinion;
    public static int modContractFulfilledTrust;
    public static float modContractFulfilledDecay;

    /** Досрочный разрыв контракта. */
    public static int modContractBrokenOpinion;
    public static int modContractBrokenTrust;
    public static float modContractBrokenDecay;

    /** Атака после дружбы. Доверие обнуляется полностью. */
    public static int modBetrayalOpinion;
    public static float modBetrayalDecay;

    /** Мир длится N ходов. */
    public static float modLongPeacePerTurn;
    public static int modLongPeaceMax;

    /** Обида основания у отделившегося государства. Затухает вчетверо медленнее. */
    public static int modFoundingGrudge;
    public static float modFoundingGrudgeDecay;

    // ------------------------------------------------------------------
    // 4.2 Сила
    // ------------------------------------------------------------------

    /** power = hexCount + 2*income + sum(unitValue) + 3*provinceCount */
    public static float powerWeightHex;
    public static float powerWeightIncome;
    public static float powerWeightUnit;
    public static float powerWeightProvince;

    // ------------------------------------------------------------------
    // 4.3 Репутация
    // ------------------------------------------------------------------

    public static int reputationMin;
    public static int reputationMax;
    public static int reputationStart;

    public static int reputationContractBroken;
    public static int reputationBetrayal;

    /** Каждые N ходов без нарушений репутация растёт. */
    public static int reputationCleanTurnsStep;
    public static int reputationCleanTurnsBonus;

    /** Ниже этого порога — отказ в новых контрактах, кроме капитуляции и дани. */
    public static int reputationRefusalThreshold;

    // ------------------------------------------------------------------
    // 4.4 Пороги решений
    // ------------------------------------------------------------------

    /** принять дружбу: opinion > BASE + CAUTION*caution И trust > TRUST */
    public static float acceptFriendshipOpinionBase;
    public static float acceptFriendshipOpinionCaution;
    public static float acceptFriendshipTrust;

    /** объявить войну: opinion < BASE + AGGRESSION*aggression */
    public static float declareWarOpinionBase;
    public static float declareWarOpinionAggression;

    /** объявить войну: powerRatio > BASE + CAUTION*caution */
    public static float declareWarPowerRatioBase;
    public static float declareWarPowerRatioCaution;

    /** Пакт игнорируется вероломными: honesty ниже этого значения. */
    public static float declareWarPactIgnoreHonesty;

    /** предложить коалицию: есть цель с leaderGap > GAP И opinion(цель) < OPINION */
    public static float proposeCoalitionLeaderGap;
    public static float proposeCoalitionTargetOpinion;

    // ------------------------------------------------------------------
    // 5.1 Характеры
    // ------------------------------------------------------------------

    /** Разброс вокруг значений пресета. */
    public static float personalitySpread;

    public static final int PRESET_CONQUEROR = 0;
    public static final int PRESET_TRADER = 1;
    public static final int PRESET_FORTRESS = 2;
    public static final int PRESET_JACKAL = 3;
    public static final int PRESET_ALLY = 4;
    public static final int PRESET_PARANOID = 5;

    public static final int PRESETS_QUANTITY = 6;

    /**
     * Пресеты характеров. Спека называет типажи, но не задаёт чисел — она
     * прямо отсылает их состав сюда (5.1).
     *
     * Порядок черт: aggression, caution, honesty, vindictiveness, greed, expansion.
     */
    public static float presets[][];

    public static final String PRESET_NAMES[] = {
            "conqueror",
            "trader",
            "fortress",
            "jackal",
            "ally",
            "paranoid",
    };

    // ------------------------------------------------------------------
    // 6.1 Оценка земли
    // ------------------------------------------------------------------

    /** Горизонт окупаемости гекса в ходах. */
    public static int landValueIncomeHorizon;

    /** Разрыв провинции: LAND_SPLIT_INCOME_WEIGHT * income * landValueIncomeHorizon. */
    public static float landSplitIncomeWeight;

    // ------------------------------------------------------------------
    // 7 Действия
    // ------------------------------------------------------------------

    /** Ультиматум: срок в ходах, после которого война без штрафа. */
    public static int ultimatumDeadline;

    /** Пакт о ненападении: допустимые сроки. */
    public static int nonAggressionDurations[];

    /** Нарушение пакта: штраф жертве, кратный её доходу. */
    public static float pactBreachPaymentIncomeMultiplier;
    public static int pactBreachReputation;

    /** Разрыв дани свободен, но бьёт по репутации. */
    public static int tributeBreakReputation;

    /** Выход из коалиции до победы. */
    public static int coalitionLeaveReputation;

    /**
     * Участники коалиции не должны враждовать между собой: если opinion(A, B)
     * ниже этого значения, A не присоединится к войне с участием B.
     * Числового лимита на размер коалиции нет.
     */
    public static int coalitionMinMutualOpinion;

    /** Предупреждение перед войной обязательно только при honesty выше этого. */
    public static float warWarningHonestyThreshold;

    // ------------------------------------------------------------------
    // 8 Инициатива ИИ
    // ------------------------------------------------------------------

    /** ИИ шлёт предложения не чаще раза в N ходов на сущность. */
    public static int aiProposalCooldownTurns;


    public static void defaultValues() {
        opinionMin = -100;
        opinionMax = 100;
        opinionStart = 0;

        trustMin = 0;
        trustMax = 100;
        trustStart = 50;

        modBorderPressureMax = -25;
        modPowerLeaderMax = -35;

        modWarWithFriend = -20;
        modWarWithFriendDecay = 2f;

        modWarWithEnemy = 15;
        modWarWithEnemyDecay = 2f;

        modGiftMoneyPerPoint = 10;
        modGiftMax = 25;
        modGiftDecay = 1f;

        modContractFulfilledOpinion = 10;
        modContractFulfilledTrust = 8;
        modContractFulfilledDecay = 0.5f;

        modContractBrokenOpinion = -30;
        modContractBrokenTrust = -25;
        modContractBrokenDecay = 1f;

        modBetrayalOpinion = -60;
        modBetrayalDecay = 0.5f;

        modLongPeacePerTurn = 0.5f;
        modLongPeaceMax = 20;

        modFoundingGrudge = -60;
        modFoundingGrudgeDecay = 0.125f;

        powerWeightHex = 1f;
        powerWeightIncome = 2f;
        powerWeightUnit = 1f;
        powerWeightProvince = 3f;

        reputationMin = 0;
        reputationMax = 100;
        reputationStart = 70;

        reputationContractBroken = -20;
        reputationBetrayal = -35;

        reputationCleanTurnsStep = 10;
        reputationCleanTurnsBonus = 5;

        reputationRefusalThreshold = 30;

        acceptFriendshipOpinionBase = 35f;
        acceptFriendshipOpinionCaution = 30f;
        acceptFriendshipTrust = 55f;

        declareWarOpinionBase = -40f;
        declareWarOpinionAggression = 40f;

        declareWarPowerRatioBase = 1.2f;
        declareWarPowerRatioCaution = 1.3f;

        declareWarPactIgnoreHonesty = 0.4f;

        proposeCoalitionLeaderGap = 0.4f;
        proposeCoalitionTargetOpinion = -30f;

        personalitySpread = 0.15f;

        initPresets();

        landValueIncomeHorizon = 12;
        landSplitIncomeWeight = 3f;

        ultimatumDeadline = 2;
        nonAggressionDurations = new int[]{10, 20, 30};

        pactBreachPaymentIncomeMultiplier = 5f;
        pactBreachReputation = -20;

        tributeBreakReputation = -15;
        coalitionLeaveReputation = -25;
        coalitionMinMutualOpinion = -10;

        warWarningHonestyThreshold = 0.5f;

        aiProposalCooldownTurns = 3;
    }


    private static void initPresets() {
        presets = new float[PRESETS_QUANTITY][];

        //                                    aggr  caut  hon   vind  greed expa
        presets[PRESET_CONQUEROR] = new float[]{0.90f, 0.20f, 0.35f, 0.50f, 0.40f, 0.90f};
        presets[PRESET_TRADER]    = new float[]{0.20f, 0.50f, 0.70f, 0.30f, 0.90f, 0.50f};
        presets[PRESET_FORTRESS]  = new float[]{0.15f, 0.90f, 0.75f, 0.40f, 0.30f, 0.20f};
        presets[PRESET_JACKAL]    = new float[]{0.65f, 0.60f, 0.15f, 0.50f, 0.80f, 0.60f};
        presets[PRESET_ALLY]      = new float[]{0.30f, 0.45f, 0.95f, 0.20f, 0.35f, 0.40f};
        presets[PRESET_PARANOID]  = new float[]{0.50f, 0.95f, 0.50f, 0.85f, 0.40f, 0.30f};
    }


    /**
     * Затухание модификатора делится на мстительность владельца отношения
     * (4.1): злопамятный забывает медленнее.
     */
    public static float getDecayWithVindictiveness(float baseDecay, float vindictiveness) {
        if (vindictiveness <= 0) return baseDecay;

        return baseDecay / vindictiveness;
    }


    public static int clampOpinion(int opinion) {
        if (opinion < opinionMin) return opinionMin;
        if (opinion > opinionMax) return opinionMax;
        return opinion;
    }


    public static int clampTrust(int trust) {
        if (trust < trustMin) return trustMin;
        if (trust > trustMax) return trustMax;
        return trust;
    }


    public static int clampReputation(int reputation) {
        if (reputation < reputationMin) return reputationMin;
        if (reputation > reputationMax) return reputationMax;
        return reputation;
    }

    static {
        // В конце класса сознательно: статический блок выполняется в порядке
        // текста, и выше он отработал бы до инициализаторов полей.
        defaultValues();
    }
}
