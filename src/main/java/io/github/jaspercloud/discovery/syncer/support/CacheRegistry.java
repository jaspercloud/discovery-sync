package io.github.jaspercloud.discovery.syncer.support;

import io.github.jaspercloud.discovery.syncer.util.SyncStringUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class CacheRegistry implements Registry {

    private Set<String> cache = new ConcurrentSkipListSet<>();

    protected boolean hasCache(SyncServiceInstance instance) {
        String id = SyncStringUtil.genId(instance);
        boolean contains = cache.contains(id);
        return contains;
    }

    protected void addCache(SyncServiceInstance instance) {
        String id = SyncStringUtil.genId(instance);
        cache.add(id);
    }

    protected void removeCache(SyncServiceInstance instance) {
        String id = SyncStringUtil.genId(instance);
        cache.remove(id);
    }
}
