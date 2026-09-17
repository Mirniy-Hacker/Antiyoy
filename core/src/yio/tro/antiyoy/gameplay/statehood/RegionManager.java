package yio.tro.antiyoy.gameplay.statehood;

import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Obj;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticEntity;
import yio.tro.antiyoy.gameplay.name_generator.CityNameGenerator;
import yio.tro.antiyoy.gameplay.rules.GameRules;

import java.util.ArrayList;

/**
 * Нарезка провинций на регионы и пересчёт лояльности. Спека, часть III.
 *
 * Нарезка детерминированная: якоря сортируются по координатам, волна идёт
 * слоями. Случайности здесь нет вовсе — иначе отмена хода и загрузка сейва
 * давали бы другую разбивку.
 */
public class RegionManager {

    private final GameController gameController;

    public final ArrayList<Region> regions;

    private final ArrayList<Hex> anchors;
    private final ArrayList<Hex> currentWave;
    private final ArrayList<Hex> nextWave;


    public RegionManager(GameController gameController) {
        this.gameController = gameController;

        regions = new ArrayList<Region>();
        anchors = new ArrayList<Hex>();
        currentWave = new ArrayList<Hex>();
        nextWave = new ArrayList<Hex>();
    }


    public void defaultValues() {
        regions.clear();
    }


    /**
     * Полная пересборка регионов по текущим провинциям.
     *
     * Вызывается при изменении формы провинций. Лояльность при этом не
     * трогается: она лежит на гексах и переживает пересборку сама.
     */
    public void recreateRegions() {
        regions.clear();

        if (!GameRules.areRegionsEnabled()) return;

        for (Province province : gameController.fieldManager.provinces) {
            sliceProvince(province);
        }

        updateRegionIndices();
        initializeFreshLoyalty();
        updateNames();
    }


    /**
     * Провинция меньше порога — один регион на всю провинцию. Крупная
     * режется по якорям (3.1).
     */
    private void sliceProvince(Province province) {
        if (province.hexList.size() == 0) return;

        if (province.hexList.size() <= StatehoodTuning.provinceSizeToSplit) {
            Region region = new Region(province);
            region.hexList.addAll(province.hexList);
            regions.add(region);
            return;
        }

        collectAnchors(province);

        if (anchors.size() < 2) {
            Region region = new Region(province);
            region.hexList.addAll(province.hexList);
            regions.add(region);
            return;
        }

        growRegionsFromAnchors(province);
    }


    /**
     * Якоря — столица, города, фермы. Если их мало для нужного числа
     * регионов, добавляются самые дальние гексы, чтобы куски не выходили
     * слишком крупными.
     */
    private void collectAnchors(Province province) {
        anchors.clear();

        for (Hex hex : province.hexList) {
            if (hex.objectInside != Obj.TOWN) continue;
            anchors.add(hex);
        }

        for (Hex hex : province.hexList) {
            if (hex.objectInside != Obj.FARM) continue;
            anchors.add(hex);
        }

        int wanted = getWantedRegionCount(province.hexList.size());

        if (anchors.size() >= wanted) {
            trimAnchors(wanted);
            return;
        }

        addSpreadAnchors(province, wanted);
    }


    private int getWantedRegionCount(int hexCount) {
        int average = (StatehoodTuning.regionSizeMin + StatehoodTuning.regionSizeMax) / 2;
        int wanted = hexCount / average;

        if (wanted < 2) return 2;

        return wanted;
    }


    /**
     * Лишние якоря отбрасываются по расстоянию: оставляются те, что дальше
     * друг от друга, иначе рядом стоящие фермы дробили бы провинцию в пыль.
     */
    private void trimAnchors(int wanted) {
        while (anchors.size() > wanted) {
            int closestIndex = -1;
            int closestDistance = Integer.MAX_VALUE;

            for (int i = 0; i < anchors.size(); i++) {
                int distance = getDistanceToNearestOtherAnchor(i);
                if (distance >= closestDistance) continue;

                closestDistance = distance;
                closestIndex = i;
            }

            if (closestIndex < 0) break;

            anchors.remove(closestIndex);
        }
    }


    private int getDistanceToNearestOtherAnchor(int index) {
        Hex hex = anchors.get(index);
        int best = Integer.MAX_VALUE;

        for (int i = 0; i < anchors.size(); i++) {
            if (i == index) continue;

            int distance = getHexDistance(hex, anchors.get(i));
            if (distance < best) {
                best = distance;
            }
        }

        return best;
    }


