package io.github.jaspercloud.discovery.syncer.support;

import java.util.HashMap;
import java.util.Map;

public class SyncServiceInstance {

    private String registryName;
    private String serviceName;
    private String instanceId;
    private String address;
    private Integer port;
    private Map<String, String> metadata = new HashMap();

    public String getRegistryName() {
        return registryName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public SyncServiceInstance() {
    }

    public SyncServiceInstance(String registryName, String serviceName, String instanceId, String address, Integer port) {
        this.registryName = registryName;
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.address = address;
        this.port = port;
    }

    public SyncServiceInstance(String registryName, String serviceName, String instanceId, String address, Integer port, Map<String, String> metadata) {
        this.registryName = registryName;
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.address = address;
        this.port = port;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "SyncServiceInstance{" +
                "registryName='" + registryName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
