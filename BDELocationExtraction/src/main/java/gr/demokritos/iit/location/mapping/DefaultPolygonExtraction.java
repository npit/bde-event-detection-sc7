/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.location.mapping;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class DefaultPolygonExtraction implements IPolygonExtraction {

    @Override
    public String extractPolygon(String locationEntity) {
        // TODO: implement API call to get polygon
        // DEBUG
        return "POLYGON("
                + "("
                + "35.312207138944146 25.300386450309578,"
                + "35.312207138944146 19.257905982399958,"
                + "41.09114708620481 19.257905982399958,"
                + "41.09114708620481 25.300386450309578,"
                + "35.312207138944146 25.300386450309578"
                + ")"
                + ")";
    }

}
