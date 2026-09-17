package yio.tro.antiyoy.gameplay.rules;

/**
 * Числа экономики. Спека, часть II.
 *
 * Значения не final: смысл класса в том, чтобы их можно было крутить на
 * прогонах автоматчей, не пересобирая логику. Магических чисел в самой
 * логике быть не должно (CLAUDE.md, правило 4).
 */
public class EconomyTuning {

    /**
     * Потолок казны: money > TREASURY_CAP_MULTIPLIER * income.
     * При доходе 80 потолок 1600.
     */
    public static float treasuryCapMultiplier;

    /**
     * Доля излишка сверх потолка, тающая за ход.
     */
    public static float treasuryLeakPerTurn;

    /**
     * Удорожание каждого следующего юнита, купленного в этой провинции в
     * этот ход. Счётчик обнуляется в начале хода. Двенадцатый юнит выходит
     * примерно впятеро дороже первого.
     */
    public static float unitPriceGrowthPerUnit;


    public static void defaultValues() {
        treasuryCapMultiplier = 20f;
        treasuryLeakPerTurn = 0.1f;
        unitPriceGrowthPerUnit = 0.15f;
    }


    /**
     * Потолок казны для заданного дохода.
     */
    public static int getTreasuryCap(int income) {
        return (int) (treasuryCapMultiplier * income);
    }


    /**
     * Сколько денег утекает за ход при текущей казне и доходе.
     * Ноль, если казна не превышает потолок.
     */
    public static int getTreasuryLeak(int money, int income) {
        int cap = getTreasuryCap(income);
        if (money <= cap) return 0;

        return (int) Math.ceil((money - cap) * treasuryLeakPerTurn);
    }


    /**
     * Цена юнита с учётом того, сколько их уже куплено в этой провинции в
     * этот ход.
     */
    public static int getEscalatedUnitPrice(int basePrice, int unitsBoughtThisTurn) {
        if (unitsBoughtThisTurn <= 0) return basePrice;

        double multiplier = Math.pow(1d + unitPriceGrowthPerUnit, unitsBoughtThisTurn);

        return (int) Math.round(basePrice * multiplier);
    }

    static {
        // В конце класса сознательно: статический блок выполняется в порядке
        // текста, и выше он отработал бы до инициализаторов полей.
        defaultValues();
    }
}
