package io.github.jaspercloud.discovery.syncer.support;

import io.github.jaspercloud.discovery.syncer.util.SyncStringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RegistrySyncWorker implements InitializingBean, DisposableBean, Registry.InstanceChanged {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Registry targetRegistry;
    private List<Registry> srcRegistryList = new ArrayList<>();
    private Map<Registry, Map<String, Map<String, SyncServiceInstance>>> syncMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduledExecutorService;

    public RegistrySyncWorker(Registry targetRegistry, List<Registry> srcRegistryList) {
        this.targetRegistry = targetRegistry;
        this.srcRegistryList.addAll(srcRegistryList);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Registry registry : srcRegistryList) {
            List<String> serviceList = registry.getServiceList();
            for (String service : serviceList) {
                List<SyncServiceInstance> instanceList = registry.getInstanceList(service);
                doSync(registry, service, instanceList, false);
            }
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (Registry registry : srcRegistryList) {
                    try {
                        List<String> serviceList = registry.getServiceList();
                        for (String service : serviceList) {
                            registry.subscribe(service, RegistrySyncWorker.this);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
        if (null == scheduledExecutorService) {
            return;
        }
        scheduledExecutorService.shutdownNow();
    }

    @Override
    public void onChanged(Registry registry, String serviceName, List<SyncServiceInstance> list) {
        doSync(registry, serviceName, list, true);
    }

    private void doSync(Registry registry, String service, List<SyncServiceInstance> instanceList, boolean deregister) {
        Map<String, Map<String, SyncServiceInstance>> serviceMap = syncMap.get(registry);
        if (null == serviceMap) {
            serviceMap = new HashMap<>();
            syncMap.put(registry, serviceMap);
        }
        Map<String, SyncServiceInstance> instanceMap = serviceMap.get(service);
        if (null == instanceMap) {
            instanceMap = new HashMap<>();
            serviceMap.put(service, instanceMap);
        }
        Map<String, SyncServiceInstance> tmp = new HashMap<>();
        for (SyncServiceInstance instance : instanceList) {
            try {
                String id = SyncStringUtil.genId(instance);
                targetRegistry.registerService(instance);
                tmp.put(id, instance);
                instanceMap.remove(id);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        serviceMap.put(service, tmp);
        if (deregister) {
            for (SyncServiceInstance instance : instanceMap.values()) {
                try {
                    targetRegistry.deregisterService(instance);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
