package io.github.jaspercloud.discovery.syncer.support;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegistrySyncManager implements InitializingBean, DisposableBean {

    private List<Registry> registryList;
    private List<RegistrySyncWorker> registrySyncWorkerList = new ArrayList<>();

    public RegistrySyncManager(List<Registry> registryList) {
        this.registryList = registryList;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Registry registry : registryList) {
            registry.init();
        }
        for (Registry registry : registryList) {
            Set<Registry> registrySet = new HashSet<>(registryList);
            registrySet.remove(registry);
            RegistrySyncWorker worker = new RegistrySyncWorker(registry, new ArrayList<>(registrySet));
            registrySyncWorkerList.add(worker);
            worker.afterPropertiesSet();
        }
    }

    @Override
    public void destroy() throws Exception {
        for (RegistrySyncWorker worker : registrySyncWorkerList) {
            worker.destroy();
        }
    }
}
