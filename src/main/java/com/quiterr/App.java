package com.quiterr;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.UserManagedCache;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.ResourceType;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.builders.UserManagedCacheBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * Created by huangchen on 2017/3/21.
 */
public class App {
    public static void main(String[] args){
//        TestEhcache();
//        TestUserManagedCache();
//        TestPersistent();
//        ByteSizedHeap();
        UpdateResourcePools();
    }

    public static void TestEhcache(){
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        newCacheConfigurationBuilder(Long.class, String.class, ResourcePoolsBuilder.heap(10)))
                .build();
        cacheManager.init();

        Cache<Long, String> preConfigured =
                cacheManager.getCache("preConfigured", Long.class, String.class);

        Cache<Long, String> myCache = cacheManager.createCache("myCache",
                newCacheConfigurationBuilder(Long.class, String.class, ResourcePoolsBuilder.heap(10)).build());

        preConfigured.put(1L,"preConfigured 1L");
        preConfigured.put(2L,"preConfigured 2L");

        myCache.put(3L, "myCache 3L");
        myCache.put(4L, "myCache 4L");

        System.out.println(preConfigured.get(1L));
        System.out.println(preConfigured.get(2L));
        System.out.println(myCache.get(3L));
        System.out.println(myCache.get(4L));

        cacheManager.removeCache("preConfigured");
        cacheManager.removeCache("myCache");

        cacheManager.close();
    }

    /**
     * Ehcache User Managed Cache
     */
    public static void TestUserManagedCache(){
        UserManagedCache<Long, String> userManagedCache =
                UserManagedCacheBuilder.newUserManagedCacheBuilder(Long.class, String.class)
                        .build(true);
//        userManagedCache.init();
        userManagedCache.put(1L, "userManagedCache 1L");
        System.out.println(userManagedCache.get(1L));
        userManagedCache.close();
    }

    /**
     * Ehcache 层级存储
     */
    public static void TestTiers(){
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("tieredCache", newCacheConfigurationBuilder(Long.class, String.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(10, EntryUnit.ENTRIES)
                                .offheap(10, MemoryUnit.MB)))
                .build(true);

        Cache<Long, String> tieredCache = cacheManager.getCache("tieredCache", Long.class, String.class);
        cacheManager.close();
    }

    /**
     * Ehcache Disk Persistent
     */
    public static void TestPersistent(){
        PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence("D:\\Ehcache" + File.separator + "myData"))
                .withCache("persistent-cache", CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(10, EntryUnit.ENTRIES)
                                .disk(10, MemoryUnit.MB, true)) //这里第三参数设为true才会持久化，否则不会
                )
                .build(true);
        Cache<Long, String> persistentCache =
                persistentCacheManager.getCache("persistent-cache", Long.class, String.class);
        persistentCache.put(1L,"persistentCache 1L");
        System.out.println(persistentCache.get(1L));
        persistentCacheManager.close();
    }

    /**
     *  size the heap tier using memory units instead of entry count
     */
    public static void ByteSizedHeap(){
        CacheConfiguration<Long, String> usesConfiguredInCacheConfig = newCacheConfigurationBuilder(Long.class, String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(10, MemoryUnit.KB)
                        .offheap(10, MemoryUnit.MB))
                .withSizeOfMaxObjectGraph(1000)
                .withSizeOfMaxObjectSize(1000, MemoryUnit.B)
                .build();

        CacheConfiguration<Long, String> usesDefaultSizeOfEngineConfig = newCacheConfigurationBuilder(Long.class, String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(10, MemoryUnit.KB)
                        .offheap(10, MemoryUnit.MB))
                .build();

        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withDefaultSizeOfMaxObjectSize(500, MemoryUnit.B)
                .withDefaultSizeOfMaxObjectGraph(2000)
                .withCache("usesConfiguredInCache", usesConfiguredInCacheConfig)
                .withCache("usesDefaultSizeOfEngine", usesDefaultSizeOfEngineConfig)
                .build(true);

        Cache<Long, String> usesConfiguredInCache = cacheManager.getCache("usesConfiguredInCache", Long.class, String.class);

        usesConfiguredInCache.put(1L, "one");
        assertThat(usesConfiguredInCache.get(1L), equalTo("one"));

        Cache<Long, String> usesDefaultSizeOfEngine = cacheManager.getCache("usesDefaultSizeOfEngine", Long.class, String.class);

        usesDefaultSizeOfEngine.put(1L, "one");
        assertThat(usesDefaultSizeOfEngine.get(1L), equalTo("one"));

        cacheManager.close();

    }

    /**
     * Update ResourcePools
     */
    public static void  UpdateResourcePools(){
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("cache",
                        newCacheConfigurationBuilder(Long.class, String.class, ResourcePoolsBuilder.heap(10)))
                .build();
        cacheManager.init();

        Cache<Long, String> cache =
                cacheManager.getCache("cache", Long.class, String.class);

        ResourcePools pools = ResourcePoolsBuilder.newResourcePoolsBuilder().heap(20L, EntryUnit.ENTRIES).build();
        cache.getRuntimeConfiguration().updateResourcePools(pools);
        assertThat(cache.getRuntimeConfiguration().getResourcePools()
                .getPoolForResource(ResourceType.Core.HEAP).getSize(), is(20L));
    }

    /**
     * Data freshness
     */
    public static void  DataFreshness(){

        CacheConfiguration<Long, String> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class, String.class,
                ResourcePoolsBuilder.heap(100))
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(20, TimeUnit.SECONDS)))
                .build();

    }
}
