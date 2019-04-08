package io.github.jaspercloud.discovery.syncer.util;

import io.github.jaspercloud.discovery.syncer.support.InstanceChangedEvent;
import io.github.jaspercloud.discovery.syncer.support.SyncServiceInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InstanceUtil {

    private InstanceUtil() {

    }

    public static Map<String, SyncServiceInstance> toMap(List<SyncServiceInstance> list) {
        Map<String, SyncServiceInstance> map = new HashMap<>();
        for (SyncServiceInstance instance : list) {
            String id = SyncStringUtil.genId(instance);
            map.put(id, instance);
        }
        return map;
    }

    public static InstanceChangedEvent processInstanceChanged(String serviceName,
                                                              List<SyncServiceInstance> oldList,
                                                              List<SyncServiceInstance> newList) {
        Map<String, SyncServiceInstance> oldMap = InstanceUtil.toMap(oldList);
        Map<String, SyncServiceInstance> newMap = InstanceUtil.toMap(newList);
        InstanceChangedEvent changedEvent = processInstanceChanged(serviceName, oldMap, newMap);
        return changedEvent;
    }

    public static InstanceChangedEvent processInstanceChanged(String serviceName,
                                                              Map<String, SyncServiceInstance> oldMap,
                                                              Map<String, SyncServiceInstance> newMap) {
        List<SyncServiceInstance> addList = new ArrayList<>();
        List<SyncServiceInstance> removeList = new ArrayList<>();
        List<SyncServiceInstance> updateList = new ArrayList<>();
        Map<String, SyncServiceInstance> allMap = new HashMap<>();
        allMap.putAll(oldMap);
        allMap.putAll(newMap);
        for (Map.Entry<String, SyncServiceInstance> entry : allMap.entrySet()) {
            String id = entry.getKey();
            SyncServiceInstance instance = entry.getValue();
            if (allMap.containsKey(id) && !oldMap.containsKey(id)) {
                addList.add(instance);
            }
            if (oldMap.containsKey(id) && !newMap.containsKey(id)) {
                removeList.add(instance);
            }
            if (oldMap.containsKey(id) && newMap.containsKey(id)) {
                updateList.add(instance);
            }
        }
        InstanceChangedEvent changedEvent = new InstanceChangedEvent(serviceName, addList, removeList, updateList);
        return changedEvent;
    }

    public static String getMetaData(SyncServiceInstance instance, String key) {
        Map<String, String> metadata = instance.getMetadata();
        if (null == metadata) {
            metadata = new HashMap<>();
        }
        String data = metadata.get(key);
        return data;
    }

}
