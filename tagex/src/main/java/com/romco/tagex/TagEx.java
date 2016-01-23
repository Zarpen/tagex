package com.romco.tagex;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;
import java.util.Stack;
import java.util.LinkedHashMap;
import com.romco.tagex.model.TagExTag;

public class TagEx{
	protected String allowedChars = "a-zA-Z0-9 ='\"\\/:.*\\-;_\\[\\]\\?{}\\^\\+\\-,\\|@#~$%&()!";
	protected String noSpaceChars = "a-zA-Z0-9='\"\\/:.*\\-;_\\[\\]\\?{}\\^\\+\\-,\\|@#~$%&()!";
	private TagExTag tagTree;
	private ArrayList<String> completeProviders = new ArrayList<String>();
	private LinkedHashMap<String,HashMap<String,String>> contentProviders = 
		new LinkedHashMap<String,HashMap<String,String>>();

	private ArrayList<String> openTags = new ArrayList<String>();
	private ArrayList<String> closeTags = new ArrayList<String>();
	private ArrayList<String> allTags = new ArrayList<String>();
	private ArrayList<String> autoCloseTags = new ArrayList<String>();
	private ArrayList<String> tagContents = new ArrayList<String>();
	private String target;
	private Object tagHandler;
	
	public TagEx(Object source,boolean normalize,Object handler) throws IOException, URISyntaxException{
		if(source instanceof URL){
			target = stringLocalResource((URL) source);
		}else if(source instanceof String){
			target = (String) source;
		}else{
			throw new Error("Invalid source object passed to TagEx");
		}
		
		tagHandler = handler;
	  
		Matcher m = Pattern.compile("(<\\/{0,}["+allowedChars+"]{0,} {0,}\\/{0,}>)").matcher(target);
		while (m.find()) {
			allTags.add(m.group().trim());
		}
		
		// replace auto closed tags with normal close for easy manipulation
		HashMap<String,String> replaces = new HashMap<String,String>();
		autoCloseTags = new ArrayList<String>();
		m = Pattern.compile("(< {0,}["+noSpaceChars+"]{0,} {0,}\\/{1}>)").matcher(target);
		while (m.find()) {
			String match = m.group().trim();
			autoCloseTags.add(match.trim());
			String tagName = getTagName(match);
			if(tagName != null){
			  replaces.put(match, "></"+getTagName(match)+">");
			}
		}
		
		if(normalize){
			for(String pattern : replaces.keySet()){
	            target = 
	                    target.replace(pattern, pattern.replace("/>", replaces.get(pattern)));
	        }
		}
	      
		m = Pattern.compile("(<[^\\/] {0,}["+allowedChars+"]{0,} {0,}[^\\/]>)").matcher(target);
		while (m.find()) {
			openTags.add(m.group().trim());
		}
		
		m = Pattern.compile("(<\\/ {0,}["+allowedChars+"]{0,} {0,}>)").matcher(target);
		while (m.find()) {
			closeTags.add(m.group().trim());
		}

		m = Pattern.compile("(?:>)(["+allowedChars+"]{1,})(?:<)").matcher(target);
		while (m.find()) {
			tagContents.add(m.group(1));
		}
	}

	public ArrayList<String> getAllTags(){
		return allTags;
	}

	public ArrayList<String> getOpenTags(){
		return openTags;
	}

	public ArrayList<String> getCloseTags(){
		return closeTags;
	}

	public ArrayList<String> getAutoCloseTags(){
		return autoCloseTags;
	}

	public ArrayList<String> getTagContents(){
		return tagContents;
	}

	public String getTagName(String tag){
		Matcher match = 
			Pattern.compile("(?:<)(?:[ \\/]){0,}(["+noSpaceChars+"]{0,})(?:[ \\/]){0,}").matcher(tag);
		if(match.find()){
			return match.group(1).replace("/","").replace("<","").replace(">","").replace("\\","").trim();
		}else{
			return null;
		}
	}

	public HashMap<String,String> getTagAttributes(String tag){
		HashMap<String,String> result = new HashMap<String,String>();

		String[] attrs = tag.split(" ");
		for(int i = 0;i < attrs.length;i++){
			if(attrs[i].indexOf("=") >= 0){
				String[] tokens = attrs[i].split("=");
				String finalName = 
						tokens[0].replace("<","").replace(">","").replace("\"","").trim();
				String finalValue = 
						tokens[1].replace("<","").replace(">","").replace("\"","").trim();
				result.put(finalName,finalValue);
			}
		}
		return result;
	}

