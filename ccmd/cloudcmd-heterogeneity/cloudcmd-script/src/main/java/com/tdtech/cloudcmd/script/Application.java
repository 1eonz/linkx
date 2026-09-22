package com.tdtech.cloudcmd.script;

import io.micronaut.runtime.Micronaut;

public class Application {

    public static void main(String[] args) {
        Micronaut.build(args)//
                .banner(false)//
                .eagerInitSingletons(true)//
                .eagerInitConfiguration(true)//
                .mainClass(Application.class)//
                .start();
    }
}