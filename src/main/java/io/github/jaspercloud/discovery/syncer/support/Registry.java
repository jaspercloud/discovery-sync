package io.github.jaspercloud.discovery.syncer.support;

import java.util.List;

public interface Registry {

    String name();

    void init() throws Exception;

    void registerService(SyncServiceInstance instance) throws Exception;

    void deregisterService(SyncServiceInstance instance) throws Exception;

    List<String> getServiceList() throws Exception;

    List<SyncServiceInstance> getInstanceList(String serviceName) throws Exception;

    void subscribe(String serviceName, InstanceChanged instanceChanged) throws Exception;

    interface InstanceChanged {

        void onChanged(Registry registry, InstanceChangedEvent changedEvent);
    }
}