	public void toTagTree(){
		if(allTags.isEmpty()) return;
		
		contentProviders.clear();
		Stack<TagExTag> stack = new Stack<TagExTag>();
		TagExTag root = null;

		int count = 0;
		for(String tag : allTags){
			String tagName = getTagName(tag);
			
			if(tagName != null && !tagName.isEmpty()){
				if(tag.contains("/") && tag.indexOf("/") == 1){
					stack.pop();
				}else{
					TagExTag node = new TagExTag(tagName.toLowerCase());
					node.setFullTagName(tag);

					HashMap<String,String> tagAttrs = getTagAttributes(tag);
					node.setTagAttributes(tagAttrs);
					String contentProvider = tagAttrs.get("tagex-content-provider");
					if(contentProvider != null && !contentProvider.isEmpty() && !contentProviders.containsKey(contentProvider)){
						HashMap<String,String> options = new HashMap<String,String>();
						options.put("call",contentProvider);
						contentProviders.put(contentProvider,options);
					}

					if(count < tagContents.size()){
						String tagContent = tagContents.get(count);
						if(tagContent != null && !tagContent.isEmpty()){
							node.appendTagChild(new TagExTag(tagContent,true));
						}
					}

					if(!stack.isEmpty()){
						TagExTag parent = stack.peek();
						parent.appendTagChild(node);
					}else{
						root = node;
					}
					
					if(!(tag.contains("/") && tag.lastIndexOf("/") == tag.lastIndexOf(">")-1)){
						stack.push(node);
					}
				}
			}
			count++;
		}

		tagTree = root;
	}
  
	public void fillTagTree(){
		if(contentProviders.isEmpty() || tagTree == null || tagHandler == null) return;
		
		// fill tag attributes
		parseAttributes(tagTree);
		
		// iterate content providers
		
		completeProviders.clear();
		for(String key : contentProviders.keySet()){
			// search structure element for this content provider
			if(!completeProviders.contains(key)){
				TagExTag targetNode = getStructureObject(tagTree,key);
				if(targetNode != null){
					getAdaptedContent(contentProviders.get(key),targetNode,new Object());
				}
			}
		}
	}

	public TagExTag getTagTree(){
		return tagTree;
	}

	public String getTagTreeString(){
		return tagTree.toString();
	}

	protected String stringLocalResource(URL url) throws IOException, URISyntaxException{
		String content = "";
	    InputStream inputStream = url.openConnection().getInputStream();
	    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
	    String inputLine;
	 
	    while ((inputLine = in.readLine()) != null) {
	    	content = content + inputLine;
	    }
	    in.close();
		return content;
	}

	protected String stringLocalResource(String url) throws IOException, URISyntaxException{
		String content = "";
		URL finalUrl;
		try{
			finalUrl = new URL(url);
		}catch(Exception e){
			// try with class loader
			finalUrl = getClass().getClassLoader().getResource(url);
		}
	    InputStream inputStream = finalUrl.openConnection().getInputStream();
	    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
	    String inputLine;
	 
	    while ((inputLine = in.readLine()) != null) {
	    	content = content + inputLine;
	    }
	    in.close();
		return content;
	}
	
	protected byte[] byteLocalResource(String url) throws IOException, URISyntaxException{
		byte[] content = null;
		URL finalUrl;
		try{
			finalUrl = new URL(url);
		}catch(Exception e){
			// try with class loader
			finalUrl = getClass().getClassLoader().getResource(url);
		}
	    BufferedInputStream in = new BufferedInputStream(finalUrl.openConnection().getInputStream());
	    content = new byte[in.available()];
	    in.read(content, 0, content.length);
	    in.close();
		return content;
	}

	protected void parseAttrScan(TagExTag node){
		if(node.getTagAttribute("tagex-i18n") != null){
			String attrValue = node.getTagAttribute("tagex-i18n");
			String[] tokens = attrValue.split(":");
			try{
				Method m = tagHandler.getClass().getDeclaredMethod("i18nRequest",String.class,String.class);
				String t = (String) m.invoke(tagHandler,tokens[0],tokens[1]);
				node.appendTagChild(new TagExTag(t,true));
			}catch(Exception e){}
			node.removeTagAttribute("tagex-i18n");
		}else if(node.getTagAttribute("tagex-embed-resource") != null){
			String attrValue = node.getTagAttribute("tagex-embed-resource");
			try{
				node.appendTagChild(new TagExTag(stringLocalResource(attrValue),true));
			}catch(Exception e){}
			node.removeTagAttribute("tagex-embed-resource");
		}else if(node.getTagAttribute("tagex-embed-resource-img") != null){
			String attrValue = node.getTagAttribute("tagex-embed-resource-img");
			try{
				byte[] fileContent = byteLocalResource(attrValue);
				node.setTagAttribute("src", "data:image/png;base64,"+DatatypeConverter.printBase64Binary(fileContent));
			}catch(Exception e){}
			node.removeTagAttribute("tagex-embed-resource-img");
		}
	}

