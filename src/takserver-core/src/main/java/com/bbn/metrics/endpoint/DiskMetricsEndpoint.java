package com.bbn.metrics.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import com.bbn.metrics.dto.DiskMetrics;

import java.io.File;

@Endpoint(id = "custom-disk-metrics")
public class DiskMetricsEndpoint {

    private static final String TAK_DIR_PATH = "/opt/tak/";

    @ReadOperation
    public DiskMetrics getDiskMetrics() {
        File takDir = new File(TAK_DIR_PATH);
        DiskMetrics metrics = new DiskMetrics();

        if (takDir.exists()) {
            long totalSpace = takDir.getTotalSpace();
            long freeSpace = takDir.getFreeSpace();
            long usableSpace = takDir.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;

            metrics.setTotalSpace(totalSpace);
            metrics.setFreeSpace(freeSpace);
            metrics.setUsableSpace(usableSpace);
            metrics.setUsedSpace(usedSpace);
        }

        return metrics;
    }
}
