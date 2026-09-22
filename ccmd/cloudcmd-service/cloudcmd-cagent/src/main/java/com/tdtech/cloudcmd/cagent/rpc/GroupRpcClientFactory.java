package com.tdtech.cloudcmd.cagent.rpc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.tdtech.cloudcmd.cagent.exception.PinGaoServiceException;
import com.tdtech.cloudcmd.cagent.server.frame.CdcFrame;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
public class GroupRpcClientFactory {

    @Resource
    private RestTemplate restTemplate;

    public GroupRpcClient newRpcClient(String ip, Integer port) {
        Objects.requireNonNull(ip);
        Objects.requireNonNull(port);
        return new GroupRpcClientImpl(ip, port);
    }

    public GroupRpcClient newRpcClient(String host) {
        Objects.requireNonNull(host);
        String[] split = host.split(":");
        return new GroupRpcClientImpl(split[0], Integer.valueOf(split[1]));
    }

    @AllArgsConstructor
    private class GroupRpcClientImpl implements GroupRpcClient {

        private String ip;
        private Integer port;

        @Override
        @SneakyThrows(URISyntaxException.class)
        public List<ChannelUpdateDTO> getAllLocalChannel() {
            URI uri = new URI("http", null, ip, port, "/channel/all", null, null);
            RequestEntity<Void> build = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();
            ResponseEntity<List<ChannelUpdateDTO>> exchange =
                restTemplate.exchange(build, new ParameterizedTypeReference<List<ChannelUpdateDTO>>() {});
            if (!exchange.getStatusCode().equals(HttpStatus.OK)) {
                throw new PinGaoServiceException("/channel/all http error:" + exchange.getStatusCodeValue());
            }
            return exchange.getBody();
        }

        @Override
        @SneakyThrows(URISyntaxException.class)
        public void onDistributeTask(TaskDTO task) {
            URI uri = new URI("http", null, ip, port, "/task/distribute", null, null);
            post(uri, task);
        }

        @Override
        @SneakyThrows(URISyntaxException.class)
        public void reportChannelUpdate(List<ChannelUpdateDTO> channelList) {
            URI uri = new URI("http", null, ip, port, "/channel/report", null, null);
            post(uri, channelList);
        }

        @Override
        @SneakyThrows(URISyntaxException.class)
        public void reportTask(CdcFrame data) {
            URI uri = new URI("http", null, ip, port, "/task/report", null, null);
            post(uri, data);
        }

        private <T> void post(URI uri, T t) {
            RequestEntity<T> body = RequestEntity.post(uri).body(t);
            ResponseEntity<Void> exchange = restTemplate.exchange(body, Void.class);
            if (!exchange.getStatusCode().equals(HttpStatus.OK)) {
                throw new PinGaoServiceException("http error:" + uri.getPath() + " --- " + exchange.getStatusCodeValue());
            }
        }
    }

}
