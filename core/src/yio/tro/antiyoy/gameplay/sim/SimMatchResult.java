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
                .append("avg_leader_map_share");

        for (int i = 0; i < fractionsQuantity; i++) {
            builder.append(",max_money_").append(i);
        }

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
                .append(String.format(java.util.Locale.US, "%.4f", averageLeaderMapShare));

        for (int money : maxMoney) {
            builder.append(',').append(money);
        }

        return builder.toString();
    }
}
