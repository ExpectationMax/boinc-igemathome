/*******************************************************************************
 * This file is part of BOINC.
 * http://boinc.berkeley.edu
 * Copyright (C) 2012 University of California
 * 
 * BOINC is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * BOINC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with BOINC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.berkeley.boinc;

import java.util.ArrayList;
import edu.berkeley.boinc.adapter.AttachProjectListAdapter;
import edu.berkeley.boinc.client.Monitor;
import edu.berkeley.boinc.rpc.ProjectInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AttachProjectListActivity extends Activity implements android.view.View.OnClickListener{
	
	private final String TAG = "BOINC AttachProjectListActivity"; 
	
	private Monitor monitor;
	private Boolean mIsBound;

	private ListView lv;
	private AttachProjectListAdapter listAdapter;
	private Dialog manualUrlInputDialog;
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been established, getService returns the Monitor object that is needed to call functions.
	        monitor = ((Monitor.LocalBinder)service).getService();
		    mIsBound = true;
		    
		    populateView();
	    }

	    public void onServiceDisconnected(ComponentName className) { // This should not happen
	        monitor = null;
		    mIsBound = false;
	    }
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
         
        Log.d(TAG, "onCreate"); 
        
        //bind monitor service
        doBindService();
    }
    
	@Override
	protected void onDestroy() {
    	Log.d(TAG, "onDestroy");
	    doUnbindService();
	    super.onDestroy();
	}

	private void doBindService() {
	    // Establish a connection with the service, onServiceConnected gets called when
		bindService(new Intent(this, Monitor.class), mConnection, Service.BIND_AUTO_CREATE);
	}

	private void doUnbindService() {
	    if (mIsBound) {
	        // Detach existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	    }
	}
	
	private void populateView(){
		//retrieve projects from monitor
		ArrayList<ProjectInfo> data = monitor.getAndroidProjectsList();
		Log.d(TAG,"monitor.getAndroidProjectsList returned with " + data.size() + " elements");
		
		// setup layout
        setContentView(R.layout.attach_project_list_layout);  
		lv = (ListView) findViewById(R.id.listview);
        listAdapter = new AttachProjectListAdapter(AttachProjectListActivity.this,R.id.listview,data);
        lv.setAdapter(listAdapter);
        
        // set title bar
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);
	}
	
	// check whether device is online before starting connection attempt
	// as needed for AttachProjectLoginActivity (retrieval of ProjectConfig)
	// note: available internet does not imply connection to project server
	// is possible!
	private Boolean checkDeviceOnline() {
	    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}
	
	// get called by manual input list item
	public void manualUrlItem(View view) {
		//Log.d(TAG,"manualUrlItem");
		//show dialog
		showDialog(view.getId());
	}
	
	// gets called by showDialog
	@Override
	protected Dialog onCreateDialog(int id) {
		manualUrlInputDialog = new Dialog(this); //instance new dialog
		manualUrlInputDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		manualUrlInputDialog.setContentView(R.layout.attach_project_list_layout_manual_dialog);
		Button button = (Button) manualUrlInputDialog.findViewById(R.id.buttonUrlSubmit);
		button.setOnClickListener(this);
		((TextView)manualUrlInputDialog.findViewById(R.id.title)).setText(R.string.attachproject_list_manual_dialog_title);
		return manualUrlInputDialog;
	}

	// gets called by dialog button
	@Override
	public void onClick(View v) {
		//Log.d(TAG,"buttonUrlSubmit clicked");
		try {
			String url = ((EditText)manualUrlInputDialog.findViewById(R.id.Input)).getText().toString();

			if(url == null) { // error while parsing
				showErrorToast(R.string.attachproject_list_manual_no_url);
			}
			else if(url.length()== 0) { //nothing in edittext
				showErrorToast(R.string.attachproject_list_manual_no_url);
			}
			else if(!checkDeviceOnline()) {
				showErrorToast(R.string.attachproject_list_no_internet);
			} else {
				manualUrlInputDialog.dismiss();
				startAttachProjectLoginActivity(null, url);
			}
		} catch (Exception e) {
			Log.w(TAG,"error parsing edit text",e);
		}
	}
	
	// gets called by project list item
	public void onProjectClick(View view) {
		//Log.d(TAG,"onProjectClick");
		if(!checkDeviceOnline()) {
			showErrorToast(R.string.attachproject_list_no_internet);
			return;
		}
		try {
			ProjectInfo project = (ProjectInfo) view.getTag();
			startAttachProjectLoginActivity(project, null); 
		} catch (Exception e) {
			Log.w(TAG,"error parsing view tag",e);
			showErrorToast(R.string.attachproject_list_manual_no_url);
		}
	}
	
	private void startAttachProjectLoginActivity(ProjectInfo project, String url) {
		//Log.d(TAG,"startAttachProjectLoginActivity ");
		Intent intent = new Intent(this, AttachProjectLoginActivity.class);
		intent.putExtra("projectInfo", project);
		intent.putExtra("url", url);
		startActivity(intent);
	}

	private void showErrorToast(int resourceId) {
		Toast toast = Toast.makeText(getApplicationContext(), resourceId, Toast.LENGTH_SHORT);
		toast.show();
	}

}