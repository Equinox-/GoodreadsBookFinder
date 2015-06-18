package grdb;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Book extends ID {
	public Book(int id) {
		super(id);
	}

	public String title;
	public String lang = "en";
	int work = -1;
	public Date pubDate = null;

	Map<Integer, String> series = new HashMap<>();

	public void series(int s, String index) {
		series.put(s, index);
		GRDB.series(s).pbook(index, id);
	}

	public String toString() {
		return id + "[ttl='" + title + "', work=" + work() + ", lang=" + lang
				+ ", pubDate=" + pubDate + ",series=" + series + "]";
	}

	public int work() {
		return work;
	}

	public void work(int work) {
		this.work = work;
		GRDB.work(work).books.add(id);
	}
}
