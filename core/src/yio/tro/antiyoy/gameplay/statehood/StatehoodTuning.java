package yio.tro.antiyoy.gameplay.statehood;

/**
 * Числа регионов, лояльности, бюджета и сепаратизма. Спека, часть III.
 *
 * Значения не final: смысл класса в том, чтобы их можно было крутить на
 * прогонах автоматчей. Магических чисел в самой логике быть не должно
 * (CLAUDE.md, правило 4).
 */
public class StatehoodTuning {

    // ------------------------------------------------------------------
    // 3.1 Нарезка на регионы
    // ------------------------------------------------------------------

    /** Провинция крупнее этого режется на регионы. */
    public static int provinceSizeToSplit;

    public static int regionSizeMin;
    public static int regionSizeMax;

    // ------------------------------------------------------------------
    // 3.2 Лояльность
    // ------------------------------------------------------------------

    public static int loyaltyMin;
    public static int loyaltyMax;

    /** Стартовая лояльность обычного региона. */
    public static int loyaltyStart;

    /** Лояльность захваченных и купленных гексов. */
    public static int loyaltyAcquired;

    /** + loyaltyDotationFactor * (dotation / baseCost) */
    public static float loyaltyDotationFactor;

    /** + если в регионе есть гарнизон */
    public static int loyaltyGarrison;

    /** - loyaltyDistanceFactor * (distanceToCapital / loyaltyDistanceStep) */
    public static float loyaltyDistanceFactor;
    public static float loyaltyDistanceStep;

    /** - если провинция воюет */
    public static int loyaltyAtWar;

    /** - если регион граничит с врагом */
    public static int loyaltyBordersEnemy;

    /** + если регион не граничит ни с кем враждебным */
    public static int loyaltyPeacefulBorders;

    /** Доход региона умножается на incomeBase + incomeLoyaltyWeight * loyalty / loyaltyMax. */
    public static float incomeBase;
    public static float incomeLoyaltyWeight;

    // ------------------------------------------------------------------
    // 3.3 Бюджет
    // ------------------------------------------------------------------

    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;

    public static final int PRIORITIES_QUANTITY = 3;

    /** Веса деления бюджета провинции по приоритетам: 1 / 2 / 4. */
    public static int priorityWeights[];

    public static final int POLICY_WAR_ECONOMY = 0;
    public static final int POLICY_BALANCE = 1;
    public static final int POLICY_APPEASEMENT = 2;
    public static final int POLICY_SIEGE = 3;

    public static final int POLICIES_QUANTITY = 4;

    /** Доля дохода провинции, уходящая на дотации, по каждой политике. */
    public static float policyDotationShares[];

    public static final String POLICY_NAMES[] = {
            "war_economy",
            "balance",
            "appeasement",
            "siege",
    };

    /** Разовая выплата: сколько лояльности добавляет. */
    public static int instantPaymentLoyalty;

    /** Разовая выплата стоит instantPaymentCostFactor * baseCost. */
    public static float instantPaymentCostFactor;

    /** Разовая выплата доступна не чаще раза в N ходов. */
    public static int instantPaymentCooldown;

    // ------------------------------------------------------------------
    // 3.4 Волнения и отделение
    // ------------------------------------------------------------------

    /** Лояльность ниже этого значения подряд unrestTriggerTurns ходов — волнения. */
    public static int unrestLoyaltyThreshold;
    public static int unrestTriggerTurns;

    /**
     * Сколько ходов волнений до отделения. По сложности: лёгкая 8, средняя 6,
     * максимальная 4.
     */
    public static int secessionTurnsEasy;
    public static int secessionTurnsMedium;
    public static int secessionTurnsHard;

    /** Регион меньше этого не отделяется: осколок в два гекса не государство. */
    public static int secessionMinRegionSize;

    /** Сколько отделившихся государств может существовать одновременно. */
    public static int maxSecededStatesAlive;

    /** Между отделениями у одной фракции обязан пройти этот срок. */
    public static int secessionCooldownTurns;

    /** Отделившееся государство не дробится дальше столько ходов. */
    public static int secessionGraceTurns;


    /** Защитники нового государства: уровень defenderBase + hexCount / defenderHexStep. */
    public static int defenderBaseLevel;
    public static int defenderHexStep;

