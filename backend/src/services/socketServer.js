const net = require('net');
const { processRegistration } = require('./registrationService');

function parseSocketPayload(line) {
  return JSON.parse(line);
}

function writeSocketResponse(socket, result) {
  socket.write(`${JSON.stringify(result.body)}\n`);
}

function createRegistrationSocketServer() {
  return net.createServer((socket) => {
    socket.setEncoding('utf8');
    let buffer = '';

    socket.on('data', (chunk) => {
      buffer += chunk;
      const messages = buffer.split('\n');
      buffer = messages.pop();

      messages.forEach(async (message) => {
        const trimmed = message.trim();
        if (!trimmed) return;

        try {
          const result = await processRegistration(parseSocketPayload(trimmed));
          writeSocketResponse(socket, result);
        } catch (error) {
          writeSocketResponse(socket, {
            status: 400,
            body: { success: false, message: 'Invalid socket registration payload. Send one JSON object per line.' }
          });
        }
      });
    });
  });
}

function startRegistrationSocketServer(port = process.env.SOCKET_PORT || 5002) {
  const server = createRegistrationSocketServer();
  server.listen(Number(port), () => {
    console.log(`Registration socket server running on port ${port}`);
  });
  return server;
}

module.exports = { createRegistrationSocketServer, startRegistrationSocketServer };
