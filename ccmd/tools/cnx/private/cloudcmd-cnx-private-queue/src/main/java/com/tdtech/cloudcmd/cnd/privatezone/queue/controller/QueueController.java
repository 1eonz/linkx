package com.tdtech.cloudcmd.cnd.privatezone.queue.controller;

import org.reactivestreams.Publisher;

import com.tdtech.cloudcmd.cnd.privatezone.queue.auth.FakeAuth;
import com.tdtech.cloudcmd.cnd.privatezone.queue.auth.UserInfo;
import com.tdtech.cloudcmd.cnd.privatezone.queue.redis.RedisQueue;
import com.tdtech.cloudcmd.cnd.privatezone.queue.repo.Organization;
import com.tdtech.cloudcmd.cnd.privatezone.queue.repo.OrganizationRepository;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller("/cnx/private")
public class QueueController {

    @Inject
    RedisQueue redisQueue;
    @Inject
    OrganizationRepository organizationRepository;
    @Inject
    FakeAuth fakeAuth;

    @Get("/queue/pull/{channel}")
    @Produces("application/json")
    @Operation(summary = "从指定频道拉取所有消息", description = "从Redis队列中拉取指定频道的所有消息")
    @ApiResponse(responseCode = "200", description = "成功拉取消息列表",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    public Flux<String> pullAll(@PathVariable String channel) {
        return redisQueue.pullAll(channel);
    }

    @Get("/orgs")
    @Produces("application/json")
    @Operation(summary = "获取所有组织信息", description = "获取系统中所有组织的详细信息")
    @ApiResponse(responseCode = "200", description = "成功获取组织列表",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = Organization.class)))
    public Publisher<Organization> allOrgs() {
        return organizationRepository.findAll();
    }

    @Post("/auth")
    @Produces("application/json")
    @Operation(summary = "用户认证", description = "使用令牌进行用户身份验证")
    @ApiResponse(responseCode = "200", description = "认证成功，返回用户信息",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfo.class)))
    public Publisher<UserInfo> auth(@Body Mono<String> token) {
        return fakeAuth.auth(token);
    }

}