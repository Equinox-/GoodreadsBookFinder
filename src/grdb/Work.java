package grdb;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Work extends ID {

	public Work(int id) {
		super(id);
	}

	Set<Integer> books = new HashSet<>();
	Map<Integer, String> series = new HashMap<>();
	public Date originalPubDate = null;

	public void book(int book) {
		books.add(book);
		GRDB.book(book).work = id;
	}

	public void series(int s, String index) {
		series.put(s, index);
		GRDB.series(s).works.put(id, index);
	}

	public String bestTitle() {
		for (int i : books) {
			Book b = GRDB.book(i);
			if (b != null && b.title != null
					&& (b.lang == null || b.lang.startsWith("en")))
				return b.title;
		}
		return null;
	}

	public boolean hasLanguage(String lang) {
		for (int i : books) {
			Book b = GRDB.book(i);
			if (b.lang.startsWith(lang))
				return true;
		}
		return false;
	}
}