	protected void parseAttributes(TagExTag node){
		parseAttrScan(node);
		for(TagExTag child : node.getTagChilds()){
			parseAttributes(child);
		}
	}
	
	protected TagExTag getStructureObject(TagExTag node,String id){
		TagExTag result = null;
		if(node.getTagAttribute("tagex-content-provider") != null && node.getTagAttribute("tagex-content-provider").equals(id)) return node;
		for(TagExTag child : node.getTagChilds()){
			result = getStructureObject(child,id);
			if(result != null) break;
		}
		return result;
	}
	
	protected Object[] hasChildContentProvider(TagExTag node){
		Object[] result = null;
		if(node.getTagAttribute("tagex-content-provider") != null) 
			return new Object[] {
				node.getTagAttribute("tagex-content-provider"),
				node
			};
		for(TagExTag child : node.getTagChilds()){
			result = hasChildContentProvider(child);
			if(result != null) break;
		}
		return result;
	}

	protected Object[] hasChildScopeRequest(TagExTag node){
		Object[] result = null;
		if(node.getTagAttribute("tagex-content-provider") != null){
			return result;
		}else if(node.getTagAttribute("tagex-content-request") != null){
			return new Object[] {
				node.getTagAttribute("tagex-content-request"),
				node
			};
		}
		
		for(TagExTag child : node.getTagChilds()){
			result = hasChildScopeRequest(child);
			if(result != null) break;
		}
		return result;
	}

	protected void getAdaptedContent(
			HashMap<String, String> contentProvider,
			TagExTag node,
			Object scope){
		String call = contentProvider.get("call");
		String var = contentProvider.get("var");
		
		if(call != null){
			// content from class method
			try {
				Method m = tagHandler.getClass().getDeclaredMethod(call,Object.class);
				Object content = m.invoke(tagHandler,scope);
				
				if(content != null){
					if(content instanceof List){
						if(!((List) content).isEmpty()){
							for(Object item : (List) content){
								TagExTag producedNode = prepareNode(node,item);
								Method sm = tagHandler.getClass().getDeclaredMethod(call+"Cycle",TagExTag.class,Object.class);
								sm.invoke(tagHandler,producedNode,item);
								Object[] nextContentProvider = hasChildContentProvider(producedNode);
								while(nextContentProvider != null){
									getAdaptedContent(contentProviders.get((String) nextContentProvider[0]),
											(TagExTag) nextContentProvider[1],item);
									if(!completeProviders.contains((String) nextContentProvider[0])){
										completeProviders.add((String) nextContentProvider[0]);
									}
									nextContentProvider = hasChildContentProvider(producedNode);
								}
							}
						}else{
							// no elements on list, remove node
							node.getTagParent().removeTagChild(node);
						}
					}else{
						// still no handler for other types, remove node
						node.getTagParent().removeTagChild(node);
					}
				}else{
					// no content for this provider remove node
					node.getTagParent().removeTagChild(node);
				}
			} catch (Exception e) {
				// Exception, do not remove node but mark provided as resolved
				node.removeTagAttribute("tagex-content-provider");
			}
		}else if(var != null){
			// content from class var
		}
	}

	private TagExTag prepareNode(TagExTag node,Object scope){
		TagExTag result = null;
		if(node.getTagChilds().size() > 0){
			TagExTag newNode = node.clone();
			TagExTag superParent = node.getTagParent();
			superParent.removeTagChild(node);
			superParent.appendTagChild(newNode);
			
			newNode.removeTagAttribute("tagex-content-provider");
			
			Object[] contentRequest = hasChildScopeRequest(newNode);
			while(contentRequest != null){
				Method m = null;
				try{
					m = scope.getClass().getDeclaredMethod(
							(String) contentRequest[0]);
				}catch(Exception e){
					// probably no Such method exception, try with superclass method
					try{
						m = scope.getClass().getSuperclass().getDeclaredMethod(
								(String) contentRequest[0]);
					}catch(Exception se){}
				}
				
				if(m != null){
					Object contentRequestResult = null;
					try{
						contentRequestResult = m.invoke(scope);
					}catch(Exception e){}
					if(contentRequestResult != null){
						((TagExTag) (contentRequest[1])).
							appendTagChild(new TagExTag(contentRequestResult.toString(),true));
					}
				}
				((TagExTag) (contentRequest[1])).removeTagAttribute("tagex-content-request");
				contentRequest = hasChildScopeRequest(newNode);
			}
			
			result = newNode;
		}else{
			result = node;
		}
		
		return result;
	}
}