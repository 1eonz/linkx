package com.tdtech.cloudcmd.script.repo;

import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.r2dbc.annotation.R2dbcRepository;
import io.micronaut.data.repository.reactive.ReactiveStreamsCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@R2dbcRepository(dialect = Dialect.MYSQL)
public interface TicketClientRepository extends ReactiveStreamsCrudRepository<TicketClient, Long> {

    Mono<TicketClient> findBySystemCodeAndStatus(String systemCode, Integer status);

    Mono<TicketClient> findByIdAndStatus(Long id, Integer status);

    List<TicketClient> findByStatus(Integer Status);

}
