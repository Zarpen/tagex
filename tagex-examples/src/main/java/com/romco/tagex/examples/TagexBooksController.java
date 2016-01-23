package com.romco.tagex.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.romco.tagex.TagEx;
import com.romco.tagex.model.TagExTag;

import fi.iki.elonen.NanoHTTPD;

public class TagexBooksController{
	private static final String dirPath = "site/";
	private HashMap<String,String> routeParams = new HashMap<String,String>();
	private ArrayList<BooksStore> bookStores = new ArrayList<BooksStore>();
	private Object lastParentScope = new Object();
	// test sample i18n (usually labels will come from files or database)
	private HashMap<String,HashMap<String,String>> i18n = 
			new HashMap<String,HashMap<String,String>>();
	
	
	public TagexBooksController(){
		// create test sample data (usually data will come from database or file)
		BooksStore testBs1 = new BooksStore(1,"Test bookstore 1");
		Book bookTest1 = new Book(1,"Test book 1");
		bookTest1.getAuthors().add("Test Author 1");
		bookTest1.getAuthors().add("Test Author 2");
		testBs1.addBook("Culture", bookTest1);
		Book bookTest2 = new Book(2,"Test book 2");
		bookTest2.getAuthors().add("Test Author 3");
		bookTest2.getAuthors().add("Test Author 4");
		testBs1.addBook("Science", bookTest2);
		
		BooksStore testBs2 = new BooksStore(2,"Test bookstore 2");
		Book bookTest3 = new Book(1,"Test book 3");
		bookTest3.getAuthors().add("Test Author 5");
		bookTest3.getAuthors().add("Test Author 6");
		testBs2.addBook("Culture", bookTest3);
		Book bookTest4 = new Book(2,"Test book 4");
		bookTest4.getAuthors().add("Test Author 7");
		bookTest4.getAuthors().add("Test Author 8");
		testBs2.addBook("Science", bookTest4);
		
		bookStores.add(testBs1);
		bookStores.add(testBs2);
		
		i18n.put("en", new HashMap<String,String>());
		i18n.get("en").put("bookstores_title", "BookStores");
		i18n.get("en").put("books_title", "Books");
		i18n.get("en").put("book_title", "Book");
		i18n.get("en").put("book_authors", "Authors");
		i18n.get("en").put("go_back", "Go back");
	}
	
	// define route methods
	@Route(method = NanoHTTPD.Method.GET, value = "bookstores")
	public String bookStoresRoute(HashMap<String,String> params) throws IOException, URISyntaxException{
		routeParams = params;
		return parseTemplate("bookstores.html");
	}
	@Route(method = NanoHTTPD.Method.GET, value = "books/:bookstoreId/:categoryName")
	public String booksRoute(HashMap<String,String> params) throws IOException, URISyntaxException{
		routeParams = params;
		return parseTemplate("books.html");
	}
	
	@Route(method = NanoHTTPD.Method.GET, value = "book/:bookstoreId/:categoryName/:bookId")
	public String bookRoute(HashMap<String,String> params) throws IOException, URISyntaxException{
		routeParams = params;
		return parseTemplate("book.html");
	}
	
	private String parseTemplate(String templateFile) throws IOException, URISyntaxException{
		TagEx template = new TagEx(getClass().getClassLoader().getResource(dirPath+templateFile),false,this);
		template.toTagTree();
		template.fillTagTree();
		return "<!DOCTYPE html>"+template.getTagTreeString();
	}
	
	// define content providers
	public ArrayList<BooksStore> getBookStores(Object scope){
		return bookStores;
	}
	public void getBookStoresCycle(TagExTag node,Object scope){
		// called each content provider iteration, we can do custom node transform here
		// or store last parent provider scope
		lastParentScope = scope;
	}
	
	public List<String> getStoreCategories(Object scope){
		List<String> result = new ArrayList<String>();
		HashMap<String, ArrayList<Book>> categories = ((BooksStore) scope).getCategories();
		for(String category : categories.keySet()){
			result.add(category);
		}
		
		return result;
	}
	public void getStoreCategoriesCycle(TagExTag node,Object scope){
		// build link
		node.getTagChilds().get(0).setTagAttribute("href", "books/"+
				((BooksStore) lastParentScope).getId()+"/"+(String) scope);
	}
	
	public List<Book> getBooks(Object scope){
		return bookStores.get(Integer.parseInt((String) routeParams.get("bookstoreId"))-1)
				.getBooksByCategory(routeParams.get("categoryName"));
	}
	public void getBooksCycle(TagExTag node,Object scope){
		node.getTagChilds().get(0).setTagAttribute("href", "book/"+
				routeParams.get("bookstoreId")+"/"+routeParams.get("categoryName")+"/"+((Book) scope).getId());
	};
	
	public List<Book> getBook(Object scope){
		List<Book> result = new ArrayList<Book>();
		result.add(bookStores.get(Integer.parseInt((String) routeParams.get("bookstoreId"))-1)
				.getBooksByCategory(routeParams.get("categoryName")).get(
						Integer.parseInt((String) routeParams.get("bookId"))-1));
		return result;
	}
	public void getBookCycle(TagExTag node,Object scope){
		node.getTagChilds().get(4).getTagChilds().get(0).setTagAttribute("href", "books/"+
				routeParams.get("bookstoreId")+"/"+routeParams.get("categoryName"));
	};
	
	public List<String> getAuthors(Object scope){
		return ((Book) scope).getAuthors();
	}
	public void getAuthorsCycle(TagExTag node,Object scope){};
	
	// define general methods
	public String i18nRequest(String lng,String id){
		String result = "";
		if(i18n.get(lng) != null){
			if(i18n.get(lng).get(id) != null){
				result = i18n.get(lng).get(id);
			}
		}
		return result;
	}
}
