/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.tdtech.cloudcmd.log.rolling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;

/**
 * @author zWX446107
 * @Description 扩展日志归档滚动策略。按照安全红线要求，归档日志文件权限为440.后续简单扩展可通过配置实现。
 * @create 2021-01-12 19:39
 */
public class SecuritySizeAndTimeBasedRollingPolicy<E> extends SizeAndTimeBasedRollingPolicy<E> {
    /**
     * @throws RolloverFailure 滚动错误
     */
    @Override
    public void rollover() throws RolloverFailure {
        super.rollover();

        // 日志滚动归档后修改归档日志的权限。
        String elapsedPeriodsFileName = getTimeBasedFileNamingAndTriggeringPolicy().getElapsedPeriodsFileName();
        switch (compressionMode) {
            case GZ:
                elapsedPeriodsFileName = elapsedPeriodsFileName + ".gz";
                break;
            case ZIP:
                elapsedPeriodsFileName = elapsedPeriodsFileName + ".zip";
                break;
        }
        changeFilePermission(elapsedPeriodsFileName);
    }

    /**
     * 文件权限设置为440
     * 
     * @param posixFile 目标文件
     */
    private void changeFilePermission(String posixFile) {
        File file = new File(posixFile);
        if (file.exists()) {
            changeFilePermission(file);
        } else {
            addInfo("File [" + posixFile + "] not exists!");
        }

    }

    /**
     * 文件权限设置为440 perms.add(PosixFilePermission.OWNER_READ); perms.add(PosixFilePermission.OWNER_WRITE);
     * perms.add(PosixFilePermission.OWNER_EXECUTE); perms.add(PosixFilePermission.GROUP_READ);
     * perms.add(PosixFilePermission.GROUP_WRITE); perms.add(PosixFilePermission.GROUP_EXECUTE);
     * perms.add(PosixFilePermission.OTHERS_READ); perms.add(PosixFilePermission.OTHERS_WRITE);
     * perms.add(PosixFilePermission.OTHERS_EXECUTE);
     * 
     * @param posixFile 目标文件
     */
    private void changeFilePermission(File posixFile) {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.GROUP_READ);
        try {
            Path path = Paths.get(posixFile.getCanonicalPath());
            Files.setPosixFilePermissions(path, perms);
        } catch (Exception e) {
            try {
                addError(("Change folder [" + posixFile.getCanonicalPath() + "] permission failed."), e);
            } catch (IOException ioException) {
            }
        }
    }
}
