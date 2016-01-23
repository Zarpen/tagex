package com.romco.tagex.examples;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

public class TagexRoute {
	private NanoHTTPD.Method method;
	private ArrayList<String> tokens = new ArrayList<String>();
	private Object handler = null;
	private Method handlerMethod = null;
	private HashMap<String,String> handlerParams = new HashMap<String,String>();
	
	public TagexRoute(){}
	
	public TagexRoute(NanoHTTPD.Method method,ArrayList<String> tokens,Object handler,Method handlerMethod){
		this.method = method;
		this.tokens = tokens;
		this.handler = handler;
		this.handlerMethod = handlerMethod;
		
		for(Annotation an : handlerMethod.getDeclaredAnnotations()){
			if(an instanceof Route){
				Route routeAn = (Route) an;
				String temp = routeAn.value();
				if(temp.indexOf(":") >= 0){
					String[] anTokens = routeAn.value().split("/");
					for(int i = 0;i < anTokens.length;i++){
						if(anTokens[i].indexOf(":") >= 0){
							handlerParams.put(anTokens[i].replace(":", ""), tokens.get(i));
						}
					}
				}
				break;
		    }
		}
	}
	
	public void setMethod(NanoHTTPD.Method method){
		this.method = method;
	}
	
	public void setTokens(ArrayList<String> tokens){
		this.tokens = tokens;
	}
	
	public void setHandler(Object handler){
		this.handler = handler;
	}
	
	public void setHandlerMethod(Method handlerMethod){
		this.handlerMethod = handlerMethod;
	}
	
	public NanoHTTPD.Method getMethod(){
		return this.method;
	}
	
	public ArrayList<String> getTokens(){
		return this.tokens;
	}
	
	public Object getHandler(){
		return this.handler;
	}
	
	public Method getHandlerMethod(){
		return this.handlerMethod;
	}
	
	public HashMap<String,String> getHandlerParams(){
		return this.handlerParams;
	}
}
