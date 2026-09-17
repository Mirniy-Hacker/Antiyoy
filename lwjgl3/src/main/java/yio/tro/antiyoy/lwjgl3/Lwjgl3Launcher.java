package yio.tro.antiyoy.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import yio.tro.antiyoy.PlatformType;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.ai.Difficulty;
import yio.tro.antiyoy.gameplay.LevelSize;
import yio.tro.antiyoy.gameplay.rules.EconomyTuning;
import yio.tro.antiyoy.gameplay.rules.GameRules;
import yio.tro.antiyoy.gameplay.sim.SimConfig;
import yio.tro.antiyoy.gameplay.statehood.StatehoodTuning;

/**
 * Десктопная точка входа. Оригинал собирался через сгенерированный
 * DesktopLauncher, которого в репозитории yiotro/Antiyoy нет.
 */
public class Lwjgl3Launcher {

    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 960;


    public static void main(String[] args) {
        YioGdxGame.platformType = PlatformType.pc;

        parseArguments(args);

        new Lwjgl3Application(new YioGdxGame(), createConfiguration());

        // Lwjgl3Application возвращает управление после Gdx.app.exit().
        // Проверки обязаны валить процесс, иначе они бесполезны в CI.
        SimConfig config = SimConfig.getInstance();
        if (config.selfTest || config.replayCheckMatches > 0) {
            System.exit(YioGdxGame.getSimulationExitCode());
        }
    }


