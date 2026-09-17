package yio.tro.antiyoy.gameplay.statehood;

import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.rules.GameRules;

import java.util.ArrayList;

/**
 * Бюджет провинции: сколько дохода уходит на дотации регионам и как оно
 * делится между ними. Спека, 3.3.
 *
 * Ручного выставления сумм каждому региону здесь нет сознательно — на
 * телефоне это неиграбельно. Гибкость даётся комбинацией политики провинции
 * и трёх уровней приоритета региона.
 */
public class ProvinceBudget {

    private final GameController gameController;


    public ProvinceBudget(GameController gameController) {
        this.gameController = gameController;
    }


    /**
     * Политика провинции хранится на её гексах: сам объект Province
     * пересоздаётся при загрузке и отмене хода.
     */
    public static int getPolicy(Province province) {
        if (province == null) return StatehoodTuning.POLICY_BALANCE;
        if (province.hexList.size() == 0) return StatehoodTuning.POLICY_BALANCE;

        return province.hexList.get(0).provincePolicy;
    }


    public static void setPolicy(Province province, int policy) {
        if (province == null) return;

        for (Hex hex : province.hexList) {
            hex.provincePolicy = policy;
        }
    }


    public static void switchPolicy(Province province) {
        setPolicy(province, (getPolicy(province) + 1) % StatehoodTuning.POLICIES_QUANTITY);
    }


    public static float getDotationShare(Province province) {
        int policy = getPolicy(province);

        if (policy < 0 || policy >= StatehoodTuning.policyDotationShares.length) {
            return StatehoodTuning.policyDotationShares[StatehoodTuning.POLICY_BALANCE];
        }

        return StatehoodTuning.policyDotationShares[policy];
    }


    /**
     * Сколько провинция тратит на дотации в этот ход.
     *
     * Считается от дохода, а не от казны: дотации — это постоянная статья
     * расходов, а не разовая трата накоплений.
     */
    public static int getDotationBudget(Province province) {
        int income = province.getIncome();
        if (income <= 0) return 0;

        return (int) (income * getDotationShare(province));
    }


    /**
     * Раздаёт дотации регионам провинции и поднимает их лояльность.
     * Возвращает потраченную сумму.
     *
     * Деньги списываются с казны: без реального списания дотации были бы
     * бесплатным добром и ничего не меняли бы в экономике.
     */
    public int payDotations(Province province) {
        if (!GameRules.areRegionsEnabled()) return 0;

        int budget = getDotationBudget(province);
        if (budget <= 0) return 0;
        if (province.money <= 0) return 0;

        if (budget > province.money) {
            budget = province.money;
        }

        ArrayList<Region> regions = gameController.regionManager.getRegionsOfProvince(province);
        if (regions.size() == 0) return 0;

        int totalWeight = 0;
        for (Region region : regions) {
            totalWeight += region.getPriorityWeight();
        }

        if (totalWeight == 0) return 0;

        int spent = 0;

        for (Region region : regions) {
            int share = budget * region.getPriorityWeight() / totalWeight;
            if (share <= 0) continue;

            applyDotation(region, share);
            spent += share;
        }

        province.money -= spent;

        return spent;
    }


    /**
     * Прибавка лояльности за дотацию: спека, 3.2 — четыре единицы за
     * отношение дотации к опорной стоимости региона.
     *
     * Опорная стоимость растёт с размером региона, поэтому крупный регион
     * дороже держать довольным.
     */
    private void applyDotation(Region region, int amount) {
        float baseCost = region.getBaseCost();
        if (baseCost <= 0) return;

        int delta = (int) (StatehoodTuning.loyaltyDotationFactor * amount / baseCost);
        if (delta <= 0) return;

        region.addLoyalty(delta);
        region.addReason("dotations", delta);
    }


    /**
     * Разовая выплата: спека, 3.3. Кнопка в карточке региона.
     *
     * Ограничена не только ценой, но и частотой: иначе она подменяла бы
     * собой всю систему приоритетов.
     */
    public boolean payInstantBonus(Region region) {
        if (!GameRules.areRegionsEnabled()) return false;
        if (region == null) return false;

        Province province = region.province;
        if (province == null) return false;

        int turn = gameController.matchStatistics.turnsMade;
        if (turn - region.lastInstantPaymentTurn < StatehoodTuning.instantPaymentCooldown) {
            return false;
        }

        int cost = StatehoodTuning.getInstantPaymentCost(region.getHexCount());
        if (province.money < cost) return false;

        province.money -= cost;
        region.addLoyalty(StatehoodTuning.instantPaymentLoyalty);
        region.lastInstantPaymentTurn = turn;

        return true;
    }


    public boolean canPayInstantBonus(Region region) {
        if (!GameRules.areRegionsEnabled()) return false;
        if (region == null) return false;
        if (region.province == null) return false;

        int turn = gameController.matchStatistics.turnsMade;
        if (turn - region.lastInstantPaymentTurn < StatehoodTuning.instantPaymentCooldown) {
            return false;
        }

        return region.province.money >= StatehoodTuning.getInstantPaymentCost(region.getHexCount());
    }
}
