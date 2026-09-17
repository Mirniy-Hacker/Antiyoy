package yio.tro.antiyoy.menu.scenes;

import yio.tro.antiyoy.SettingsManager;
import yio.tro.antiyoy.gameplay.rules.GameRules;
import yio.tro.antiyoy.menu.Animation;
import yio.tro.antiyoy.menu.ButtonYio;
import yio.tro.antiyoy.menu.CheckButtonYio;
import yio.tro.antiyoy.menu.MenuControllerYio;
import yio.tro.antiyoy.menu.behaviors.Reaction;

/**
 * Включение механик мода. Спека, введение: старое поведение обязано
 * оставаться доступным, поэтому каждая механика — отдельный флаг, а не
 * один общий переключатель.
 *
 * Флаги в GameRules заведены раздельно для обычного режима и slay, но здесь
 * на механику приходится одна строка и ставятся сразу оба: два десятка
 * переключателей на телефоне не читаются, а разводить их по режимам нужно
 * прогонам, а не игроку.
 */
public class SceneModOptions extends AbstractScene {

    private static final int BASE_ID = 6000;

    /** Высота строки: десять механик обязаны поместиться на один экран. */
    private static final double ROW_HEIGHT = 0.062;

    private ButtonYio label;
    private ButtonYio switchAllButton;
    private CheckButtonYio checks[];


    public SceneModOptions(MenuControllerYio menuControllerYio) {
        super(menuControllerYio);

        checks = null;
    }


    @Override
    public void create() {
        menuControllerYio.beginMenuCreation();
        menuControllerYio.getYioGdxGame().beginBackgroundChange(2, true, true);
        menuControllerYio.spawnBackButton(BASE_ID, getBackReaction());

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


    private void createLabel() {
        label = buttonFactory.getButton(generateRectangle(0.05, 0.1, 0.9, 0.76), BASE_ID + 1, " ");
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

        checks = new CheckButtonYio[GameRules.MOD_FLAG_KEYS.length];

        for (int i = 0; i < checks.length; i++) {
            CheckButtonYio check = CheckButtonYio.getFreshCheckButton(menuControllerYio);
            check.setParent(label);
            check.setHeight(ROW_HEIGHT);

            if (i == 0) {
                check.alignTop(0.03);
            } else {
                check.alignUnderPreviousElement();
            }

            check.setTitle(GameRules.MOD_FLAG_KEYS[i]);
            check.centerHorizontal(0.05);

            checks[i] = check;
        }
    }


    /**
     * Включить или выключить всё разом: обычно игрок хочет либо мод, либо
     * исходную игру, а не выборку из десяти галочек.
     */
    private void createSwitchAllButton() {
        switchAllButton = buttonFactory.getButton(generateRectangle(0.15, 0.13, 0.7, 0.07), BASE_ID + 2, getString("mod_switch_all"));
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
            checks[i].setChecked(allFlags[i][GameRules.MODE_GENERIC]);
        }
    }


    private void applyValues() {
        boolean allFlags[][] = GameRules.getAllModFlags();

        for (int i = 0; i < checks.length; i++) {
            allFlags[i][GameRules.MODE_GENERIC] = checks[i].isChecked();
            allFlags[i][GameRules.MODE_SLAY] = checks[i].isChecked();
        }
    }
}
