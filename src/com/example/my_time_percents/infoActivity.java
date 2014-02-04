package com.example.my_time_percents;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class infoActivity extends Activity {

	TextView info_tv;
	
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		info_tv = (TextView)findViewById(R.id.infoTextView);
		
		String action = getIntent().getAction();
		if (action.equals("mytimepercents.action.show.help")) {
			info_tv.setText(R.string.full_info_help);
		} else if (action.equals("mytimepercents.action.show.about")) {
			info_tv.setText(R.string.full_info_about);
		}
	}
}
