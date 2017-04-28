package gr.demokritos.iit.location.extraction;

import java.util.Set;

/**
 * Created by nik on 2/15/17.
 */
public interface IRestfulFilter {

    enum EntityType
    {
        LOCATION("location"), GENERIC("generic");
        EntityType(String str)
        {
            value = str;
        }
        String value;
        public static boolean supports(String candidate)
        {
            for(EntityType elem : EntityType.values())
            {
                if(elem.toString().equals(candidate)) return true;
            }
            return false;
        }
        public String toString(){ return value;}

    }
    boolean initialize(String str, boolean AppendProject, boolean verbose);
    void filter(Object input);
    String toString();
    Set<String> getEntities();
    void clear();
}
