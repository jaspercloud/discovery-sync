package io.github.jaspercloud.discovery.syncer.util;

import com.alibaba.nacos.common.util.Md5Utils;
import io.github.jaspercloud.discovery.syncer.support.SyncServiceInstance;

public final class SyncStringUtil {

    private SyncStringUtil() {

    }

    public static String genId(SyncServiceInstance instance) {
        String id = Md5Utils.getMD5(new StringBuilder()
                .append(instance.getServiceName())
                .append(":")
                .append(instance.getAddress())
                .append(":")
                .append(instance.getPort())
                .toString().getBytes());
        return id;
    }
}
