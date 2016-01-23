package com.romco.tagex.examples;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;

public class BooksStore {
	private HashMap<String,ArrayList<Book>> categories = new HashMap<String,ArrayList<Book>>();
	private int id;
	private String title;
	private String friendlyTitle;
	
	public BooksStore(){}
	
	public BooksStore(int id,String title){
		this.title = title;
		this.id = id;
		this.friendlyTitle = toFriendlyURL(title);
	}
	
	public void addBook(String category,Book book){
		if(!categories.containsKey(category)){
			ArrayList<Book> booksList = new ArrayList<Book>();
			booksList.add(book);
			categories.put(category, booksList);
		}else{
			categories.get(category).add(book);
		}
	}
	
	public HashMap<String,ArrayList<Book>> getCategories(){
		return categories;
	}
	
	public Book getBookByTitle(String title){
		Book result = null;
		for(String key : categories.keySet()){
			ArrayList<Book> books = categories.get(key);
			for(Book b : books){
				if(b.getTitle().equals(title)){
					return b;
				}
			}
		}
		return result;
	}
	
	public Book getBookByFriendlyTitle(String friendlyTitle){
		Book result = null;
		for(String key : categories.keySet()){
			ArrayList<Book> books = categories.get(key);
			for(Book b : books){
				if(b.getFriendlyTitle().equals(title)){
					return b;
				}
			}
		}
		return result;
	}
	
	public Book getBookById(int id){
		Book result = null;
		for(String key : categories.keySet()){
			ArrayList<Book> books = categories.get(key);
			for(Book b : books){
				if(b.getId() == id){
					return b;
				}
			}
		}
		return result;
	}
	
	public ArrayList<Book> getBooksByCategory(String category){
		return categories.get(category);
	}
	
	public int getId(){
		return id;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public String getTitle(){
		return title;
	}
	
	public void setTitle(String title){
		this.title = title;
		this.friendlyTitle = toFriendlyURL(title);
	}
	
	public String getFriendlyTitle(){
		return friendlyTitle;
	}
	
	private String toFriendlyURL(String string) {
	    return Normalizer.normalize(string.toLowerCase(), Form.NFD)
	        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
	        .replaceAll("[^\\p{Alnum}]+", "-");
	}
}
