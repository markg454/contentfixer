package com.yayhi.content;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yayhi.utils.YLogger;
import com.yayhi.utils.YProperties;

/**
 * -----------------------------------------------------------------------------
 * @version 1.0
 * @author  Mark Gaither
 * @date	Dec 3, 2009
 * -----------------------------------------------------------------------------
 */

public class Fixer {

	private static Properties sysProps				= null;
	private static YProperties iProps				= null;
	private static Properties appProps				= null;
    private static String logFilePath				= null;
    private static Boolean logIt					= null;
    private static String inputFilePath				= null;
    private static String outputFilePath			= null;
    private static boolean debug 					= false;
    private static YLogger logger					= null;
    
    // constructor
    Fixer() {
    	
    }
    
    /**
     * Sole entry point to the class and application.
     * @param args Array of String arguments.
     * @exception java.lang.InterruptedException
     *            Thrown from the Thread class.
     * @throws IOException 
     */
    public static void main(String[] args) throws Exception {
    	
    	//*********************************************************************************************
        //* Get Command Line Arguments - overwrites the properties file value, if any
        //*********************************************************************************************   	
    	String usage = "Usage:\n" + "java -jar contentfixer.jar [-i INPUT_FILE] [-o OUTPUT_FILE] [-d] (DEBUG optional)\n";
    	String example = "Example:\n" + "java -jar contentfixer.jar -i /tmp/input.sql -o /tmp/sql.out \n" +
    	"java -jar contentfixer.jar -i /tmp/input.sql -o /tmp/output.sql -d\n";
    	
    	// get system properties
    	sysProps = System.getProperties();

        // get command line arguments
    	if (args.length >= 4) {
    		
	    	for (int optind = 0; optind < args.length; optind++) {
	    	    
	    		if (args[optind].equals("-i")) {
	    			inputFilePath = args[++optind];
				} else if (args[optind].equals("-o")) {
					outputFilePath = args[++optind];
				} else if (args[optind].equals("-d")) {
					debug = true; 
		    	}
	    		
	    	}
        }
        else {
        	
        	System.err.println(usage);
        	System.err.println(example);
            System.exit(1);
            
        }

    	//*********************************************************************************************
        //* Get Properties File Data
        //*********************************************************************************************
    	iProps = new YProperties();
    	appProps = iProps.loadProperties();
    	
    	// get file replacement patterns
    	List<String> findReplaceList = new ArrayList<String>();
    	String rp = appProps.getProperty("replacementPatterns");
    	String [] rps = rp.split("@@");
    	for (String r: rps) {
    		findReplaceList.add(r);
    	}
    	
    	// set log file line
    	logFilePath = appProps.getProperty("logFilePath");
    	logIt = Boolean.valueOf(appProps.getProperty("logIt"));
        
    	//*********************************************************************************************
        //* Set up logging
        //*********************************************************************************************

    	// create string of todays date and time
    	Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss");

        // log data, if true
        if (logIt.booleanValue()) {
            	
        	// open log file
    	    try {
    	    	logger = new YLogger(appProps.getProperty("logFilePath") +  "/contentfixer_" + sdf.format(cal.getTime()) + ".log");
    	    } catch (IOException e) {
    	    	System.out.println("exception: " + e.getMessage());
    	    }
    	    
        } 
        
        if (debug) {
    		System.out.println("   property log line :  " + appProps.getProperty("logFilePath") + "/contentfixer_" + sdf.format(cal.getTime()) + ".log");
    		System.out.println("   property log it :  " + logIt.toString());
    		System.out.println("   inputFile file line :  " + inputFilePath);
        }
        
        //*********************************************************************************************
        //* Read input sql
        //*********************************************************************************************
    	
    	// Create input file
		File inputFile = new File(inputFilePath);
		
		// Create output file
		FileWriter fw = new FileWriter(outputFilePath);

		// Save input line
		List<String> lines = new ArrayList<String>();

		// Read inputFile file
		if (inputFile.exists()) {
			
			if (debug) {
				System.out.println("Processing inputFile: " + inputFilePath);
				System.out.println("\n===============================================");
	        }
		    
			if (logIt.booleanValue()) {
      			
      			try {
      				logger.write("Processing inputFile: " + inputFilePath);
      				logger.write("\n===============================================");
          	    } catch (IOException e) {
          	    }
          	    
      		}
			
			// read the inputFile 
			// each asset line is on a line to itself
		    try {

		    	BufferedReader input =  new BufferedReader(new FileReader(inputFilePath));
  
		    	try {
    
		    		String line = null; //not declared within while loop

		    		while (( line = input.readLine()) != null) {

		    			//if (debug) {
		    				//System.out.println("   line: " + line);
		    			//}
		    			
		    			// save beid to array list
		    			lines.add(line);
	
		    		}
		    	}
		    	finally {
		    		input.close();
		    	}
		    }
		    catch (IOException ex){
		    	ex.printStackTrace();
		    }
		    
		    if (debug) {
		    	System.out.println("   lines length: " + lines.size());
			}
		    
		    //*********************************************************************************************
	        //* Do substitutions
	        //*********************************************************************************************
		    for (String line: lines) {
		    	
	        	if (debug) {
	        		System.out.println("------ line: " + line);
	        	}
	         	
	        	for (String f: findReplaceList) {
	        		
			    	String [] r = f.split("\\|\\|");
			    	String patternMatch = r[0];
			    	String replaceWith = r[1];
			    	patternMatch = patternMatch.replaceAll("^\"","");
			    	replaceWith = replaceWith.replaceAll("\"$","");
			    	
			    	if (debug) {
			    		System.out.println(" patternMatch: " + patternMatch);
			    		System.out.println(" replaceWith: " + replaceWith);
			    	}
	        	
		        	Pattern p = Pattern.compile(patternMatch);
		        	Matcher m = p.matcher(line);
		        	if (m.find()) {
		        		line = m.replaceAll(replaceWith);
	            	
		        		if (debug) {
		        			System.out.println("replaced with: " + line);
		        		}
		        		
		        	}
		        	
	        	}
	        	
	        	fw.write(line + "\n");
	        	
		    }   
		
		}
		
		logger.close();
		
		fw.close();
  	    
        System.exit(0);
		
    }
    
}