    // ------------------------------------------------------------------
    // 3.5 Характер нового государства
    // ------------------------------------------------------------------

    /** Доля родительских expansion и greed, наследуемая новым государством. */
    public static float inheritanceFactor;

    /** База для остальных черт. */
    public static float inheritanceBase;

    public static final int REASON_NEGLECT = 0;
    public static final int REASON_WAR_EXHAUSTION = 1;
    public static final int REASON_FOREIGN_RULE = 2;
    public static final int REASON_OPPORTUNISM = 3;

    /** Заброшенность: дотации были ниже этой доли нормы. */
    public static float reasonNeglectDotationShare;

    /** Военное истощение: провинция воевала столько ходов подряд. */
    public static int reasonWarExhaustionTurns;

    /** Чужое владычество: регион куплен или захвачен менее стольких ходов назад. */
    public static int reasonForeignRuleTurns;

    /** Оппортунизм: репутация родителя ниже этой. */
    public static int reasonOpportunismReputation;

    /** powerRatio к сильнейшему соседу выше этого — режим осторожности. */
    public static float neighbourRatioStrong;

    /** powerRatio ниже этого — режим напора. */
    public static float neighbourRatioWeak;

    /** Меньше этого числа гексов — режим выживания. */
    public static int sizeSurvivalThreshold;

    /** Больше этого числа гексов — прибавка к экспансии. */
    public static int sizeAmbitionThreshold;

    // ------------------------------------------------------------------
    // 3.6 Стартовые отношения
    // ------------------------------------------------------------------

    /** Доверие к родителю в момент отделения. */
    public static int startingTrustToParent;

    /** К соседям: за каждого, кто помогал родителю в войне. */
    public static int startingOpinionPerParentAlly;

    /** Тому, кто первым признал независимость. */
    public static int firstRecognitionOpinion;
    public static int firstRecognitionTrust;

    // ------------------------------------------------------------------
    // 3.7 Признание независимости
    // ------------------------------------------------------------------

    /** Любой признавший получает прибавку к мнению нового государства. */
    public static int recognitionOpinion;

    /** Родитель, признавший отделение, получает больше. */
    public static int parentRecognitionOpinion;

    /** И снимает с себя обиду основания на эту долю. */
    public static float parentRecognitionGrudgeRelief;

    /** Родитель, отказавшийся признавать, сохраняет casus belli столько ходов. */
    public static int casusBelliTurns;

    // ------------------------------------------------------------------
    // 3.8 ИИ и бюджет
    // ------------------------------------------------------------------

    /** Резерв, который ИИ не тратит на дотации: caution * aiReserveFactor * income. */
    public static float aiReserveFactor;

    /** Ниже этой лояльности ИИ переходит на умиротворение. */
    public static int aiAppeasementThreshold;

    /** Ниже этой — на баланс; выше обеих деньги идут на армию. */
    public static int aiBalanceThreshold;


    /**
     * Переопределение числа по имени — для подбора баланса прогонами.
     * Явный перебор, а не рефлексия: её в проекте нет, и веб её не
     * поддерживает.
     */
    public static boolean setByName(String name, float value) {
        if (name.equals("maxSecededStatesAlive")) {
            maxSecededStatesAlive = (int) value;
            return true;
        }

        if (name.equals("secessionCooldownTurns")) {
            secessionCooldownTurns = (int) value;
            return true;
        }

        if (name.equals("secessionGraceTurns")) {
            secessionGraceTurns = (int) value;
            return true;
        }

        if (name.equals("secessionMinRegionSize")) {
            secessionMinRegionSize = (int) value;
            return true;
        }

        if (name.equals("unrestLoyaltyThreshold")) {
            unrestLoyaltyThreshold = (int) value;
            return true;
        }

        if (name.equals("unrestTriggerTurns")) {
            unrestTriggerTurns = (int) value;
            return true;
        }

        if (name.equals("secessionTurnsHard")) {
            secessionTurnsHard = (int) value;
            secessionTurnsMedium = (int) value;
            secessionTurnsEasy = (int) value;
            return true;
        }

        if (name.equals("aiAppeasementThreshold")) {
            aiAppeasementThreshold = (int) value;
            return true;
        }

        if (name.equals("aiBalanceThreshold")) {
            aiBalanceThreshold = (int) value;
            return true;
        }

        if (name.equals("loyaltyAcquired")) {
            loyaltyAcquired = (int) value;
            return true;
        }

        if (name.equals("loyaltyDotationFactor")) {
            loyaltyDotationFactor = value;
            return true;
        }

        if (name.equals("policyAppeasementShare")) {
            policyDotationShares[POLICY_APPEASEMENT] = value;
            return true;
        }

        if (name.equals("policyBalanceShare")) {
            policyDotationShares[POLICY_BALANCE] = value;
            return true;
        }

        return false;
    }


