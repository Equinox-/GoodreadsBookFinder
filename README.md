# GoodreadsBookFinder
Uses a list of Goodreads book IDs to compute any new books in any of the series those books are a part of.

- First you need a file called `output.csv` in your working directory with lines containing the expression "goodreads:[0-9]+", where the numeric portion is the goodreads ID of the books you have read.
- Then you need to run the program using your goodreads API key: `java -cp [class path] -DKEY=[your API key] Main`.
- The first run this will take a long time as it builds up information for each book.
- For subsequent runs you may wish to delete the files in `cache/https:/www.goodreads.com/series/` so that the book listing for each series is refreshed.
