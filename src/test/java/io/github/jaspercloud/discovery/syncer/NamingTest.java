package io.github.jaspercloud.discovery.syncer;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.AbstractHealthChecker;
import com.alibaba.nacos.api.naming.pojo.Cluster;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class NamingTest {

    @Test
    public void test() throws Exception {
        NamingService namingService = NamingFactory.createNamingService("172.168.1.26:8848");
        Instance instance = new Instance();
//        instance.setInstanceId("test");
        instance.setIp("172.168.1.132");
        instance.setPort(33001);
        instance.setClusterName("test");
//        instance.setHealthy(true);
//        instance.setWeight(1.0);
//        Service service = new Service("nacos-test");
//        service.setApp("nacos-test");
//        service.setHealthCheckMode("client");
//        instance.setService(service);
//        Cluster cluster = new Cluster("test");
//        cluster.setServiceName("nacos_test");
//        cluster.setHealthChecker(new AbstractHealthChecker.Tcp());
//        instance.setCluster(cluster);
        namingService.deregisterInstance("nacos_test", instance.getIp(), instance.getPort());
        namingService.registerInstance("nacos_test", instance);
//        namingService.deregisterInstance("nacos_test", "172.168.1.132", 33001);
//        namingService.registerInstance("nacos_test", "172.168.1.132", 33001);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
