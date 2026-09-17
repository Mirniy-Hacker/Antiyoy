package yio.tro.antiyoy.stuff;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import yio.tro.antiyoy.gameplay.DebugFlags;

public class LanguagesManager {
    private static LanguagesManager _instance = null;

    private static final String LANGUAGES_FILE = "languages.xml";
    private static final String DEFAULT_LANGUAGE = "en_UK";

    /**
     * Язык системы, если платформа умеет сообщить его лучше, чем
     * Locale.getDefault(). В вебе тот возвращает язык не браузера, и игра
     * открывалась по-английски независимо от настроек телефона.
     */
    public static String systemLanguageOverride = null;

    private HashMap<String, String> _language = null;

    /**
     * Английские строки как запасные.
     *
     * Строки мода заведены только на английском и русском, а языков в
     * файле двадцать пять. Без запасного словаря игрок с любым другим
     * языком увидел бы вместо подписей сырые ключи.
     */
    private HashMap<String, String> _fallback = null;
    private String _languageName = null;


    private LanguagesManager() {
        // Create language map
        _language = new HashMap<>();
        _fallback = new HashMap<>();

        // Try to load system language
        // If it fails, fallback to default language
        _languageName = (systemLanguageOverride != null)
                ? systemLanguageOverride
                : java.util.Locale.getDefault().toString();
        boolean loaded = loadLanguage(_languageName);
        yio.tro.antiyoy.YioGdxGame.reportStartup("ДИАГНОСТИКА язык: запрошен " + _languageName
                + ", подошёл " + loaded);

        if (!loaded) {
            loadLanguage(DEFAULT_LANGUAGE);
            _languageName = DEFAULT_LANGUAGE;
        }
    }


    public static void initialize() {
        _instance = null;
    }


    public static LanguagesManager getInstance() {
        if (_instance == null) {
            _instance = new LanguagesManager();
        }

        return _instance;
    }


    public String getLanguage() {
        return _languageName;
    }


    public void setLanguage(String langName) {
        loadLanguage(langName);
        _languageName = langName;
    }


    public String getString(String key) {
        if (DebugFlags.forceKeys) return key;

        String string;

        if (_language != null) {
            // Look for string in selected language
            string = _language.get(key);

            if (string != null) {
                return string;
            }
        }

        // Строка может быть не переведена: берём английскую.
        String fallback = _fallback.get(key);
        if (fallback != null) {
            return fallback;
        }

        // Нет и там — остаётся показать сам ключ.
        return key;
    }


    public String getString(String key, Object... args) {
        return String.format(getString(key), args);
    }


    public ArrayList<LanguageChooseItem> getChooseListItems() {
        ArrayList<LanguageChooseItem> result = new ArrayList<>();

        try {
            XmlReader.Element root = readLanguagesFile();

            for (int i = 0; i < root.getChildCount(); i++) {
                XmlReader.Element language = root.getChild(i);
                if (!language.getName().equals("language")) continue;

                LanguageChooseItem chooseItem = new LanguageChooseItem();
                chooseItem.name = language.getAttribute("name", "");
                chooseItem.title = language.getAttribute("title", "");
                chooseItem.author = language.getAttribute("author", "");

                result.add(chooseItem);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    /**
     * Языковой файл читается через XmlReader из libGDX, а не через
     * javax.xml: последнего нет ни в вебе, ни на TeaVM, и это было
     * единственное место во всей игре, которое туда заглядывало.
     */
    private XmlReader.Element readLanguagesFile() {
        FileHandle fileHandle = Gdx.files.internal(LANGUAGES_FILE);

        String source = fileHandle.readString("UTF-8");

        // Файл начинается с BOM. javax.xml его проглатывал молча, XmlReader
        // из libGDX — нет, и парсинг разваливался целиком.
        if (source.length() > 0 && source.charAt(0) == '﻿') {
            source = source.substring(1);
        }

        return new XmlReader().parse(source);
    }


    public boolean loadLanguage(String languageName) {
        XmlReader.Element prefixMatch = null;

        try {
            XmlReader.Element root = readLanguagesFile();

            for (int i = 0; i < root.getChildCount(); i++) {
                XmlReader.Element language = root.getChild(i);
                if (!language.getName().equals("language")) continue;

                String name = language.getAttribute("name", null);
                String secondName = language.getAttribute("second_name", null);

                boolean matches = languageName.equals(name)
                        || (secondName != null && secondName.equals(languageName));

                // Браузер сообщает язык как ru-RU или просто ru, а в файле
                // названия вида ru_RU. Совпадения по двухбуквенному коду
                // достаточно: второго русского в файле нет.
                if (!matches && name != null && sameLanguageCode(languageName, name)) {
                    prefixMatch = language;
                }

                if (!matches) continue;

                _language.clear();
                loadStringsInto(language);

                loadFallback(root);

                return true;
            }
        } catch (Exception e) {
            System.out.println("Error loading languages file " + LANGUAGES_FILE);
            return false;
        }

        if (prefixMatch != null) {
            _language.clear();
            loadStringsInto(prefixMatch);
            loadFallback(prefixMatch.getParent());
            return true;
        }

        return false;
    }


    /**
     * Английский словарь заполняется тем же разбором файла: читать
     * 700 килобайт XML второй раз незачем.
     */
    /**
     * Совпадают ли двухбуквенные коды языков. Разделитель бывает и
     * дефисом, и подчёркиванием: браузер пишет ru-RU, файл — ru_RU.
     */
    private boolean sameLanguageCode(String first, String second) {
        String firstCode = languageCode(first);
        String secondCode = languageCode(second);

        return firstCode.length() > 0 && firstCode.equals(secondCode);
    }


    private String languageCode(String name) {
        String lowerCase = name.toLowerCase();

        for (int i = 0; i < lowerCase.length(); i++) {
            char symbol = lowerCase.charAt(i);
            if (symbol == '_' || symbol == '-') {
                return lowerCase.substring(0, i);
            }
        }

        return lowerCase;
    }


    private void loadFallback(XmlReader.Element root) {
        _fallback.clear();

        for (int i = 0; i < root.getChildCount(); i++) {
            XmlReader.Element language = root.getChild(i);
            if (!language.getName().equals("language")) continue;
            if (!DEFAULT_LANGUAGE.equals(language.getAttribute("name", null))) continue;

            loadStringsInto(language, _fallback);
            return;
        }
    }


    private void loadStringsInto(XmlReader.Element language) {
        loadStringsInto(language, _language);
    }


    private void loadStringsInto(XmlReader.Element language, HashMap<String, String> target) {
        for (int j = 0; j < language.getChildCount(); j++) {
            XmlReader.Element string = language.getChild(j);
            if (!string.getName().equals("string")) continue;

            String key = string.getAttribute("key", null);
            String value = string.getAttribute("value", null);
            if (key == null || value == null) continue;

            // Перенос строки задаётся только через <br />, и делается это
            // после нормализации.
            target.put(key, normalizeAttribute(value).replace("<br />", "\n"));
        }
    }


    /**
     * Нормализация значения атрибута по стандарту XML: перевод строки и
     * табуляция внутри атрибута схлопываются в пробел.
     *
     * javax.xml делал это сам, XmlReader из libGDX — нет. Без этого длинные
     * тексты справки, записанные в несколько строк, приезжали с настоящими
     * переносами вместо пробелов.
     */
    private String normalizeAttribute(String value) {
        if (value.indexOf('\n') < 0 && value.indexOf('\r') < 0 && value.indexOf('\t') < 0) {
            return value;
        }

        return value.replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}
