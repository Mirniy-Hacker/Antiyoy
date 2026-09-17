package yio.tro.antiyoy.gameplay.statehood;

import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticEntity;
import yio.tro.antiyoy.gameplay.diplomacy.DiplomaticRelation;
import yio.tro.antiyoy.gameplay.diplomacy.Personality;
import yio.tro.antiyoy.gameplay.rules.GameRules;

import java.util.ArrayList;

/**
 * Отделение регионов и рождение новых государств. Спека, 3.4-3.6.
 *
 * Здесь наконец расходятся ownerId и цвет, ради чего делался этап 1:
 * новое государство получает идентификатор за пределами палитры, а цвет —
 * по кругу, со штриховкой для различения.
 */
public class SecessionManager {

    private final GameController gameController;

    /** Сколько государств отделилось за партию. Идёт в CSV автоматчей. */
    public int secededCount;

    private final ArrayList<Region> readyToSecede;

    /** Ход последнего отделения у фракции и ход рождения государства. */
    private int lastSecessionTurn[];
    private int birthTurn[];


    public SecessionManager(GameController gameController) {
        this.gameController = gameController;

        readyToSecede = new ArrayList<Region>();
        lastSecessionTurn = new int[GameRules.MAX_FRACTIONS_QUANTITY + 8];
        birthTurn = new int[GameRules.MAX_FRACTIONS_QUANTITY + 8];
        secededCount = 0;

        defaultValues();
    }


    public void defaultValues() {
        secededCount = 0;

        for (int i = 0; i < lastSecessionTurn.length; i++) {
            lastSecessionTurn[i] = -1000;
            birthTurn[i] = -1;
        }
    }


    /**
     * Проверка регионов фракции на отделение. Вызывается раз в ход.
     */
    public void checkForSecession(int ownerId) {
        if (!GameRules.isSecessionEnabled()) return;

        readyToSecede.clear();

        for (Region region : gameController.regionManager.regions) {
            if (region.province == null) continue;
            if (region.province.getFraction() != ownerId) continue;
            if (!isReadyToSecede(region)) continue;

            readyToSecede.add(region);
        }

        // Список собирается заранее: отделение пересобирает провинции и
        // регионы, и продолжать обход по ним было бы нельзя.
        for (Region region : readyToSecede) {
            secede(region);
        }
    }


    private boolean isReadyToSecede(Region region) {
        if (!region.isInUnrest()) return false;

        // Карта не может прокормить неограниченное число государств: без
        // этого предела отделения идут лавиной и победитель не определяется
        // вовсе.
        if (countAliveSecededStates() >= StatehoodTuning.maxSecededStatesAlive) return false;
        if (region.hexList.size() < StatehoodTuning.secessionMinRegionSize) return false;

        // Отделение — симптом перерастяжения, а не способ дробить осколки.
        // Провинция должна быть достаточно крупной, чтобы вообще делиться.
        if (region.province.hexList.size() <= StatehoodTuning.provinceSizeToSplit) return false;

        int turn = gameController.matchStatistics.turnsMade;
        int ownerId = region.province.getFraction();

        // Ограничения частоты. Без них одно отделение создаёт новую границу,
        // соседние регионы злятся, отделяются дальше, и карта рассыпается
        // лавиной: победитель не определяется вовсе.
        if (turn - getLastSecessionTurn(ownerId) < StatehoodTuning.secessionCooldownTurns) {
            return false;
        }

        if (isYoungState(ownerId, turn)) return false;

        int turnsOfUnrest = region.lowLoyaltyTurns - StatehoodTuning.unrestTriggerTurns;

        return turnsOfUnrest >= getSecessionDelay();
    }


    /**
     * Живые отделившиеся государства: те, у кого идентификатор за пределами
     * палитры и осталась хоть одна провинция.
     */
    private int countAliveSecededStates() {
        int count = 0;

        for (Province province : gameController.fieldManager.provinces) {
            int ownerId = province.getFraction();
            if (ownerId < GameRules.MAX_FRACTIONS_QUANTITY) continue;

            count++;
        }

        return count;
    }


    private int getLastSecessionTurn(int ownerId) {
        if (ownerId < 0 || ownerId >= lastSecessionTurn.length) return -1000;

        return lastSecessionTurn[ownerId];
    }


    private void setLastSecessionTurn(int ownerId, int turn) {
        ensureCapacity(ownerId);

        if (ownerId < 0 || ownerId >= lastSecessionTurn.length) return;

        lastSecessionTurn[ownerId] = turn;
    }


