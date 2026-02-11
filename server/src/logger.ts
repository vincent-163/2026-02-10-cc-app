const LEVELS: Record<string, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

let currentLevel = 1;

export function setLogLevel(level: string): void {
  currentLevel = LEVELS[level] ?? 1;
}

function timestamp(): string {
  return new Date().toISOString();
}

export const logger = {
  debug(msg: string, ...args: unknown[]): void {
    if (currentLevel <= 0) console.debug(`${timestamp()} [DEBUG] ${msg}`, ...args);
  },
  info(msg: string, ...args: unknown[]): void {
    if (currentLevel <= 1) console.log(`${timestamp()} [INFO] ${msg}`, ...args);
  },
  warn(msg: string, ...args: unknown[]): void {
    if (currentLevel <= 2) console.warn(`${timestamp()} [WARN] ${msg}`, ...args);
  },
  error(msg: string, ...args: unknown[]): void {
    if (currentLevel <= 3) console.error(`${timestamp()} [ERROR] ${msg}`, ...args);
  },
};
