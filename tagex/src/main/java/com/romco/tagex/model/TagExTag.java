package com.romco.tagex.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.StringBuffer;

public class TagExTag{
	protected String tagName = "";
	protected String fullTagName = "";
	protected HashMap<String,String> tagAttributes = new HashMap<String,String>();
	protected ArrayList<TagExTag> tagChilds = new ArrayList<TagExTag>();
	protected TagExTag tagParent = null;
	protected boolean isTagContent = false;

	public TagExTag(){}

	public TagExTag(String tagName){
		this.tagName = tagName;
	}

	public TagExTag(String tagName, boolean isTagContent){
		this.tagName = tagName;
		this.isTagContent = isTagContent;
	}

	public void setTagName(String tagName){
		this.tagName = tagName;
	}
	
	public void setFullTagName(String fullTagName){
		this.fullTagName = fullTagName;
	}

	public String getTagName(){
		return this.tagName;
	}

	public String getFullTagName(){
		return this.fullTagName;
	}
	
	public void setTagParent(TagExTag tagParent){
		this.tagParent = tagParent;
	}

	public TagExTag getTagParent(){
		return this.tagParent;
	}

	public void setTagAttributes(HashMap<String,String> tagAttributes){
		this.tagAttributes = tagAttributes;
	}

	public HashMap<String,String> getTagAttributes(){
		return this.tagAttributes;
	}

	public String getTagAttribute(String attributeName){
		return tagAttributes.get(attributeName);
	}

	public void setTagAttribute(String attributeName,String attributeValue){
		tagAttributes.put(attributeName,attributeValue);
	}

	public void removeTagAttribute(String attributeName){
		tagAttributes.remove(attributeName);
	}

	public void setTagChilds(ArrayList<TagExTag> tagChilds){
		this.tagChilds = tagChilds;
	}

	public ArrayList<TagExTag> getTagChilds(){
		return this.tagChilds;
	}

	public TagExTag appendTagChild(TagExTag child){
		if(this == child){
			throw new Error("Cannot append a node to itself.");
		}
		child.setTagParent(this);
		tagChilds.add(child);
		return this;
	}

	public TagExTag appendTagChild(int index,TagExTag child){
		if(this == child){
			throw new Error("Cannot append a node to itself.");
		}
		child.setTagParent(this);
		tagChilds.add(index,child);
		return this;
	}

	public TagExTag appendTagChild(ArrayList<TagExTag> children){
		if(children != null){
			for(TagExTag child: children){
				appendTagChild(child);
			}
		}
		return this;
	}

	public TagExTag appendTagChild(TagExTag... children){
		for(int i = 0; i < children.length; i++){
			appendTagChild(children[i]);
		}
		return this;
	}

	public TagExTag removeTagChild(TagExTag child){
		tagChilds.remove(child);
		return this;
	}

	public TagExTag removeTagChildren(){
		tagChilds.clear();
		return this;
	}

	public ArrayList<TagExTag> tagXPath(){
		// TODO: implement search or xpath system over childs
		return null;
	}

	public TagExTag clone(){
		TagExTag clone = null;

		if(isTagContent){
			clone = new TagExTag(tagName,isTagContent);
			clone.setFullTagName(fullTagName);
		}else{
			clone = new TagExTag(tagName);
			clone.setFullTagName(fullTagName);

			for(TagExTag child : getTagChilds()){
				TagExTag clonedNode = child.clone();
				if(clonedNode != null){
					clone.appendTagChild(clonedNode);
				}
			}

			for(String key : tagAttributes.keySet()){
				clone.setTagAttribute(key,tagAttributes.get(key));
			}

			clone.setTagParent(getTagParent());
		}
		
		return clone;
	}

	@Override
	public String toString(){
		if(!isTagContent){
			StringBuffer b = new StringBuffer("<");
			b.append(tagName);

			if(tagAttributes.size() > 0){
				for(String key: tagAttributes.keySet()){
					String value = tagAttributes.get(key);
					b.append(" ");
					b.append(key);
					if(value != null){
						b.append("=\"");
						b.append(value);
						b.append("\"");
					}
				}
			}

			// if full tag name specified, use it to determine if it is an auto-close tag
			// if it is specified on full tag, reproduce original
			
			boolean isAutoClose = false;
			if(fullTagName != null && !fullTagName.isEmpty()){
				if(fullTagName.contains("/") && fullTagName.lastIndexOf("/") == fullTagName.lastIndexOf(">")-1){
					b.append("/>");
					isAutoClose = true;
				}else{
					b.append(">");
				}
			}else{
				b.append(">");
			}
			
			if(!isAutoClose){
				if(tagChilds.size() > 0){
					for(TagExTag child: tagChilds){
						b.append(child.toString());
					}
				}

				b.append("</");
				b.append(tagName);
				b.append(">");
			}

			return b.toString();
		}else{
			return tagName;
		}
	}
}
