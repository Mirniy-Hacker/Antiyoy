package yio.tro.antiyoy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.audio.Music;

public class MusicManager {


    private static MusicManager instance = null;

    public Music music;


    public static void initialize() {
        instance = null;
    }


    public static MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }


    public void onMusicStatusChanged() {
        // Музыки может не быть вовсе: в вебе формат может не поддерживаться,
        // а файла нужного формата может и не оказаться.
        if (music == null) return;

        if (SettingsManager.musicEnabled) {
            if (music.isPlaying()) return;
            play();
        } else {
            if (!music.isPlaying()) return;
            stop();
        }
    }


    public void play() {
        if (music == null) return;
        music.play();
        music.setLooping(true);
    }


    public void stop() {
        if (music == null) return;
        music.stop();
    }


    /**
     * Музыка грузится терпимо к отсутствию файла и формата.
     *
     * Ветка iOS просит mp3, а в ассетах оригинала нет ни одного mp3 — только
     * ogg. Раньше это роняло generalInitialization целиком, и игра
     * застревала на заставке.
     */
    public void load() {
        music = null;

        FileHandle fileHandle = findMusicFile();
        if (fileHandle == null) {
            System.out.println("Музыка не найдена ни в одном формате");
            return;
        }

        try {
            music = Gdx.audio.newMusic(fileHandle);
        } catch (Exception exception) {
            System.out.println("Не удалось загрузить музыку: " + fileHandle.path());
        }
    }


    private FileHandle findMusicFile() {
        String names[] = YioGdxGame.platformType == PlatformType.ios
                ? new String[]{"sound/music.mp3", "sound/music.ogg"}
                : new String[]{"sound/music.ogg", "sound/music.mp3"};

        for (String name : names) {
            FileHandle fileHandle = Gdx.files.internal(name);
            if (fileHandle.exists()) return fileHandle;
        }

        return null;
    }

}
