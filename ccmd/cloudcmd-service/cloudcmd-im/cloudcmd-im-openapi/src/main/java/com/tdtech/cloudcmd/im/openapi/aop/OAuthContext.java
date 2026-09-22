package com.tdtech.cloudcmd.im.openapi.aop;

import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;

public class OAuthContext {

    private static final ThreadLocal<Token> tokenLocal =new ThreadLocal<>();

    public static void setToken(Token token){
        tokenLocal.set(token);
    }

    public static Token getToken(){
        return tokenLocal.get();
    }

    public static void clear(){
        tokenLocal.remove();
    }

}
