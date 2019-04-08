package io.github.jaspercloud.discovery.syncer.support;

import io.github.jaspercloud.discovery.syncer.util.InstanceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RegistrySyncWorker implements InitializingBean, DisposableBean, Registry.InstanceChanged {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Registry targetRegistry;
    private List<Registry> srcRegistryList = new ArrayList<>();

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
                List<SyncServiceInstance> oldList = targetRegistry.getInstanceList(service);
                List<SyncServiceInstance> newList = registry.getInstanceList(service);
                InstanceChangedEvent changedEvent = InstanceUtil.processInstanceChanged(service, oldList, newList);
                doSync(registry, changedEvent, false);
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
    public void onChanged(Registry registry, InstanceChangedEvent changedEvent) {
        doSync(registry, changedEvent, true);
    }

    private void doSync(Registry registry, InstanceChangedEvent changedEvent, boolean syncDelete) {
        List<SyncServiceInstance> addList = changedEvent.getAddList();
        List<SyncServiceInstance> updateList = changedEvent.getUpdateList();
        List<SyncServiceInstance> removeList = changedEvent.getRemoveList();
        for (SyncServiceInstance instance : addList) {
            try {
                targetRegistry.registerService(instance);
                logger.info("{}->{} sync register: {}", registry.name(), targetRegistry.name(), instance);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        for (SyncServiceInstance instance : updateList) {
            try {
//                targetRegistry.registerService(instance);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (syncDelete) {
            for (SyncServiceInstance instance : removeList) {
                try {
                    targetRegistry.deregisterService(instance);
                    logger.info("{}->{} sync deregister: {}", registry.name(), targetRegistry.name(), instance);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
