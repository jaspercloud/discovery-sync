package io.github.jaspercloud.discovery.syncer.support;

import io.github.jaspercloud.discovery.syncer.util.Constants;
import io.github.jaspercloud.discovery.syncer.util.InstanceUtil;
import io.github.jaspercloud.discovery.syncer.util.SyncStringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CacheRegistry implements Registry {

    private Map<String, Map<String, SyncServiceInstance>> cache = new HashMap<>();
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Override
    public void init() throws Exception {
        Map<String, Map<String, SyncServiceInstance>> tmp = getServiceList().stream().flatMap(new Function<String, Stream<Map<String, Map<String, SyncServiceInstance>>>>() {
            @Override
            public Stream<Map<String, Map<String, SyncServiceInstance>>> apply(String service) {
                try {
                    List<SyncServiceInstance> list = getInstanceList(service);
                    Map<String, SyncServiceInstance> map = new HashMap<>();
                    for (SyncServiceInstance instance : list) {
                        String registryName = InstanceUtil.getMetaData(instance, Constants.RegistryName);
                        if (StringUtils.isNotEmpty(registryName)) {
                            //同步镜像，非真实注册
                            continue;
                        }
                        String id = SyncStringUtil.genId(instance);
                        map.put(id, instance);
                    }
                    Map<String, Map<String, SyncServiceInstance>> rs = Collections.singletonMap(service, map);
                    return Stream.of(rs);
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }
        }).reduce(new BinaryOperator<Map<String, Map<String, SyncServiceInstance>>>() {
            @Override
            public Map<String, Map<String, SyncServiceInstance>> apply(Map<String, Map<String, SyncServiceInstance>> map1, Map<String, Map<String, SyncServiceInstance>> map2) {
                Map<String, Map<String, SyncServiceInstance>> map = new HashMap<>();
                map.putAll(map1);
                map.putAll(map2);
                return map;
            }
        }).get();
        try {
            readWriteLock.writeLock().lock();
            cache = tmp;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    protected List<SyncServiceInstance> filterMirrorInstanceList(List<SyncServiceInstance> list) {
        List<SyncServiceInstance> instanceList = list.stream().filter(new Predicate<SyncServiceInstance>() {
            @Override
            public boolean test(SyncServiceInstance instance) {
                String registryName = InstanceUtil.getMetaData(instance, Constants.RegistryName);
                if (StringUtils.isNotEmpty(registryName)) {
                    //同步镜像，非真实注册
                    return false;
                }
                return true;
            }
        }).collect(Collectors.toList());
        return instanceList;
    }

    protected boolean hasCache(SyncServiceInstance instance) {
        try {
            readWriteLock.readLock().lock();
            String serviceName = instance.getServiceName();
            String id = SyncStringUtil.genId(instance);
            Map<String, SyncServiceInstance> instanceMap = cache.get(serviceName);
            if (null == instanceMap) {
                instanceMap = new HashMap<>();
                cache.put(serviceName, instanceMap);
            }
            boolean contains = instanceMap.containsKey(id);
            return contains;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    protected void addCache(SyncServiceInstance instance) {
        try {
            readWriteLock.writeLock().lock();
            String serviceName = instance.getServiceName();
            String id = SyncStringUtil.genId(instance);
            Map<String, SyncServiceInstance> instanceMap = cache.get(serviceName);
            if (null == instanceMap) {
                instanceMap = new HashMap<>();
                cache.put(serviceName, instanceMap);
            }
            instanceMap.put(id, instance);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    protected void removeCache(SyncServiceInstance instance) {
        try {
            readWriteLock.writeLock().lock();
            String serviceName = instance.getServiceName();
            String id = SyncStringUtil.genId(instance);
            Map<String, SyncServiceInstance> instanceMap = cache.get(serviceName);
            if (null == instanceMap) {
                instanceMap = new HashMap<>();
                cache.put(serviceName, instanceMap);
            }
            instanceMap.remove(id);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private Map<String, SyncServiceInstance> getInstanceMap(String serviceName) {
        try {
            readWriteLock.readLock().lock();
            return cache.get(serviceName);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private void setInstanceMap(String serviceName, Map<String, SyncServiceInstance> map) {
        try {
            readWriteLock.writeLock().lock();
            cache.put(serviceName, map);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    protected InstanceChangedEvent processInstanceChanged(String serviceName, List<SyncServiceInstance> list) {
        Map<String, SyncServiceInstance> oldMap = getInstanceMap(serviceName);
        if (null == oldMap) {
            oldMap = new HashMap<>();
        }
        Map<String, SyncServiceInstance> newMap = InstanceUtil.toMap(list);
        InstanceChangedEvent changedEvent = InstanceUtil.processInstanceChanged(serviceName, oldMap, newMap);
        setInstanceMap(serviceName, newMap);
        return changedEvent;
    }

}
