package io.github.jaspercloud.discovery.syncer.config;

import com.alibaba.boot.nacos.config.properties.NacosConfigProperties;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.client.naming.net.NamingProxy;
import io.github.jaspercloud.discovery.syncer.support.NacosRegistry;
import io.github.jaspercloud.discovery.syncer.support.Registry;
import io.github.jaspercloud.discovery.syncer.support.RegistrySyncManager;
import io.github.jaspercloud.discovery.syncer.support.SpringBootZookeeperRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.zookeeper.discovery.ZookeeperDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Map;

@EnableConfigurationProperties({ZookeeperDiscoveryProperties.class, NacosConfigProperties.class})
@Configuration
public class AppConfig {

    @Bean
    public SpringBootZookeeperRegistry springBootZookeeperRegistry(CuratorFramework curator, ZookeeperDiscoveryProperties properties) {
        return new SpringBootZookeeperRegistry(curator, properties);
    }

    @Bean
    public NamingService namingService(NacosConfigProperties properties) throws Exception {
        return NamingFactory.createNamingService(properties.getServerAddr());
    }

    @Bean
    public NamingProxy namingProxy(NacosConfigProperties properties) {
        return new NamingProxy(properties.getNamespace(), properties.getEndpoint(), properties.getServerAddr());
    }

    @Bean
    public NacosRegistry nacosRegistry(NamingService namingService, NamingProxy namingProxy) {
        return new NacosRegistry(namingService, namingProxy);
    }

    @Bean
    public RegistrySyncManager registrySyncManager(Map<String, Registry> provider) {
        return new RegistrySyncManager(new ArrayList<>(provider.values()));
    }
}
