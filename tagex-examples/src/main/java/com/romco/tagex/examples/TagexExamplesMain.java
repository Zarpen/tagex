package com.romco.tagex.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map; 
import java.io.IOException; 
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import fi.iki.elonen.NanoHTTPD;

public class TagexExamplesMain extends NanoHTTPD{
	private static final int port = 7080;
	private HashMap<NanoHTTPD.Method,ArrayList<TagexRoute>> routes = 
			new HashMap<NanoHTTPD.Method,ArrayList<TagexRoute>>();
	
	public TagexExamplesMain() throws IOException {
	    super(port);
	    
	    for(NanoHTTPD.Method method : NanoHTTPD.Method.values()){
	    	routes.put(method,new ArrayList<TagexRoute>());
	    }
	    
	    registerControllers();
	    
	    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	    System.out.println( "\nRunning! Point your browers to http://localhost:"+port+"/ \n" );
	}
	

	public static void main(String[] args) {
	    try {
	        new TagexExamplesMain();
	    }
	    catch( IOException ioe ) {
	        System.err.println( "Couldn't start server:\n" + ioe );
	    }
	}


	@Override
	public Response serve(IHTTPSession session) {
		Response result = null;
		
		TagexRoute requestRoute = matchRoute(session.getMethod(),normalizeUri(session.getUri()));
		
		if(requestRoute != null){
			try {
				result = newFixedLengthResponse(
						(String) requestRoute.getHandlerMethod().invoke(requestRoute.getHandler(), 
						requestRoute.getHandlerParams()));
			} catch (Exception e){
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String msg = "<html><body><h1>500 - Server internal error: "+sw.toString()+"</h1>\n";
				result = newFixedLengthResponse( msg + "</body></html>\n" );
			}
		}else{
			String msg = "<html><body><h1>404 - The route can not be handled by the examples server</h1>\n";
		    Map<String, String> parms = session.getParms();
		    result = newFixedLengthResponse( msg + "</body></html>\n" );
		}
		
		return result;
	}
	
	private void registerControllers(){
		registerController(TagexBooksController.class);
	}
	
	private void registerController(Class<?> controller){
		for(java.lang.reflect.Method me : controller.getDeclaredMethods()){
			for(Annotation an : me.getDeclaredAnnotations()){
				if(an instanceof Route){
					Route routeAn = (Route) an;
					try{
						Object handler = controller.newInstance();
						addRoute(routeAn.method(),routeAn.value(),handler,me);
					}catch(Exception e){}
			    }
			}
		}
	}
	
	private void addRoute(NanoHTTPD.Method method,String uri,Object handler,
			java.lang.reflect.Method handlerMethod){
		routes.get(method).add(new TagexRoute(method,getRouteTokens(uri),handler,handlerMethod));
	}
	
	private ArrayList<String> getRouteTokens(String uri){
		ArrayList<String> tokens = new ArrayList<String>();
		if(uri.indexOf("/") >= 0){
			String[] temp = uri.split("/");
			for(int i = 0;i < temp.length;i++) tokens.add(temp[i]);
		}else{
			tokens.add(uri);
		}
		return tokens;
	}
	
	private TagexRoute matchRoute(NanoHTTPD.Method method,String uri){
		TagexRoute result = null;
		ArrayList<TagexRoute> routesPerMethod = routes.get(method);
		
		ArrayList<String> requestTokens = getRouteTokens(uri);
		for(TagexRoute tr : routesPerMethod){
			ArrayList<String> trTokens = tr.getTokens();
			if(trTokens.size() == requestTokens.size()){
				boolean returnRoute = true;
				for(int i = 0;i < trTokens.size();i++){
					if(trTokens.get(i).indexOf(":") < 0){
						if(!trTokens.get(i).equals(requestTokens.get(i))){
							returnRoute = false;
						}
					}
				}
				if(returnRoute){
					result = tr;
					
					for(int i = 0;i < trTokens.size();i++){
						if(trTokens.get(i).indexOf(":") >= 0){
							result.getHandlerParams().put(trTokens.get(i).replace(":", ""),requestTokens.get(i));
						}
					}
					
					break;
				}
			}
		}
		
		return result;
	}
	
	private String normalizeUri(String value) {
        if (value == null) {
            return value;
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;

    }
}