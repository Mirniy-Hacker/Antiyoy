package yio.tro.antiyoy.gameplay.sim;

import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomacyInfoCondensed;
import yio.tro.antiyoy.gameplay.rules.GameRules;

/**
 * Контрольная сумма состояния партии.
 *
 * Нужна приёмке этапа 1: сравнения победителя и числа ходов мало, два разных
 * кода могут совпасть по итогу случайно. По-ходовой след позволяет увидеть
 * не «расхождение есть», а «расхождение на таком-то ходу».
 *
 * Важно: поле обходится по матрице, а не по спискам вроде activeHexes.
 * Порядок в списках — деталь реализации, и рефакторинг может его поменять,
 * не меняя само состояние. Обход по индексам сравнивает состояние, а не
 * внутреннее устройство.
 *
 * По той же причине провинции складываются коммутативно: их порядок в списке
 * ничего не значит.
 */
public class SimStateHash {

    private static final long SEED = 1125899906842597L;
    private static final long MULTIPLIER = 31L;


    public static long computeField(GameController gameController) {
        long hash = SEED;

        int width = gameController.fieldManager.fWidth;
        int height = gameController.fieldManager.fHeight;

        hash = mix(hash, width);
        hash = mix(hash, height);
        hash = mix(hash, gameController.turn);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Hex hex = gameController.fieldManager.field[i][j];
                hash = mix(hash, hashHex(hex));
            }
        }

        hash = mix(hash, hashProvinces(gameController));

        return hash;
    }


    private static long hashHex(Hex hex) {
        if (hex == null) return 0;
        if (!hex.active) return 1;

        long hash = SEED;

        hash = mix(hash, hex.index1);
        hash = mix(hash, hex.index2);
        hash = mix(hash, hex.fraction);
        hash = mix(hash, hex.objectInside);

        if (hex.unit == null) {
            hash = mix(hash, 0);
        } else {
            hash = mix(hash, 1 + hex.unit.strength);
            hash = mix(hash, hex.unit.isReadyToMove() ? 1 : 0);
        }

        return hash;
    }


    /**
     * Провинции складываются, а не домножаются: сложение коммутативно, и
     * порядок в списке на результат не влияет. Каждая провинция опознаётся
     * по координатам своего первого гекса, а не по позиции в списке.
     */
    private static long hashProvinces(GameController gameController) {
        long sum = 0;

        for (Province province : gameController.fieldManager.provinces) {
            if (province.hexList.size() == 0) continue;

            long hash = SEED;

            Hex anchor = getAnchorHex(province);
            hash = mix(hash, anchor.index1);
            hash = mix(hash, anchor.index2);
            hash = mix(hash, province.getFraction());
            hash = mix(hash, province.money);
            hash = mix(hash, province.hexList.size());

            sum += hash;
        }

        return sum;
    }


    /**
     * Опорный гекс провинции — наименьший по координатам. Первый элемент
     * hexList для этого не годится: он зависит от порядка обхода.
     */
    private static Hex getAnchorHex(Province province) {
        Hex best = null;

        for (Hex hex : province.hexList) {
            if (best == null) {
                best = hex;
                continue;
            }

            if (hex.index1 < best.index1) {
                best = hex;
                continue;
            }

            if (hex.index1 == best.index1 && hex.index2 < best.index2) {
                best = hex;
            }
        }

        return best;
    }


    public static long computeDiplomacy(GameController gameController) {
        if (!GameRules.diplomacyEnabled) return 0;

        DiplomacyInfoCondensed instance = DiplomacyInfoCondensed.getInstance();
        instance.update(gameController.fieldManager.diplomacyManager);

        String encoded = instance.getFull();
        if (encoded == null) return 0;

        return hashString(encoded);
    }


    private static long hashString(String source) {
        long hash = SEED;

        for (int i = 0; i < source.length(); i++) {
            hash = mix(hash, source.charAt(i));
        }

        return hash;
    }


    private static long mix(long hash, long value) {
        return hash * MULTIPLIER + value;
    }
}
