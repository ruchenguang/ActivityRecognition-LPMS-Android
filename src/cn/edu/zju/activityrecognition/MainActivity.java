package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.edu.zju.activityrecognition.tools.BluetoothService;
import cn.edu.zju.activityrecognition.tools.ExitApplication;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.os.*;

// Main activity. Connects to LPMS-B and displays orientation values
public class MainActivity extends android.app.Activity{
	public static final String TAG = "AR::Main";
	public static final String EXTRA_ACTIVITY = "MainActivity::extra_activity";	
	public static final String EXTRA_ACTIVITY_ISFINISHED = "MainActivity::extra_activity_isfinshed";	
	public static final String BUNDLE_FINISHED_ACTIVITIES = "MainActivity::bundle_finished_activities";
	public static final byte ASK_CODE_ZERO = 48;

	public static ArrayList<HumanActivity> activities;
	public static ArrayList<String> activityTitles;
	public static int activityNum; 

	ActivityAdapter adapter;
	
	public static File activityCompletionStateFile;
	int finishedActivities = 0;
	boolean isAllFinished = false;
	
	//flag showing the bluetooth connetion between the phoen and LPMS sensor
	boolean isConnected = true; 
	
	ListView listView;
	TextView tv;
	Button finishButton;
	MenuItem actionConnected, actionNotConnected, actionConnecting; 
	BroadcastReceiver bluetoothStateReceiver;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);	
        
        //exit activity list, used to kill all the activities
        ExitApplication.activityList.add(this);
        
        //get the connection state
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_BT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_BT_NOT_CONNECTED);
        bluetoothStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if(action.equals(BluetoothService.ACTION_BT_CONNECTED)){
		            actionConnected.setVisible(true);
		            actionNotConnected.setVisible(false);
		            Toast.makeText(MainActivity.this, "Device is connected again", Toast.LENGTH_SHORT).show();
				}
				else if(action.equals(BluetoothService.ACTION_BT_NOT_CONNECTED)){
		            actionConnected.setVisible(false);
		            actionNotConnected.setVisible(true);
					Toast.makeText(MainActivity.this, "Warning! Device is not connected anymore", Toast.LENGTH_SHORT).show();
				}
				actionConnecting.setVisible(false);
			}
		};
        registerReceiver(bluetoothStateReceiver, intentFilter);
        
        //initiate activities' title and number
        activities = getActivities();
    	activityNum = activities.size();

    	listView = (ListView) findViewById(R.id.listView1);
        adapter = new ActivityAdapter(this, activities);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(MainActivity.this, DataCollectionActivity.class);
				intent.putExtra(EXTRA_ACTIVITY, position);
				intent.putExtra(EXTRA_ACTIVITY_ISFINISHED, activities.get(position).isFinished);
				startActivity(intent);
			}
		});
        
        finishButton = (Button) findViewById(R.id.button1);
        finishButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isAllFinished){
					Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_scale);
					animation.setAnimationListener(new AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
						}
						@Override
						public void onAnimationRepeat(Animation animation) {
						}
						@Override
						public void onAnimationEnd(Animation animation) {
							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
							builder.setTitle(R.string.title_exit_finished);
							builder.setMessage(R.string.message_exit_finished);
							builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							});
							builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									List<android.app.Activity> activityList = ExitApplication.activityList;
									for(int i=0; i<activityList.size(); i++){
										if(activityList.get(i) != null)
											activityList.get(i).finish();
									}
								}
							});
							builder.create().show();
							stopService(new Intent(MainActivity.this, BluetoothService.class));
						}
					});
					v.startAnimation(animation);
				}
				else {
					Toast.makeText(MainActivity.this, "Not all the activities are finished yet. Please go on.", Toast.LENGTH_SHORT).show();
				}
			}
		});
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_connection, menu);
		actionConnected = menu.findItem(R.id.action_connected);
		actionNotConnected = menu.findItem(R.id.action_not_connected);
		actionConnecting = menu.findItem(R.id.action_connecting);
        if (BluetoothService.isConnected) {
            actionConnected.setVisible(true);
            actionNotConnected.setVisible(false);
        } else {
        	actionConnected.setVisible(false);
        	actionNotConnected.setVisible(true);
        }
        actionConnecting.setVisible(false);
        
        menu.findItem(R.id.action_settings).setEnabled(false);
		return super.onCreateOptionsMenu(menu);
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(actionNotConnected.equals(item)){
			//change the actions on the action bar, from not-connected to connecting
			actionConnected.setVisible(false);
			actionNotConnected.setVisible(false);
			actionConnecting.setVisible(true);
			Toast.makeText(this, "Connecting the device, please wait.", Toast.LENGTH_SHORT).show();
			
			//stop the old service and start a new one 
			Intent serviceIntent = new Intent(this, BluetoothService.class);
			stopService(serviceIntent);
			startService(serviceIntent);
		}
		return super.onOptionsItemSelected(item);
	}
	
    @Override
    protected void onResume() {
        activityCompletionStateFile = new File(InformationActivity.subjectDirPath, "activityCompletionState.txt");
        if(activityCompletionStateFile.exists()){
        	finishedActivities = 0;
        	try {
    			FileInputStream fis = new FileInputStream(activityCompletionStateFile);
            	byte[] buffer = new byte[activityNum];
            	fis.read(buffer);
            	fis.close();
            	for(int i=0; i<activityNum; i++){
            		if(buffer[i]-ASK_CODE_ZERO  == 1){
            			finishedActivities++;
            			activities.get(i).isFinished = true;
            		} else {
            			activities.get(i).isFinished = false;
            		}
            	}
            	Log.d(TAG, finishedActivities + " activities have been finished!");	
            } 
    		catch (FileNotFoundException e) {
    			e.printStackTrace();	
    		} 
        	catch (IOException e) {
				e.printStackTrace();	
    		}
        } 
        else {
        	//create a new file for saving finish state
			try {
				FileOutputStream fos = new FileOutputStream(activityCompletionStateFile);
	        	byte[] buffer = new byte[activityNum];
	        	for(int i=0; i<activityNum; i++){
	        		buffer[i] = 0 + ASK_CODE_ZERO;
	        	}
	        	fos.write(buffer);
	        	fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
		if(finishedActivities == activityNum){
    		finishButton.setText(R.string.button_finished);
    		finishButton.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
    		isAllFinished = true;
    	} 
    	else {
    		finishButton.setText(R.string.button_not_finished);
    		finishButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
    		isAllFinished = false;
    	}	
        
        adapter.notifyDataSetChanged();
    	super.onResume();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	unregisterReceiver(bluetoothStateReceiver);
    	
    	Intent serviceIntent = new Intent(this, BluetoothService.class);
    	Log.d(TAG, "Stop BluetoothService: " + stopService(serviceIntent));
    };
    
    ArrayList<HumanActivity> getActivities(){
    	ArrayList<HumanActivity> activities = new ArrayList<HumanActivity>();
    	
    	//initiate activities
    	ArrayList<Step> steps;
    	//********************simple activity********************
    	//sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit with phone in hand for", 60));
    	steps.add(new Step("Put the phone in pants pocket and then sit back in", 10));
    	steps.add(new Step("Keep sitting until hearing \"beep\" for", 60));
    	activities.add(new HumanActivity(
    			"sitting", 
    			R.string.sitting, 
    			R.string.instruction_activity_sitting,
    			R.drawable.sitting,
    			steps));
    	//standing
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Stand with phone in hand for", 60));
    	steps.add(new Step("Put the phone in pants pocket and then stand back in", 10));
    	steps.add(new Step("Keep standing until hearing \"beep\" for", 60));
    	activities.add(new HumanActivity(
    			"standing", 
    			R.string.standing,
    			R.string.instruction_activity_standing,
    			R.drawable.standing,
    			steps));
    	//lying
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Lay down with phone in hand for", 60));
    	steps.add(new Step("Put the phone in pants pocket and then lay back in", 10));
    	steps.add(new Step("Keep lying until hearing \"beep\" for", 60));
    	activities.add(new HumanActivity(
    			"lying", 
    			R.string.lying,
    			R.string.instruction_activity_lying,
    			R.drawable.lying,
    			steps));
    	//walking
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Walk with phone in hand for", 60));
    	steps.add(new Step("Put the phone in pants pocket and then back walk in", 10));
    	steps.add(new Step("Keep walking until hearing \"beep\" for", 60));
    	activities.add(new HumanActivity(
    			"walking", 
    			R.string.walking,
    			R.string.instruction_activity_walking,
    			R.drawable.walking,
    			steps));
    	//running
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Run with phone in hand for", 120));
    	activities.add(new HumanActivity(
    			"running", 
    			R.string.running,
    			R.string.instruction_activity_running,
    			R.drawable.running,
    			steps));
    	//climbing upstairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climb Upstairs after pressing start with phone in hand", 0));
    	activities.add(new HumanActivity(
    			"climbing_upstairs", 
    			R.string.climbing_upstairs,
    			R.string.instruction_activity_climbing_upstairs,
    			R.drawable.climbing,
    			steps
    	));
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Press start, put the phone in pocket and then climb upstairs", 0));
    	activities.add(new HumanActivity(
    			"climbing_upstairs", 
    			R.string.climbing_upstairs_pocket,
    			R.string.instruction_activity_climbing_upstairs_pocket,
    			R.drawable.climbing,
    			steps
    	));
    	//climbing downstairs
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Climb Downstairs after pressing start with phone in hand", 0));
    	activities.add(new HumanActivity(
    			"climbing_downstairs", 
    			R.string.climbing_downstairs,
    			R.string.instruction_activity_climbing_downstairs,
    			R.drawable.climbing,
    			steps
    	));
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Press start, put the phone in pocket and then climb downstairs", 0));
    	activities.add(new HumanActivity(
    			"climbing_downstairs", 
    			R.string.climbing_downstairs_pocket,
    			R.string.instruction_activity_climbing_downstairs_pocket,
    			R.drawable.climbing,
    			steps
    	));
    	
    	//********************relative ********************
    	//relative sitting
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Sit straight for", 15));
    	steps.add(new Step("Lean forward for", 5));
    	steps.add(new Step("Sit back straight for", 5));
    	steps.add(new Step("Lean backward for", 5));
    	steps.add(new Step("Sit back straight for", 5));
    	steps.add(new Step("Rotate trunk to right for", 5));
    	steps.add(new Step("Rotate back to straight for", 5));
    	steps.add(new Step("Rotate trunk to left for", 5));
    	steps.add(new Step("Rotate back to straight for", 5));
    	steps.add(new Step("Stand up for", 5));
    	activities.add(new HumanActivity(
    			"relative_sitting", 
    			R.string.relative_sitting,
    			R.string.instruction_activity_relative_sitting,
    			R.drawable.sitting,
    			steps));
    	//relative standing
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Stand still for", 15));
    	steps.add(new Step("Rotate trunk to right for", 5));
    	steps.add(new Step("Rotate back to the front for", 5));
    	steps.add(new Step("Rotate trunk to left for", 5));
    	steps.add(new Step("Rotate back to the front for", 5));
    	steps.add(new Step("Move up and down left arm for", 5));
    	steps.add(new Step("Stand still for", 5));
    	steps.add(new Step("Move up and down right arm for", 5));
    	steps.add(new Step("Stand still", 5));
    	activities.add(new HumanActivity(
    			"relative_standing", 
    			R.string.relative_standing,
    			R.string.instruction_activity_relative_standing,
    			R.drawable.standing, 
    			steps));
    	//relative lying
    	steps = new ArrayList<Step>();
    	steps.add(new Step("Lying with face up for", 15));
    	steps.add(new Step("Turn to left", 5));
    	steps.add(new Step("Lying back with face up for", 5));
    	steps.add(new Step("Turn to right for", 5));
    	steps.add(new Step("Lying back with face up for", 5));
    	steps.add(new Step("Sit up for", 5));
    	activities.add(new HumanActivity(
    			"relative_lying", 
    			R.string.relative_lying,
    			R.string.instruction_activity_relative_lying,
    			R.drawable.lying,
    			steps));
    	
    	//********************phone in pocket ********************
//    	//sitting with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then sit straight", 10));
//    	steps.add(new Step("Keep sitting straight", 60));
//    	activities.add(new HumanActivity(
//    			"sitting_with_phone_in_pocket", 
//    			R.string.sitting_with_phone_in_pocket, 
//    			R.string.instruction_activity_pocket_sitting,
//    			R.drawable.sitting,
//    			steps));
//    	//standing with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then stand", 10));
//    	steps.add(new Step("Keep standing", 60));
//    	activities.add(new HumanActivity(
//    			"standding_with_phone_in_pocket", 
//    			R.string.standing_with_phone_in_pocket, 
//    			R.string.instruction_activity_pocket_standing,
//    			R.drawable.standing,
//    			steps));
//    	//lying with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then lay down", 10));
//    	steps.add(new Step("Keep lying", 60));
//    	activities.add(new HumanActivity(
//    			"lying_with_phone_in_pocket", 
//    			R.string.lying_with_phone_in_pocket, 
//    			R.string.instruction_activity_pocket_lying,
//    			R.drawable.lying,
//    			steps));
//    	//walking with phone in pocket
//    	steps = new ArrayList<Step>();
//    	steps.add(new Step("Put the phone in pocket and then start walking", 10));
//    	steps.add(new Step("Keep walking", 60));
//    	activities.add(new HumanActivity(
//    			"walking_with_phone_in_pocket", 
//    			R.string.walking_with_phone_in_pocket, 
//    			R.string.instruction_activity_pocket_walking,
//    			R.drawable.walking,
//    			steps));
//    	
    	return activities;
    }
    
    class HumanActivity {
    	public String title;
    	String name;
    	String instruction;
    	int picResourceId;
    	boolean isFinished = false;
    	
    	ArrayList<Step> steps = new ArrayList<Step>();
    	public HumanActivity(
    			String activityName, 
    			int titleResouceId, int instructionResouceId, int picResourceId, 
    			ArrayList<Step> steps) {
    		this.name = activityName;
    		this.title = getResources().getString(titleResouceId);
    		this.instruction = getResources().getString(instructionResouceId);
    		this.picResourceId = picResourceId;
    		this.steps.addAll(steps);
    	}
    	
    	public ArrayList<Step> getSteps() {
    		ArrayList<Step> stepsToCopy = new ArrayList<Step>();
    		for(int i=0; i<steps.size(); i++){
    			Step step = steps.get(i);
    			stepsToCopy.add(new Step(step.stepDescription, step.time));
    		}
    		return stepsToCopy;
    	}
    }
    
	class Step{
		String stepDescription;
		int time;
		public Step(String description, int seconds) {
			time = seconds;
			stepDescription = description;
		}
	}

	class ActivityAdapter extends BaseAdapter {
		Context context;
		LayoutInflater inflater;
		ArrayList<HumanActivity> activities;
		
		public ActivityAdapter(Context context, ArrayList<HumanActivity> activities) {
			this.context = context;
			inflater = LayoutInflater.from(context);
			this.activities = activities;
		}
		
		@Override
		public int getCount() {
			return activities.size();
		}

		@Override
		public Object getItem(int position) {
			return activities.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null){
				convertView = inflater.inflate(R.layout.listview_string, parent, false);
				holder = new ViewHolder();
				holder.tv = (TextView) convertView.findViewById(R.id.textView1);
				convertView.setTag(holder);
			} 
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			HumanActivity activity = activities.get(position);
			holder.tv.setText(activity.title);
			if(activity.isFinished) 
				holder.tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
			else
				holder.tv.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
			return convertView;
		}

		class ViewHolder{
			public TextView tv;
		}
	}
}