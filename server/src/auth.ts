import { Request, Response, NextFunction } from 'express';
import { Config } from './config';

export function authMiddleware(config: Config) {
  return (req: Request, res: Response, next: NextFunction): void => {
    // If no tokens configured, auth is disabled
    if (config.authTokens.length === 0) {
      next();
      return;
    }

    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'Missing or invalid Authorization header' });
      return;
    }

    const token = authHeader.slice(7);
    if (!config.authTokens.includes(token)) {
      res.status(403).json({ error: 'Invalid token' });
      return;
    }

    next();
  };
}
