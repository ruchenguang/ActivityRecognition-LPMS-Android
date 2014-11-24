package cn.edu.zju.activityrecognition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
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
	EditText ageEditText;
	Button howtoButton;
	TextView idTextView;
	
	int id = -1;
	int age = -1;
	int gender = -1;
	static final int FEMALE = 0;
	static final int MALE = 1;
	
	File activityRecognitionDir;
	public static String subjectDirPath;
	String idKey = "id_number";
	final String EXTRA_PATH = "ActivityRecognition::SubjectDataPath";
	
	SharedPreferences sp;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_information);
		idTextView = (TextView) findViewById(R.id.textView2);
		ageEditText = (EditText) findViewById(R.id.editText1);
		
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
		
		howtoButton = (Button) findViewById(R.id.button1);
		howtoButton.setOnClickListener(new OnClickListener() {
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
							age = Integer.parseInt(ageEditText.getText().toString());
						} catch (Exception e){
							Toast.makeText(InformationActivity.this, "Please enter your age...", Toast.LENGTH_LONG).show();
						}
						if(age>=0 && gender>=0){
							initSubject();

							
							Intent intent = new Intent(InformationActivity.this, InstructionActivity.class);
							intent.putExtra(EXTRA_PATH, subjectDirPath);
							startActivity(intent);
						} else {
							Toast.makeText(InformationActivity.this, "Please fill in your age and gender...", Toast.LENGTH_LONG).show();
						}
					}
				});
				v.startAnimation(animation);
			}
		});
		
		//create the root directory ActivityRecognitionTest
		activityRecognitionDir = new File(Environment.getExternalStorageDirectory(), "ActivityRecognitionTest");
		if(!activityRecognitionDir.exists()){
			activityRecognitionDir.mkdir();
		}
		
		sp = InformationActivity.this.getSharedPreferences("id_record", MODE_PRIVATE);
		id = sp.getInt(idKey, -1);
		if(id == -1) id = 1;
		else id++;
		
		idTextView.setText(String.format("%1$04d", id));
	}
	
	void initSubject(){
		//create a folder for this subject with its ID
		File subjectDir = new File(activityRecognitionDir.getAbsoluteFile(), "subject_"+id);
		if(!subjectDir.exists()){
			subjectDir.mkdir();
		}
		
		//save subject info to a file
		try {
			File subjectInfo = new File(subjectDir.getAbsoluteFile(), "subject_info.txt");
			FileOutputStream fos = new FileOutputStream(subjectInfo);
			fos.write(String.valueOf(age).getBytes());
			fos.write(" ".getBytes());
			fos.write(String.valueOf(gender).getBytes());
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
