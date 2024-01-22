package com.kv.phonerecorder;

import static com.kv.phonerecorder.utils.PermissionHandling.checkRequiredPermissions;
import static com.kv.phonerecorder.utils.PermissionHandling.displayNeverAskAgainDialog;
import static com.kv.phonerecorder.utils.Utils.isFileDeleted;
import static com.kv.phonerecorder.utils.Utils.showToast;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kv.phonerecorder.utils.Utilities;
import com.kv.phonerecorder.utils.visualizer.LineBarVisualizer;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class RecordListActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener, SwipeRefreshLayout.OnRefreshListener, SensorEventListener {

    RecyclerView recyclerView;
    TextView tv_nodata;
    Button btn_allowPermission;
    ImageButton btnPlay;
    SeekBar seekBar;
    TextView songCurrentDurationLabel;
    TextView songTotalDurationLabel;
    LineBarVisualizer lbVisualizer;
    TextView tv_number;
    TextView tv_date_time;
    RelativeLayout rlBottomsheet;
    TextView tvDir;
    ImageButton btnPlayTop;
    ImageView ivCallType;
    ImageView ivBack;
    ImageView ivDelete;
    TextView tvCount;
    RelativeLayout sheetHeader;
    SwipeRefreshLayout swipeToRefresh;
    //    @BindView(R.id.adView)
//    AdView adView;

    ImageButton btClose;

    ImageButton btnShare;
    ImageButton btnDelete;
    SimpleDateFormat firstformat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat newformat = new SimpleDateFormat("dd-MM-yyyy");
    AppCompatActivity activity;
    ArrayList<AudioModel> audiolist = new ArrayList<>();
    ArrayList<String> dummylist;
    MediaPlayer mediaPlayer;
    SensorManager mySensorManager;
    Sensor sensor;
    private RecordListAdapter recordAdapter;
    private String path;
    private Handler mHandler = new Handler();
    private Utilities utils;
    private int currentDuration, totalDuration, currentAudioPosition;
    private boolean notifyOnResume, seekTrackTouch;
    private BottomSheetBehavior bottomSheetBehavior;
    private Paint p = new Paint();
    private boolean longclicked;
    private Handler handler = new Handler();
//    private InterstitialAd mInterstitialAd;

    public static boolean isAccessibilitySettingsOn(Context mContext, Class<? extends AccessibilityService> clazz) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + clazz.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setBodyUI();
    }

    private void setBodyUI() {
        findViews();
        setViewsClickable();
        activity = this;
        newInstall();
        refreshRecyclerView();

        setProximitySensor();
        if (!isAccessibilitySettingsOn(this, MediaRecorderService.class)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    private void setViewsClickable() {
        btnShare.setOnClickListener(v -> shareFile());
        btnDelete.setOnClickListener(v -> showDeleteDialog());
        ivDelete.setOnClickListener(v -> showDeleteDialog());
        btnPlay.setOnClickListener(v -> mediaPlayerState(false));
        btnPlayTop.setOnClickListener(v -> mediaPlayerState(false));
        btn_allowPermission.setOnClickListener(v -> refreshRecyclerView());
        btClose.setOnClickListener(v ->  onSheetClose());
        sheetHeader.setOnClickListener(v ->  {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        ivBack.setOnClickListener(v ->  onBackPressed());



    }

    private void findViews() {
        recyclerView = findViewById(R.id.recycler_view);
        tv_nodata = findViewById(R.id.tv_nodata);
        btn_allowPermission = findViewById(R.id.btn_allowPermission);
        btnPlay = findViewById(R.id.btnPlay);
        seekBar = findViewById(R.id.seekBar2);
        songCurrentDurationLabel = findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = findViewById(R.id.songTotalDurationLabel);
        lbVisualizer = findViewById(R.id.lbVisualizer);
        tv_number = findViewById(R.id.tv_number);
        tv_date_time = findViewById(R.id.tv_date_time);
        rlBottomsheet = findViewById(R.id.rl_bottomsheet);
        tvDir = findViewById(R.id.tv_dir);
        btnPlayTop = findViewById(R.id.btnPlayTop);
        ivCallType = findViewById(R.id.iv_call_type);
        ivBack = findViewById(R.id.iv_back);
        ivDelete = findViewById(R.id.iv_delete);
        tvCount = findViewById(R.id.tv_count);
        sheetHeader = findViewById(R.id.sheet_header);
        swipeToRefresh = findViewById(R.id.swipeToRefresh);
        btClose = findViewById(R.id.bt_close);
        btnShare = findViewById(R.id.btnShare);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void setProximitySensor() {
        mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (sensor == null) {
            Log.e("Record", "No Proximity Sensor!");
        } else {
            Log.e("Record", "Proximity Sensor!");
            mySensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] == 0) {
                //Near
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    pauseMP();
                }
            } else {
                //Away
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void newInstall() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> subscription = SubscriptionManager.from(getApplicationContext()).getActiveSubscriptionInfoList();
                for (SubscriptionInfo sbInfo : subscription) {
                    sbInfo.getNumber();
                }
            } else {
                TelephonyManager tMgr = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                String mPhoneNumber = tMgr.getLine1Number();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(rlBottomsheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
                if (v > 0.15) {
                    btnPlayTop.setVisibility(View.GONE);
                } else {
                    btnPlayTop.setVisibility(View.VISIBLE);
                }
            }

        });
        bottomSheetBehavior.setPeekHeight(107);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mediaPlayer = new MediaPlayer();
        utils = new Utilities();
        seekBar.setOnSeekBarChangeListener(this);
        mediaPlayer.setOnCompletionListener(this);
        seekBar.setMax(100);
        lbVisualizer.setColor(activity.getResources().getColor(R.color.colorAccent));
        lbVisualizer.setDensity(70);
        lbVisualizer.setPlayer(mediaPlayer.getAudioSessionId());
    }

    private void refreshRecyclerView() {
        if (checkRequiredPermissions(activity)) {
            btn_allowPermission.setVisibility(View.GONE);

            getRecordedFiles();

            notifyOnResume = true;

            if (audiolist.size() > 0) {
                tv_nodata.setVisibility(View.GONE);

                if (recordAdapter == null) {
                    recordAdapter = new RecordListAdapter(activity, audiolist);
                    recyclerView.setHasFixedSize(true);
                    recyclerView.setLayoutManager(new LinearLayoutManager(activity));
                    recyclerView.setAdapter(recordAdapter);
                    initSwipe();
                    //viewAdsBanner();
                    //        viewAdsInterstitial();
                    itemClickHandle();
                    setBottomSheet();
                    swipeToRefresh.setColorSchemeResources(R.color.colorAccent);
                    swipeToRefresh.setOnRefreshListener(this);
                } else {
                    recordAdapter.notifyDataSetChanged();
                }
            } else {
                tv_nodata.setVisibility(View.VISIBLE);
            }
        } else {
            btn_allowPermission.setVisibility(View.VISIBLE);
        }
    }

    private void initSwipe() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                TextView tv_number = viewHolder.itemView.findViewById(R.id.tv_number);

                if (direction == ItemTouchHelper.LEFT) { //message
                    Intent sms_intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + tv_number.getText().toString()));
                    sms_intent.putExtra("sms_body", "");

                    if (sms_intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(sms_intent);
                    }
                } else {
//                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tv_number.getText().toString())));
                    if (checkRequiredPermissions(activity)) {
                        startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse("tel:" + tv_number.getText().toString())));
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                Bitmap icon;
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if (dX > 0) {
                        p.setColor(Color.parseColor("#6EBD52"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        c.drawRect(background, p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.callout_white);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width, (float) itemView.getTop() + width, (float) itemView.getLeft() + 2 * width, (float) itemView.getBottom() - width);
                        c.drawBitmap(icon, null, icon_dest, p);
                    } else {
                        p.setColor(Color.parseColor("#EB9500"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background, p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.envelope);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2 * width, (float) itemView.getTop() + width, (float) itemView.getRight() - width, (float) itemView.getBottom() - width);
                        c.drawBitmap(icon, null, icon_dest, p);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void itemClickHandle() {
        RecordListAdapter.setBinder(new ClickListener() {
            @Override
            public void onClick(int position, String number, String date_time, String call_type) {
                setBottomSheetData(position, number, date_time, call_type);
            }

            @Override
            public void onSelect(boolean isChecked, int position) {
                if (dummylist == null) {
                    dummylist = new ArrayList<>();
                    ivBack.setVisibility(View.VISIBLE);
                    ivDelete.setVisibility(View.VISIBLE);
                    tvCount.setVisibility(View.VISIBLE);
                    longclicked = true;
                    onSheetClose();
                }

                if (isChecked) {
                    dummylist.add(position + "");
                } else {
                    dummylist.remove(position + "");
                }
                tvCount.setText(dummylist.size() + "");
                if (dummylist.size() == 0) {
                    updateViews();
                }
            }
        });
    }

    private void setBottomSheetData(int position, String number, String date_time, String call_type) {
        path = audiolist.get(position).getPath();
        currentAudioPosition = position;
        if (call_type.equals("IN")) {
            ivCallType.setImageDrawable(activity.getResources().getDrawable(R.drawable.call_in));
        } else {
            ivCallType.setImageDrawable(activity.getResources().getDrawable(R.drawable.calls_out));
        }
        tv_number.setText("Unknown call");
        tv_date_time.setText(date_time);
        tvDir.setText(path);
        rlBottomsheet.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mediaPlayerState(true);
        if (bottomSheetBehavior.getPeekHeight() == 107) {
            sheetHeader.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    sheetHeader.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    bottomSheetBehavior.setPeekHeight(sheetHeader.getHeight());
                }
            });
        }
    }

    private void getRecordedFiles() {
        if (audiolist.size() > 0) {
            audiolist.clear();
        }

        String path = Environment.getExternalStorageDirectory().toString() + "/Call Recorder";
        File folder = new File(path);

        if (folder.isDirectory()) {
            File[] listOfDirs = folder.listFiles();
            for (File listOfDir : listOfDirs) {

                if (listOfDir.isDirectory()) {
                    File folder2 = new File(path + '/' + listOfDir.getName());
                    File[] listOfFile = folder2.listFiles();
                    for (File audio : listOfFile) {
                        if (!audio.getName().equals(".nomedia")) {
                            AudioModel audioModel = new AudioModel();
                            audioModel.setName(audio.getName());
                            audioModel.setPath(audio.getAbsolutePath());
                            audioModel.setLength(audio.length());

                            try {
                                audioModel.setDate(newformat.format(firstformat.parse(listOfDir.getName())));
                            } catch (ParseException e) {
                                audioModel.setDate(listOfDir.getName());
                            }

                            audiolist.add(audioModel);
                        }
                    }
                }
            }
            Collections.reverse(audiolist);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int permission = 1; //PERMISSION_GRANTED
        for (int status : grantResults) {
            if (status == PackageManager.PERMISSION_DENIED) {
                permission = 0;//PERMISSION_DENIED
            }
        }

        if (permission == 1) {
            refreshRecyclerView();
        } else {
            displayNeverAskAgainDialog(activity);
        }
    }


    private void deleteFiles() {
        Collections.sort(dummylist, Collections.<String>reverseOrder());
        for (String str : dummylist) {
            int index = Integer.parseInt(str);
            try {
                isFileDeleted(audiolist.get(index).getPath());
                audiolist.remove(index);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        onBackPressed();
    }

    private void onSheetClose() {
        setViewOnFinish();

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        rlBottomsheet.setVisibility(View.GONE);
    }

    private void setCurrentDurationText() {
        currentDuration = mediaPlayer.getCurrentPosition();
        songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer(currentDuration));
    }

    private void startMediaPlayer() {
        try {
            setPauseDrawable();
            seekBar.setProgress(0);
            songCurrentDurationLabel.setText(getString(R.string.zero));
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    addSeekHandler();
                    totalDuration = mediaPlayer.getDuration();
                    songTotalDurationLabel.setText("" + utils.milliSecondsToTimer(totalDuration));
                }
            });
            mediaPlayer.prepareAsync();
//            mediaPlayer.prepare();
//            mediaPlayer.start();
        } catch (Exception e) {
            showToast(activity, "Playback error occurred");
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            setCurrentDurationText();

            if (!seekTrackTouch) {
                seekBar.setProgress(utils.getProgressPercentage(currentDuration, totalDuration));
            }

            addSeekHandler();
        }
    };

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        seekTrackTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekTrackTouch = false;
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), mediaPlayer.getDuration());
        mediaPlayer.seekTo(currentPosition);
        setCurrentDurationText();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (notifyOnResume) {
            refreshRecyclerView();
        }
    }

    private void mediaPlayerState(boolean play) {
        if (play) {
            startMediaPlayer();
        } else if (mediaPlayer.isPlaying()) {
            pauseMP();
        } else if (currentDuration != 0 && currentDuration + 1000 < totalDuration) {
            resumeMP();
        } else {
            startMediaPlayer();
        }
    }

    private void pauseMP() {
        setPlayDrawable();
        mediaPlayer.pause();
        removeSeekHandler();
    }

    private void resumeMP() {
        addSeekHandler();
        setPauseDrawable();
        mediaPlayer.seekTo(currentDuration);
        mediaPlayer.start();
    }

    private void setPlayDrawable() {
        btnPlayTop.setBackground(this.getResources().getDrawable(R.drawable.img_btn_play));
        btnPlay.setBackground(this.getResources().getDrawable(R.drawable.img_btn_play));
    }

    private void setPauseDrawable() {
        btnPlayTop.setBackground(this.getResources().getDrawable(R.drawable.img_btn_pause));
        btnPlay.setBackground(this.getResources().getDrawable(R.drawable.img_btn_pause));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pauseMP();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            setViewOnFinish();
            mediaPlayer.release();
        }
    }

    private void setViewOnFinish() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            seekBar.setProgress(100);
            songCurrentDurationLabel.setText(songTotalDurationLabel.getText().toString());
        }
        setPlayDrawable();

        if (mUpdateTimeTask != null)
            removeSeekHandler();
    }

    private void shareFile() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
        startActivity(Intent.createChooser(share, "Share Recording"));
    }

    private void showDeleteDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (longclicked) {
                            deleteFiles();
                        } else {
                            isFileDeleted(path);
                            onDeleteItem();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        new AlertDialog.Builder(activity)
                .setMessage("Are you sure you want to delete this audio??")
                .setPositiveButton("YES", dialogClickListener)
                .setNegativeButton("NO", dialogClickListener)
                .show();
    }

    private void onDeleteItem() {
        recordAdapter.removeItem(currentAudioPosition);
        onSheetClose();
        showToast(activity, "Audio deleted");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        setViewOnFinish();
    }

    private void removeSeekHandler() {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private void addSeekHandler() {
        mHandler.postDelayed(mUpdateTimeTask, 1000);
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior != null && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (longclicked) {
            updateViews();
        } else {
            super.onBackPressed();
        }
    }

    private void updateViews() {
        dummylist = null;
        ivBack.setVisibility(View.GONE);
        tvCount.setVisibility(View.GONE);
        ivDelete.setVisibility(View.GONE);
        longclicked = false;
        recordAdapter.cancelLongClick();
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    @Override
    public void onRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshRecyclerView();
                swipeToRefresh.setRefreshing(false);
            }
        }, 2000);
    }




}

