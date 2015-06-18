package grdb;
import java.util.HashSet;
import java.util.Set;

public class Author extends ID {

	public Author(int id) {
		super(id);
	}

	public Set<Integer> series = new HashSet<>();
	public Set<Integer> books = new HashSet<>();
	public Set<Integer> works = new HashSet<>();
}
