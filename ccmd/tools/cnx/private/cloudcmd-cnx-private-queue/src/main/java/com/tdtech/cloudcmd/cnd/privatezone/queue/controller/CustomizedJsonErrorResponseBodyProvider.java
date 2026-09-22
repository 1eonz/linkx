package com.tdtech.cloudcmd.cnd.privatezone.queue.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Resource;
import io.micronaut.http.server.exceptions.response.Error;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.JsonErrorResponseBodyProvider;
import io.micronaut.json.JsonConfiguration;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

@Singleton
public class CustomizedJsonErrorResponseBodyProvider
    implements JsonErrorResponseBodyProvider<CustomizedJsonErrorResponseBodyProvider.CodeMsgJsonError> {
    private static final Logger log = LoggerFactory.getLogger(CustomizedJsonErrorResponseBodyProvider.class);

    private final boolean alwaysSerializeErrorsAsList;

    CustomizedJsonErrorResponseBodyProvider(JsonConfiguration jacksonConfiguration) {
        this.alwaysSerializeErrorsAsList = jacksonConfiguration.isAlwaysSerializeErrorsAsList();
    }

    @Override
    public @NonNull CodeMsgJsonError body(@NonNull ErrorContext errorContext, @NonNull HttpResponse<?> response) {
        CodeMsgJsonError error;
        var reason = response.reason();
        var uri = errorContext.getRequest().getUri();
        if (!errorContext.hasErrors()) {
            error = new CodeMsgJsonError(-1, reason);
            log.error("error,{} {}", uri, reason);
        } else if (errorContext.getErrors().size() == 1 && !alwaysSerializeErrorsAsList) {
            var jsonError = errorContext.getErrors().get(0);
            error = new CodeMsgJsonError(-1, jsonError.getMessage());
            log.error("error,{} {} {}", uri, jsonError.getMessage(), jsonError.getPath());
        } else {
            error = new CodeMsgJsonError(-1, reason);
            List<Resource> errors = new ArrayList<>(errorContext.getErrors().size());
            for (Error jsonError : errorContext.getErrors()) {
                errors.add(new JsonError(jsonError.getMessage()).path(jsonError.getPath().orElse(null)));
            }
            log.error("error,{} {} {}", uri, reason, errors);
        }
        return error;
    }

    @Serdeable
    @Introspected
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Produces(MediaType.APPLICATION_JSON)
    public static class CodeMsgJsonError extends JsonError {

        private Integer code;

        /**
         * @param message The message
         */
        public CodeMsgJsonError(@Nonnull Integer code, @Nonnull String message) {
            super(message);
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }
    }
}
