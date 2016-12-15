package gr.demokritos.iit.clustering.model;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;
import gr.demokritos.iit.jinsect.supportUtils.linguistic.ObjectFactory;

/**
 * Created by npittaras on 15/12/2016.
 */
public class IdentifiableDocumentWordGraph extends DocumentWordGraph
{

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    int Id;
    public IdentifiableDocumentWordGraph()
    {
        super();

    }
}
