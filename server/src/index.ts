import express from 'express';
import { loadConfig } from './config';
import { setLogLevel, logger } from './logger';
import { SessionManager } from './session';
import { authMiddleware } from './auth';
import { createRoutes } from './routes';

import * as path from 'path';

const config = loadConfig();
setLogLevel(config.logLevel);

const sessionsDir = path.resolve(config.sessionsDir);
const manager = new SessionManager(config, sessionsDir);
const app = express();

app.use(express.json());

// Health endpoint is public
app.get('/health', (_req, res, next) => next());

// Auth middleware for all other routes
app.use('/sessions', authMiddleware(config));

// Mount routes
app.use(createRoutes(manager));

// Graceful shutdown
async function shutdown(signal: string) {
  logger.info(`Received ${signal}, shutting down...`);
  manager.shutdown();
  await manager.destroyAll();
  logger.info('All sessions destroyed, exiting.');
  process.exit(0);
}

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));

const server = app.listen(config.port, config.host, () => {
  logger.info(`cc-server listening on http://${config.host}:${config.port}`);
  logger.info(`Auth: ${config.authTokens.length > 0 ? 'enabled' : 'disabled (no tokens configured)'}`);
  logger.info(`Max sessions: ${config.maxSessions}, buffer size: ${config.bufferSize}`);
  logger.info(`Session timeout: ${config.sessionTimeoutSec}s`);
});

server.on('error', (err) => {
  logger.error(`Server error: ${err.message}`);
  process.exit(1);
});
