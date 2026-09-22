package com.cs.datatool.service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import com.cs.datatool.repo.SimpleMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Entrypoint implements ApplicationRunner {

    private static final String[] tables = new String[] {"tb_task", "tb_task_response", "tb_task_status_history"};

    private final SimpleMapper simpleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        var sourceArgs = args.getSourceArgs();
        if (sourceArgs.length == 0) {
            log.error("Need Action Param: dump|restore");
            return;
        }
        switch (sourceArgs[0]) {
            case "dump" -> dump();
            case "restore" -> restore();
            default -> log.error("Param:{} not Allowed here,Examples: dump|restore", sourceArgs[0]);
        }
    }

    @SneakyThrows
    public void dump() {
        Files.createDirectories(Path.of("/app/dump"));
        for (var table : tables) {
            var json = simpleMapper.selectAll(table);
            var fileName = "/app/dump/" + table + ".json";
            try (var os = new FileOutputStream(fileName)) {
                objectMapper.writeValue(os, json);
                log.warn("write file:{} size:{}", fileName, json.size());
            }
        }
    }

    @SneakyThrows
    public void restore() {
        for (var table : tables) {
            var fileName = "/app/dump/" + table + ".json";
            if (!Files.exists(Path.of(fileName))) {
                log.warn("data file not exists:{}", fileName);
                continue;
            }
            var data = objectMapper.readValue(new File(fileName), new TypeReference<List<Map<String, Object>>>() {});
            log.warn("read file:{} size:{}", fileName, data.size());
            IntStream.range(0, (data.size() + 499) / 500)
                .mapToObj(i -> data.subList(i * 500, Math.min((i + 1) * 500, data.size()))).forEach(batch -> {
                    // 在这里处理每批数据，每批最多500条记录
                    simpleMapper.insertBatch(table, batch);
                    log.warn("write to {} batch:{}", table, batch.size());
                });
        }
    }

}
