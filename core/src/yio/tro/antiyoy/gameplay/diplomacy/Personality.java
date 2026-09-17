package yio.tro.antiyoy.gameplay.diplomacy;

import java.util.Random;

/**
 * Характер дипломатической сущности. Спека, часть V.
 *
 * Черты скрыты от игрока: узнаются только по поведению, никаких ярлыков в
 * интерфейсе. В сейв пишутся id пресета и сид, а не сами числа — так они
 * восстанавливаются точно и занимают два поля вместо шести.
 */
public class Personality {

    public static final int AGGRESSION = 0;
    public static final int CAUTION = 1;
    public static final int HONESTY = 2;
    public static final int VINDICTIVENESS = 3;
    public static final int GREED = 4;
    public static final int EXPANSION = 5;

    public static final int TRAITS_QUANTITY = 6;

    private final float traits[];

    /** Из какого пресета выведен характер и с каким сидом. */
    public int presetIndex;
    public long seed;


    public Personality() {
        traits = new float[TRAITS_QUANTITY];

        presetIndex = -1;
        seed = 0;

        setNeutral();
    }


    private void setNeutral() {
        for (int i = 0; i < traits.length; i++) {
            traits[i] = 0.5f;
        }
    }


    public float get(int trait) {
        if (trait < 0 || trait >= traits.length) return 0.5f;

        return traits[trait];
    }


    public void set(int trait, float value) {
        if (trait < 0 || trait >= traits.length) return;

        traits[trait] = clamp(value);
    }


    public void add(int trait, float delta) {
        set(trait, get(trait) + delta);
    }


    private static float clamp(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;

        return value;
    }


    /**
     * Характер из пресета с разбросом. Спека, 5.1: не равномерно по всему
     * диапазону, а вокруг выраженных типажей — иначе все сущности выходят
     * одинаково серыми.
     *
     * Случайность берётся из переданного сида, а не из общего источника:
     * характер обязан восстанавливаться из сейва один в один.
     */
    public void generateFromPreset(int presetIndex, long seed) {
        this.presetIndex = presetIndex;
        this.seed = seed;

        float preset[] = DiplomacyTuning.presets[presetIndex % DiplomacyTuning.PRESETS_QUANTITY];

        Random random = new Random(seed);

        for (int i = 0; i < traits.length; i++) {
            float spread = DiplomacyTuning.personalitySpread;
            float offset = (float) ((random.nextDouble() * 2 - 1) * spread);

            traits[i] = clamp(preset[i] + offset);
        }
    }


    /**
     * Характер отделившегося государства. Спека, 3.5: считается один раз в
     * момент отделения из четырёх источников, а не бросается случайно.
     *
     * Здесь применяется только наследование от родителя; сдвиги за причину,
     * соседей и размер накладываются вызывающим кодом поверх.
     */
    public void generateFromParent(Personality parent) {
        presetIndex = parent.presetIndex;
        seed = parent.seed;

        for (int i = 0; i < traits.length; i++) {
            if (i == EXPANSION || i == GREED) {
                // Наследуются только эти две, и наполовину.
                traits[i] = clamp(parent.get(i) * yio.tro.antiyoy.gameplay.statehood.StatehoodTuning.inheritanceFactor
                        + yio.tro.antiyoy.gameplay.statehood.StatehoodTuning.inheritanceBase
                        * (1 - yio.tro.antiyoy.gameplay.statehood.StatehoodTuning.inheritanceFactor));
                continue;
            }

            traits[i] = yio.tro.antiyoy.gameplay.statehood.StatehoodTuning.inheritanceBase;
        }
    }


    public String encode() {
        return presetIndex + " " + seed;
    }


    public void decode(String source) {
        String split[] = source.split(" ");
        if (split.length < 2) return;

        int index = Integer.valueOf(split[0]);
        long value = Long.valueOf(split[1]);

        if (index < 0) {
            presetIndex = -1;
            seed = value;
            setNeutral();
            return;
        }

        generateFromPreset(index, value);
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("[Personality preset ").append(presetIndex).append(": ");
        for (int i = 0; i < traits.length; i++) {
            if (i > 0) builder.append(' ');
            builder.append((int) (traits[i] * 100));
        }
        builder.append(']');

        return builder.toString();
    }
}
