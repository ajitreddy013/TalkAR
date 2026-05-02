type NonceStore = {
  reserve(nonceKey: string, ttlMs: number): Promise<boolean>;
};

class InMemoryNonceStore implements NonceStore {
  private readonly store = new Map<string, number>();

  private cleanup(now: number) {
    for (const [key, expiresAt] of this.store.entries()) {
      if (expiresAt <= now) {
        this.store.delete(key);
      }
    }
  }

  async reserve(nonceKey: string, ttlMs: number): Promise<boolean> {
    const now = Date.now();
    this.cleanup(now);
    if (this.store.has(nonceKey)) {
      return false;
    }
    this.store.set(nonceKey, now + ttlMs);
    return true;
  }
}

class RedisNonceStore implements NonceStore {
  private client: any | null = null;
  private readonly redisUrl: string;
  private connectPromise: Promise<void> | null = null;

  constructor(redisUrl: string) {
    this.redisUrl = redisUrl;
  }

  private async connect(): Promise<void> {
    if (this.client) return;
    if (this.connectPromise) return this.connectPromise;

    this.connectPromise = (async () => {
      // Lazy runtime import keeps Redis optional and avoids hard compile-time dependency.
      const dynamicImport = new Function("m", "return import(m)") as (
        moduleName: string
      ) => Promise<any>;
      const redisLib = await dynamicImport("redis");
      this.client = redisLib.createClient({ url: this.redisUrl });
      this.client.on("error", (err: Error) => {
        console.warn("[worker-auth] redis nonce store error", { message: err.message });
      });
      await this.client.connect();
    })();

    try {
      await this.connectPromise;
    } finally {
      this.connectPromise = null;
    }
  }

  async reserve(nonceKey: string, ttlMs: number): Promise<boolean> {
    await this.connect();
    if (!this.client) return false;
    const seconds = Math.max(1, Math.ceil(ttlMs / 1000));
    const result = await this.client.set(nonceKey, "1", {
      NX: true,
      EX: seconds,
    });
    return result === "OK";
  }
}

const inMemoryStore = new InMemoryNonceStore();
let redisStore: RedisNonceStore | null = null;

export async function reserveWorkerNonce(
  nonceKey: string,
  ttlMs: number
): Promise<boolean> {
  const backend = (process.env.WAV2LIP_NONCE_STORE || "memory").toLowerCase();
  if (backend !== "redis") {
    return inMemoryStore.reserve(nonceKey, ttlMs);
  }

  const redisUrl = process.env.WAV2LIP_NONCE_REDIS_URL;
  if (!redisUrl) {
    console.warn("[worker-auth] redis nonce store requested but WAV2LIP_NONCE_REDIS_URL is missing; using memory fallback");
    return inMemoryStore.reserve(nonceKey, ttlMs);
  }

  try {
    if (!redisStore) {
      redisStore = new RedisNonceStore(redisUrl);
    }
    return await redisStore.reserve(nonceKey, ttlMs);
  } catch (error) {
    console.warn("[worker-auth] redis nonce reserve failed; using memory fallback", {
      message: (error as Error).message,
    });
    return inMemoryStore.reserve(nonceKey, ttlMs);
  }
}
