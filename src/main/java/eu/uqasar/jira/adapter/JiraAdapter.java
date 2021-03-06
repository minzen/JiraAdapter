package eu.uqasar.jira.adapter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import com.google.common.collect.Iterables;

import eu.uqasar.adapter.SystemAdapter;
import eu.uqasar.adapter.exception.uQasarException;
import eu.uqasar.adapter.model.BindedSystem;
import eu.uqasar.adapter.model.Measurement;
import eu.uqasar.adapter.model.User;
import eu.uqasar.adapter.model.uQasarMetric;
import eu.uqasar.adapter.query.QueryExpression;

/**
 * Created with IntelliJ IDEA.
 * User: eleni
 * Date: 1/9/14
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class JiraAdapter implements SystemAdapter {


	// Define a sample value for max results to be fetched
	private final int MAX_RESULTS = 1000; 
	private final static Logger LOGGER = Logger.getLogger(JiraAdapter.class.getName());

    public JiraAdapter() {
    	LOGGER.setLevel(Level.INFO);
    }

    @Override
    public List<Measurement> query(BindedSystem bindedSystem, User user, QueryExpression queryExpression) throws uQasarException {
        URI uri = null;
        LinkedList<Measurement> measurements = new LinkedList<Measurement>();


        try {

            /* Connection to JIRA instance */
            uri = new URI(bindedSystem.getUri());
            JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
            JiraRestClient client = factory.createWithBasicHttpAuthentication(uri, user.getUsername(), user.getPassword());

            /* START -- Metrics implementation */
            if (queryExpression.getQuery().equalsIgnoreCase(uQasarMetric.PROJECTS_PER_SYSTEM_INSTANCE.name())){

                JSONArray measurementResultJSONArray = new JSONArray();

                Iterable<BasicProject> basicProjects = client.getProjectClient().getAllProjects().claim();

                for (BasicProject basicProject : basicProjects) {

                    JSONObject bp = new JSONObject();
                    bp.put("self",basicProject.getSelf());
                    bp.put("key",basicProject.getKey());
                    bp.put("name",basicProject.getName());
                    measurementResultJSONArray.put(bp);
                }

                measurements.add(new Measurement(uQasarMetric.PROJECTS_PER_SYSTEM_INSTANCE, measurementResultJSONArray.toString()));

            }   else if (queryExpression.getQuery().equalsIgnoreCase(uQasarMetric.ISSUES_PER_PROJECTS_PER_SYSTEM_INSTANCE.name())) {

            	LOGGER.info("Fetching data for metric " +uQasarMetric.ISSUES_PER_PROJECTS_PER_SYSTEM_INSTANCE.name());            	
                
            	List<Iterable<Issue> > issues = new ArrayList<Iterable<Issue>>();
            	String query = " ORDER BY project DESC";
            	issues = getIssues(client, query);
            	
                // Add all the issues to the measurements
                measurements.add(new Measurement(uQasarMetric.ISSUES_PER_PROJECTS_PER_SYSTEM_INSTANCE,formatIssuesResult(issues)));

            }  else if (queryExpression.getQuery().contains(uQasarMetric.FIXED_ISSUES_PER_PROJECT.name())) {

            	LOGGER.info("Fetching data for metric " +uQasarMetric.FIXED_ISSUES_PER_PROJECT.name());

                List<Iterable<Issue> > issues = new ArrayList<Iterable<Issue>>();
            	String query = "resolution = Fixed ORDER BY updatedDate DESC";
            	issues = getIssues(client, query);

                measurements.add(new Measurement(uQasarMetric.FIXED_ISSUES_PER_PROJECT, formatIssuesResult(issues)));

            }  else if (queryExpression.getQuery().contains(uQasarMetric.UNRESOLVED_ISSUES_PER_PROJECT.name())) {

            	LOGGER.info("Fetching data for metric " +uQasarMetric.UNRESOLVED_ISSUES_PER_PROJECT.name());

                List<Iterable<Issue> > issues = new ArrayList<Iterable<Issue>>();
            	String query = "resolution = Unresolved ORDER BY updatedDate DESC";
            	issues = getIssues(client, query);

            	measurements.add(new Measurement(uQasarMetric.UNRESOLVED_ISSUES_PER_PROJECT, formatIssuesResult(issues)));

            } else if (queryExpression.getQuery().contains(uQasarMetric.UNRESOLVED_BUG_ISSUES_PER_PROJECT.name())) {

            	LOGGER.info("Fetching data for metric " +uQasarMetric.UNRESOLVED_BUG_ISSUES_PER_PROJECT.name());

                List<Iterable<Issue> > issues = new ArrayList<Iterable<Issue>>();
            	String query = "issuetype = Bug AND status = \"To Do\"";
            	issues = getIssues(client, query);

                measurements.add(new Measurement(uQasarMetric.UNRESOLVED_BUG_ISSUES_PER_PROJECT, formatIssuesResult(issues)));
                
            } else if (queryExpression.getQuery().contains(uQasarMetric.UNRESOLVED_TASK_ISSUES_PER_PROJECT.name())) {
            	
            	LOGGER.info("Fetching data for metric " +uQasarMetric.UNRESOLVED_TASK_ISSUES_PER_PROJECT.name());

                List<Iterable<Issue> > issues = new ArrayList<Iterable<Issue>>();
            	String query = "issuetype = Task AND status = \"To Do\"";
            	issues = getIssues(client, query);
            	
                measurements.add(new Measurement(uQasarMetric.UNRESOLVED_TASK_ISSUES_PER_PROJECT, formatIssuesResult(issues)));
                
            } else {
            	throw new uQasarException(uQasarException.UQasarExceptionType.UQASAR_NOT_EXISTING_METRIC,queryExpression.getQuery());
            }

            // Close the JiraRestClient
            client.close();
            
            /* END -- Metrics implementation */


        } catch (JSONException e) {
            e.printStackTrace(); 
        } catch (URISyntaxException e) {
            throw new uQasarException(uQasarException.UQasarExceptionType.BINDING_SYSTEM_BAD_URI_SYNTAX,bindedSystem,e.getCause());
        }  catch (RuntimeException e){
            throw new uQasarException(uQasarException.UQasarExceptionType.BINDING_SYSTEM_CONNECTION_REFUSED,bindedSystem,e.getCause());
        } catch (IOException e) {
			e.printStackTrace();
		}
        return measurements;


    }

    @Override
    public List<Measurement> query(String bindedSystemURL, String credentials, String queryExpression) throws uQasarException {
        List<Measurement> measurements = null;

        BindedSystem bindedSystem = new BindedSystem();
        bindedSystem.setUri(bindedSystemURL);
        User user = new User();

        String[] creds = credentials.split(":");

        user.setUsername(creds[0]);
        user.setPassword(creds[1]);

        JiraQueryExpresion jiraQueryExpresion = new JiraQueryExpresion(queryExpression);

        JiraAdapter jiraAdapter = new JiraAdapter();

        measurements = jiraAdapter.query(bindedSystem,user,jiraQueryExpresion);


        return measurements;
    }

    public void printMeasurements(List<Measurement> measurements){
        String newLine = System.getProperty("line.separator");
        for (Measurement measurement : measurements) {
            System.out.println("----------TEST metric: "+measurement.getMetric()+" ----------"+newLine);
            System.out.println(measurement.getMeasurement()+newLine+newLine);
            System.out.println();

        }
    }


    /**
     * Get a list of Iterable Issues based on a query 
     * @param client
     * @param query
     * @return
     */
    public List<Iterable<Issue> > getIssues(JiraRestClient client, String query) {

    	// List that will contain the results
        List<Iterable<Issue> > allIssues = new ArrayList<Iterable<Issue>>();

    	HashSet<String> fields = new HashSet<String>();
    	fields.add("*all");
    	
    	// Start fetching the results from index 0
    	Integer startIdx = 0;
        Promise<SearchResult> searchResultPromise = client.getSearchClient().searchJql(query, new Integer(MAX_RESULTS), startIdx, fields); 
        
        // How many issues there are in total?
        int totalIssues = searchResultPromise.claim().getTotal();
    	LOGGER.info("Total issues: " +totalIssues);
        
        // Get the actual issues 
        Iterable<Issue> issues =  searchResultPromise.claim().getIssues();
        allIssues.add(issues);
        
        // Get the number of the issues
        int nrOfObtained = getSizeValues(issues);                
        LOGGER.info("Obtained issues: " +nrOfObtained);
        
        // If there are more issues continue from the next index that has not yet been obtained
        while (nrOfObtained < totalIssues) {
        	LOGGER.info("Total number of issues is higher than has been fetched. Proceeding to the next start index...");
        	startIdx += MAX_RESULTS;
        	LOGGER.info("New start index: " +startIdx);
            // New search; fetch results from the following index
        	searchResultPromise = client.getSearchClient().searchJql(query, new Integer(MAX_RESULTS), startIdx, fields);
            // Get the actual issues and concatenate to the previous ones
            Iterable<Issue> furtherIssues = searchResultPromise.claim().getIssues();
            nrOfObtained += getSizeValues(furtherIssues);
            LOGGER.info("Obtained issues: " +nrOfObtained);
            allIssues.add(furtherIssues);
        }

    	
    	return allIssues;

    }
    
    
    /**
     * Create a String containing the JSON with issues. 
     * This should be optimized for performance, and also enhanced to fetch the content pointed by the URL
     * to prevent calls in the U-QASAR platform. 
     * @param listOfIssues
     * @return
     * @throws JSONException
     */
    public String formatIssuesResult( List<Iterable<Issue> > listOfIssues) throws JSONException {

        JSONArray measurementResultJSONArray = new JSONArray();

        for (Iterable<Issue> iterableIssue : listOfIssues) {
            for (BasicIssue issue : iterableIssue) {
                JSONObject i = new JSONObject();
                i.put("self", issue.getSelf());
                i.put("key", issue.getKey());
                measurementResultJSONArray.put(i);
            }
		}
        
        return  measurementResultJSONArray.toString();
    }

    
    /**
     * Returns the size of the iterable
     * @param values
     * @return
     */
    private int getSizeValues(Iterable<?> values) {

        if (values instanceof Collection<?>) {
        	 return ((Collection<?>)values).size();
        }
        
        return 0;
    }


    //in order to invoke main from outside jar
    //mvn exec:java -Dexec.mainClass="eu.uqasar.jira.adapter.JiraAdapter" -Dexec.args="http://95.211.223.9:8084 soaptester:soaptester ISSUES_PER_PROJECTS_PER_SYSTEM_INSTANCE"

    public static void main(String[] args) {
        List<Measurement> measurements;
        String newLine = System.getProperty("line.separator");
        BindedSystem bindedSystem = new BindedSystem();
        bindedSystem.setUri(args[0]);
        User user = new User();
        String[] credentials = args[1].split(":");
        user.setUsername(credentials[0]);
        user.setPassword(credentials[1]);

        JiraQueryExpresion jiraQueryExpresion = new JiraQueryExpresion(args[2]);

        try {
        JiraAdapter jiraAdapter = new JiraAdapter();

            measurements = jiraAdapter.query(bindedSystem,user,jiraQueryExpresion);
            jiraAdapter.printMeasurements(measurements);


        } catch (uQasarException e) {
            e.printStackTrace();
        }
    }

}
