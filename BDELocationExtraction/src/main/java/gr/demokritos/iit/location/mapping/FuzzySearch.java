package gr.demokritos.iit.location.mapping;

        import com.vividsolutions.jts.io.ParseException;
        import java.io.BufferedReader;
        import java.io.FileReader;
        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.List;
        import java.util.logging.Level;
        import java.util.logging.Logger;
        import org.apache.lucene.analysis.Analyzer;
        import org.apache.lucene.analysis.standard.StandardAnalyzer;
        import org.apache.lucene.document.Document;
        import org.apache.lucene.document.Field;
        import org.apache.lucene.document.TextField;
        import org.apache.lucene.index.DirectoryReader;
        import org.apache.lucene.index.IndexReader;
        import org.apache.lucene.index.IndexWriter;
        import org.apache.lucene.index.IndexWriterConfig;
        import org.apache.lucene.queryparser.classic.QueryParser;
        import org.apache.lucene.search.IndexSearcher;
        import org.apache.lucene.search.Query;
        import org.apache.lucene.search.ScoreDoc;
        import org.apache.lucene.search.TopDocs;
        import org.apache.lucene.store.Directory;
        import org.apache.lucene.store.RAMDirectory;

/**
 *
 * @author G.A.P. II
 */
public class FuzzySearch implements Constants {

    private Analyzer analyzer;
    private Directory indexDir;
    private IndexSearcher searcher;
    private QueryParser queryParser;

    public FuzzySearch(String dataPath) throws Exception {
        indexDataset(dataPath);
        prepareQueries();
    }

    private List<String> getFileLines(String filePath) throws Exception {
        final List<String> lines = new ArrayList<String>();

        final BufferedReader reader = new BufferedReader(new FileReader(filePath));
        for (String line; (line = reader.readLine()) != null;) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }

    private void indexDataset(String dataPath) throws Exception {

        analyzer = new StandardAnalyzer();
        indexDir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter w = new IndexWriter(indexDir, config);

        List<String> lines = getFileLines(dataPath);
        for (String line : lines) {
            String box = line.substring(line.lastIndexOf(DATASET_DELIMITER)+1);
            String segmentedLocation = line.substring(0, line.lastIndexOf(DATASET_DELIMITER));
            String normalizedLocation = segmentedLocation.replaceAll(DATASET_DELIMITER, " ");

            Document doc = new Document();
            doc.add(new Field(FIELD_NAMES[0], normalizedLocation, TextField.TYPE_STORED));
            doc.add(new Field(FIELD_NAMES[1], segmentedLocation, TextField.TYPE_STORED));
            doc.add(new Field(FIELD_NAMES[2], box, TextField.TYPE_STORED));
            w.addDocument(doc);
        }

        w.close();
    }

    private String normalizeLocationName(String name) {
        String[] tokens = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            sb.append(token).append("~1 ");
        }
        return sb.toString().trim();
    }

    private void prepareQueries() throws IOException {
        IndexReader reader = DirectoryReader.open(indexDir);
        searcher = new IndexSearcher(reader);
        queryParser = new QueryParser(FIELD_NAMES[0], analyzer);
    }

    private Location parseHits(ScoreDoc[] hits) throws ParseException, IOException {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Total results\t:\t{0}", hits.length);

        List<Location> sortedLocations = new ArrayList<>();
        for (ScoreDoc hit : hits) {
            sortedLocations.add(new Location(hit.score, searcher.doc(hit.doc)));
        }
        Collections.sort(sortedLocations);
        return sortedLocations.get(0);
    }

    public Location processQuery(String q) {
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Searching for: {0}", q);

            Query query = queryParser.parse(q);
            TopDocs results = searcher.search(query, MAX_NUMBER_OF_RESULTS);
            ScoreDoc[] hits = results.scoreDocs;
            if (0 < hits.length) {
                return parseHits(hits);
            } else {
                Logger.getLogger(FuzzySearch.class.getName()).log(Level.INFO, "Fuzzy search activated!");
                query = queryParser.parse(normalizeLocationName(q));
                results = searcher.search(query, MAX_NUMBER_OF_RESULTS);
                hits = results.scoreDocs;
                if (0 < hits.length) {
                    return parseHits(hits);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(FuzzySearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No results found");
        return null;
    }

    public static void main(String[] args) throws Exception {
        String filePath = "gadm28.csv";
        FuzzySearch fs = new FuzzySearch(filePath);
        String[] queries = {"Berlin", "Paris", "Wien", "Amsterdam", "Brussels", "London", "Rome", "Athens", "Warsaw", "Moscow", "Amatrice" };
        for (String q : queries) {
            Location result = fs.processQuery(q);
            if (result != null) {
                System.out.println("\n\n" + result.toString());
            }
        }
    }
}