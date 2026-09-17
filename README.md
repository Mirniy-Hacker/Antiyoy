# Antiyoy Mod

Мод игры [Antiyoy](https://github.com/yiotro/Antiyoy) (libGDX, Java).
Переработка дипломатии, экономики и ИИ.

**Некоммерческий проект** — этого требует лицензия оригинала. Коммерческое
использование запрещено.

## Происхождение

Исходный код ядра (`core/`) и ресурсы (`assets/`) взяты из
[yiotro/Antiyoy](https://github.com/yiotro/Antiyoy), автор — yiotro
(yiotro93@gmail.com). История оригинала влита в этот репозиторий; upstream
подключён вторым remote:

```
git remote add upstream https://github.com/yiotro/Antiyoy.git
git fetch upstream
```

Оригинальный репозиторий содержит только `core/` и `assets/` — сборочной
обвязки, десктопного и iOS-модулей в нём нет, они добавлены здесь.

## Документация

- [docs/SPEC.md](docs/SPEC.md) — что делаем
- [docs/ROADMAP.md](docs/ROADMAP.md) — в каком порядке и как проверяем
- [CLAUDE.md](CLAUDE.md) — правила работы с кодом

## Сборка

Десктоп (основной цикл разработки):

```
./gradlew lwjgl3:run
```

iOS собирается только на macOS-раннере GitHub Actions вручную
(`.github/workflows/ios.yml`) и ставится на телефон через AltStore.