    private static Lwjgl3ApplicationConfiguration createConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        configuration.setTitle("Antiyoy Mod");
        configuration.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);

        if (SimConfig.getInstance().enabled) {
            // Окно нужно только ради GL-контекста: ядро игры завязано на
            // Scenes и Fonts, поэтому под HeadlessApplication оно не поднимется.
            // Само окно не показывается, а кадры не ограничиваются частотой.
            configuration.setInitialVisible(false);
            configuration.useVsync(false);
            configuration.setForegroundFPS(0);
            configuration.setIdleFPS(0);
        } else {
            configuration.useVsync(true);
            configuration.setForegroundFPS(60);
        }

        return configuration;
    }


    private static void parseArguments(String args[]) {
        SimConfig config = SimConfig.getInstance();

        for (int i = 0; i < args.length; i++) {
            String key = args[i];

            if (key.equals("--help") || key.equals("-h")) {
                showUsage();
                System.exit(0);
            }

            // Ключи без значения обрабатываются до проверки на следующий аргумент.
            if (key.equals("--selftest")) {
                config.enabled = true;
                config.selfTest = true;
                continue;
            }

            String value = (i + 1 < args.length) ? args[i + 1] : null;
            if (value == null) continue;

            if (key.equals("--sim")) {
                config.enabled = true;
                config.matches = Integer.parseInt(value);
                i++;
            } else if (key.equals("--seed")) {
                config.seed = Long.parseLong(value);
                i++;
            } else if (key.equals("--fractions")) {
                config.fractionsQuantity = Integer.parseInt(value);
                i++;
            } else if (key.equals("--level-size")) {
                config.levelSize = parseLevelSize(value);
                i++;
            } else if (key.equals("--difficulty")) {
                config.difficulty = parseDifficulty(value);
                i++;
            } else if (key.equals("--slay")) {
                config.slayRules = Boolean.parseBoolean(value);
                i++;
            } else if (key.equals("--diplomacy")) {
                config.diplomacy = Boolean.parseBoolean(value);
                i++;
            } else if (key.equals("--max-turns")) {
                config.maxTurns = Integer.parseInt(value);
                i++;
            } else if (key.equals("--out")) {
                config.outputPath = value;
                i++;
            } else if (key.equals("--screenshot")) {
                config.screenshotPath = value;
                if (config.playtestTurns == 0) config.playtestTurns = 12;
                i++;
            } else if (key.equals("--scene")) {
                config.startScene = value;
                i++;
            } else if (key.equals("--playtest-turns")) {
                config.playtestTurns = Integer.parseInt(value);
                i++;
            } else if (key.equals("--trace")) {
                config.tracePath = value;
                i++;
            } else if (key.equals("--mod")) {
                applyModFlags(value);
                i++;
            } else if (key.equals("--tune")) {
                applyTuning(value);
                i++;
            } else if (key.equals("--replay-check")) {
                config.enabled = true;
                config.replayCheckMatches = Integer.parseInt(value);
                i++;
            }
        }
    }


    /**
     * Включает механики мода для прогона: "all" или список ключей через
     * запятую. Флаги ставятся сразу обоим режимам, обычному и slay, потому
     * что режим прогона задаётся отдельно через --slay.
     *
     * GameRules.defaultValues() флаги мода не трогает, поэтому выставить их
     * один раз при старте достаточно.
     */
    private static void applyModFlags(String value) {
        GameRules.defaultModFlags();
        SimConfig.getInstance().modFlagsOverridden = true;

        if (value.equalsIgnoreCase("all")) {
            GameRules.setAllModFlags(true);
            return;
        }

        boolean allFlags[][] = GameRules.getAllModFlags();

        for (String name : value.split(",")) {
            String trimmed = name.trim();
            boolean found = false;

            for (int i = 0; i < GameRules.MOD_FLAG_KEYS.length; i++) {
                if (!GameRules.MOD_FLAG_KEYS[i].equals(trimmed)) continue;

                allFlags[i][GameRules.MODE_GENERIC] = true;
                allFlags[i][GameRules.MODE_SLAY] = true;
                found = true;
                break;
            }

            if (!found) {
                System.out.println("Неизвестный флаг мода: " + trimmed);
                System.out.println("Доступные: " + String.join(", ", GameRules.MOD_FLAG_KEYS));
                System.exit(2);
            }
        }
    }


    /**
     * Переопределение чисел настройки: key=value через запятую. Нужно, чтобы
     * подбирать баланс прогонами, а не пересборками.
     */
    private static void applyTuning(String value) {
        for (String pair : value.split(",")) {
            String parts[] = pair.trim().split("=");

            if (parts.length != 2) {
                System.out.println("Ожидается key=value, получено: " + pair);
                System.exit(2);
            }

            String name = parts[0].trim();
            float number = Float.parseFloat(parts[1].trim());

            if (EconomyTuning.setByName(name, number)) continue;
            if (StatehoodTuning.setByName(name, number)) continue;

            System.out.println("Неизвестное число настройки: " + name);
            System.exit(2);
        }

        System.out.println("Tuning: " + EconomyTuning.describe());
    }


    private static int parseLevelSize(String value) {
        if (value.equalsIgnoreCase("small")) return LevelSize.SMALL;
        if (value.equalsIgnoreCase("medium")) return LevelSize.MEDIUM;
        if (value.equalsIgnoreCase("big")) return LevelSize.BIG;
        if (value.equalsIgnoreCase("huge")) return LevelSize.HUGE;

        return Integer.parseInt(value);
    }


    private static int parseDifficulty(String value) {
        if (value.equalsIgnoreCase("easy")) return Difficulty.EASY;
        if (value.equalsIgnoreCase("normal")) return Difficulty.NORMAL;
        if (value.equalsIgnoreCase("hard")) return Difficulty.HARD;
        if (value.equalsIgnoreCase("expert")) return Difficulty.EXPERT;
        if (value.equalsIgnoreCase("balancer")) return Difficulty.BALANCER;
        if (value.equalsIgnoreCase("master")) return Difficulty.MASTER;

        return Integer.parseInt(value);
    }


    private static void showUsage() {
        System.out.println("Antiyoy Mod");
        System.out.println();
        System.out.println("Без аргументов запускается обычная игра.");
        System.out.println();
        System.out.println("Режим автоматчей:");
        System.out.println("  --sim N            число партий ИИ против ИИ");
        System.out.println("  --seed S           базовый сид прогона");
        System.out.println("  --fractions N      число фракций (по умолчанию 5)");
        System.out.println("  --level-size S     small|medium|big|huge");
        System.out.println("  --difficulty D     easy|normal|hard|expert|balancer|master");
        System.out.println("  --slay B           true|false");
        System.out.println("  --diplomacy B      true|false");
        System.out.println("  --max-turns N      потолок ходов, дальше партия считается зависшей");
        System.out.println("  --out PATH         путь к CSV (по умолчанию sim-results/sim.csv)");
        System.out.println("  --trace PATH       по-ходовой след состояния; diff двух следов");
        System.out.println("  --mod LIST         включить механики мода: all или ключи через запятую");
        System.out.println("                     показывает ход, на котором прогоны разошлись");
        System.out.println();
        System.out.println("Снимок экрана:");
        System.out.println("  --screenshot PATH  разыграть партию и сохранить кадр в PNG");
        System.out.println("  --playtest-turns N сколько ходов разыграть до снимка");
        System.out.println("  --scene NAME       открыть экран меню: mod|settings");
        System.out.println();
        System.out.println("Самопроверка:");
        System.out.println("  --selftest         сериализация и флаги; ненулевой код возврата при провале");
        System.out.println("  --replay-check N   проверка воспроизводимости реплеев на N партиях");
    }
}
