package gr.demokritos.iit.clustering.clustering;

import gr.demokritos.iit.jinsect.structs.GraphSimilarity;

import java.util.List;

import org.scify.newsum.server.clustering.BaseArticleClusterer;
import org.scify.newsum.server.model.structures.Article;

/**
 * Created by nik on 11/17/16.
 */
public class ParameterizedBaseArticleClusterer extends BaseArticleClusterer {
    double NVSThreshold;
    public ParameterizedBaseArticleClusterer(List<? extends Article> lsArticles, double thresh) {
        super(lsArticles);
        NVSThreshold = thresh;
    }
    @Override
    public boolean getMatch(Article aA, Article aB) {
        if("SciFY News".equals(aA.getCategory())) {
            return false;
        } else {
            GraphSimilarity gs = this.compareArticles(aA, aB);
            double NVS = gs.SizeSimilarity == 0.0D?0.0D:gs.ValueSimilarity / gs.SizeSimilarity;
            boolean bMatch = NVS > NVSThreshold && gs.SizeSimilarity > 0.1D;
            boolean TitleMatch = this.isPossiblySameSentence(aA.getTitle(), aB.getTitle());
            //System.out.println(String.format("%s - %s ||| nvs: %f , gs: %f , returning %d",aA.getTitle(),aB.getTitle(),NVS,gs.SizeSimilarity,bMatch || TitleMatch));
            return bMatch || TitleMatch;
        }
    }


}
