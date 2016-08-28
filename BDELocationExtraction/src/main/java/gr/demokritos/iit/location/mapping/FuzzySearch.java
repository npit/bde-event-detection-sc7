package gr.demokritos.iit.location.mapping;


        import java.io.BufferedReader;
        import java.io.FileReader;
        import java.io.IOException;
        import java.util.ArrayList;
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
    private QueryParser[] queryParsers;

    public FuzzySearch(String dataPath) throws Exception {
        System.out.print("Indexing local location extraction dataset...");
        indexDataset(dataPath);
        System.out.println("done.");
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
            String[] parts = line.split(DATASET_DELIMITER);

            Document doc = new Document();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < FIELD_NAMES.length - 1; i++) {
                sb.append(parts[i]).append(" ");
                doc.add(new Field(FIELD_NAMES[i], parts[i], TextField.TYPE_STORED));
            }
            doc.add(new Field(FIELD_NAMES[FIELD_NAMES.length - 1], sb.toString().trim(), TextField.TYPE_STORED));
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
        queryParsers = new QueryParser[FIELD_NAMES.length];
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            queryParsers[i] = new QueryParser(FIELD_NAMES[i], analyzer);
        }
    }

    public Location processQuery(String q) throws Exception{
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Searching for: {0}", q);

            for (int i = queryParsers.length - 2; 0 <= i; i--) {
                Query query = queryParsers[i].parse(q);
                TopDocs results = searcher.search(query, MAX_NUMBER_OF_RESULTS);
                ScoreDoc[] hits = results.scoreDocs;
                if (0 < hits.length) {
                    return new Location(searcher.doc(hits[0].doc));
                }
            }

            Query query = queryParsers[queryParsers.length-1].parse(normalizeLocationName(q));
            TopDocs results = searcher.search(query, MAX_NUMBER_OF_RESULTS);
            ScoreDoc[] hits = results.scoreDocs;
            if (0 < hits.length) {
                return new Location(searcher.doc(hits[0].doc));
            }
        } catch (Exception ex) {
            Logger.getLogger(FuzzySearch.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No results found");
        return null;
    }

    public static void main(String[] args) throws Exception {
        String filePath = "gadm28.csv";
        FuzzySearch fs = new FuzzySearch(filePath);
        String[] queries = {"Berlin", "Barlin", "Paris", "Vienna, Austria", "Viena", "London" , "Landon", "US", "USA"};
        for (String q : queries) {
            Location result = fs.processQuery(q);
            System.out.println(result.toString() + "\t\t" + result.getGeometry());
        }
    }
}