package com.tdtech.cloudcmd.msip.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class DiskUtil {

    /**
     * 计算空间是否足够
     * @param path 目录
     * @param remainingSize 剩余空间大小
     * @return true：空间足够，false：空间不足
     * @throws Exception
     */
    public static boolean isEnoughSpace(String path, Integer remainingSize) {
        String[] cmd = {"df", "-h", path};
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                br.readLine();          // 跳过表头 Filesystem
                String line = br.readLine();
                if (line == null) throw new IllegalStateException("df 解析失败");
                /*  line 示例：/dev/sda1   110G   90G   15G  86% /opt   */
                String percent = line.split("\\s+")[4].replace("%", "");
                int usage = Integer.parseInt(percent);
                return 100 - usage > remainingSize;
            }
        } catch (Exception e) {
            log.error("isEnoughSpace: {}", e.getMessage());
        }
        return true;
    }
}