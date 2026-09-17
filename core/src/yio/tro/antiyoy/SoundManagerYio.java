package yio.tro.antiyoy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.audio.Sound;


public class SoundManagerYio {

    public static Sound soundPressButton;
    public static Sound soundSelectUnit;
    public static Sound soundAttack;
    public static Sound soundCoin;
    public static Sound soundBuild;
    public static Sound soundWalk;
    public static Sound soundEndTurn;
    public static Sound soundHoldToMarch;
    public static Sound soundKeyboardPress;


    public static void loadAllSounds() {
        soundPressButton = loadSound("menu_button");
        soundSelectUnit = loadSound("select_unit");
        soundAttack = loadSound("attack");
        soundCoin = loadSound("coin");
        soundBuild = loadSound("build");
        soundWalk = loadSound("walk");
        soundEndTurn = loadSound("end_turn");
        soundHoldToMarch = loadSound("hold_to_march");
        soundKeyboardPress = loadSound("kb_press");
    }


    /**
     * Звук грузится терпимо к отсутствию файла.
     *
     * До этого отсутствующий файл ронял generalInitialization, а вместе с ним
     * и весь запуск: игра застревала на заставке. Для веба это была не
     * гипотетическая ситуация — там нет ни одного mp3, которые просит ветка
     * iOS.
     */
    private static Sound loadSound(String name) {
        FileHandle fileHandle = getSoundFile(name);
        if (fileHandle == null) return null;

        try {
            return Gdx.audio.newSound(fileHandle);
        } catch (Exception exception) {
            System.out.println("Не удалось загрузить звук: " + fileHandle.path());
            return null;
        }
    }


    /**
     * Ищет файл в предпочтительном для платформы формате, а при его
     * отсутствии откатывается на второй.
     */
    private static FileHandle getSoundFile(String name) {
        FileHandle preferred = Gdx.files.internal("sound/" + name + getExtention());
        if (preferred.exists()) return preferred;

        FileHandle fallback = Gdx.files.internal("sound/" + name + getFallbackExtention());
        if (fallback.exists()) return fallback;

        System.out.println("Звук не найден ни в одном формате: " + name);
        return null;
    }


    private static String getFallbackExtention() {
        if (getExtention().equals(".mp3")) return ".ogg";

        return ".mp3";
    }


    private static String getExtention() {
        if (YioGdxGame.platformType == PlatformType.ios) {
            return ".mp3";
        }

        return ".ogg";
    }


    public static void playSound(Sound sound) {
        if (!SettingsManager.soundEnabled) return;

        // Звук может не загрузиться: в вебе часть форматов не поддерживается,
        // а файла нужного формата может и не оказаться. Молчание допустимо,
        // падение — нет.
        if (sound == null) return;

        sound.play();
    }
}
