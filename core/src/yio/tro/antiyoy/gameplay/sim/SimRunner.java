package yio.tro.antiyoy.gameplay.sim;

import yio.tro.antiyoy.YioGdxGame;
import yio.tro.antiyoy.gameplay.DebugFlags;
import yio.tro.antiyoy.gameplay.GameController;
import yio.tro.antiyoy.gameplay.Hex;
import yio.tro.antiyoy.gameplay.MatchStatistics;
import yio.tro.antiyoy.gameplay.Province;
import yio.tro.antiyoy.gameplay.loading.LoadingManager;
import yio.tro.antiyoy.gameplay.loading.LoadingParameters;
import yio.tro.antiyoy.gameplay.loading.LoadingType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Режим автоматчей: N партий ИИ против ИИ без отрисовки, результат в CSV.
 *
 * Отрисовки нет за счёт DebugFlags.testMode — в нём GameView и
 * RenderBackgroundCache выходят сразу, а конец хода не ждёт анимаций.
 * Поэтому один вызов GameController.move() соответствует примерно одному ходу.
 */
public class SimRunner {

    private final YioGdxGame yioGdxGame;
    private final GameController gameController;
    private final SimConfig config;
    private final ArrayList<SimMatchResult> results;

    private int maxMoney[];
    private double leaderShareSum;
    private int leaderShareSamples;
    private int lastSampledTurn;


    public SimRunner(YioGdxGame yioGdxGame) {
        this.yioGdxGame = yioGdxGame;
        this.gameController = yioGdxGame.gameController;
        this.config = SimConfig.getInstance();

        results = new ArrayList<SimMatchResult>();
    }


    public void perform() {
        long startTime = System.currentTimeMillis();

        System.out.println("Simulation started: " + config.describe());

        DebugFlags.testMode = true;
        yioGdxGame.gamePaused = false;

        for (int i = 0; i < config.matches; i++) {
            showProgress(i);
            results.add(runSingleMatch(i));
        }

        DebugFlags.testMode = false;

        writeCsv();
        showSummary(System.currentTimeMillis() - startTime);
    }


    private void showProgress(int matchIndex) {
        if (matchIndex == 0) return;
        if (matchIndex % 10 != 0) return;

        System.out.println("  match " + matchIndex + " / " + config.matches);
    }


    private SimMatchResult runSingleMatch(int matchIndex) {
        long matchSeed = config.getMatchSeed(matchIndex);

        seedRandomSources(matchSeed);
        launchMatch(matchSeed);
        prepareAi();
        resetSampling();

        SimMatchResult result = new SimMatchResult(config.fractionsQuantity);
        result.matchIndex = matchIndex;
        result.seed = matchSeed;

        simulate(result);
        fillStatistics(result);

        return result;
    }


    /**
     * Сидируются все три источника случайности сразу.
     *
     * gameController.random используется генератором карты и в оригинале
     * никогда не сеялся. predictableRandom перезаписывается внутри
     * LoadingManager.beginCreation из campaignLevelIndex, поэтому сид туда
     * передаётся ещё и через параметры загрузки. YioGdxGame.random
     * статический, и именно им пользуются ИИ и дипломатия.
     */
    private void seedRandomSources(long matchSeed) {
        gameController.random.setSeed(matchSeed);
        gameController.predictableRandom.setSeed(matchSeed);
        YioGdxGame.random.setSeed(matchSeed);
    }


    private void launchMatch(long matchSeed) {
        LoadingParameters parameters = LoadingParameters.getInstance();

        parameters.loadingType = LoadingType.skirmish;
        parameters.levelSize = config.levelSize;
        parameters.playersNumber = 0; // включает aiOnlyMode
        parameters.fractionsQuantity = config.fractionsQuantity;
        parameters.difficulty = config.difficulty;
        parameters.colorOffset = 0;
        parameters.slayRules = config.slayRules;
        parameters.fogOfWar = false;
        parameters.diplomacy = config.diplomacy;
        parameters.genProvinces = 0;
        parameters.treesPercentageIndex = 2;
        parameters.campaignLevelIndex = (int) matchSeed;

        LoadingManager.getInstance().startGame(parameters);
    }


