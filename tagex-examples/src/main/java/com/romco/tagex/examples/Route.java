package com.romco.tagex.examples;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import fi.iki.elonen.NanoHTTPD;

@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
	String value();
	NanoHTTPD.Method method();
}
