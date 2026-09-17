package yio.tro.antiyoy.gameplay;

import yio.tro.antiyoy.gameplay.rules.GameRules;

import java.util.ArrayList;

public class ColorsManager {

    /**
     * Сколько в игре различимых цветов гексов. Это число текстур, а не
     * предел числа государств: сверх палитры цвета начинают повторяться, и
     * совпавшие различаются штриховкой (спека, 1.2).
     */
    public static final int PALETTE_SIZE = 11;

    public static final int HATCHING_NONE = 0;
    public static final int HATCHING_DIAGONAL = 1;
    public static final int HATCHING_DOTTED = 2;

    public static final int HATCHING_VARIANTS = 3;

    /** Цвета, доступные государствам: вся палитра, кроме нейтрального. */
    public static final int NON_NEUTRAL_COLORS = PALETTE_SIZE - 1;

    /**
     * Сколько владельцев различимы на глаз: палитра без штриховки плюс по
     * набору цветов на каждый непустой вариант штриховки.
     *
     * Дальше пары начинают повторяться — это предел метода, а не ошибка.
     * Спека допускает совпадение цветов, требуя лишь различать их
     * штриховкой, пока она не исчерпана.
     */
    public static final int DISTINGUISHABLE_OWNERS =
            PALETTE_SIZE + NON_NEUTRAL_COLORS * (HATCHING_VARIANTS - 1);

    GameController gameController;
    public int colorOffset;


    public ColorsManager(GameController gameController) {
        this.gameController = gameController;
    }


    public int getColorByFraction(int fraction) {
        if (GameRules.inEditorMode) return fraction;
        if (fraction == GameRules.NEUTRAL_FRACTION) return fraction;
        if (colorOffset == 0) return fraction;

        if (colorOffset <= GameRules.NEUTRAL_FRACTION && GameRules.fractionsQuantity <= GameRules.NEUTRAL_FRACTION) {
            return getLegacyColorByFraction(fraction);
        }

        fraction += colorOffset;

        fraction = getLimitedByMaxFractionsValue(fraction);
        if (fraction == GameRules.NEUTRAL_FRACTION) {
            return getExcludedByNeutralColor();
        }

        fraction = getLimitedByMaxFractionsValue(fraction);

        return fraction;
    }


    /**
     * Цвет владельца. Для владельцев в пределах палитры возвращает ровно то
     * же, что и раньше, — поведение старых партий не меняется.
     *
     * Сверх палитры цвета идут по кругу: число государств больше не
     * ограничено числом текстур (спека, часть I).
     */
    public int getColorByOwner(int ownerId) {
        if (ownerId < 0) return 0;
        if (ownerId < PALETTE_SIZE) return getColorByFraction(ownerId);

        // Сверх палитры цвета берутся из набора без нейтрального: он занят.
        // Раньше здесь стоял сдвиг на единицу при попадании в нейтральный, и
        // он ломал главное свойство — два владельца одного круга получали
        // один цвет и одну штриховку, то есть становились неразличимы.
        int slot = (ownerId - PALETTE_SIZE) % NON_NEUTRAL_COLORS;

        if (slot < GameRules.NEUTRAL_FRACTION) return slot;

        return slot + 1;
    }


    /**
     * Штриховка поверх гекса. Нужна, когда палитра исчерпана и один цвет
     * достался двум государствам: без неё их не различить.
     *
     * Владельцы в пределах палитры штриховки не получают, поэтому до
     * появления новых государств на экране ничего не меняется.
     */
    public int getHatchingByOwner(int ownerId) {
        if (ownerId < PALETTE_SIZE) return HATCHING_NONE;

        // Владельцам сверх палитры пустой вариант не достаётся: иначе они
        // слились бы с теми, кому цвет достался без штриховки.
        int lap = (ownerId - PALETTE_SIZE) / NON_NEUTRAL_COLORS;

        return HATCHING_DIAGONAL + lap % (HATCHING_VARIANTS - 1);
    }


    private int getLimitedByMaxFractionsValue(int fraction) {
        if (fraction >= GameRules.MAX_FRACTIONS_QUANTITY) {
            fraction -= GameRules.MAX_FRACTIONS_QUANTITY;
        }
        return fraction;
    }