    private void prepareAi() {
        int difficulties[] = new int[config.fractionsQuantity];
        for (int i = 0; i < difficulties.length; i++) {
            difficulties[i] = config.difficulty;
        }

        gameController.aiFactory.createCustomAiList(difficulties);
    }


    private void resetSampling() {
        maxMoney = new int[config.fractionsQuantity];
        leaderShareSum = 0;
        leaderShareSamples = 0;
        lastSampledTurn = -1;
    }


    private void simulate(SimMatchResult result) {
        DebugFlags.testWinner = -1;

        // Страховка от партии, которая вообще не двигает ходы.
        int moveLimit = config.maxTurns * 20;

        while (DebugFlags.testWinner == -1) {
            gameController.move();

            checkToSample();

            if (gameController.matchStatistics.turnsMade >= config.maxTurns) {
                result.timedOut = true;
                break;
            }

            moveLimit--;
            if (moveLimit <= 0) {
                result.timedOut = true;
                break;
            }
        }

        result.winner = result.timedOut ? -1 : DebugFlags.testWinner;
    }


    /**
     * Замеры делаются раз в ход, а не раз в кадр: обходить все гексы чаще
     * незачем.
     */
    private void checkToSample() {
        int turnsMade = gameController.matchStatistics.turnsMade;
        if (turnsMade == lastSampledTurn) return;

        lastSampledTurn = turnsMade;

        sampleMoney();
        sampleLeaderMapShare();
    }


    private void sampleMoney() {
        int moneyByFraction[] = new int[config.fractionsQuantity];

        for (Province province : gameController.fieldManager.provinces) {
            int fraction = province.getFraction();
            if (fraction < 0 || fraction >= moneyByFraction.length) continue;

            moneyByFraction[fraction] += province.money;
        }

        for (int i = 0; i < moneyByFraction.length; i++) {
            if (moneyByFraction[i] > maxMoney[i]) {
                maxMoney[i] = moneyByFraction[i];
            }
        }
    }


    private void sampleLeaderMapShare() {
        int hexesByFraction[] = new int[config.fractionsQuantity];
        int total = 0;

        for (Hex hex : gameController.fieldManager.activeHexes) {
            if (hex.isNeutral()) continue;

            total++;

            int fraction = hex.fraction;
            if (fraction < 0 || fraction >= hexesByFraction.length) continue;

            hexesByFraction[fraction]++;
        }

        if (total == 0) return;

        int leader = 0;
        for (int count : hexesByFraction) {
            if (count > leader) {
                leader = count;
            }
        }

        leaderShareSum += (double) leader / (double) total;
        leaderShareSamples++;
    }


    private void fillStatistics(SimMatchResult result) {
        MatchStatistics statistics = gameController.matchStatistics;

        result.turns = statistics.turnsMade;
        result.warsDeclared = statistics.warsDeclared;
        result.contractsSigned = statistics.contractsSigned;
        result.friendshipsBroken = statistics.friendshipsBroken;
        result.unitsProduced = statistics.unitsProduced;
        result.unitsDied = statistics.unitsDied;
        result.maxMoney = maxMoney;

        if (leaderShareSamples > 0) {
            result.averageLeaderMapShare = leaderShareSum / leaderShareSamples;
        }
    }


    private void writeCsv() {
        File file = new File(config.outputPath);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

            writer.write(SimMatchResult.getCsvHeader(config.fractionsQuantity));
            writer.write("\n");

            for (SimMatchResult result : results) {
                writer.write(result.toCsvRow());
                writer.write("\n");
            }
        } catch (Exception exception) {
            System.out.println("SimRunner: failed to write CSV");
            exception.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }

        System.out.println("CSV written: " + file.getAbsolutePath());
    }


    private void showSummary(long elapsedMillis) {
        int finished = 0;
        int timedOut = 0;
        int totalTurns = 0;

        for (SimMatchResult result : results) {
            if (result.timedOut) {
                timedOut++;
            } else {
                finished++;
            }
            totalTurns += result.turns;
        }

        System.out.println();
        System.out.println("Simulation finished in " + (elapsedMillis / 1000.0) + " s");
        System.out.println("  matches:   " + results.size());
        System.out.println("  finished:  " + finished);
        System.out.println("  timed out: " + timedOut);

        if (results.size() > 0) {
            System.out.println("  avg turns: " + (totalTurns / results.size()));
        }
    }
}
