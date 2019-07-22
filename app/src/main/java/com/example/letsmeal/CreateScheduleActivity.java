package com.example.letsmeal;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.letsmeal.dummy.Person;
import com.example.letsmeal.dummy.Schedule;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;

public class CreateScheduleActivity extends AppCompatActivity {

    private final String TAG = "CreateScheduleActivity";

    Schedule schedule;
    Calendar dateCalendar, timeCalendar;
    EditText titleText;
    EditText dateText;
    EditText timeText;
    EditText placeText;
    EditText participantsText;
    EditText descriptionText;
    TextView personNotFoundLabel;
    Button findUidBtn;
    Button submitBtn;
    Toolbar createScheduleToolBar;

    ArrayList<String> participantUIDs;

    FirebaseFirestore db;
    CollectionReference scheduleCollection;
    CollectionReference userCollection;

    Handler usersHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);

        // this.schedule = (Schedule) getIntent().getSerializableExtra("schedule");
        String organizerUid = getIntent().getStringExtra("organizerUid");
        participantUIDs = new ArrayList<>();
        participantUIDs.add(organizerUid);

        this.schedule = new Schedule(organizerUid);
        this.dateCalendar = Calendar.getInstance();
        this.timeCalendar = Calendar.getInstance();

        this.titleText = findViewById(R.id.titleText);
        this.dateText = findViewById(R.id.dateText);
        this.timeText = findViewById(R.id.timeText);
        this.placeText = findViewById(R.id.placeText);
        this.participantsText = findViewById(R.id.participantsText);
        this.descriptionText = findViewById(R.id.descriptionText);
        this.personNotFoundLabel = findViewById(R.id.personNotFoundLabel);
        this.findUidBtn = findViewById(R.id.findUidBtn);
        this.submitBtn = findViewById(R.id.submitBtn);
        this.createScheduleToolBar = findViewById(R.id.createScheduleToolBar);

        setSupportActionBar(this.createScheduleToolBar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_black_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        dateText.setOnClickListener(new EditText.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDatePickerDialog().show();
            }
        });
        timeText.setOnClickListener(new EditText.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTimePickerDialog().show();
            }
        });

        db = FirebaseFirestore.getInstance();
        scheduleCollection = db.collection(getString(R.string.firestore_schedule_collection));
        userCollection = db.collection(getString(R.string.firestore_user_collection));        this.usersHandler = new Handler();
        findUidBtn.setOnClickListener(findUidBtnOnClickListener);

        submitBtn.setOnClickListener(submitBtnOnclickListener);

    }



    /**
     * A helper function to initialize a DatePickerDialog using current date.
     * @return A DatePickerDialog initialized with current date
     */
    private DatePickerDialog currentDatePickerDialog() {
        Calendar cal = Calendar.getInstance();

        return new DatePickerDialog(
                this, onDateSetListener,
                cal.get(cal.YEAR), cal.get(cal.MONTH), cal.get(cal.DAY_OF_MONTH));
    }

    /**
     * A helper function to initialize a TimePickerDialog using current time.
     * TimePicker does NOT show 24-hour view.
     * @return A TimePickerDialog initialized with current time
     */
    private TimePickerDialog currentTimePickerDialog() {
        Calendar cal = Calendar.getInstance();
        return new TimePickerDialog(
                this, onTimeSetListener,
                cal.get(cal.HOUR_OF_DAY), cal.get(cal.MINUTE), false);
    }

    /**
     * This function modifies this.schedule's YEAR, MONTH, DAY_OF_MONTH fields.
     * Beware of month; it starts from 0, not 1.
     */
    private DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
            dateCalendar.set(year, month, dayOfMonth);
            dateText.setText(Schedule.getDateString(dateCalendar));
        }
    };



    private TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
            timeCalendar.set(0, 0, 0, hourOfDay, minute);
            timeText.setText(Schedule.getTimeString(timeCalendar));
        }
    };


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private Button.OnClickListener findUidBtnOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {

            CreateScheduleActivity.this.personNotFoundLabel.setVisibility(View.INVISIBLE);

            //TODO: Check if the requested name's UID is already in local device due to a previous query.

            class FetchUidFromFirestore implements Runnable {
                public void run() {
                    final String name = participantsText.getText().toString();
                    final Query query = userCollection.whereEqualTo("name", name);
                    query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Got " + task.getResult().size() + " matching users");
                                // For now, we don't care about duplicate names.
                                if (task.getResult().size() == 0) {
                                    CreateScheduleActivity.this.personNotFoundLabel.setText(
                                            "사용자 " + name + "을 찾을 수 없습니다.");
                                    CreateScheduleActivity.this.personNotFoundLabel.setVisibility(View.VISIBLE);
                                }
                                for (final QueryDocumentSnapshot document : task.getResult()) {
                                    // User document has UID as its document ID.
                                    final String uid = document.getId();
                                    if (!participantUIDs.contains(uid)) {
                                        usersHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                participantUIDs.add(uid);
                                                // TODO: Update UI using user name tags.
                                                Log.d(TAG, "Added " + document.get("name") + ", uid = " + document.get("uid"));
                                            }
                                        });
                                    } else {
                                        Log.d(TAG, "Duplicate uid for name " + document.get("name"));
                                    }
                                }
                            } else {
                                Log.d(TAG, "Failed to get initial schedules: " + task.getException());
                            }
                        }
                    });
                }
            };

            Thread fetchUidFromFirestore = new Thread(new FetchUidFromFirestore());
            fetchUidFromFirestore.start();

        }
    };

    /**
     * Listens to onClick event of submitBtn.
     * Wraps up all the data into a Schedule instance,
     * and returns it to MainActivity.
     */
    private Button.OnClickListener submitBtnOnclickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            schedule.setTimestamp(
                    dateCalendar.get(dateCalendar.YEAR),
                    dateCalendar.get(dateCalendar.MONTH),
                    dateCalendar.get(dateCalendar.DAY_OF_MONTH),
                    timeCalendar.get(timeCalendar.HOUR_OF_DAY),
                    timeCalendar.get(timeCalendar.MINUTE));
            schedule.setDescription(descriptionText.getText().toString());

            schedule.setTitle(titleText.getText().toString());
            Log.d(TAG, Schedule.getDateString(dateCalendar));
            Log.d(TAG, Schedule.getTimeString(timeCalendar));
            Log.d(TAG, schedule.timestampToCalendar().toString());
            schedule.setPlace(placeText.getText().toString());

            Intent resultSchedule = new Intent();
            resultSchedule.putExtra("schedule", schedule);
            setResult(RESULT_OK, resultSchedule);
            finish();
        }
    };



}
