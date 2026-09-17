package yio.tro.antiyoy.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import yio.tro.antiyoy.PlatformType;
import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.ai.Difficulty;
import yio.tro.antiyoy.gameplay.LevelSize;
import yio.tro.antiyoy.gameplay.sim.SimConfig;

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
            }
        }
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
    }
}