    /**
     * Добивает якоря гексами, максимально удалёнными от уже выбранных.
     */
    private void addSpreadAnchors(Province province, int wanted) {
        while (anchors.size() < wanted) {
            Hex best = null;
            int bestDistance = -1;

            for (Hex hex : province.hexList) {
                if (anchors.contains(hex)) continue;

                int distance = anchors.size() == 0
                        ? 0
                        : getDistanceToNearestAnchor(hex);

                // При равенстве берётся меньший по координатам: нарезка
                // обязана быть воспроизводимой.
                if (distance > bestDistance
                        || (distance == bestDistance && best != null && isBefore(hex, best))) {
                    bestDistance = distance;
                    best = hex;
                }
            }

            if (best == null) break;

            anchors.add(best);
        }
    }


    private int getDistanceToNearestAnchor(Hex hex) {
        int best = Integer.MAX_VALUE;

        for (Hex anchor : anchors) {
            int distance = getHexDistance(hex, anchor);
            if (distance < best) {
                best = distance;
            }
        }

        return best;
    }


    private static boolean isBefore(Hex one, Hex two) {
        if (one.index1 != two.index1) return one.index1 < two.index1;

        return one.index2 < two.index2;
    }


    /**
     * Расстояние в осевых координатах поля.
     */
    private static int getHexDistance(Hex one, Hex two) {
        int dx = one.index1 - two.index1;
        int dy = one.index2 - two.index2;

        if ((dx < 0) == (dy < 0)) {
            return Math.abs(dx + dy);
        }

        return Math.max(Math.abs(dx), Math.abs(dy));
    }


    /**
     * Конкурентный flood fill: все регионы растут одновременно по слою за
     * раз, поэтому граница проходит примерно посередине между якорями.
     */
    private void growRegionsFromAnchors(Province province) {
        sortAnchors();

        ArrayList<Region> created = new ArrayList<Region>();

        for (Hex anchor : anchors) {
            Region region = new Region(province);
            region.hexList.add(anchor);
            created.add(region);
            regions.add(region);

            anchor.genFlag = true;
        }

        for (Hex hex : province.hexList) {
            if (anchors.contains(hex)) continue;
            hex.genFlag = false;
        }

        boolean grown = true;
        while (grown) {
            grown = false;

            for (Region region : created) {
                if (growOneLayer(region, province)) {
                    grown = true;
                }
            }
        }

        // Гексы, до которых волна не дошла (провинция могла быть разорвана
        // по диагонали), отдаются ближайшему региону.
        attachOrphans(province, created);
    }


    private void sortAnchors() {
        for (int i = 1; i < anchors.size(); i++) {
            Hex current = anchors.get(i);
            int j = i - 1;

            while (j >= 0 && isBefore(current, anchors.get(j))) {
                anchors.set(j + 1, anchors.get(j));
                j--;
            }

            anchors.set(j + 1, current);
        }
    }


    private boolean growOneLayer(Region region, Province province) {
        currentWave.clear();
        currentWave.addAll(region.hexList);

        nextWave.clear();

        for (Hex hex : currentWave) {
            for (int direction = 0; direction < 6; direction++) {
                Hex adjacent = hex.getAdjacentHex(direction);

                if (adjacent == null) continue;
                if (!adjacent.active) continue;
                if (adjacent.genFlag) continue;
                if (!province.containsHex(adjacent)) continue;
                if (nextWave.contains(adjacent)) continue;

                nextWave.add(adjacent);
            }
        }

        if (nextWave.size() == 0) return false;

        for (Hex hex : nextWave) {
            hex.genFlag = true;
            region.hexList.add(hex);
        }

        return true;
    }


    private void attachOrphans(Province province, ArrayList<Region> created) {
        for (Hex hex : province.hexList) {
            if (hex.genFlag) continue;

            Region best = null;
            int bestDistance = Integer.MAX_VALUE;

            for (Region region : created) {
                if (region.hexList.size() == 0) continue;

                int distance = getHexDistance(hex, region.hexList.get(0));
                if (distance >= bestDistance) continue;

                bestDistance = distance;
                best = region;
            }

            if (best == null) continue;

            best.hexList.add(hex);
            hex.genFlag = true;
        }
    }


    private void updateRegionIndices() {
        for (int i = 0; i < regions.size(); i++) {
            for (Hex hex : regions.get(i).hexList) {
                hex.regionIndex = i;
            }
        }
    }


    /**
     * Имена берутся у существующего генератора названий городов (3.1).
     */
    private void updateNames() {
        for (Region region : regions) {
            if (region.hexList.size() == 0) continue;

            region.name = CityNameGenerator.getInstance().generateName(region.hexList.get(0));
        }
    }


