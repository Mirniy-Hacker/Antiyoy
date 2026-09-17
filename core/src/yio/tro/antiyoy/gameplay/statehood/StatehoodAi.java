package yio.tro.antiyoy.gameplay.statehood;

import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.rules.GameRules;

import java.util.ArrayList;

/**
 * Решения ИИ по бюджету регионов. Спека, 3.8.
 *
 * Отдельно от боевого ИИ: тот огромный и завязан на захват, а это
 * экономическое решение раз в ход. Черт характера здесь пока нет — они
 * появятся на этапе 8, и осторожность с жадностью войдут сюда параметрами,
 * а не переписыванием.
 */
public class StatehoodAi {

    private final GameController gameController;

    private final ArrayList<Region> sorted;


    public StatehoodAi(GameController gameController) {
        this.gameController = gameController;

        sorted = new ArrayList<Region>();
    }


    /**
     * Ход ИИ по всем провинциям фракции: выбор политики и расстановка
     * приоритетов. Сама раздача денег идёт общим путём, тем же, что у
     * игрока.
     */
    public void makeDecisions(int ownerId) {
        if (!GameRules.areRegionsEnabled()) return;

        for (Province province : gameController.fieldManager.provinces) {
            if (province.getFraction() != ownerId) continue;

            choosePolicy(province);
            choosePriorities(province);
        }
    }


    /**
     * Выбор политики. Спека привязывает его к StrategicState, которого ещё
     * нет (он появится вместе с характерами на этапе 8), поэтому пока
     * решение принимается по наблюдаемому состоянию провинции.
     */
    private void choosePolicy(Province province) {
        ArrayList<Region> regions = gameController.regionManager.getRegionsOfProvince(province);
        if (regions.size() == 0) return;

        int worstLoyalty = StatehoodTuning.loyaltyMax;
        boolean anyUnrest = false;

        for (Region region : regions) {
            int loyalty = region.getLoyalty();
            if (loyalty < worstLoyalty) {
                worstLoyalty = loyalty;
            }
            if (region.isInUnrest()) {
                anyUnrest = true;
            }
        }

        // Волнения тушатся в первую очередь: регион в волнениях не приносит
        // вообще ничего, и дешевле вернуть его, чем содержать армию.
        if (anyUnrest || worstLoyalty < StatehoodTuning.aiAppeasementThreshold) {
            ProvinceBudget.setPolicy(province, StatehoodTuning.POLICY_APPEASEMENT);
            return;
        }

        if (worstLoyalty < StatehoodTuning.aiBalanceThreshold) {
            ProvinceBudget.setPolicy(province, StatehoodTuning.POLICY_BALANCE);
            return;
        }

        // Всё спокойно — деньги идут на армию.
        ProvinceBudget.setPolicy(province, StatehoodTuning.POLICY_WAR_ECONOMY);
    }


    /**
     * Приоритеты: регионы сортируются по (100 - loyalty) * ценность, и
     * высокий приоритет достаётся верхушке списка (спека, 3.8).
     *
     * Ценность региона — число его гексов: крупный регион и приносит
     * больше, и терять его больнее.
     */
    private void choosePriorities(Province province) {
        sorted.clear();
        sorted.addAll(gameController.regionManager.getRegionsOfProvince(province));

        if (sorted.size() == 0) return;

        sortByNeed();

        // Верхняя треть получает высокий приоритет, нижняя — низкий.
        int high = Math.max(1, sorted.size() / 3);
        int low = sorted.size() - Math.max(1, sorted.size() / 3);

        for (int i = 0; i < sorted.size(); i++) {
            Region region = sorted.get(i);

            if (i < high) {
                region.setPriority(StatehoodTuning.PRIORITY_HIGH);
            } else if (i >= low) {
                region.setPriority(StatehoodTuning.PRIORITY_LOW);
            } else {
                region.setPriority(StatehoodTuning.PRIORITY_NORMAL);
            }
        }
    }


    /**
     * Сортировка вставками по убыванию нужды. При равной нужде порядок
     * задаётся координатами: решения ИИ обязаны быть воспроизводимы.
     */
    private void sortByNeed() {
        for (int i = 1; i < sorted.size(); i++) {
            Region current = sorted.get(i);
            int j = i - 1;

            while (j >= 0 && isMoreNeedy(current, sorted.get(j))) {
                sorted.set(j + 1, sorted.get(j));
                j--;
            }

            sorted.set(j + 1, current);
        }
    }


    private boolean isMoreNeedy(Region one, Region two) {
        int needOne = getNeed(one);
        int needTwo = getNeed(two);

        if (needOne != needTwo) return needOne > needTwo;

        if (one.hexList.size() == 0 || two.hexList.size() == 0) return false;

        if (one.hexList.get(0).index1 != two.hexList.get(0).index1) {
            return one.hexList.get(0).index1 < two.hexList.get(0).index1;
        }

        return one.hexList.get(0).index2 < two.hexList.get(0).index2;
    }


    private int getNeed(Region region) {
        return (StatehoodTuning.loyaltyMax - region.getLoyalty()) * region.getHexCount();
    }
}
