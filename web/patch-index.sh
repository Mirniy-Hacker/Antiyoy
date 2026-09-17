#!/bin/sh
# Дописывает в сгенерированный index.html то, без чего iOS не считает
# страницу приложением: viewport, манифест и apple-теги, плюс показ ошибок
# прямо на странице.
#
# Плагин gdx-teavm генерирует index.html сам и шаблон не принимает, поэтому
# всё дописывается после сборки. Свой index.html целиком не кладём: в нём
# лежит бутстрап плагина, и он может поменяться.
set -e

INDEX="$1"
[ -f "$INDEX" ] || { echo "нет файла: $INDEX"; exit 1; }

DIR=$(dirname "$0")
HEAD_PART=$(mktemp)
BODY_PART=$(mktemp)
trap 'rm -f "$HEAD_PART" "$BODY_PART"' EXIT

cat > "$HEAD_PART" <<'HEADEOF'
        <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no">
        <meta name="apple-mobile-web-app-capable" content="yes">
        <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
        <meta name="apple-mobile-web-app-title" content="Antiyoy">
        <meta name="theme-color" content="#1b2a30">
        <link rel="manifest" href="manifest.webmanifest">
        <link rel="apple-touch-icon" href="icon-192.png">
HEADEOF

# Показ ошибок прямо на странице.
#
# Исключение внутри кадра убивает цикл requestAnimationFrame, и на экране
# просто застывает последняя картинка — заставка. Без этого перехвата
# отличить «игра грузится» от «игра упала» с телефона невозможно: консоли
# на iOS нет.
cat > "$BODY_PART" <<'BODYEOF'
        <div id="errbox" style="display:none;position:fixed;top:0;left:0;right:0;z-index:99;background:#300;color:#fdd;font:12px monospace;padding:10px;white-space:pre-wrap;max-height:60vh;overflow:auto"></div>
        <script>
            window.showError = function (text) {
                var box = document.getElementById("errbox");
                if (!box) return;
                box.style.display = "block";
                box.textContent += text + "\n";
            }
            window.addEventListener("error", function (e) {
                showError("ОШИБКА: " + (e.message || e.error));
            });
            window.addEventListener("unhandledrejection", function (e) {
                showError("ОТКАЗ: " + e.reason);
            });
        </script>
BODYEOF

# Теги в head — перед заголовком.
sed -i "/<title>/r /dev/null" "$INDEX"
awk -v f="$HEAD_PART" '
  /<title>/ && !done { while ((getline line < f) > 0) print line; done = 1 }
  { print }
' "$INDEX" > "$INDEX.tmp" && mv "$INDEX.tmp" "$INDEX"

# Перехват ошибок — сразу после холста.
awk -v f="$BODY_PART" '
  { print }
  /<canvas id="canvas"><\/canvas>/ && !done { while ((getline line < f) > 0) print line; done = 1 }
' "$INDEX" > "$INDEX.tmp" && mv "$INDEX.tmp" "$INDEX"

# Холст должен занимать весь экран, иначе на телефоне появятся поля.
sed -i "s|#canvas {|#canvas { width: 100vw; height: 100dvh; touch-action: none;|" "$INDEX"

echo "index.html пропатчен"
echo "  viewport: $(grep -c 'name=\"viewport\"' "$INDEX")"
echo "  манифест: $(grep -c 'manifest.webmanifest' "$INDEX")"
echo "  перехват ошибок: $(grep -c 'errbox' "$INDEX")"
