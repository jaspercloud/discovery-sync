package io.github.jaspercloud.discovery.syncer.support;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.jaspercloud.discovery.syncer.util.SyncStringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NacosRegistry extends CacheRegistry implements Registry {

    private static final String InstanceId = "instanceId";

    private NamingService namingService;
    private NamingProxy namingProxy;
    private Gson gson;

    public NacosRegistry(NamingService namingService, NamingProxy namingProxy) {
        this.namingService = namingService;
        this.namingProxy = namingProxy;
        this.gson = new Gson();
    }

    @Override
    public List<String> getServiceList() throws Exception {
        List<String> serviceList = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("startPg", "1");
        map.put("pgSize", String.valueOf(Integer.MAX_VALUE));
        String json = namingProxy.reqAPI("/nacos/v1/ns/catalog/serviceList", map, "GET");
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        JsonArray serviceListArray = jsonObject.get("serviceList").getAsJsonArray();
        for (int i = 0; i < serviceListArray.size(); i++) {
            String name = serviceListArray.get(i).getAsJsonObject().get("name").getAsString();
            serviceList.add(name);
        }
        return Collections.unmodifiableList(serviceList);
    }

    @Override
    public List<SyncServiceInstance> getInstanceList(String serviceName) throws Exception {
        List<Instance> instanceList = namingService.getAllInstances(serviceName);
        List<SyncServiceInstance> list = mapping(serviceName, instanceList);
        return Collections.unmodifiableList(list);
    }

    @Override
    public void registerService(SyncServiceInstance instance) throws Exception {
        if (hasCache(instance)) {
            return;
        }
        Instance ins = new Instance();
        ins.setIp(instance.getAddress());
        ins.setPort(instance.getPort());
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(instance.getMetadata());
        metaData.put(InstanceId, StringUtils.isNotEmpty(instance.getInstanceId()) ? instance.getInstanceId() : SyncStringUtil.genId(instance));
        ins.setMetadata(metaData);
        namingService.registerInstance(instance.getServiceName(), ins);
        addCache(instance);
    }

    @Override
    public void deregisterService(SyncServiceInstance instance) throws Exception {
        namingService.deregisterInstance(instance.getServiceName(), instance.getAddress(), instance.getPort());
        removeCache(instance);
    }

    @Override
    public void subscribe(String serviceName, InstanceChanged instanceChanged) throws Exception {
        namingService.subscribe(serviceName, new EventListener() {
            @Override
            public void onEvent(Event event) {
                NamingEvent namingEvent = (NamingEvent) event;
                List<Instance> instances = namingEvent.getInstances();
                List<SyncServiceInstance> list = instances.stream().map(new Function<Instance, SyncServiceInstance>() {
                    @Override
                    public SyncServiceInstance apply(Instance instance) {
                        Map<String, String> metadata = instance.getMetadata();
                        if (null == metadata) {
                            metadata = new HashMap<>();
                        }
                        String instanceId = metadata.get(InstanceId);
                        SyncServiceInstance serviceInstance = new SyncServiceInstance();
                        serviceInstance.setServiceName(serviceName);
                        serviceInstance.setInstanceId(instanceId);
                        serviceInstance.setAddress(instance.getIp());
                        serviceInstance.setPort(instance.getPort());
                        serviceInstance.setMetadata(metadata);
                        return serviceInstance;
                    }
                }).collect(Collectors.toList());
                instanceChanged.onChanged(NacosRegistry.this, serviceName, list);
            }
        });
    }

    private List<SyncServiceInstance> mapping(String serviceName, List<Instance> instances) {
        return instances.stream().map(new Function<Instance, SyncServiceInstance>() {
            @Override
            public SyncServiceInstance apply(Instance instance) {
                Map<String, String> metadata = instance.getMetadata();
                if (null == metadata) {
                    metadata = new HashMap<>();
                }
                String instanceId = metadata.get(InstanceId);
                SyncServiceInstance serviceInstance = new SyncServiceInstance();
                serviceInstance.setServiceName(serviceName);
                serviceInstance.setInstanceId(instanceId);
                serviceInstance.setAddress(instance.getIp());
                serviceInstance.setPort(instance.getPort());
                serviceInstance.setMetadata(metadata);
                return serviceInstance;
            }
        }).collect(Collectors.toList());
    }
}