    private int getExcludedByNeutralColor() {
        int color = GameRules.NEUTRAL_FRACTION + colorOffset;

        color = getLimitedByMaxFractionsValue(color);

        return color;
    }


    private int getLegacyColorByFraction(int fraction) {
        fraction += colorOffset;

        if (fraction >= GameRules.NEUTRAL_FRACTION) {
            fraction -= GameRules.NEUTRAL_FRACTION;
        }

        return fraction;
    }


    public int getFractionByColor(int color) {
        for (int fraction = 0; fraction < GameRules.MAX_FRACTIONS_QUANTITY; fraction++) {
            if (getColorByFraction(fraction) != color) continue;
            return fraction;
        }

        return -1;
    }


    public void defaultValues() {
        colorOffset = 0;
    }


    public void applyEditorChosenColorFix() {
        gameController.updateRuleset();

        ArrayList<Hex> activeHexes = gameController.fieldManager.activeHexes;
        for (Hex activeHex : activeHexes) {
            if (!GameRules.slayRules && activeHex.isNeutral()) continue;

            // Это не перекраска, а переиндексация владельцев: меняются обе
            // величины разом. После этапа 1 менять здесь можно будет только
            // цвет, но пока владелец и цвет обязаны совпадать.
            activeHex.setOwnerSilently(
                    gameController.colorsManager.getFractionByColor(activeHex.getOwnerId()));
        }

        gameController.fieldManager.detectProvinces();
        gameController.stopAllUnitsFromJumping();
        gameController.prepareCertainUnitsToMove();
    }


    public void takeControlOverColor(int targetColor) {
        int targetFraction = getFractionByColor(targetColor);
        shiftColors(-targetFraction);
        setColorOffset(targetColor);
    }


    public void shiftColors(int delta) {
        for (Hex activeHex : gameController.fieldManager.activeHexes) {
            if (!GameRules.slayRules && activeHex.isNeutral()) continue;

            activeHex.setOwnerSilently(getShiftedColor(activeHex.getOwnerId(), delta));
        }
    }


    private int getShiftedColor(int color, int delta) {
        color += delta;

        if (color >= GameRules.fractionsQuantity) {
            color -= GameRules.fractionsQuantity;
        }

        if (color < 0) {
            color += GameRules.fractionsQuantity;
        }

        return color;
    }


    public void doShiftFractionsInEditorMode() {
        ArrayList<Hex> activeHexes = gameController.fieldManager.activeHexes;
        for (Hex activeHex : activeHexes) {
            if (activeHex.isNeutral()) continue;

            activeHex.fraction++;
            if (activeHex.fraction >= GameRules.MAX_FRACTIONS_QUANTITY) {
                activeHex.fraction -= GameRules.MAX_FRACTIONS_QUANTITY;
            }
        }

        gameController.yioGdxGame.gameView.updateCacheLevelTextures();
    }


    public void setColorOffset(int colorOffset) {
        this.colorOffset = colorOffset;
    }


    public void doShowInConsole() {
        System.out.println();
        System.out.println("ColorsManager.doShowInConsole");
        System.out.println("GameRules.MAX_FRACTIONS_QUANTITY = " + GameRules.MAX_FRACTIONS_QUANTITY);
        System.out.println("GameRules.NEUTRAL_FRACTION = " + GameRules.NEUTRAL_FRACTION);
        System.out.println("GameRules.fractionsQuantity = " + GameRules.fractionsQuantity);
        System.out.println("colorOffset = " + gameController.colorsManager.colorOffset);
        for (int fraction = 0; fraction < GameRules.fractionsQuantity; fraction++) {
            int colorByFraction = gameController.colorsManager.getColorByFraction(fraction);
            System.out.println(fraction + " -> " + colorByFraction);
        }
    }


    public void doShowColorInfoAboutHex(Hex hex) {
        System.out.println();
        System.out.println("ColorsManager.doShowColorInfoAboutHex");
        System.out.println("hex = " + hex);
        int colorByFraction = getColorByFraction(hex.fraction);
        System.out.println("colorByFraction = " + colorByFraction);
    }
}
