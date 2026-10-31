package com.com.visualizer.domain

import com.com.visualizer.domain.model.VisualizerCacheEntry

/**
 * Provides a caching layer  for visualizer data.
 */
internal interface VisualizerCache {

    /**
     * Stores a visualizer cache entry under the specified key
     * If an entry already exists for the key, it is replaced.
     * @param key Unique identifier for the cache entry.
     * @param entry Visualizer data to cache.
     */
    suspend fun setCacheEntry(key: String, entry: VisualizerCacheEntry)

    /**
     * Retrieves a visualizer cache entry by its key.
     * @param key Unique identifier of the cache entry.
     * @return The cached entry, or `null` if no entry exists or the entry cannot be retrieved
     */
    suspend fun getCacheEntry(key: String): VisualizerCacheEntry?
}
