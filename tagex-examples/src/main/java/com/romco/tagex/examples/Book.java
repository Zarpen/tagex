package com.romco.tagex.examples;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;

public class Book {
	private String title;
	private ArrayList<String> authors = new ArrayList<String>();
	private int id;
	private String friendlyTitle;
	
	public Book(){}
	
	public Book(int id,String title){
		this.title = title;
		this.id = id;
		this.friendlyTitle = toFriendlyURL(title);
	}
	
	public Book(int id,String title,ArrayList<String> authors){
		this.title = title;
		this.authors = authors;
		this.id = id;
		this.friendlyTitle = toFriendlyURL(title);
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getFriendlyTitle(){
		return friendlyTitle;
	}
	
	public int getId(){
		return id;
	}
	
	public ArrayList<String> getAuthors(){
		return authors;
	}
	
	public void setTitle(String title){
		this.title = title;
		this.friendlyTitle = toFriendlyURL(title);
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public void setAuthors(ArrayList<String> authors){
		this.authors = authors;
	}
	
	private String toFriendlyURL(String string) {
	    return Normalizer.normalize(string.toLowerCase(), Form.NFD)
	        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
	        .replaceAll("[^\\p{Alnum}]+", "-");
	}
}
