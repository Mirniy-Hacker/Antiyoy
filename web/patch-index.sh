#!/bin/sh
# Дописывает в сгенерированный index.html то, без чего iOS не считает
# страницу приложением: viewport, манифест и apple-теги.
#
# Плагин gdx-teavm генерирует index.html сам и шаблон не принимает, поэтому
# теги вставляются после сборки. Свой index.html целиком не кладём: в нём
# лежит бутстрап плагина, и он может поменяться.
set -e

INDEX="$1"
[ -f "$INDEX" ] || { echo "нет файла: $INDEX"; exit 1; }

TAGS='<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, user-scalable=no">\
<meta name="apple-mobile-web-app-capable" content="yes">\
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">\
<meta name="apple-mobile-web-app-title" content="Antiyoy">\
<meta name="theme-color" content="#1b2a30">\
<link rel="manifest" href="manifest.webmanifest">\
<link rel="apple-touch-icon" href="icon-192.png">'

sed -i "s|<title>|${TAGS}\n        <title>|" "$INDEX"

# Холст должен занимать весь экран, иначе на телефоне появятся поля.
sed -i "s|#canvas {|#canvas { width: 100vw; height: 100dvh; touch-action: none;|" "$INDEX"

echo "index.html пропатчен"
grep -c "apple-mobile-web-app-capable" "$INDEX"
