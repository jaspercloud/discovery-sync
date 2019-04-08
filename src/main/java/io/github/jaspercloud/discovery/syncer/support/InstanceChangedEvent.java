package io.github.jaspercloud.discovery.syncer.support;

import java.util.Collections;
import java.util.List;

public class InstanceChangedEvent {

    private String serviceName;
    private List<SyncServiceInstance> addList;
    private List<SyncServiceInstance> removeList;
    private List<SyncServiceInstance> updateList;

    public String getServiceName() {
        return serviceName;
    }

    public List<SyncServiceInstance> getAddList() {
        return addList;
    }

    public List<SyncServiceInstance> getRemoveList() {
        return removeList;
    }

    public List<SyncServiceInstance> getUpdateList() {
        return updateList;
    }

    public InstanceChangedEvent(String serviceName,
                                List<SyncServiceInstance> addList,
                                List<SyncServiceInstance> removeList,
                                List<SyncServiceInstance> updateList) {
        this.serviceName = serviceName;
        this.addList = Collections.unmodifiableList(addList);
        this.removeList = Collections.unmodifiableList(removeList);
        this.updateList = Collections.unmodifiableList(updateList);
    }
}
