import eu.uqasar.adapter.exception.uQasarException;
import eu.uqasar.adapter.model.uQasarMetric;
import eu.uqasar.jira.adapter.JiraAdapter;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: eleni
 * Date: 1/9/14
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class JiraAdapterTest {

    JiraAdapter jiraAdapter = new JiraAdapter();
    String newLine = System.getProperty("line.separator");
    String bindedSystemURL = "http://95.211.223.9:8084";
    String credentials = "soaptester:soaptester";

    // Test uqasar db connection
    // Test   metrics : { RESOURCES_PER_BINDING , ISSUES_PER_RESOURCE_PER_BINDING }
    @Test
    public void queryTest(){
        JSONArray measurements = null;

        try{
            for (uQasarMetric metric  :uQasarMetric.values()) {

                measurements = jiraAdapter.query(bindedSystemURL, credentials, metric.name());
                System.out.println("------------------------------------------"+newLine);
                System.out.println(measurements);
            }

        }catch (uQasarException e){
            e.printStackTrace();
        }
    }


    // Try to pass a non existing metric
    @Test
    public void queryTest_erroneus_metric(){

    try{
        JSONArray measurements = jiraAdapter.query(bindedSystemURL, credentials, "ERRONEUS METRIC");
    }catch (uQasarException e){
        e.printStackTrace();
        assertTrue(true);
    }

    }

}
