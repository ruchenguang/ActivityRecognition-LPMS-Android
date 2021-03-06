package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

import cn.edu.zju.activityrecognition.tools.BluetoothService;
import cn.edu.zju.activityrecognition.tools.ExitApplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class InformationActivity extends Activity {
	RadioButton	femaleButton, maleButton;
	EditText heightEditText;
	Button nextButton;
	
	int id = -1;
	float height = -1;
	int gender = -1;
	static final int FEMALE = 0;
	static final int MALE = 1;
	
	File activityRecognitionDir;
	File subjectDir;
	public static String subjectDirPath;
	String idKey = "id_number";
	final String EXTRA_PATH = "ActivityRecognition::SubjectDataPath";
	
	SharedPreferences sp;
	
	int clickCnt = 0;
	boolean isArchiveCreated = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_information);
		ExitApplication.activityList.add(this);
		setTitle(R.string.title_information_activity);
		
		TextView welcomTv = (TextView) findViewById(R.id.textView2);
		welcomTv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickCnt ++;
				if(clickCnt > 5){
					clickCnt = 0;
					BluetoothService.isDebug = !BluetoothService.isDebug;
					if(BluetoothService.isDebug == true)
						Toast.makeText(InformationActivity.this, 
								"Debug mode is on.", Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(InformationActivity.this, 
								"Debug mode is off.", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		heightEditText = (EditText) findViewById(R.id.editText1);
		
		femaleButton = (RadioButton) findViewById(R.id.radioButton1);
		femaleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				maleButton.setChecked(false);
				gender = FEMALE;
			}
		});
		maleButton = (RadioButton) findViewById(R.id.RadioButton01);
		maleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				femaleButton.setChecked(false);
				gender = MALE;
			}
		});
		
		nextButton = (Button) findViewById(R.id.buttonNext);
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation animation = AnimationUtils.loadAnimation(InformationActivity.this, R.anim.button_scale);
				animation.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}
					@Override
					public void onAnimationRepeat(Animation animation) {
					}
					@Override
					public void onAnimationEnd(Animation animation) {
						try{
							height = Float.parseFloat(heightEditText.getText().toString());
						} catch (Exception e){
							height = -1;
						} 
						
						if(gender<0 && height<= 0)
							Toast.makeText(InformationActivity.this, 
									getResources().getString(R.string.info_incomplete), 
									Toast.LENGTH_SHORT).show();
						else if(gender<0)
							Toast.makeText(InformationActivity.this, 
									getResources().getString(R.string.info_gender_incomplete), 
									Toast.LENGTH_SHORT).show();
						else if(height<=0)
							Toast.makeText(InformationActivity.this, 
									getResources().getString(R.string.info_height_incomplete), 
									Toast.LENGTH_SHORT).show();
						else{
							final Intent intent = new Intent(InformationActivity.this, ConnectionActivity.class);
							
							if(isArchiveCreated){
								Toast.makeText(InformationActivity.this, "Your information has been changed.", Toast.LENGTH_SHORT).show();
								createAchive();
								startActivity(intent);
							} else {
								AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
								builder.setCancelable(true);
								builder.setTitle("Notice");
								builder.setMessage("Your personal archive folder will be created after your pressing \"Okay\", " +
										"and this archive folder will always be used for saving your information and data unless the app was killed.");
								builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										isArchiveCreated = true;
										createAchive();
										startActivity(intent);
									}
								});
								builder.create().show();
							}
						}
					}
				});
				v.startAnimation(animation);
			}
		});
		
		//create the root directory ActivityRecognitionTest
		activityRecognitionDir = new File(Environment.getExternalStorageDirectory(), "ActivityRecognitionExperiment");
		if(!activityRecognitionDir.exists()){
			activityRecognitionDir.mkdir();
		}
		
		sp = InformationActivity.this.getSharedPreferences("id_record", MODE_PRIVATE);
		id = sp.getInt(idKey, -1);
		if(id == -1) id = 1;
		else id++;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(null != subjectDir){
				AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
				builder.setTitle("Notice");
				builder.setMessage("Press \"Yes\" will stop this app and none of your info or data will be avaiable again. Are you sure you want to exit?");
				builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						File readme = new File(subjectDir, "readme.txt");
						try {
							FileOutputStream fos = new FileOutputStream(readme);
							fos.write("Data from this subject should not be used because he or she left this app in an unappropriate way.".getBytes());
							fos.close();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						InformationActivity.this.finish();
					}
				});
				builder.create().show();
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	void createAchive(){
		//create a folder for this subject with its ID
		DecimalFormat df = new DecimalFormat("00000");
		subjectDir = new File(activityRecognitionDir.getAbsoluteFile(), "subject_"+df.format(id));
		if(!subjectDir.exists()){
			subjectDir.mkdir();
		}
		
		//save subject info to a file
		try {
			File subjectInfo = new File(subjectDir.getAbsoluteFile(), "subject_info.txt");
			FileOutputStream fos = new FileOutputStream(subjectInfo);
			
			//write the gender
			fos.write(getResources().getString(R.string.info_gender).getBytes());
			if(gender == FEMALE)
				fos.write(getResources().getString(R.string.gender_female).getBytes());
			else 
				fos.write(getResources().getString(R.string.gender_male).getBytes());
			fos.write(";".getBytes());
			
			//write the height
			fos.write(getResources().getString(R.string.info_height).getBytes());
			fos.write(String.valueOf(height).getBytes());
			fos.write("ft".getBytes());
			fos.write(";".getBytes());
			
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		subjectDirPath = subjectDir.getAbsolutePath();
		
		//edit the subject number in the sp file
		Editor editor = sp.edit();
		editor.putInt(idKey, id);
		editor.commit();
	}
}
