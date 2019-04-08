package io.github.jaspercloud.discovery.syncer.support;

import io.github.jaspercloud.discovery.syncer.util.Constants;
import io.github.jaspercloud.discovery.syncer.util.SyncStringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.utils.ZKPaths;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryProperties;
import org.springframework.cloud.zookeeper.discovery.ZookeeperInstance;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpringBootZookeeperRegistry extends CacheRegistry implements Registry {

    private CuratorFramework client;
    private ZookeeperDiscoveryProperties properties;
    private InstanceSerializer<ZookeeperInstance> serializer;

    @Override
    public String name() {
        return "spring-boot-zk";
    }

    public SpringBootZookeeperRegistry(CuratorFramework client, ZookeeperDiscoveryProperties properties) {
        this.client = client;
        this.properties = properties;
        this.serializer = new JsonInstanceSerializer<>(ZookeeperInstance.class);
    }

    @Override
    public List<String> getServiceList() throws Exception {
        List<String> serviceList = client.getChildren().forPath(properties.getRoot());
        if (null == serviceList) {
            serviceList = new ArrayList<>();
        }
        return Collections.unmodifiableList(serviceList);
    }

    @Override
    public List<SyncServiceInstance> getInstanceList(String serviceName) throws Exception {
        List<String> children = client.getChildren().forPath(pathForName(serviceName));
        List<SyncServiceInstance> instanceList = mapping(serviceName, children);
        return Collections.unmodifiableList(instanceList);
    }

    @Override
    public void registerService(SyncServiceInstance instance) throws Exception {
        if (hasCache(instance)) {
            return;
        }
        Map<String, String> metaData = new HashMap<>();
        metaData.putAll(instance.getMetadata());
        metaData.put(Constants.InstanceId, StringUtils.isNotEmpty(instance.getInstanceId()) ? instance.getInstanceId() : SyncStringUtil.genId(instance));
        metaData.put(Constants.RegistryName, name());
        String id = StringUtils.isNotEmpty(instance.getInstanceId()) ? instance.getInstanceId() : SyncStringUtil.genId(instance);
        org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> zookeeperInstanceServiceInstance = org.apache.curator.x.discovery.ServiceInstance.<ZookeeperInstance>builder()
                .name(instance.getServiceName())
                .payload(new ZookeeperInstance(id, instance.getServiceName(), metaData))
                .address(instance.getAddress())
                .port(instance.getPort())
                .uriSpec(new UriSpec(properties.getUriSpec()))
                .build();
        byte[] bytes = serializer.serialize(zookeeperInstanceServiceInstance);
        String path = pathForInstance(instance.getServiceName(), id);
        try {
            CreateMode mode = CreateMode.EPHEMERAL;
            client.create().creatingParentContainersIfNeeded().withMode(mode).forPath(path, bytes);
            addCache(instance);
        } catch (KeeperException.NodeExistsException e) {
            client.delete().forPath(path);
        }
    }

    @Override
    public void deregisterService(SyncServiceInstance instance) throws Exception {
        try {
            String id = SyncStringUtil.genId(instance);
            String path = pathForInstance(instance.getServiceName(), id);
            client.delete().guaranteed().forPath(path);
            removeCache(instance);
        } catch (KeeperException.NoNodeException ignore) {
            // ignore
        }
    }

    @Override
    public void subscribe(String serviceName, InstanceChanged instanceChanged) throws Exception {
        client.getChildren().usingWatcher(new CuratorWatcher() {
            @Override
            public void process(WatchedEvent event) throws Exception {
                String path = event.getPath();
                if (StringUtils.isEmpty(path)) {
                    return;
                }
                String service = ZKPaths.getNodeFromPath(path);
                if (StringUtils.isEmpty(service)) {
                    return;
                }
                client.getChildren().usingWatcher(this).forPath(path);
                List<SyncServiceInstance> list = filterMirrorInstanceList(getInstanceList(service));
                InstanceChangedEvent changedEvent = processInstanceChanged(service, list);
                instanceChanged.onChanged(SpringBootZookeeperRegistry.this, changedEvent);
            }
        }).forPath(pathForName(serviceName));
    }

    private List<SyncServiceInstance> mapping(String serviceName, List<String> children) {
        if (null == children) {
            children = new ArrayList<>();
        }
        return children.stream().map(new Function<String, SyncServiceInstance>() {
            @Override
            public SyncServiceInstance apply(String id) {
                try {
                    return getInstance(serviceName, id);
                } catch (Exception e) {
                    return null;
                }
            }
        }).filter(e -> null != e).collect(Collectors.toList());
    }

    private SyncServiceInstance getInstance(String name, String id) throws Exception {
        String path = pathForInstance(name, id);
        try {
            byte[] bytes = client.getData().forPath(path);
            org.apache.curator.x.discovery.ServiceInstance<ZookeeperInstance> instance = serializer.deserialize(bytes);
            Map<String, String> metadata = instance.getPayload().getMetadata();
            SyncServiceInstance serviceInstance = new SyncServiceInstance();
            serviceInstance.setServiceName(name);
            serviceInstance.setInstanceId(instance.getId());
            serviceInstance.setAddress(instance.getAddress());
            serviceInstance.setPort(instance.getPort());
            serviceInstance.setMetadata(metadata);
            return serviceInstance;
        } catch (KeeperException.NoNodeException ignore) {
            // ignore
        }
        return null;
    }

    private String pathForInstance(String name, String id) {
        return ZKPaths.makePath(pathForName(name), id);
    }

    private String pathForName(String name) {
        return ZKPaths.makePath(properties.getRoot(), name);
    }
}
