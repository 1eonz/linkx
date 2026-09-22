package com.tdtech.cloudcmd.cnd.privatezone.queue;

import io.micronaut.runtime.Micronaut;

public class Application {

    public static void main(String[] args) {
        Micronaut.build(args).mainClass(Application.class).banner(false).start();
    }
}