    /**
     * Молодое государство не дробится: ему нужно дать пожить, иначе оно
     * распадается в тот же десяток ходов, в который родилось.
     */
    private boolean isYoungState(int ownerId, int turn) {
        ensureCapacity(ownerId);

        if (ownerId < 0 || ownerId >= birthTurn.length) return false;
        if (birthTurn[ownerId] < 0) return false;

        return turn - birthTurn[ownerId] < StatehoodTuning.secessionGraceTurns;
    }


    private void ensureCapacity(int ownerId) {
        if (ownerId < lastSecessionTurn.length) return;

        int newSize = ownerId + 8;

        int newLast[] = new int[newSize];
        int newBirth[] = new int[newSize];

        for (int i = 0; i < newSize; i++) {
            newLast[i] = i < lastSecessionTurn.length ? lastSecessionTurn[i] : -1000;
            newBirth[i] = i < birthTurn.length ? birthTurn[i] : -1;
        }

        lastSecessionTurn = newLast;
        birthTurn = newBirth;
    }


    /**
     * Сколько ходов волнений до отделения. Спека, 3.4: зависит от сложности —
     * на лёгкой у игрока больше времени всё исправить.
     */
    private int getSecessionDelay() {
        switch (GameRules.difficulty) {
            case 0:
            case 1:
                return StatehoodTuning.secessionTurnsEasy;
            case 2:
            case 3:
                return StatehoodTuning.secessionTurnsMedium;
            default:
                return StatehoodTuning.secessionTurnsHard;
        }
    }


    private void secede(Region region) {
        Province parentProvince = region.province;
        if (parentProvince == null) return;

        int parentOwnerId = parentProvince.getFraction();

        int newOwnerId = getFreeOwnerId();
        if (newOwnerId < 0) return;

        int reason = detectReason(region, parentProvince);
        int hexCount = region.hexList.size();

        // Гексы меняют владельца напрямую: setHexFraction пересобирает
        // провинции на каждом вызове, а нам нужно перевести регион целиком.
        ArrayList<Hex> hexes = new ArrayList<Hex>(region.hexList);

        for (Hex hex : hexes) {
            hex.setOwnerSilently(newOwnerId);
            // Новое государство начинает не с нуля: оно только что добилось
            // своего, и это его собственная земля.
            hex.loyalty = StatehoodTuning.loyaltyStart;
        }

        GameRules.ensureOwnerLimit(newOwnerId);

        spawnDefenders(hexes, hexCount);

        gameController.fieldManager.detectProvinces();

        DiplomaticEntity entity = createEntity(newOwnerId, parentOwnerId, reason, hexCount);
        if (entity != null) {
            applyStartingRelations(entity, parentOwnerId);
        }

        int turn = gameController.matchStatistics.turnsMade;
        setLastSecessionTurn(parentOwnerId, turn);
        ensureCapacity(newOwnerId);
        birthTurn[newOwnerId] = turn;

        secededCount++;
    }


    /**
     * Свободный идентификатор за пределами палитры: базовые цвета заняты
     * игроками, и переиспользовать их нельзя.
     */
    private int getFreeOwnerId() {
        for (int candidate = GameRules.MAX_FRACTIONS_QUANTITY; candidate < 200; candidate++) {
            if (isOwnerIdTaken(candidate)) continue;

            return candidate;
        }

        return -1;
    }


    private boolean isOwnerIdTaken(int ownerId) {
        for (DiplomaticEntity entity : gameController.fieldManager.diplomacyManager.entities) {
            if (entity.entityId == ownerId) return true;
        }

        return false;
    }


    /**
     * Причина отделения по состоянию в момент бунта. Спека, 3.5: она потом
     * определяет характер нового государства, поэтому определяется один раз
     * и здесь.
     */
    private int detectReason(Region region, Province province) {
        int policy = ProvinceBudget.getPolicy(province);

        // Заброшенность: провинция почти не платила дотаций.
        if (ProvinceBudget.getDotationShare(province) <= StatehoodTuning.reasonNeglectDotationShare) {
            return StatehoodTuning.REASON_NEGLECT;
        }

        if (policy == StatehoodTuning.POLICY_SIEGE) {
            return StatehoodTuning.REASON_NEGLECT;
        }

        // Чужое владычество: регион недавно сменил хозяина, и лояльность у
        // него стартовала с низкой.
        if (region.getLoyalty() <= StatehoodTuning.loyaltyAcquired) {
            return StatehoodTuning.REASON_FOREIGN_RULE;
        }

        if (isProvinceAtWar(province)) {
            return StatehoodTuning.REASON_WAR_EXHAUSTION;
        }

        return StatehoodTuning.REASON_OPPORTUNISM;
    }


