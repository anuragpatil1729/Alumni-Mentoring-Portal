/**
 * MySQL connection pool configuration.
 *
 * mysql2 is the Node.js equivalent of JDBC for this Express application: it
 * uses prepared statements and a pooled connection to communicate with MySQL.
 */
let pool;

function getDatabasePool() {
  if (pool) return pool;

  // Load lazily so validation-only consumers do not need a live database.
  const mysql = require('mysql2/promise');
  pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'alumni_mentoring_portal',
    waitForConnections: true,
    connectionLimit: Number(process.env.DB_CONNECTION_LIMIT || 10),
    queueLimit: 0
  });
  return pool;
}

// Exported for automated tests, which inject a transaction-capable fake pool.
function setDatabasePool(databasePool) {
  pool = databasePool;
}

async function closeDatabasePool() {
  if (pool && typeof pool.end === 'function') await pool.end();
  pool = undefined;
}

module.exports = { getDatabasePool, setDatabasePool, closeDatabasePool };
