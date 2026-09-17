package yio.tro.antiyoy.gameplay.sim;

/**
 * Итог одного автоматча. Одна строка CSV.
 */
public class SimMatchResult {

    public int matchIndex;
    public long seed;
    public int winner;
    public boolean timedOut;
    public int turns;
    public int warsDeclared;
    public int contractsSigned;
    public int friendshipsBroken;
    public int unitsProduced;
    public int unitsDied;
    public int secededRegions;
    public int maxMoney[];
    public double averageLeaderMapShare;

    /** Контрольные суммы конечного состояния. Приёмка этапа 1. */
    public long fieldHash;
    public long diplomacyHash;


    public SimMatchResult(int fractionsQuantity) {
        maxMoney = new int[fractionsQuantity];
        winner = -1;
        timedOut = false;
        secededRegions = 0;
        averageLeaderMapShare = 0;
    }


    public static String getCsvHeader(int fractionsQuantity) {
        StringBuilder builder = new StringBuilder();

        builder.append("match,seed,winner,timed_out,turns,wars_declared,contracts_signed,")
                .append("friendships_broken,units_produced,units_died,seceded_regions,")
                .append("avg_leader_map_share,field_hash,diplomacy_hash");

        for (int i = 0; i < fractionsQuantity; i++) {
            builder.append(",max_money_").append(i);
        }

        return builder.toString();
    }


    /**
     * Своё форматирование вместо String.format: GWT его не поддерживает, а
     * веб выбран основным путём доставки. Заодно не зависит от локали —
     * в CSV разделителем дробной части обязана быть точка.
     */
    private static String formatShare(double value) {
        long scaled = Math.round(value * 10000d);

        long whole = scaled / 10000;
        long fraction = scaled % 10000;

        if (fraction < 0) {
            fraction = -fraction;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(whole).append('.');

        if (fraction < 1000) builder.append('0');
        if (fraction < 100) builder.append('0');
        if (fraction < 10) builder.append('0');

        builder.append(fraction);

        return builder.toString();
    }


    public String toCsvRow() {
        StringBuilder builder = new StringBuilder();

        builder.append(matchIndex).append(',')
                .append(seed).append(',')
                .append(winner).append(',')
                .append(timedOut ? 1 : 0).append(',')
                .append(turns).append(',')
                .append(warsDeclared).append(',')
                .append(contractsSigned).append(',')
                .append(friendshipsBroken).append(',')
                .append(unitsProduced).append(',')
                .append(unitsDied).append(',')
                .append(secededRegions).append(',')
                .append(formatShare(averageLeaderMapShare)).append(',')
                .append(fieldHash).append(',')
                .append(diplomacyHash);

        for (int money : maxMoney) {
            builder.append(',').append(money);
        }

        return builder.toString();
    }
}
