/**
 * Express Application Server Entry Point
 */

require('dotenv').config();

const express = require('express');
const cors = require('cors');
const authRoutes = require('./routes/authRoutes');
const { errorHandler } = require('./middleware/errorMiddleware');

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

// Global Error Handler
app.use(errorHandler);

const PORT = process.env.PORT || 5001;

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
  });
}

module.exports = app;
