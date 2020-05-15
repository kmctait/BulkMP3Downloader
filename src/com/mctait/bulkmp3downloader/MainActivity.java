package com.mctait.bulkmp3downloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	String 		className = this.getClass().getSimpleName();
	String 		appName;
	
	EditText	inputTextField;
	Button		submitButton;
	TextView	screenBottom;
	
	int			numSearchResults = 3;
	int			maxFileDownloads = 10;

	String		globalFilename = new String();
	
	private static final int RESULT_SETTINGS = 1;
	
	private static final String MP3_PATTERN = "([^\\s]+(\\.(?i)(mp3|m4a))$)";
	private Pattern pattern;
	private Matcher matcher;
	
	ArrayList<String> resultFilenames = new ArrayList<String>();
	
	// declare the dialog as a member field of your activity
	ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		pattern = Pattern.compile(MP3_PATTERN);
		
		// instantiate it within the onCreate method
		mProgressDialog = new ProgressDialog(MainActivity.this);
		mProgressDialog.setMessage("Downloading file...");
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(true);
		
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

		appName = getResources().getString(R.string.app_name);
		screenBottom = 		(TextView) findViewById(R.id.screenBottom);
		inputTextField =	(EditText) findViewById(R.id.textbox);
		submitButton =		(Button) findViewById(R.id.submitButton);
		submitButton.setOnClickListener(this);
	    
		showUserSettings();
	}
	
  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivityForResult(i, RESULT_SETTINGS);
			break;
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case RESULT_SETTINGS:
			showUserSettings();
			break;
		}
	}
	
	private void showUserSettings() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		// num of files to download
		String prefMaxResults = sharedPrefs.getString("pref_maxresults", "10");
		maxFileDownloads = Integer.valueOf(prefMaxResults);
		
		// num of Google search results to retrieve
		String prefMaxSearchResults = sharedPrefs.getString("pref_maxSearchResults", "3");
		numSearchResults = Integer.valueOf(prefMaxSearchResults);
	}

	@Override
	public void onClick(View v) {

		if (v != null) {
			InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(inputTextField.getWindowToken(), 0);
		}

		// check for internet connectivity
		if(!isConnectedToInternet()) {
			Toast.makeText(getBaseContext(),"No Internet Connection!", Toast.LENGTH_LONG).show();
		} else {
			screenBottom.setText("");
	        String str = inputTextField.getText().toString();
	        inputTextField.setText("");
	        
	        // Get search results in Set
			Search search = new GoogleSearch(str, numSearchResults);
	        search.printSearchResults();
	        
	        if(search.getNumResultsObtained() < 1) {
	        	Toast.makeText(getBaseContext(),"Sorry, no results!", Toast.LENGTH_LONG).show();
	        	return;
	        }
	        
	        Set<String> googleLinks = search.getResultLinks();
	        HashSet<LinkAndFilename> linkPairs = new HashSet<LinkAndFilename>();
	        
	        // Iterate thro list of google pagelinks
			for(String link : googleLinks){
				Log.d(className, link);
				
				Set<LinkAndFilename> mp3List = new HashSet<LinkAndFilename>();
				
				if(isUrlHealthy(link)){
					mp3List = getMP3LinksFromPage(link);
					
					// iterate thro mp3 links
					for(LinkAndFilename pair : mp3List){
						linkPairs.add(pair);
					}
				}
			}
			
			LinkAndFilename[] linkPairsArray = linkPairs.toArray(new LinkAndFilename[linkPairs.size()]);
			final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
			downloadTask.execute(linkPairsArray);
			
			// TODO: put as option in Settings
			// uncomment if you wish to be able to cancel the download process (and stop all further downloads)
/*			mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					downloadTask.cancel(true);
				}
			});*/
		}
	}
	
	private Set<LinkAndFilename> getMP3LinksFromPage(String link) {
		Set<LinkAndFilename> links = new HashSet<LinkAndFilename>();
		Log.d(className, "searching link: " + link);
		
		try {
			Document doc = Jsoup
					.connect(link)
					.timeout(5000).get();
			
			// get all links
			Elements alinks = doc.select("a[href]");
			for (Element alink : alinks) {
				
				String text = alink.text();
				String mp3link = alink.attr("href");
				
				matcher = pattern.matcher(mp3link);
				if(matcher.matches()) {
					links.add(new LinkAndFilename(link+mp3link, text));
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		return links;
	}
	
	// usually, subclasses of AsyncTask are declared inside the activity class.
	// that way, you can easily modify the UI thread from here
	// params, progress, result
	private class DownloadTask extends AsyncTask<LinkAndFilename, Integer, String> {

	    private Context context;
	    private PowerManager.WakeLock mWakeLock;

	    public DownloadTask(Context context) {
	        this.context = context;
	    }

	    @Override
	    protected String doInBackground(LinkAndFilename... linkPairs) {
	    	return downloadFiles(linkPairs);
	    }
	    
	    protected String downloadFiles(LinkAndFilename... linkPairs) {
	    	// iterate thro link pairs
	    	Looper.prepare();
	    	for(int i = 0; i < linkPairs.length && i < maxFileDownloads ; i++) {

		    	String link = linkPairs[i].getLink();
		    	String filename = linkPairs[i].getFilename();
		    	globalFilename = filename;
	    		
		        InputStream input = null;
		        OutputStream output = null;
		        HttpURLConnection connection = null;
		        try {
		            URL url = new URL(link);
		            connection = (HttpURLConnection) url.openConnection();
		            connection.connect();
	
		            // expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
		            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
		                return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
		            }
		            
		            // is sdcard present / mounted
		            if(!isSDCardPresent()){
		            	return "Storage media not present or mounted.";
		            }
		            
		            // this will be useful to display download percentage
		            // might be -1: server did not report the length
		            int fileLength = connection.getContentLength();
	
		            // download the file
		            input = connection.getInputStream();
		            String musicpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/";
		            
		            output = new FileOutputStream(musicpath + filename);
		            
		            byte data[] = new byte[4096];
		            long total = 0;
		            int count;
		            while ((count = input.read(data)) != -1) {
		                // allow canceling with back button
		                if (isCancelled()) {
		                    input.close();
		                    return null;
		                }
		                total += count;
		                // publishing the progress....
		                if (fileLength > 0) // only if total length is known
		                    publishProgress((int) (total * 100 / fileLength));
		                output.write(data, 0, count);
		            }
		            
		        } catch (Exception e) {
		        	Log.e(className, e.getMessage());
		            return e.toString();
		        } finally {
		            try {
		                if (output != null)
		                    output.close();
		                if (input != null)
		                    input.close();
		            } catch (IOException ignored) {
		            }
		            if (connection != null)
		                connection.disconnect();
		            
		            resultFilenames.add(globalFilename);
		        }
	    	}
	    	return null;
	    }
	    
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        
	        // take CPU lock to prevent CPU from going off if the user  presses the power button during download
	        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
	        mWakeLock.acquire();
	        mProgressDialog.show();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        // if we get here, length is known, now set indeterminate to false
	        mProgressDialog.setIndeterminate(false);
	        mProgressDialog.setMax(100);
	        mProgressDialog.setProgress(progress[0]);
	        mProgressDialog.setMessage("Downloading: " + globalFilename);
	        mProgressDialog.show();
	    }

	    @Override
	    protected void onPostExecute(String result) {
	        mWakeLock.release();
	        mProgressDialog.dismiss();
	        if (result != null)
	            Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
	        else{
	        	// push notification to notify that all files and results found are downloaded 
	            showTestNotification(findViewById(R.layout.activity_main), "All files downloaded. Process complete.");
	            //showListOfResults();
	            launchResultsActivity();
	        }
	    }
	}
	
	public void launchResultsActivity(){
		Intent intent = new Intent(this, ResultsActivity.class);
		intent.putStringArrayListExtra("results", resultFilenames);
		startActivity(intent);
	}
	
	public void showTestNotification(View view, String str) {
		// Get an instance of NotificationManager//
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Download notification")
				.setContentText(str);

		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.notify(001, mBuilder.build());
	}
	
	// is storage present
	private boolean isSDCardPresent(){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			return true;
		return false;
	}
	
    //Check if internet is present or not
    private boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager
                .getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private boolean isUrlHealthy(String link) {
	    HttpURLConnection connection = null;
	    try {
	        URL url = new URL(link);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.connect();
	
	        // expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
	        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	            return false;
	        }
		} catch(MalformedURLException mue) {
			Log.e(className, "Malformed URL");
		} catch(IOException ioe) {
			Log.e(className, "IOException");
		}
	    return true;
    }

}
