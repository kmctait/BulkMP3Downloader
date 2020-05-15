package com.mctait.bulkmp3downloader;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ResultsActivity extends Activity {

	private Button backButton;
	
	private ListView mainListView ;  
	private ArrayAdapter<String> listAdapter ;
	
	ArrayList resultFilenames = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.results);
        
        Intent intent = getIntent();
        // Receiving the Data
        resultFilenames = intent.getStringArrayListExtra("results");
        
        
        showListOfResults();
        
        backButton = (Button) findViewById(R.id.backButton);
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
	}
	
	public void showListOfResults(){
		// Find the ListView resource.   
	    mainListView = (ListView) findViewById( R.id.mainListView ); 
	    
	    // Create and populate a List of planet names.  
	    String[] planets = new String[] { "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"};    
	    ArrayList<String> planetList = new ArrayList<String>();  
	    planetList.addAll( Arrays.asList(planets) ); 
	    
	    // Create ArrayAdapter using the planet list.  
	    //listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, planetList);  
	    listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, resultFilenames);
	    
	    // Set the ArrayAdapter as the ListView's adapter.  
	    mainListView.setAdapter(listAdapter);
	}
	
	public void clearListOfResults() {
		if(listAdapter != null) {
			listAdapter.clear();
			listAdapter.notifyDataSetChanged();
		}
		
		if(mainListView != null)
			mainListView.setAdapter(null);
	}
	
}