    private boolean isProvinceAtWar(Province province) {
        if (!GameRules.diplomacyEnabled) return false;

        DiplomaticEntity entity = gameController.fieldManager.diplomacyManager
                .getEntityById(province.getFraction());
        if (entity == null) return false;

        return entity.isAtWarPublic();
    }


    /**
     * Защитники: спека, 3.4. Без них новое государство сносят в тот же ход,
     * и вся механика превращается в мигание.
     */
    private void spawnDefenders(ArrayList<Hex> hexes, int hexCount) {
        int strength = StatehoodTuning.getDefenderLevel(hexCount);
        if (strength <= 0) return;

        if (strength > 4) {
            strength = 4;
        }

        Hex best = null;
        for (Hex hex : hexes) {
            if (!hex.isFree()) continue;

            if (best == null || hex.index1 < best.index1
                    || (hex.index1 == best.index1 && hex.index2 < best.index2)) {
                best = hex;
            }
        }

        if (best == null) return;

        gameController.fieldManager.addUnit(best, strength);
    }


    private DiplomaticEntity createEntity(int newOwnerId, int parentOwnerId, int reason, int hexCount) {
        if (!GameRules.diplomacyEnabled) return null;

        DiplomaticEntity entity = gameController.fieldManager.diplomacyManager
                .createSecededEntity(newOwnerId);
        if (entity == null) return null;

        DiplomaticEntity parent = gameController.fieldManager.diplomacyManager
                .getEntityById(parentOwnerId);

        buildPersonality(entity, parent, reason, hexCount);

        return entity;
    }


    /**
     * Характер из четырёх источников. Спека, 3.5: родитель, причина,
     * соседи и размер. Случайности здесь нет — характер выводится из
     * обстоятельств рождения.
     */
    private void buildPersonality(DiplomaticEntity entity, DiplomaticEntity parent,
                                  int reason, int hexCount) {
        Personality personality = entity.personality;

        if (parent != null) {
            personality.generateFromParent(parent.personality);
        }

        applyReasonShift(personality, reason);
        applySizeShift(personality, hexCount);
    }


    private void applyReasonShift(Personality personality, int reason) {
        switch (reason) {
            case StatehoodTuning.REASON_NEGLECT:
                personality.add(Personality.VINDICTIVENESS, 0.25f);
                personality.add(Personality.CAUTION, 0.2f);
                break;

            case StatehoodTuning.REASON_WAR_EXHAUSTION:
                personality.add(Personality.AGGRESSION, -0.3f);
                personality.add(Personality.HONESTY, 0.25f);
                break;

            case StatehoodTuning.REASON_FOREIGN_RULE:
                personality.add(Personality.AGGRESSION, 0.3f);
                personality.add(Personality.VINDICTIVENESS, 0.3f);
                break;

            case StatehoodTuning.REASON_OPPORTUNISM:
                personality.add(Personality.GREED, 0.2f);
                personality.add(Personality.HONESTY, -0.2f);
                break;
        }
    }


    private void applySizeShift(Personality personality, int hexCount) {
        if (hexCount < StatehoodTuning.sizeSurvivalThreshold) {
            personality.add(Personality.CAUTION, 0.3f);
            personality.add(Personality.EXPANSION, -0.3f);
            return;
        }

        if (hexCount > StatehoodTuning.sizeAmbitionThreshold) {
            personality.add(Personality.EXPANSION, 0.2f);
        }
    }


    /**
     * Стартовые отношения. Спека, 3.6: к родителю вражда, к остальным
     * безразличие. Обида основания как затухающий модификатор появится
     * вместе с системой мнения на этапе 6.
     */
    private void applyStartingRelations(DiplomaticEntity entity, int parentOwnerId) {
        DiplomaticEntity parent = gameController.fieldManager.diplomacyManager
                .getEntityById(parentOwnerId);
        if (parent == null) return;

        gameController.fieldManager.diplomacyManager
                .setRelation(entity, parent, DiplomaticRelation.ENEMY);
    }
}
