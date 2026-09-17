// Простейший статический сервер: нужен только проверке веб-сборки.
// Открывать собранную папку файлом нельзя — сборка грузит ассеты запросами.
//
// Использование: node web/serve.js <папка> [порт]
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = process.argv[2];
const port = Number(process.argv[3] || 8099);

const types = {
  '.html': 'text/html', '.js': 'text/javascript', '.wasm': 'application/wasm',
  '.png': 'image/png', '.ogg': 'audio/ogg', '.mp3': 'audio/mpeg',
  '.xml': 'text/xml', '.txt': 'text/plain', '.otf': 'font/otf', '.ttf': 'font/ttf',
  '.webmanifest': 'application/manifest+json'
};

http.createServer((request, response) => {
  let requested = decodeURIComponent(request.url.split('?')[0]);
  if (requested === '/') requested = '/index.html';

  const file = path.join(root, requested);

  fs.readFile(file, (error, data) => {
    if (error) {
      response.writeHead(404);
      response.end('404');
      return;
    }

    response.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
    response.end(data);
  });
}).listen(port, () => console.log('раздаётся ' + root + ' на порту ' + port));
