package yio.tro.antiyoy.gameplay.statehood;

import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Obj;
import yio.tro.antiyoy.gameplay.Province;

import java.util.ArrayList;

/**
 * Регион провинции. Спека, часть III.
 *
 * Объект живёт ровно до следующей нарезки: провинции пересоздаются при
 * загрузке и отмене хода, а вместе с ними и регионы. Всё состояние, которое
 * обязано пережить это, лежит на гексах — прежде всего лояльность.
 */
public class Region {

    public final ArrayList<Hex> hexList;

    public Province province;
    public String name;

    /** Сколько ходов подряд лояльность держится ниже порога волнений (3.4). */
    public int lowLoyaltyTurns;

    /** Ход последней разовой выплаты: она доступна не чаще раза в N ходов (3.3). */
    public int lastInstantPaymentTurn;

    /** Разбивка последнего пересчёта: причина -> вклад. Нужна карточке (3.2). */
    public final ArrayList<String> lastReasons;
    public final ArrayList<Integer> lastValues;


    public Region(Province province) {
        this.province = province;

        hexList = new ArrayList<Hex>();
        lastReasons = new ArrayList<String>();
        lastValues = new ArrayList<Integer>();

        lowLoyaltyTurns = 0;
        lastInstantPaymentTurn = -StatehoodTuning.instantPaymentCooldown;
    }


    /**
     * Приоритет хранится на гексах: объект региона пересоздаётся при каждой
     * нарезке, и всё, что должно пережить её, лежит на них.
     */
    public int getPriority() {
        if (hexList.size() == 0) return StatehoodTuning.PRIORITY_NORMAL;

        return hexList.get(0).regionPriority;
    }


    public void setPriority(int priority) {
        for (Hex hex : hexList) {
            hex.regionPriority = priority;
        }
    }


    /**
     * Переключение по кругу: низкий, обычный, высокий. Спека, 3.3 — один тап
     * по карточке региона.
     */
    public void switchPriority() {
        setPriority((getPriority() + 1) % StatehoodTuning.PRIORITIES_QUANTITY);
    }


    public int getPriorityWeight() {
        int priority = getPriority();

        if (priority < 0 || priority >= StatehoodTuning.priorityWeights.length) {
            return StatehoodTuning.priorityWeights[StatehoodTuning.PRIORITY_NORMAL];
        }

        return StatehoodTuning.priorityWeights[priority];
    }


    /**
     * Лояльность региона — среднее по его гексам.
     */
    public int getLoyalty() {
        if (hexList.size() == 0) return StatehoodTuning.loyaltyStart;

        int sum = 0;
        for (Hex hex : hexList) {
            sum += hex.loyalty;
        }

        return sum / hexList.size();
    }


    /**
     * Изменение лояльности применяется ко всем гексам региона. Хранение на
     * гексах — это плата за переживание пересборки провинций; здесь она и
     * отрабатывается.
     */
    public void addLoyalty(int delta) {
        for (Hex hex : hexList) {
            hex.loyalty = StatehoodTuning.clampLoyalty(hex.loyalty + delta);
        }
    }


    public void setLoyalty(int value) {
        int clamped = StatehoodTuning.clampLoyalty(value);

        for (Hex hex : hexList) {
            hex.loyalty = clamped;
        }
    }


    public int getHexCount() {
        return hexList.size();
    }


    public float getBaseCost() {
        return StatehoodTuning.getBaseCost(hexList.size());
    }


    /**
     * Есть ли в регионе гарнизон: юнит или башня (3.2).
     */
    public boolean hasGarrison() {
        for (Hex hex : hexList) {
            if (hex.containsUnit()) return true;
            if (hex.objectInside == Obj.TOWER) return true;
            if (hex.objectInside == Obj.STRONG_TOWER) return true;
        }

        return false;
    }


    public boolean isInUnrest() {
        return lowLoyaltyTurns >= StatehoodTuning.unrestTriggerTurns;
    }


    /**
     * Множитель дохода: злой регион приносит меньше (3.2).
     */
    public float getIncomeMultiplier() {
        if (isInUnrest()) return 0f;

        return StatehoodTuning.getIncomeMultiplier(getLoyalty());
    }


    void clearReasons() {
        lastReasons.clear();
        lastValues.clear();
    }


    void addReason(String reason, int value) {
        if (value == 0) return;

        lastReasons.add(reason);
        lastValues.add(value);
    }


    @Override
    public String toString() {
        return "[Region " + name + ": " + hexList.size() + " гексов, лояльность " + getLoyalty() + "]";
    }
}
