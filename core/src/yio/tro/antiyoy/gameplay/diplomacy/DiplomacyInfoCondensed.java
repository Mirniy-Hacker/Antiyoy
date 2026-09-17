package yio.tro.antiyoy.gameplay.diplomacy;

import yio.tro.antiyoy.gameplay.rules.GameRules;
import yio.tro.antiyoy.stuff.object_pool.ReusableYio;

import java.util.ArrayList;

public class DiplomacyInfoCondensed implements ReusableYio {


    private static DiplomacyInfoCondensed instance;

    /**
     * Префикс версии формата (SPEC, часть X). Сейвы оригинала его не имеют и
     * читаются как VERSION_LEGACY с подстановкой умолчаний.
     */
    public static final String VERSION_PREFIX = "v2/";

    public static final int VERSION_LEGACY = 1;
    public static final int VERSION_CURRENT = 2;

    private int loadedVersion;

    String full;
    String relations;
    String contracts;
    String cooldowns;
    String messages;
    String debts;
    DiplomacyManager diplomacyManager;
    private StringBuilder builder;


    public DiplomacyInfoCondensed() {
        builder = new StringBuilder();
    }


    public static DiplomacyInfoCondensed getInstance() {
        if (instance == null) {
            instance = new DiplomacyInfoCondensed();
        }

        instance.reset();

        return instance;
    }


    public static void onGeneralInitialization() {
        instance = null;
    }


    @Override
    public void reset() {
        full = null;
        loadedVersion = VERSION_CURRENT;
        relations = null;
        diplomacyManager = null;
        contracts = null;
        cooldowns = null;
        messages = null;
        debts = null;
    }


    public void update(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;

        updateRelations();
        updateContracts();
        updateCooldowns();
        updateMessages();
        updateDebts();
        updateFull();
    }


    private void updateDebts() {
        debts = diplomacyManager.encodeDebts();
    }


    private void updateMessages() {
        builder.setLength(0);

        for (DiplomaticMessage message : diplomacyManager.log.messages) {
            builder.append(getSingleMessageCode(message)).append(",");
        }

        if (builder.length() == 0) {
            builder.append(" ");
        }

        messages = builder.toString();
    }


    private String getSingleMessageCode(DiplomaticMessage message) {
        return message.type + "=" + message.getSenderFraction() + "=" + message.getRecipientFraction() + "=" + message.arg1 + "=" + message.arg2 + "=" + message.arg3;
    }


    private void updateCooldowns() {
        builder.setLength(0);

        for (DiplomaticCooldown cooldown : diplomacyManager.cooldowns) {
            builder.append(getSingleCooldownCode(cooldown)).append(",");
        }

        if (builder.length() == 0) {
            builder.append(" ");
        }

        cooldowns = builder.toString();
    }


    private String getSingleCooldownCode(DiplomaticCooldown cooldown) {
        return cooldown.type + " " + cooldown.counter + " " + cooldown.getOneFraction() + " " + cooldown.getTwoFraction();
    }


    private void updateContracts() {
        builder.setLength(0);

        for (DiplomaticContract contract : diplomacyManager.contracts) {
            builder.append(getSingleContractCode(contract)).append(",");
        }

        if (builder.length() == 0) {
            builder.append(" ");
        }

        contracts = builder.toString();
    }


    private String getSingleContractCode(DiplomaticContract contract) {
        return contract.type + " " + contract.getOneFraction() + " " + contract.getTwoFraction() + " " + contract.dotations + " " + contract.expireCountDown;
    }


    public void apply(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;

        if (full == null) return;
        if (stripVersionPrefix(full).equals("-")) return;

        // Восстановление заново прогоняет setRelation и addContract, а они
        // ведут счётчики статистики. Без этого флага отмена хода и загрузка
        // сейва накручивали бы число войн и сделок.
        diplomacyManager.restoringState = true;
        try {
            diplomacyManager.clearCooldowns();
            diplomacyManager.clearContracts();
            applyFull();
            applyRelations();
            applyContracts();
            applyCooldowns();
            applyMessages();
            applyDebts();
        } finally {
            diplomacyManager.restoringState = false;
        }

        diplomacyManager.onRelationsChanged();
    }


    private void applyDebts() {
        diplomacyManager.decodeDebts(debts);
    }


    private void applyMessages() {
        if (messages == null) return;

        diplomacyManager.log.clear();
        for (String s : messages.split(",")) {
            applySingleMessage(s);
        }
    }


    private void applySingleMessage(String s) {
        if (!s.contains("=")) return;
        if (countSymbol(s, '=') < 5) return;
        String[] split = s.split("=");

        if (split.length < 3) return;

        DipMessageType type = DipMessageType.valueOf(split[0]);
        int fraction1 = Integer.valueOf(split[1]);
        int fraction2 = Integer.valueOf(split[2]);
        String arg1 = getSplitPart(split, 3);
        String arg2 = getSplitPart(split, 4);
        String arg3 = getSplitPart(split, 5);

        DiplomaticEntity entity1 = diplomacyManager.getEntity(fraction1);
        DiplomaticEntity entity2 = diplomacyManager.getEntity(fraction2);
        if (entity1 == null || entity2 == null) return;

        DiplomaticMessage diplomaticMessage = diplomacyManager.log.addMessage(type, entity1, entity2);
        if (diplomaticMessage == null) return;

        diplomaticMessage.setArg1(arg1);
        diplomaticMessage.setArg2(arg2);
        diplomaticMessage.setArg3(arg3);
    }


