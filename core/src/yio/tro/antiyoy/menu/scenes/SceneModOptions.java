package yio.tro.antiyoy.menu.scenes;

import yio.tro.antiyoy.SettingsManager;
import yio.tro.antiyoy.gameplay.rules.GameRules;
import yio.tro.antiyoy.menu.Animation;
import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.menu.CheckButtonYio;
import yio.tro.antiyoy.menu.MenuControllerYio;
import yio.tro.antiyoy.menu.behaviors.Reaction;

import java.util.ArrayList;

/**
 * Включение механик мода. Спека, введение: старое поведение обязано
 * оставаться доступным, поэтому каждая механика — отдельный флаг, а не
 * один общий переключатель.
 *
 * Показываются только готовые механики: остальные ключи заведены заранее и
 * пока ничего не меняют, см. GameRules.MOD_FLAG_READY. Переключатель без
 * последствий хуже его отсутствия.
 *
 * Флаги в GameRules заведены раздельно для обычного режима и slay, но здесь
 * на механику приходится одна строка и ставятся сразу оба: два десятка
 * переключателей на телефоне не читаются, а разводить их по режимам нужно
 * прогонам, а не игроку.
 */
public class SceneModOptions extends AbstractScene {

    private static final int BASE_ID = 6000;

    /** Высота строки: механики обязаны поместиться на один экран. */
    private static final double ROW_HEIGHT = 0.062;

    /** Верх панели, отступы внутри неё и высота кнопки. */
    private static final double TOP = 0.86;
    private static final double PADDING = 0.03;
    private static final double BUTTON_HEIGHT = 0.07;

    private ButtonYio label;
    private ButtonYio switchAllButton;
    private CheckButtonYio checks[];

    /** Номера показанных механик в GameRules.getAllModFlags(). */
    private int shownFlags[];
    private double labelY;


    public SceneModOptions(MenuControllerYio menuControllerYio) {
        super(menuControllerYio);

        checks = null;
    }


    @Override
    public void create() {
        menuControllerYio.beginMenuCreation();
        menuControllerYio.getYioGdxGame().beginBackgroundChange(2, true, true);
        menuControllerYio.spawnBackButton(BASE_ID, getBackReaction());

        shownFlags = getReadyFlags();

        createLabel();
        createChecks();
        createSwitchAllButton();

        loadValues();

        menuControllerYio.endMenuCreation();
    }


    private Reaction getBackReaction() {
        return new Reaction() {
            @Override
            public void perform(ButtonYio buttonYio) {
                applyValues();
                SettingsManager.getInstance().saveModFlags();

                Scenes.sceneSettings.create();
            }
        };
    }


    /**
     * Панель подгоняется под число строк: готовых механик пока
     * меньше, чем заведённых ключей, и пустая половина экрана
     * выглядела бы поломкой.
     */
    private void createLabel() {
        double height = 2 * PADDING + shownFlags.length * ROW_HEIGHT + BUTTON_HEIGHT + PADDING;
        labelY = TOP - height;

        label = buttonFactory.getButton(generateRectangle(0.05, labelY, 0.9, height), BASE_ID + 1, " ");
        label.setTouchable(false);
        label.setAnimation(Animation.fixed_up);
    }


    private void createChecks() {
        initChecks();

        for (CheckButtonYio check : checks) {
            check.appear();
        }
    }


    private void initChecks() {
        if (checks != null) return;

        checks = new CheckButtonYio[shownFlags.length];

        for (int i = 0; i < checks.length; i++) {
            CheckButtonYio check = CheckButtonYio.getFreshCheckButton(menuControllerYio);
            check.setParent(label);
            check.setHeight(ROW_HEIGHT);

            if (i == 0) {
                check.alignTop(PADDING);
            } else {
                check.alignUnderPreviousElement();
            }

            check.setTitle(GameRules.MOD_FLAG_KEYS[shownFlags[i]]);
            check.centerHorizontal(0.05);

            checks[i] = check;
        }
    }


    private int[] getReadyFlags() {
        ArrayList<Integer> ready = new ArrayList<Integer>();

        for (int i = 0; i < GameRules.MOD_FLAG_KEYS.length; i++) {
            if (!GameRules.MOD_FLAG_READY[i]) continue;

            ready.add(i);
        }

        int result[] = new int[ready.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ready.get(i);
        }

        return result;
    }


    /**
     * Включить или выключить всё разом: обычно игрок хочет либо мод, либо
     * исходную игру, а не выборку из галочек.
     */
    private void createSwitchAllButton() {
        switchAllButton = buttonFactory.getButton(generateRectangle(0.15, labelY + PADDING, 0.7, BUTTON_HEIGHT), BASE_ID + 2, getString("mod_switch_all"));
        switchAllButton.setReaction(getSwitchAllReaction());
        switchAllButton.setAnimation(Animation.down);
    }


    private Reaction getSwitchAllReaction() {
        return new Reaction() {
            @Override
            public void perform(ButtonYio buttonYio) {
                boolean value = !isEverythingChecked();

                for (CheckButtonYio check : checks) {
                    check.setChecked(value);
                }
            }
        };
    }


    private boolean isEverythingChecked() {
        for (CheckButtonYio check : checks) {
            if (!check.isChecked()) return false;
        }

        return true;
    }


    private void loadValues() {
        boolean allFlags[][] = GameRules.getAllModFlags();

        for (int i = 0; i < checks.length; i++) {
            checks[i].setChecked(allFlags[shownFlags[i]][GameRules.MODE_GENERIC]);
        }
    }


    private void applyValues() {
        boolean allFlags[][] = GameRules.getAllModFlags();

        for (int i = 0; i < checks.length; i++) {
            allFlags[shownFlags[i]][GameRules.MODE_GENERIC] = checks[i].isChecked();
            allFlags[shownFlags[i]][GameRules.MODE_SLAY] = checks[i].isChecked();
        }
    }
}
