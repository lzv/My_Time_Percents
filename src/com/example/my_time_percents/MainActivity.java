package com.example.my_time_percents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	// recreate(); - для перезапуска
	
	enum AppState {NOT_IN_PERIOD, IN_PERIOD};
	SimpleDateFormat dt_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat show_dt_format = new SimpleDateFormat("d MMMM HH:mm:ss");
	final String DB_TABLE_NAME = "time_periods";
	
	View rowReset, tableOfInfo, infoText;
	Button buttonStart, buttonPlus, buttonMinus;
	TextView valuePeriodStarted, valuePlusMinutes, valueMinusMinutes, valueAllMinutes;
	TextView valuePercent, valueFromLast;
	
	AppState app_state;
	DBHelper dbHelper;
	Timer updateTimer;
	boolean timer_is_running = false;
	
	final Handler myUpdateHandler = new Handler();
	
	final Runnable myUpdateRunnable = new Runnable() {
		public void run() {
			updateFromLastCheckValue();
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuItemHelp:
				startActivity(new Intent("mytimepercents.action.show.help"));
				break;
			case R.id.menuItemAbout:
				startActivity(new Intent("mytimepercents.action.show.about"));
				break;
		}
		return true;
	}
	
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		rowReset = findViewById(R.id.rowReset);
		tableOfInfo = findViewById(R.id.TableOfInfo);
		infoText = findViewById(R.id.infoPeriodNotStarted);
		buttonStart = (Button)findViewById(R.id.buttonStartPeriod);
		buttonPlus = (Button)findViewById(R.id.button_plus);
		buttonMinus = (Button)findViewById(R.id.button_minus);
		valuePeriodStarted = (TextView)findViewById(R.id.valuePeriodStarted);
		valuePlusMinutes = (TextView)findViewById(R.id.valuePlusMinutes);
		valueMinusMinutes = (TextView)findViewById(R.id.valueMinusMinutes);
		valueAllMinutes = (TextView)findViewById(R.id.valueAllMinutes);
		valuePercent = (TextView)findViewById(R.id.valuePercent);
		valueFromLast = (TextView)findViewById(R.id.valueFromLast);
		
		dbHelper = new DBHelper(this);
		
		// Проверим, идет учет периода или нет.
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		//query(boolean distinct, String table, String[] columns, String selection, 
		//	String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
		Cursor c = db.query(false, DB_TABLE_NAME, new String[] {"id"}, null, null, null, null, null, "1");
		app_state = c.moveToFirst() ? AppState.IN_PERIOD : AppState.NOT_IN_PERIOD;
		dbHelper.close();
		
		// Устанавливаем видимость элементов.
		setElementsVisibility();
	}
	
	/*@Override
	protected void onRestart () {
	    super.onRestart();
	    
	}*/
	
	/*@Override
	protected void onStart () {
	    super.onStart();
	    
	}*/
	
	@Override
	protected void onResume () {
	    super.onResume();
	    if (app_state == AppState.IN_PERIOD) {
	    	setValues();
	    	runUpdateTimer();
	    }
	}
	
	@Override
	protected void onPause () {
	    super.onPause();
	    if (app_state == AppState.IN_PERIOD) {
	    	stopUpdateTimer();
	    }
	}
	
	/*@Override
	protected void onStop () {
	    super.onStop();
	    
	}*/
	   
	/*@Override
	protected void onDestroy () {
	    super.onDestroy();
	    
	}*/
	
	protected void setValues () {
		if (app_state == AppState.IN_PERIOD) {
			Date start_period = new Date(), prev_dt = new Date();
			long time_plus = 0, time_minus = 0;
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			Cursor c = db.query(
					DB_TABLE_NAME, new String[] {"date_time", "type"}, 
					null, null, null, null, 
					"date_time asc");
			if (c.moveToFirst()) {
				int date_time_index = c.getColumnIndex("date_time");
				int type_index = c.getColumnIndex("type");
				try {
					do {
						String date_time = c.getString(date_time_index);
						String type = c.getString(type_index);
						Date row_dt = dt_format.parse(date_time);
						switch (type.charAt(0)) {
							case '0':
								start_period = row_dt;
								break;
							case '+':
								time_plus += row_dt.getTime() - prev_dt.getTime();
								break;
							case '-':
								time_minus += row_dt.getTime() - prev_dt.getTime();
								break;
						}
						prev_dt = row_dt;
					} while (c.moveToNext());
					valuePeriodStarted.setText(show_dt_format.format(start_period));
					valuePlusMinutes.setText(getTimeFromMiliseconds(time_plus));
					valueMinusMinutes.setText(getTimeFromMiliseconds(time_minus));
					valueAllMinutes.setText(getTimeFromMiliseconds(time_plus + time_minus));
					int percents = 0;
					if (time_plus > 0 || time_minus > 0) 
						percents = (int) (((double) time_plus / (double) (time_plus + time_minus)) * 100);
					valuePercent.setText(String.valueOf(percents) + " %");
					valueFromLast.setText(getTimeFromMiliseconds((new Date()).getTime() - prev_dt.getTime()));
				} catch (ParseException e) {
					Toast.makeText(this, "DB error - wrong datetime format", Toast.LENGTH_SHORT).show();
				} catch (IndexOutOfBoundsException e) {
					Toast.makeText(this, "DB error - no 'type' info", Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(this, "Error - DB is empty", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	protected String getTimeFromMiliseconds (long value)
	{
		value /= 1000;
		int seconds = (int) (value % 60);
		value /= 60;
		int minuts = (int) (value % 60);
		value /= 60; // а это уже количество часов
		return num2str((int) value, 1) + ":" + num2str(minuts, 2) + ":" + num2str(seconds, 2);
	}
	
	protected String num2str (int number, int min_length) {
		String result = String.valueOf(number);
		while (result.length() < min_length) result = "0" + result;
		return result;
	}
	
	public void onClickReset (View v)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.delete(DB_TABLE_NAME, null, null);
		dbHelper.close();
		app_state = AppState.NOT_IN_PERIOD;
		setElementsVisibility();
		stopUpdateTimer();
	}
	
	public void onClickStartPeriod (View v)
	{
		addDBRow("0");
		app_state = AppState.IN_PERIOD;
		setElementsVisibility();
		setValues();
		runUpdateTimer();
	}
	
	public void onClickPlus (View v)
	{
		addDBRow("+");
		setValues();
	}
	
	public void onClickMinus (View v)
	{
		addDBRow("-");
		setValues();
	}
	
	protected void runUpdateTimer () {
		if (!timer_is_running) {
			updateTimer = new Timer();
			updateTimer.schedule(new updateFromLastCheckTask(), 1000, 1000);
			timer_is_running = true;
		}
	}
	
	protected void stopUpdateTimer () {
		if (timer_is_running) {
			updateTimer.cancel();
			timer_is_running = false;
		}
	}
	
	protected void addDBRow (String type)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("date_time", dt_format.format(new Date()));
		cv.put("type", type);
		db.insert(DB_TABLE_NAME, null, cv);
		dbHelper.close();
	}
	
	protected void setElementsVisibility () {
		boolean in_period = (app_state == AppState.IN_PERIOD);
		rowReset.setVisibility(in_period ? View.VISIBLE : View.GONE);
		tableOfInfo.setVisibility(in_period ? View.VISIBLE : View.GONE);
		buttonPlus.setVisibility(in_period ? View.VISIBLE : View.GONE);
		buttonMinus.setVisibility(in_period ? View.VISIBLE : View.GONE);
		infoText.setVisibility(in_period ? View.GONE : View.VISIBLE);
		buttonStart.setVisibility(in_period ? View.GONE : View.VISIBLE);
	}

	protected void updateFromLastCheckValue () {
		if (app_state == AppState.IN_PERIOD) {
    		SQLiteDatabase db = dbHelper.getWritableDatabase();
    		Cursor c = db.query(
					false, DB_TABLE_NAME, new String[] {"date_time"}, 
					null, null, null, null, 
					"date_time desc", "1");
    		if (c.moveToFirst()) {
    			String date_time = c.getString(c.getColumnIndex("date_time"));
    			try {
    				Date row_dt = dt_format.parse(date_time);
    				long from_last = (new Date()).getTime() - row_dt.getTime();
    				valueFromLast.setText(getTimeFromMiliseconds(from_last));
    			} catch (ParseException e) {
    				Toast.makeText(this, "DB error - wrong datetime format", Toast.LENGTH_SHORT).show();
    			}
    		}
    	}
	}

	protected class updateFromLastCheckTask extends TimerTask {
	    public void run() {
	    	myUpdateHandler.post(myUpdateRunnable);
	    	//updateFromLastCheckValue(); - runtime error 
	    }
	}
	
	protected class DBHelper extends SQLiteOpenHelper {
		
		public DBHelper (Context context) {
			super(context, "time_periods_DB", null, 1);
		}
		
		@Override
		public void onCreate (SQLiteDatabase db) {
			// date_time - в формате YYYY-MM-DD HH:MM:SS
			// type - один символ: +, - или 0 (для начала периода)
			db.execSQL("create table " + DB_TABLE_NAME + " ("
			          + "id integer primary key autoincrement," 
			          + "date_time text,"
			          + "type text" + ");");
		}
		
		@Override
		public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {}
	}
}
