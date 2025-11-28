export class SimpleCache<K, V> {
  private cache = new Map<K, V>();
  private max: number;
  private ttl: number;
  private accessOrder: K[] = [];

  constructor(options: { max: number; ttl: number }) {
    this.max = options.max;
    this.ttl = options.ttl;
  }

  set(key: K, value: V) {
    // Remove key from access order if it exists
    const index = this.accessOrder.indexOf(key);
    if (index !== -1) {
      this.accessOrder.splice(index, 1);
    }
    
    // Evict least recently used item if cache is full
    if (this.cache.size >= this.max) {
      const lruKey = this.accessOrder.shift();
      if (lruKey !== undefined) {
        this.cache.delete(lruKey);
      }
    }
    
    // Add key to end of access order (most recently used)
    this.accessOrder.push(key);
    this.cache.set(key, value);
    
    if (this.ttl) {
      setTimeout(() => {
        this.cache.delete(key);
        const index = this.accessOrder.indexOf(key);
        if (index !== -1) {
          this.accessOrder.splice(index, 1);
        }
      }, this.ttl);
    }
  }

  get(key: K): V | undefined {
    // Move accessed key to end of access order (most recently used)
    const index = this.accessOrder.indexOf(key);
    if (index !== -1) {
      this.accessOrder.splice(index, 1);
      this.accessOrder.push(key);
    }
    return this.cache.get(key);
  }
  
  has(key: K): boolean {
    return this.cache.has(key);
  }
  
  delete(key: K): boolean {
    const index = this.accessOrder.indexOf(key);
    if (index !== -1) {
      this.accessOrder.splice(index, 1);
    }
    return this.cache.delete(key);
  }
  
  clear(): void {
    this.cache.clear();
    this.accessOrder = [];
  }
  
  size(): number {
    return this.cache.size;
  }
}
