package gov.healthit.chpl.tempApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class App {
	private static final String DEFAULT_PROPERTIES_FILE = "environment.properties";
	
	protected String getDownloadFolderPath(String[] args, Properties props){
		String downloadFolderPath;
        if (args.length > 0) {
        	downloadFolderPath = args[0];
        } else {
        	downloadFolderPath = props.getProperty("downloadFolderPath");
        }
        return downloadFolderPath;
	}
	
	protected File getDownloadFolder(String downloadFolderPath){
		File downloadFolder = new File(downloadFolderPath);
        if(!downloadFolder.exists()) {
        	downloadFolder.mkdirs();
        }
        return downloadFolder;
	}
	
	protected Properties getProperties() throws IOException{
		Properties props = null;
		InputStream in = App.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE);
		if (in == null) {
			props = null;
			throw new FileNotFoundException("Environment Properties File not found in class path.");
		} else {
			props = new Properties();
			props.load(in);
			in.close();
		}
		return props;
	}
}
