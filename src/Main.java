import grdb.GRDB;
import grdb.Series;
import grdb.Work;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;

public class Main {
	public static final Gson gson = new Gson();
	private static final boolean PROGRESS = false;
	private static final Pattern GRID = Pattern.compile("goodreads:([0-9]+)");

	public static Set<Integer> books() {
		Set<Integer> ids = new HashSet<>();
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader("output.csv"));
		} catch (FileNotFoundException e1) {
		}
		while (true) {
			try {
				String l = r.readLine();
				if (l == null)
					break;
				Matcher m = GRID.matcher(l);
				while (m.find()) {
					ids.add(Integer.parseInt(m.group(1)));
				}
			} catch (Exception e) {
			}
		}
		try {
			r.close();
		} catch (IOException e) {
		}
		return ids;
	}

	private static final String API_KEY = System.getProperty("KEY");
	private static long pReq = System.currentTimeMillis();

	private static Document read(String url) {
		String ourl = url;
		File out = new File("cache/" + ourl);
		url += "format=xml&key=" + API_KEY;
		try {
			URL u = new URL(url);
			if (!out.exists()) {
				out.getParentFile().mkdirs();
				long next = (long) (1000 + 500 * Math.random());
				while (pReq + next > System.currentTimeMillis())
					try {
						Thread.sleep(100L);
					} catch (InterruptedException e) {
					}

				System.out.println("FETCH " + ourl);
				FileOutputStream o = new FileOutputStream(out);
				InputStream i = u.openStream();
				byte[] buff = new byte[1024];
				int read = 0;
				while ((read = i.read(buff)) > 0)
					o.write(buff, 0, read);
				o.close();
				i.close();
				pReq = System.currentTimeMillis();
			}
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			DocumentBuilder b = f.newDocumentBuilder();
			Document o = b.parse(out);
			return o;
		} catch (Exception e) {
			System.err.println(url);
			// out.delete();
			return null;
		}
	}

	private static void children(Node n, String name, Consumer<Node> c) {
		NodeList ll = n.getChildNodes();
		for (int i = 0; i < ll.getLength(); i++) {
			Node q = ll.item(i);
			if (q.getNodeName().equals(name))
				c.accept(q);
		}
	}

	static Set<Integer> myBooks;
	static Set<Integer> myWorks = new HashSet<>();
	static Set<Integer> dropped = new HashSet<>();

	private static Date makeDate(Node node, String pfx) {
		NodeList ll = node.getChildNodes();
		String pubYear = null, pubMonth = null, pubDay = null;
		for (int i = 0; i < ll.getLength(); i++) {
			Node q = ll.item(i);
			if (q.getNodeName().equals(pfx + "_year")
					&& q.getChildNodes().getLength() > 0)
				pubYear = q.getChildNodes().item(0).getNodeValue();
			else if (q.getNodeName().equals(pfx + "_month")
					&& q.getChildNodes().getLength() > 0)
				pubMonth = q.getChildNodes().item(0).getNodeValue();
			else if (q.getNodeName().equals(pfx + "_day")
					&& q.getChildNodes().getLength() > 0)
				pubDay = q.getChildNodes().item(0).getNodeValue();
		}
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, Integer.valueOf(pubYear));
			try {
				cal.set(Calendar.MONTH, Integer.valueOf(pubMonth) - 1);
				try {
					cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(pubDay));
				} catch (Exception e) {
				}
			} catch (Exception e) {
			}
			return cal.getTime();
		} catch (Exception e) {
		}
		return null;
	}

	private static void placeCommonValues(int book, Node bookN) {
		children(bookN, "language_code", l -> {
			String lang = "en";
			if (l.getChildNodes().getLength() > 0)
				lang = l.getChildNodes().item(0).getNodeValue();

			GRDB.book(book).lang = lang;
		});
		children(bookN, "title", l -> {
			if (l.getChildNodes().getLength() > 0) {
				String ttl = l.getChildNodes().item(0).getNodeValue();
				GRDB.book(book).title = ttl;
			} else
				System.err.println("No title for: " + book);
		});
		Date pubDate = makeDate(bookN, "publication");
		if (pubDate != null)
			GRDB.book(book).pubDate = pubDate;
		{
			NodeList ll = bookN.getChildNodes();
			String pubYear = null, pubMonth = null, pubDay = null;
			for (int i = 0; i < ll.getLength(); i++) {
				Node q = ll.item(i);
				if (q.getNodeName().equals("publication_year")
						&& q.getChildNodes().getLength() > 0)
					pubYear = q.getChildNodes().item(0).getNodeValue();
				else if (q.getNodeName().equals("publication_monthr")
						&& q.getChildNodes().getLength() > 0)
					pubMonth = q.getChildNodes().item(0).getNodeValue();
				else if (q.getNodeName().equals("publication_day")
						&& q.getChildNodes().getLength() > 0)
					pubDay = q.getChildNodes().item(0).getNodeValue();
			}
			try {
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.YEAR, Integer.valueOf(pubYear));
				try {
					cal.set(Calendar.MONTH, Integer.valueOf(pubMonth) - 1);
					try {
						cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(pubDay));
					} catch (Exception e) {
					}
				} catch (Exception e) {
				}
				GRDB.book(book).pubDate = cal.getTime();
			} catch (Exception e) {
			}
		}
		children(
				bookN,
				"work",
				w -> {
					final Date opubDate = makeDate(w, "original_publication");
					children(
							w,
							"id",
							id -> {
								int idV = Integer.valueOf(id.getChildNodes()
										.item(0).getNodeValue());
								GRDB.work(idV).book(book);
								if (opubDate != null
										&& GRDB.work(idV).originalPubDate == null)
									GRDB.work(idV).originalPubDate = opubDate;
								GRDB.book(book).work(idV);
							});
				});
		children(
				bookN,
				"series_works",
				ss -> {
					children(
							ss,
							"series_work",
							sw -> {
								String userPost = null;
								{
									NodeList ll = sw.getChildNodes();
									for (int i = 0; i < ll.getLength(); i++) {
										Node q = ll.item(i);
										if (q.getNodeName().equals(
												"user_position")) {
											if (q.getChildNodes().getLength() > 0)
												userPost = q.getChildNodes()
														.item(0).getNodeValue();
										}
									}
								}
								final String userPos = userPost;
								children(
										sw,
										"series",
										s -> {
											children(
													s,
													"id",
													id -> {
														int series = Integer
																.valueOf(id
																		.getChildNodes()
																		.item(0)
																		.getNodeValue());
														Series ssss = GRDB
																.series(series);
														if (userPos != null)
															ssss.book(userPos,
																	book);
														else
															ssss.book(
																	"generic #"
																			+ ssss.books()
																					.size(),
																	book);
													});
										});
							});
				});
	}

	public static void processMyBooks() {
		int i = 0;
		for (int book : myBooks) {
			i++;
			Document o = read("https://www.goodreads.com/book/show/" + book
					+ "?");
			if (o == null)
				dropped.add(book);
			else {
				children(
						o.getDocumentElement(),
						"book",
						n -> {
							placeCommonValues(book, n);
							children(
									n,
									"authors",
									as -> {
										children(
												as,
												"author",
												a -> {
													children(
															a,
															"id",
															id -> {
																int idV = Integer
																		.valueOf(id
																				.getChildNodes()
																				.item(0)
																				.getNodeValue());
																GRDB.author(idV);
															});
												});
									});
						});

				int work = GRDB.book(book).work();
				if (work >= 0)
					myWorks.add(GRDB.book(book).work());
				else
					System.err.println("No work ID for " + book);
			}

			if (PROGRESS)
				System.out.println(i + "/" + myBooks.size() + "\t\t"
						+ (10000 * i / myBooks.size()) / 100f + "%");
		}
	}

	public static void populateAuthorMaps() {
		int i = 0;
		for (int author : GRDB.authors()) {
			if (PROGRESS)
				System.out.print("A\t" + i + "/" + GRDB.authors().size()
						+ "\t\t" + (10000 * i / GRDB.authors().size()) / 100f
						+ "%\tPages:");
			i++;
			int page = 1;
			while (true) {
				if (PROGRESS)
					System.out.print(" " + page);
				Document d = read("https://www.goodreads.com/author/list/"
						+ author + "?page=" + page + "&");
				final AtomicBoolean kill = new AtomicBoolean(false);
				children(
						d.getDocumentElement(),
						"author",
						a -> {
							children(
									a,
									"books",
									books -> {
										int end = Integer.parseInt(books
												.getAttributes()
												.getNamedItem("end")
												.getNodeValue());
										int total = Integer.parseInt(books
												.getAttributes()
												.getNamedItem("total")
												.getNodeValue());
										if (end >= total)
											kill.set(true);
										children(
												books,
												"book",
												book -> {
													children(
															book,
															"id",
															id -> {
																int bookN = Integer
																		.parseInt(id
																				.getChildNodes()
																				.item(0)
																				.getNodeValue());
																GRDB.author(author).books
																		.add(bookN);
																GRDB.book(bookN);
															});
												});
									});
						});
				if (kill.get())
					break;
				page++;
			}
			if (PROGRESS)
				System.out.println();
		}
	}

	public static void populateSeriesMaps() {
		int i = 0;
		for (int series : GRDB.series()) {
			if (PROGRESS)
				System.out.println("S\t" + i + "/" + GRDB.series().size()
						+ "\t\t" + (10000 * i / GRDB.series().size()) / 100f
						+ "%");
			i++;
			Document d = read("https://www.goodreads.com/series/" + series
					+ "?");
			children(
					d.getDocumentElement(),
					"series",
					a -> {
						children(a, "title", ttl -> {
							NodeList l = ttl.getChildNodes();
							if (l.getLength() > 1) {
								GRDB.series(series).title = l.item(1)
										.getNodeValue().replace("\n", "")
										.trim();
							}
						});
						children(
								a,
								"series_works",
								books -> {
									children(
											books,
											"series_work",
											swork -> {
												String userPost = null;
												{
													NodeList ll = swork
															.getChildNodes();
													for (int k = 0; k < ll
															.getLength(); k++) {
														Node q = ll.item(k);
														if (q.getNodeName()
																.equals("user_position")) {
															if (q.getChildNodes()
																	.getLength() > 0)
																userPost = q
																		.getChildNodes()
																		.item(0)
																		.getNodeValue();
														}
													}
												}
												final String userPos = userPost;
												children(
														swork,
														"work",
														work -> {
															children(
																	work,
																	"id",
																	id -> {
																		final int workID = Integer
																				.parseInt(id
																						.getChildNodes()
																						.item(0)
																						.getNodeValue());
																		GRDB.work(workID);
																		Series ssss = GRDB
																				.series(series);
																		if (userPos != null)
																			ssss.work(
																					userPos,
																					workID);
																		else
																			ssss.work(
																					"generic #"
																							+ ssss.works()
																									.size(),
																					workID);
																	});
														});
											});
								});
					});
		}
	}

	public static void fetchBookData() {
		int i = 0;
		int cap = GRDB.books().size();
		for (int book : GRDB.books()) {
			i++;
			try {
				Document o = read("https://www.goodreads.com/book/show/" + book
						+ "?");
				children(o.getDocumentElement(), "book", n -> {
					placeCommonValues(book, n);
				});
			} catch (Exception e) {
				System.err.println("Failed to process book " + book);
			}
			if (PROGRESS)
				System.out
						.println("BI\t"
								+ i
								+ "/"
								+ cap
								+ " ("
								+ (i + "/" + cap + ")\t" + (10000 * i / cap)
										/ 100f + "%"));
			i++;
		}
	}

	public static void generateAuthorWorkMap() {
		for (int author : GRDB.authors()) {
			for (int book : GRDB.author(author).books) {
				int work = GRDB.book(book).work();
				if (work >= 0)
					GRDB.author(author).works.add(work);
				else
					System.err.println("Failed to lookup work ID for " + book);
			}
		}
	}

	public static void printSeriesMissing(int series) {
		Set<Integer> v2 = GRDB.series(series).worksExcept(myWorks);
		for (Iterator<Integer> itr = v2.iterator(); itr.hasNext();) {
			Work b = GRDB.work(itr.next());
			if (!b.hasLanguage("en") || b.originalPubDate == null
					|| b.originalPubDate.after(new Date()))
				itr.remove();
		}
		if (v2.size() > 0) {
			Set<Float> fufilled = GRDB.series(series).fulfilled(myWorks);
			if (fufilled.size() > 0) {
				System.out
						.println("Series https://www.goodreads.com/series/show/"
								+ series
								+ " ("
								+ GRDB.series(series).title
								+ ")\tRead: " + fufilled);
				// System.out.println("\t" + GRDB.series(series));
				for (int i : v2) {
					Work work = GRDB.work(i);
					String s = work.bestTitle();
					// String key = GRDB.series(series).keyForWork(i);
					System.out
							.println("\thttps://www.goodreads.com/work/editions/"
									+ i + "\t" + s);
					/*
					 * + "\t KEY(" + key + ")\tFULFILL(" + Series.fulfill(key) +
					 * ")");
					 */
				}
			}
		}
	}

	public static void printSeriesMissing() {
		for (int s : GRDB.series()) {
			printSeriesMissing(s);
		}
	}

	public static void main(String[] args) {
		myBooks = books();
		processMyBooks();
		populateSeriesMaps();
		populateAuthorMaps();
		fetchBookData();
		generateAuthorWorkMap();
		printSeriesMissing();
	}
}
