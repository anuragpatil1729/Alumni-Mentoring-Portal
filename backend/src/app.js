/**
 * Express Application Server Entry Point
 */

require('dotenv').config();

const express = require('express');
const cors = require('cors');
const authRoutes = require('./routes/authRoutes');
const { errorHandler } = require('./middleware/errorMiddleware');
const { processRegistration } = require('./services/registrationService');
const { startRegistrationSocketServer } = require('./services/socketServer');

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Root API endpoint
app.get('/', (req, res) => {
  res.status(200).json({
    message: 'Alumni Mentoring Portal Backend API is running',
    status: 'OK',
    frontendUrl: 'http://localhost:5173',
    healthCheck: '/api/health'
  });
});

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Auth Routes
app.use('/api/auth', authRoutes);

// Servlet-style endpoint: mirrors a Java servlet registration POST handler.
app.post('/api/servlet/register', async (req, res) => {
  const result = await processRegistration(req.body);
  return res.status(result.status).json(result.body);
});

// CGI-style endpoint: accepts URL-encoded form submissions and returns JSON.
app.post('/cgi-bin/register', async (req, res) => {
  const result = await processRegistration(req.body);
  return res.status(result.status).json(result.body);
});

// Global Error Handler
app.use(errorHandler);

const PORT = process.env.PORT || 5001;

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
  });

  if (process.env.ENABLE_SOCKET_SERVER !== 'false') {
    startRegistrationSocketServer();
  }
}

module.exports = app;
