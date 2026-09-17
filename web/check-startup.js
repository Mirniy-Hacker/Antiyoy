// Проверка веб-сборки настоящим браузером.
//
// Автоматчи гоняют десктоп и отрисовку не трогают вовсе, поэтому поломки,
// которые видны только в вебе, доходили до телефона. Здесь страница
// открывается в headless Chrome, разыгрывается партия и проверяется, что
// запуск дошёл до конца и на поле есть что показывать.
//
// Использование: node web/check-startup.js <chrome> <url>
const { spawn } = require('child_process');
const http = require('http');
const os = require('os');

const CHROME = process.argv[2];
const URL_ = process.argv[3];
const PORT = 9333;

// Столько шагов отмечает YioGdxGame за успешный запуск.
const EXPECTED_STEPS = 12;

const TURNS = 3;
const WAIT_MS = 40000;

const sleep = ms => new Promise(r => setTimeout(r, ms));

const getJson = path => new Promise((resolve, reject) =>
  http.get({ host: '127.0.0.1', port: PORT, path }, response => {
    let data = '';
    response.on('data', chunk => data += chunk);
    response.on('end', () => resolve(JSON.parse(data)));
  }).on('error', reject));

function fail(message) {
  console.error('ПРОВАЛ: ' + message);
  process.exit(1);
}

const chrome = spawn(CHROME, [
  '--headless=new', '--disable-gpu', '--use-gl=swiftshader', '--enable-unsafe-swiftshader',
  '--window-size=393,852', '--no-sandbox',
  `--remote-debugging-port=${PORT}`, '--user-data-dir=' + os.tmpdir() + '/antiyoy-check',
  'about:blank'
], { stdio: 'ignore' });

(async () => {
  for (let i = 0; i < 60; i++) {
    try { await getJson('/json/version'); break; } catch { await sleep(500); }
  }

  const page = (await getJson('/json/list')).find(target => target.type === 'page');
  if (!page) fail('браузер не открыл страницу');

  const socket = new WebSocket(page.webSocketDebuggerUrl);

  let nextId = 0;
  const pending = {};
  const consoleLines = [];

  const send = (method, params) => {
    const id = ++nextId;
    socket.send(JSON.stringify({ id, method, params: params || {} }));
    return new Promise(resolve => pending[id] = resolve);
  };

  socket.onmessage = event => {
    const message = JSON.parse(event.data);

    if (message.id && pending[message.id]) {
      pending[message.id](message.result);
      delete pending[message.id];
      return;
    }

    if (message.method === 'Runtime.consoleAPICalled') {
      consoleLines.push(message.params.args
        .map(arg => arg.value !== undefined ? arg.value : arg.description)
        .join(' '));
    }
  };

  await new Promise(resolve => socket.onopen = resolve);
  await send('Page.enable');
  await send('Runtime.enable');
  await send('Emulation.setDeviceMetricsOverride',
    { width: 393, height: 852, deviceScaleFactor: 3, mobile: true });
  await send('Page.navigate', { url: URL_ + '?turns=' + TURNS });
  await sleep(WAIT_MS);

  const result = await send('Runtime.evaluate', {
    expression: 'JSON.stringify(window.startupLog || [])',
    returnByValue: true
  });

  const log = JSON.parse(result && result.result ? result.result.value : '[]');

  console.log('шагов запуска: ' + log.length + ' из ' + EXPECTED_STEPS);
  consoleLines.forEach(line => console.log('  ' + line));

  socket.close();
  chrome.kill();

  const failures = log.filter(step => /^(УПАЛО|ОШИБКА|ОТКАЗ|КОНСОЛЬ)/.test(step));
  if (failures.length > 0) fail('запуск сорвался: ' + failures[0]);

  if (log.length < EXPECTED_STEPS) {
    fail('запуск не дошёл до конца, последний шаг: ' + (log[log.length - 1] || 'ни одного'));
  }

  // Партия разыграна, значит на поле обязаны быть гексы и объекты.
  const summary = consoleLines.find(line => line.startsWith('Ходов '));
  if (!summary) fail('партия не разыгралась: нет сводки по полю');

  const hexes = Number((summary.match(/гексов (\d+)/) || [])[1] || 0);
  const objects = Number((summary.match(/объектов (\d+)/) || [])[1] || 0);

  if (hexes === 0) fail('поле пустое: ' + summary);
  if (objects === 0) fail('на поле нет объектов: ' + summary);

  console.log('веб-сборка запускается и рисует поле');
  process.exit(0);
})();
