package com.example.agervail.timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Calendar;


public class MyActivity extends Activity {

    private static final String DEBUG_TAG = "MyActivity";
    public static final String PREFS_NAME = "MyPrefsFile";


    Chronometer chrono = null;
    long timeWhenStopped = -1;
    int nbSet = 0;
    TextView restTime = null;
    TextView nbSetText = null;
    Button oldClicked = null;
    CountDownTimer cdt = null;

    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        chrono = (Chronometer)findViewById(R.id.chronometer);
        restTime = (TextView)findViewById(R.id.restTime);
        nbSetText = (TextView)findViewById(R.id.nbSet);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openSettings(){

        LayoutInflater linf = LayoutInflater.from(this);
        final View inflator = linf.inflate(R.layout.dialog_settings, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Settings");
        alert.setView(inflator);

        final EditText user = (EditText) inflator.findViewById(R.id.username);
        final EditText cal = (EditText) inflator.findViewById(R.id.calendarName);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String username = settings.getString("username", "");
        String calendarName = settings.getString("calendarName", "");
        user.setText(username);
        cal.setText(calendarName);
        alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String s1=user.getText().toString();
                String s2=cal.getText().toString();
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username",s1);
                editor.putString("calendarName",s2);
                editor.commit();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long tws = chrono.getBase() - SystemClock.elapsedRealtime();
        long timeAtPause = SystemClock.elapsedRealtime();
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("timePause",tws);
        editor.putLong("timeAtPause",timeAtPause);
        editor.commit();
        Log.i(DEBUG_TAG, "Pause : " + tws);

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long timePause = settings.getLong("timePause", -1);
        long timeAtPause = settings.getLong("timeAtPause", -1);

        if(timePause != -1) {
            chrono.setBase(SystemClock.elapsedRealtime() + timePause - (SystemClock.elapsedRealtime() - timeAtPause));
        }
        Log.i(DEBUG_TAG, "Pause : " + timePause);
        Log.i(DEBUG_TAG, "temps " + (SystemClock.elapsedRealtime() - timeAtPause));
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(DEBUG_TAG, "stop : ");
    }

    public void resetButton(View view){
        chrono.setBase(SystemClock.elapsedRealtime());
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long tws = chrono.getBase() - SystemClock.elapsedRealtime();
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("timePause",tws);
        editor.putLong("timeAtPause",-1);
        editor.commit();
        Log.i(DEBUG_TAG, "Reset");
    }

    public void onToggleClicked(View view) {
        // Is the toggle on?
        boolean on = ((ToggleButton) view).isChecked();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        long timePause = settings.getLong("timePause", -1);
        long timeAtPause = settings.getLong("timeAtPause", -1);
        if (on) {
            if(timePause == -1){
                chrono.setBase(SystemClock.elapsedRealtime());
            }else{
                if(timeAtPause != -1) {
                    chrono.setBase(SystemClock.elapsedRealtime() + timePause - (SystemClock.elapsedRealtime() - timeAtPause));
                } else{
                    chrono.setBase(SystemClock.elapsedRealtime() + timePause);
                }
            }
            chrono.start();
        } else {
            long tws = chrono.getBase() - SystemClock.elapsedRealtime();
            chrono.stop();
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("timePause",tws);
            editor.commit();
        }
    }

    public void onRestClick(View view){
        Button restButton = ((Button) view);
        if(oldClicked != restButton) {
            if (oldClicked != null) {
                oldClicked.setHeight(oldClicked.getHeight() / 2);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.weight = 1.f;
                oldClicked.setLayoutParams(lp);
            }
            //restButton.getHeight();
            restButton.setHeight(restButton.getHeight() * 2);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.weight = 4.f;
            restButton.setLayoutParams(lp);
            oldClicked = restButton;
        }
        String txt = restButton.getText().toString();
        if(!(txt.equals("+"))){
            nbSet++;
            nbSetText.setText("" + nbSet);
            int tps = Integer.parseInt(txt);
            if(cdt != null){cdt.cancel();}
            cdt = new CountDownTimer(tps * 1000, 1000) {

                public void onTick(long millisUntilFinished) {
                    restTime.setText("" + millisUntilFinished / 1000);
                }

                public void onFinish() {
                    restTime.setText("-");
                }
            };
            cdt.start();

        }

    }

    public void onLapClick(View view){
        nbSet = 0;
        nbSetText.setText("" + nbSet);
    }

    public void onShareClick(View view){
        Cursor cur = null;
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND ("
                + CalendarContract.Calendars.ACCOUNT_TYPE + " = ?))";
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String username = settings.getString("username", "");
        String calendarName = settings.getString("calendarName", "");
        String[] selectionArgs = new String[] {username, "com.google"};
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
        // Use the cursor to step through the returned records
        long trainingID = -1;
        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;
            calID = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            Log.i(DEBUG_TAG, "Rows updated: " + displayName);
            if(displayName.equals(calendarName)){
                trainingID = calID;
                Log.i(DEBUG_TAG, "Training id " + calID);
            }
        }

        if(trainingID != -1) {
            //Create the event
            long startMillis = 0;
            long endMillis = 0;
            Calendar endTime = Calendar.getInstance();
            Calendar beginTime = Calendar.getInstance();
            int duration = getSecondsFromDurationString(chrono.getText().toString());
            beginTime.add(Calendar.SECOND, -duration);
            startMillis = beginTime.getTimeInMillis();
            endMillis = endTime.getTimeInMillis();
            Log.i(DEBUG_TAG, "begin " + beginTime.getTime().toString() + "end " + endTime.getTime().toString());

            final ContentResolver cr2 = getContentResolver();
            final ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, startMillis);
            values.put(CalendarContract.Events.DTEND, endMillis);
            values.put(CalendarContract.Events.TITLE, "Work out");
            values.put(CalendarContract.Events.DESCRIPTION, "");
            values.put(CalendarContract.Events.CALENDAR_ID, trainingID);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Paris");


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm");
            Log.i(DEBUG_TAG, "begin " + (int) ((endMillis - startMillis)));
            Log.i(DEBUG_TAG, " bla" + getSecondsFromDurationString(chrono.getText().toString()));
            builder.setMessage("Do you want to add training of " + ((int) (duration / 60)) + "'" + (duration % 60) + " ?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing but close the dialog
                    Uri uri = cr2.insert(CalendarContract.Events.CONTENT_URI, values);

                    long eventID = Long.parseLong(uri.getLastPathSegment());
                    Log.i(DEBUG_TAG, "Event added to the calendar");
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Training successfully added !",
                            Toast.LENGTH_LONG).show();
                }

            });

            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Canceled..",
                            Toast.LENGTH_LONG).show();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
            //uri = cr2.insert(CalendarContract.Events.CONTENT_URI, values);
            //long eventID = Long.parseLong(uri.getLastPathSegment());
        } else {
            Toast.makeText(getApplicationContext(), "Can't find the calendar "+calendarName+" with address " + username,
                    Toast.LENGTH_LONG).show();
        }
    }


    // Expects a string in the form MM:SS or HH:MM:SS
    public static int getSecondsFromDurationString(String value){

        String [] parts = value.split(":");

        // Wrong format, no value for you.
        if(parts.length < 2 || parts.length > 3)
            return 0;

        int seconds = 0, minutes = 0, hours = 0;

        if(parts.length == 2){
            seconds = Integer.parseInt(parts[1]);
            minutes = Integer.parseInt(parts[0]);
        }
        else if(parts.length == 3){
            seconds = Integer.parseInt(parts[2]);
            minutes = Integer.parseInt(parts[1]);
            hours = Integer.parseInt(parts[0]);
        }

        return seconds + (minutes*60) + (hours*3600);
    }

}
