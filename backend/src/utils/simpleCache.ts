export class SimpleCache<K, V> {
  private cache = new Map<K, V>();
  private max: number;
  private ttl: number;

  constructor(options: { max: number; ttl: number }) {
    this.max = options.max;
    this.ttl = options.ttl;
  }

  set(key: K, value: V) {
    if (this.cache.size >= this.max) {
      const firstKey = this.cache.keys().next().value;
      if (firstKey !== undefined) {
        this.cache.delete(firstKey);
      }
    }
    this.cache.set(key, value);
    if (this.ttl) {
      setTimeout(() => this.cache.delete(key), this.ttl);
    }
  }

  get(key: K): V | undefined {
    return this.cache.get(key);
  }
  
  has(key: K): boolean {
    return this.cache.has(key);
  }
  
  delete(key: K): boolean {
    return this.cache.delete(key);
  }
  
  clear(): void {
    this.cache.clear();
  }
}
