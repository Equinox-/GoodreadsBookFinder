package grdb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Series extends ID {
	public Series(int id) {
		super(id);
	}

	public String title;
	Map<String, Set<Integer>> books = new HashMap<>();
	Map<Integer, String> works = new HashMap<>();

	public Map<String, Set<Integer>> books() {
		// For every work make sure its books are present
		for (Entry<Integer, String> e : works.entrySet())
			for (int book : GRDB.work(e.getKey()).books)
				pbook(e.getValue(), book);

		return books;
	}

	public Map<Integer, String> works() {
		// For every book make sure the work is present
		for (Entry<String, Set<Integer>> e : books.entrySet())
			for (int book : e.getValue())
				works.put(GRDB.book(book).work, e.getKey());
		return works;
	}

	void pbook(String index, int book) {
		Set<Integer> bs = books.get(index);
		if (bs == null)
			books.put(index, bs = new HashSet<>());
		bs.add(book);
	}

	public void book(String index, int book) {
		pbook(index, book);
		GRDB.book(book).series.put(id, index);
	}

	public void work(String index, int work) {
		works.put(work, index);
		GRDB.work(work).series.put(id, index);
	}

	private static final Pattern RANGE = Pattern.compile("([0-9]+)-([0-9]+)");

	public static Set<Float> fulfill(String key) {
		Set<Float> res = new HashSet<>();
		try {
			res.add(Float.valueOf(key.trim()));
		} catch (Exception e) {
			Matcher mm = RANGE.matcher(key);
			while (mm.find()) {
				try {
					int start = Integer.valueOf(mm.group(1));
					int end = Integer.valueOf(mm.group(2));
					for (int i = start; i <= end; i++)
						res.add((float) i);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return res;
	}

	public String keyForWork(int work) {
		return works.get(work);
	}

	public Set<Float> fulfilled(Set<Integer> works) {
		Set<Float> fulfilled = new HashSet<>();
		for (int i : works) {
			String key = keyForWork(i);
			if (key != null)
				fulfilled.addAll(fulfill(key));
		}
		return fulfilled;
	}

	public Set<Integer> worksExcept(Set<Integer> works) {
		Set<Float> fulfilled = fulfilled(works);
		Set<Integer> res = new HashSet<>();
		for (Entry<Integer, String> e : works().entrySet()) {
			if (!works.contains(e.getValue())) {
				// Check fulfillment table:
				Set<Float> check = fulfill(e.getValue());
				if (check.isEmpty()) {
					String lw = e.getValue().toLowerCase();
					if (lw.contains("part")) {
						// Edition of one split into multiple chunks.
						String[] chunk = lw.split("part");
						if (chunk.length == 2) {
							try {
								int prim = Integer.valueOf(chunk[0].trim());
								if (fulfilled.contains(prim))
									continue;
							} catch (Exception ex) {
							}
						}
					}
				}
				if (fulfilled.containsAll(check))
					continue;
				res.add(e.getKey());
			}
		}
		return res;
	}

	public String toString() {
		return id + "[title='" + title + "', works=" + works() + ",books="
				+ books() + "]";
	}
}
