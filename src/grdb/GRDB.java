package grdb;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GRDB {
	private static Map<Integer, Book> books = new HashMap<>();
	private static Map<Integer, Author> authors = new HashMap<>();
	private static Map<Integer, Series> series = new HashMap<>();
	private static Map<Integer, Work> works = new HashMap<>();

	public static Set<Integer> books() {
		return books.keySet();
	}

	public static Set<Integer> authors() {
		return authors.keySet();
	}

	public static Set<Integer> series() {
		return series.keySet();
	}

	public static Set<Integer> works() {
		return works.keySet();
	}

	public static Book book(int id) {
		Book b = books.get(id);
		if (b == null)
			books.put(id, b = new Book(id));
		return b;
	}

	public static Author author(int id) {
		Author a = authors.get(id);
		if (a == null)
			authors.put(id, a = new Author(id));
		return a;
	}

	public static Series series(int id) {
		Series s = series.get(id);
		if (s == null)
			series.put(id, s = new Series(id));
		return s;
	}

	public static Work work(int id) {
		Work w = works.get(id);
		if (w == null)
			works.put(id, w = new Work(id));
		return w;
	}
}
