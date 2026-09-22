package com.tdtech.cloudcmd.sqlrunner.runner;

import com.tdtech.cloudcmd.sqlrunner.mapper.RunnerMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MainRunner implements ApplicationRunner {

    private final RunnerMapper runnerMapper;

    @Override
    public void run(ApplicationArguments args) {
        var sourceArgs = args.getSourceArgs();
        if (sourceArgs.length == 0) {
            throw new RuntimeException("缺少文件参数");
        }
        var filePath = Path.of(sourceArgs[0]);
        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        // 使用SnakeYAML读取文件
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            Yaml yaml = new Yaml();
            var yamlData = yaml.loadAs(inputStream, Config.class);

            log.info("read config:{}", yamlData);
            // 这里可以根据实际需求处理YAML数据
            processYamlData(yamlData);
        } catch (IOException e) {
            throw new RuntimeException("读取YAML文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理YAML数据的方法
     */
    private void processYamlData(Config config) {
        if (config.getCreate() != null) {
            for (var ci : config.getCreate()) {
                try {
                    runnerMapper.createIndex(ci.idx, ci.table, ci.columns);
                    log.info("create index {} on table {} columns:{} ", ci.idx, ci.table, ci.columns);
                } catch (Exception e) {
                    log.error("create Index error:{}", ci, e);
                }
            }
        }
        if (config.getDrop() != null) {
            for (var di : config.getDrop()) {
                try {
                    runnerMapper.dropIndex(di.idx, di.table);
                    log.info("drop index {} on table {}", di.idx, di.table);
                } catch (Exception e) {
                    log.error("drop Index error:{}", di, e);
                }
            }
        }
    }

    @Getter
    @Setter
    @ToString
    public static class Config {
        private List<CreateIndex> create;
        private List<DropIndex> drop;
    }

    @Getter
    @Setter
    @ToString
    public static class DropIndex {
        private String idx;
        private String table;
    }

    @Getter
    @Setter
    @ToString
    public static class CreateIndex {
        private String idx;
        private String table;
        private String columns;
    }
}
