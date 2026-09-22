package com.tdtech.cloudcmd.icp.proxy.util;

import com.tdtech.cloudcmd.icp.proxy.entity.CameraLevel;
import com.tdtech.cloudcmd.icp.proxy.entity.Department;
import com.tdtech.cloudcmd.icp.proxy.entity.IcpConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SyncUtil {

    private static final AtomicReference<List<Department>> departmentListRef = 
            new AtomicReference<>(Collections.emptyList());

    private static final AtomicReference<List<CameraLevel>> cameraLevelListRef = 
            new AtomicReference<>(Collections.emptyList());

    private static final AtomicReference<IcpConfig> icpConfigRef = 
            new AtomicReference<>(new IcpConfig());

    private static final ReentrantLock deptLock = new ReentrantLock();
    private static final ReentrantLock cameraLevelLock = new ReentrantLock();

    // 缓存对接ICP失败信息
    private static final Map<Integer, String> connectStatus = new HashMap<>();

    public static final Integer ERROR_CODE = 1;

    public static List<Department> getDepartmentList() {
        return departmentListRef.get();
    }

    public static List<CameraLevel> getCameraLevelList() {
        return cameraLevelListRef.get();
    }

    public static IcpConfig getIcpConfig() {
        return icpConfigRef.get();
    }

    public static void setIcpConfig(IcpConfig config) {
        if (config == null) {
            log.warn("icp config is null, ignore update");
            return;
        }
        icpConfigRef.set(config);
    }

    public static void initDept(List<Department> departments) {
        if (departments == null) {
            log.warn("Department list is null, ignore initialization");
            return;
        }
        deptLock.lock();
        try {
            List<Department> immutableList = List.copyOf(departments);
            departmentListRef.set(immutableList);
            log.debug("Department list initialized, size: {}", immutableList.size());
        } finally {
            deptLock.unlock();
        }
    }

    public static void initCameraLevel(List<CameraLevel> cameraLevels) {
        if (cameraLevels == null) {
            log.warn("CameraLevel list is null, ignore initialization");
            return;
        }
        cameraLevelLock.lock();
        try {
            List<CameraLevel> immutableList = Collections.unmodifiableList(new ArrayList<>(cameraLevels));
            cameraLevelListRef.set(immutableList);
            log.debug("CameraLevel list initialized, size: {}", immutableList.size());
        } finally {
            cameraLevelLock.unlock();
        }
    }

    public static void setConnectStatus(Integer code, String msg) {
        connectStatus.put(code, msg);
    }

    public static Map<Integer, String> getConnect() {
        return connectStatus;
    }

    public static String getConnectStatus(Integer code) {
        return connectStatus.get(code);
    }

    public static void clearConnectStatus() {
        connectStatus.clear();
    }

}