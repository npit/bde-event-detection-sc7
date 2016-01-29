/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.crawlers.twitter;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.core.schemabuilder.UDTType;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import gr.demokritos.iit.crawlers.twitter.structures.TGeoLoc;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import twitter4j.Status;

/**
 *
 * @author George K. <gkiom@iit.demokritos.gr>
 */
public class Test {

    public static void main(String[] args) {

//        UDTType udtLiteral = SchemaBuilder.udtLiteral(TGeoLoc.UDT_LITERAL);
//        String asCQLString = udtLiteral.asCQLString();
//        System.out.println(asCQLString);
//        Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").withClusterName("Test Cluster").build();
//        Session session =  cluster.connect("bde_twitter");
//        MappingManager mapper = new MappingManager(session);
//        
//        TypeCodec<TGeoLoc> udtCodec = mapper.udtCodec(TGeoLoc.class);
        
//        udtCodec.
//        MappedUDTCodec muc = 
    }
}