    public static void defaultValues() {
        provinceSizeToSplit = 15;
        regionSizeMin = 8;
        regionSizeMax = 12;

        loyaltyMin = 0;
        loyaltyMax = 100;
        loyaltyStart = 60;
        loyaltyAcquired = 25;

        loyaltyDotationFactor = 4f;
        loyaltyGarrison = 3;
        loyaltyDistanceFactor = 2f;
        loyaltyDistanceStep = 5f;
        loyaltyAtWar = -4;
        loyaltyBordersEnemy = -3;
        loyaltyPeacefulBorders = 2;

        incomeBase = 0.4f;
        incomeLoyaltyWeight = 0.6f;

        priorityWeights = new int[]{1, 2, 4};

        policyDotationShares = new float[POLICIES_QUANTITY];
        policyDotationShares[POLICY_WAR_ECONOMY] = 0.1f;
        policyDotationShares[POLICY_BALANCE] = 0.35f;
        policyDotationShares[POLICY_APPEASEMENT] = 0.7f;
        policyDotationShares[POLICY_SIEGE] = 0f;

        instantPaymentLoyalty = 15;
        instantPaymentCostFactor = 3f;
        instantPaymentCooldown = 3;

        unrestLoyaltyThreshold = 20;
        unrestTriggerTurns = 3;

        secessionTurnsEasy = 8;
        secessionTurnsMedium = 6;
        secessionTurnsHard = 4;

        secessionMinRegionSize = 8;
        maxSecededStatesAlive = 3;
        secessionCooldownTurns = 25;
        secessionGraceTurns = 40;
        defenderBaseLevel = 1;
        defenderHexStep = 8;

        inheritanceFactor = 0.5f;
        inheritanceBase = 0.5f;

        reasonNeglectDotationShare = 0.2f;
        reasonWarExhaustionTurns = 6;
        reasonForeignRuleTurns = 15;
        reasonOpportunismReputation = 40;

        neighbourRatioStrong = 2f;
        neighbourRatioWeak = 0.8f;

        sizeSurvivalThreshold = 8;
        sizeAmbitionThreshold = 15;

        startingTrustToParent = 0;
        startingOpinionPerParentAlly = -15;

        firstRecognitionOpinion = 25;
        firstRecognitionTrust = 20;

        recognitionOpinion = 25;
        parentRecognitionOpinion = 40;
        parentRecognitionGrudgeRelief = 0.5f;
        casusBelliTurns = 20;

        aiReserveFactor = 0.4f;
        aiAppeasementThreshold = 40;
        aiBalanceThreshold = 70;
    }


    /**
     * baseCost = 1 + hexCount / 4. Опорная величина для дотаций и разовой
     * выплаты (3.2, 3.3).
     */
    public static float getBaseCost(int hexCount) {
        return 1f + hexCount / 4f;
    }


    /**
     * Множитель дохода региона по его лояльности (3.2).
     */
    public static float getIncomeMultiplier(int loyalty) {
        return incomeBase + incomeLoyaltyWeight * loyalty / (float) loyaltyMax;
    }


    public static int clampLoyalty(int loyalty) {
        if (loyalty < loyaltyMin) return loyaltyMin;
        if (loyalty > loyaltyMax) return loyaltyMax;
        return loyalty;
    }


    public static int getInstantPaymentCost(int hexCount) {
        return (int) Math.ceil(instantPaymentCostFactor * getBaseCost(hexCount));
    }


    public static int getDefenderLevel(int hexCount) {
        return defenderBaseLevel + hexCount / defenderHexStep;
    }

    static {
        // В конце класса сознательно: статический блок выполняется в порядке
        // текста, и выше он отработал бы до инициализаторов полей.
        defaultValues();
    }
}
