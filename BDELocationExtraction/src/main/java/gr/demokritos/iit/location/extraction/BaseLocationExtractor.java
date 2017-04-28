package gr.demokritos.iit.location.extraction;

import java.util.ArrayList;

/**
 * Created by nik on 4/27/17.
 */
public class BaseLocationExtractor {
    ILocationExtractor.LE_RESOURCE_TYPE RequiredResource;
    String objective;

    public ILocationExtractor.LE_RESOURCE_TYPE getRequiredResource() {
        return RequiredResource;
    }

    public String ChooseRequiredResource(String url, String text)
    {
        String res = "";
        if ( RequiredResource.equals(ILocationExtractor.LE_RESOURCE_TYPE.URL))
            res = url;
        else if(RequiredResource.equals(ILocationExtractor.LE_RESOURCE_TYPE.TEXT))
        {
            if(text == null || text.isEmpty())
            {
                System.out.println(" (!) Empty or null clean text");
            }
            else
                res = text;
        }
        return res;
    }
    public boolean canHandleResource(ILocationExtractor.LE_RESOURCE_TYPE res)
    {

        boolean val = RequiredResource.equals(res);
        if (val == false)
        {
            System.err.println("[" + objective + "] extractor cannot handle required resource type: " + res.toString());
        }
        return val;
    }
}