    private int countSymbol(String string, char c) {
        int counter = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) != c) continue;
            counter++;
        }
        return counter;
    }


    private String getSplitPart(String[] split, int index) {
        if (index >= split.length) return "";

        return split[index];
    }


    private void applyCooldowns() {
        if (cooldowns == null) return;

        for (String s : cooldowns.split(",")) {
            applySingleCooldown(s);
        }
    }


    private void applySingleCooldown(String s) {
        String[] split = s.split(" ");

        if (split.length == 0) return;

        int type = Integer.valueOf(split[0]);
        int counter = Integer.valueOf(split[1]);
        int fraction1 = Integer.valueOf(split[2]);
        int fraction2 = Integer.valueOf(split[3]);

        DiplomaticEntity entity1 = diplomacyManager.getEntity(fraction1);
        DiplomaticEntity entity2 = diplomacyManager.getEntity(fraction2);
        if (entity1 == null || entity2 == null) return;

        diplomacyManager.addCooldown(type, counter, entity1, entity2);
    }


    private void applyContracts() {
        for (String s : contracts.split(",")) {
            applySingleContract(s);
        }
    }


    private void applySingleContract(String s) {
        String[] split = s.split(" ");

        if (split.length == 0) return;

        int type = Integer.valueOf(split[0]);
        int fraction1 = Integer.valueOf(split[1]);
        int fraction2 = Integer.valueOf(split[2]);
        int dotations = Integer.valueOf(split[3]);
        int expire = Integer.valueOf(split[4]);

        DiplomaticEntity entity1 = diplomacyManager.getEntity(fraction1);
        DiplomaticEntity entity2 = diplomacyManager.getEntity(fraction2);
        if (entity1 == null || entity2 == null) return;

        DiplomaticContract contract = diplomacyManager.findContract(type, entity1, entity2);

        if (contract == null) {
            contract = diplomacyManager.addContract(type, entity1, entity2);
        }

        // Стороны восстанавливаются в том же порядке, в каком были записаны.
        //
        // Оригинал полагался на то, что addContract кладёт их наоборот, и
        // компенсировал это сменой знака дотаций. По смыслу это одно и то же
        // (getDotationsFromEntityPerspective симметричен), но строка при
        // каждом цикле сохранения зеркалилась, и сравнить два сейва побайтово
        // было нельзя. Для приёмки этапа 1 это обязательное свойство.
        contract.setOne(entity1);
        contract.setTwo(entity2);

        contract.setDotations(dotations);
        contract.setExpireCountDown(expire);
    }


    private void applyFull() {
        String payload = stripVersionPrefix(full);

        String[] split = payload.split("#");

        relations = null;
        contracts = null;
        cooldowns = null;
        messages = null;
        debts = null;

        // Секции читаются по одной, с выходом на первой недостающей: так
        // формат остаётся совместимым и вперёд, и назад. Старый сейв без
        // префикса просто не содержит хвостовых секций, и они остаются null.
        if (split.length < 1) return;
        relations = split[0];

        if (split.length < 2) return;
        contracts = split[1];

        if (split.length < 3) return;
        cooldowns = split[2];

        if (split.length < 4) return;
        messages = split[3];

        if (split.length < 5) return;
        debts = split[4];
    }


    /**
     * Снимает префикс версии и запоминает, какая версия пришла.
     *
     * Сейвы до мода префикса не имеют — они читаются как версия 1.
     */
    private String stripVersionPrefix(String source) {
        if (source == null) {
            loadedVersion = VERSION_LEGACY;
            return "";
        }

        if (source.startsWith(VERSION_PREFIX)) {
            loadedVersion = VERSION_CURRENT;
            return source.substring(VERSION_PREFIX.length());
        }

        loadedVersion = VERSION_LEGACY;
        return source;
    }


    /**
     * Версия последней прочитанной строки. Нужна загрузчику, чтобы понимать,
     * каким полям подставлять умолчания.
     */
    public int getLoadedVersion() {
        return loadedVersion;
    }


    private void updateFull() {
        // Новые секции дописываются в конец: загрузчик читает их по одной и
        // выходит на первой недостающей, поэтому старые сейвы не ломаются.
        full = VERSION_PREFIX +
                relations + "#" +
                contracts + "#" +
                cooldowns + "#" +
                messages + "#" +
                debts + "#";
    }


    void updateRelations() {
        ArrayList<DiplomaticEntity> entities = diplomacyManager.entities;

        builder.setLength(0);

        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                String singleRelationCode = getSingleRelationCode(entities.get(i), entities.get(j));
                builder.append(singleRelationCode).append(",");
            }
        }

        relations = builder.toString();
    }


    String getSingleRelationCode(DiplomaticEntity one, DiplomaticEntity two) {
        return one.fraction + " " + two.fraction + " " + one.getRelation(two);
    }


    void applyRelations() {
        boolean lockBackup = GameRules.diplomaticRelationsLocked;
        GameRules.diplomaticRelationsLocked = false;

        for (String token : relations.split(",")) {
            applySingleRelation(token);
        }

        GameRules.diplomaticRelationsLocked = lockBackup;
    }


    void applySingleRelation(String token) {
        String[] split = token.split(" ");

        int fraction1 = Integer.valueOf(split[0]);
        int fraction2 = Integer.valueOf(split[1]);
        int relation = Integer.valueOf(split[2]);

        DiplomaticEntity entity1 = diplomacyManager.getEntity(fraction1);
        DiplomaticEntity entity2 = diplomacyManager.getEntity(fraction2);
        diplomacyManager.setRelation(entity1, entity2, relation);
    }


    public String getFull() {
        return full;
    }


    public void setFull(String full) {
        this.full = full;
    }
}