    /**
     * Пересчёт лояльности за ход для регионов текущего игрока. Спека, 3.2.
     *
     * Разбивка причин сохраняется в регионе: карточке региона нужно
     * показывать не только число, но и из чего оно сложилось. Половина
     * ощущения «непонятно, почему всё плохо» лечится именно этим.
     */
    public void updateLoyaltyForFraction(int ownerId) {
        if (!GameRules.areRegionsEnabled()) return;

        for (Region region : regions) {
            if (region.province == null) continue;
            if (region.province.getFraction() != ownerId) continue;

            updateSingleRegion(region);
        }
    }


    private void updateSingleRegion(Region region) {
        region.clearReasons();

        int delta = 0;

        if (region.hasGarrison()) {
            delta += StatehoodTuning.loyaltyGarrison;
            region.addReason("garrison", StatehoodTuning.loyaltyGarrison);
        }

        int distancePenalty = getDistancePenalty(region);
        delta += distancePenalty;
        region.addReason("distance", distancePenalty);

        if (isProvinceAtWar(region.province)) {
            delta += StatehoodTuning.loyaltyAtWar;
            region.addReason("war", StatehoodTuning.loyaltyAtWar);
        }

        if (bordersEnemy(region)) {
            delta += StatehoodTuning.loyaltyBordersEnemy;
            region.addReason("enemy_border", StatehoodTuning.loyaltyBordersEnemy);
        } else {
            delta += StatehoodTuning.loyaltyPeacefulBorders;
            region.addReason("calm_border", StatehoodTuning.loyaltyPeacefulBorders);
        }

        region.addLoyalty(delta);

        updateUnrestCounter(region);
    }


    private int getDistancePenalty(Region region) {
        Hex capital = region.province.getCapital();
        if (capital == null) return 0;
        if (region.hexList.size() == 0) return 0;

        int distance = getHexDistance(region.hexList.get(0), capital);

        float steps = distance / StatehoodTuning.loyaltyDistanceStep;

        return -(int) (StatehoodTuning.loyaltyDistanceFactor * steps);
    }


    private boolean isProvinceAtWar(Province province) {
        if (!GameRules.diplomacyEnabled) return false;

        DiplomaticEntity entity = gameController.fieldManager.diplomacyManager
                .getEntityById(province.getFraction());
        if (entity == null) return false;

        return entity.isAtWarPublic();
    }


    private boolean bordersEnemy(Region region) {
        for (Hex hex : region.hexList) {
            for (int direction = 0; direction < 6; direction++) {
                Hex adjacent = hex.getAdjacentHex(direction);

                if (adjacent == null) continue;
                if (!adjacent.active) continue;
                if (adjacent.isNeutral()) continue;
                if (adjacent.sameOwner(hex)) continue;

                return true;
            }
        }

        return false;
    }


    /**
     * Счётчик волнений: лояльность ниже порога подряд несколько ходов (3.4).
     */
    private void updateUnrestCounter(Region region) {
        if (region.getLoyalty() < StatehoodTuning.unrestLoyaltyThreshold) {
            region.lowLoyaltyTurns++;
            return;
        }

        region.lowLoyaltyTurns = 0;
    }


    /**
     * Множитель дохода гекса по лояльности его региона.
     *
     * Обращение по индексу, без поиска по списку: доход пересчитывается ИИ и
     * интерфейсом многократно за ход, и линейный поиск здесь превратился бы
     * в квадрат по числу гексов (CLAUDE.md, правило 6).
     */
    public float getIncomeMultiplierForHex(Hex hex) {
        if (!GameRules.areRegionsEnabled()) return 1f;
        if (hex == null) return 1f;
        if (hex.regionIndex < 0 || hex.regionIndex >= regions.size()) return 1f;

        return regions.get(hex.regionIndex).getIncomeMultiplier();
    }


    /**
     * Гексы, которым лояльность ещё не назначалась, получают стартовую.
     *
     * Отличать «не назначена» от «упала до нуля» обязательно: иначе регион в
     * волнениях сам собой возвращался бы к шестидесяти.
     */
    private void initializeFreshLoyalty() {
        for (Region region : regions) {
            for (Hex hex : region.hexList) {
                if (hex.loyalty >= 0) continue;

                hex.loyalty = StatehoodTuning.loyaltyStart;
            }
        }
    }


    public Region getRegionByHex(Hex hex) {
        if (hex == null) return null;
        if (hex.regionIndex < 0 || hex.regionIndex >= regions.size()) return null;

        Region region = regions.get(hex.regionIndex);
        if (!region.hexList.contains(hex)) return null;

        return region;
    }


    public ArrayList<Region> getRegionsOfProvince(Province province) {
        ArrayList<Region> result = new ArrayList<Region>();

        for (Region region : regions) {
            if (region.province != province) continue;
            result.add(region);
        }

        return result;
    }
}
