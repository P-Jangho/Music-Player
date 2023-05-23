package jp.ac.jec.cm0135.musicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private MediaPlayer mPlayer;
    private ImageButton btnPlay;
    private ImageButton btnStop;
    private Button btnSDList;
    private TextView fileName;
    String path;
    private final int SEARCH_REQCD = 123;
    private static final int EXTERNAL_STORAGE = 1;
    private SeekBar seekBar;
    private int mTotalTime;
    private int currentPosition;
    private Thread thread;
    private boolean playStart = true;

    private int currentTime;
    private TextView txtCurrentTime; // 현재 재생 시간을 표시할 TextView
    private TextView txtTotalTime; // 전체 재생 시간을 표시할 TextView
    private Handler timeHandler; // 시간 업데이트를 처리할 Handler 객체
    private static final int UPDATE_TIME = 1; // Handler 메시지 식별자
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);

        seekBar = findViewById(R.id.seek);
        fileName = findViewById(R.id.fileName);

        if(Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE
                );
            }
        }

        btnPlay = findViewById(R.id.btnPlay);
        btnStop = findViewById(R.id.btnStop);
        btnPlay.setOnClickListener(new BtnEvent());
        btnStop.setOnClickListener(new BtnEvent());

        btnPlay.setEnabled(false);
        btnStop.setEnabled(false);
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //シークバーが操作されたら、その位置に MediaPlayer の再生箇所を移動する
                if(fromUser) {
                    Log.i("MainActivity", "progress = " + progress);
                    thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                while (mPlayer.isPlaying()) {
                                    //現在の曲の再生位置
                                    currentPosition = mPlayer.getCurrentPosition();
                                    Log.i("aaa", "aaa + " + currentPosition);
//                                //スレッドからメインスレッドに currentPosition を渡すために
//                                //Message クラスを使う
                                    Message msg = new Message();
                                    msg.what = currentPosition;
                                    //threadHandler で Message オブジェクトを渡す
                                    threadHandler.sendMessage(msg);
                                    Thread.sleep(100);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    mPlayer.seekTo(progress);
                    seekBar.setProgress(progress);
                    thread.start();
                    updateCurrentTime();
                    startUpdateTimeThread();
                    }
                }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSDList = findViewById(R.id.btnSDList);
        btnSDList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if(mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.seekTo(0);
                    seekBar.setProgress(0);
                    mPlayer.stop();
                    txtCurrentTime.setText("00:00");

                    playStart = true;
                    btnPlay.setImageResource(R.drawable.play);
                    setDefaultButtons();
                }

                Intent i = new Intent(MainActivity.this, SDListActivity.class);
//                startActivity(i);
                startActivityForResult(i, SEARCH_REQCD);
            }
        });

        timeHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case UPDATE_TIME:
                        updateCurrentTime(); // 현재 재생 시간 업데이트
                        break;
                }
                return true;
            }
        });
    }

    private void startUpdateTimeThread() {
        // 현재 재생 시간을 업데이트하는 스레드
        Thread updateTimeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (mPlayer.isPlaying()) {
                        // 현재 재생 시간을 메시지로 전달하여 핸들러에서 처리
                        Message msg = new Message();
                        msg.what = UPDATE_TIME;
                        timeHandler.sendMessage(msg);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        updateTimeThread.start();
    }

    private void updateCurrentTime() {
        // 현재 재생 시간 및 전체 재생 시간을 텍스트뷰에 표시
        currentTime = mPlayer.getCurrentPosition();
        int totalDuration = mPlayer.getDuration();

        String currentFormattedTime = formatTime(currentTime);
        String totalFormattedTime = formatTime(totalDuration);

        txtCurrentTime.setText(currentFormattedTime);
        txtTotalTime.setText(totalFormattedTime);
    }

    private String formatTime(int milliseconds) {
        // 밀리초 단위의 시간을 "mm:ss" 형식으로 변환하여 반환
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @SuppressLint("HandlerLeak")
    private Handler threadHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            Log.i("MainActivity", "msg.what = " + msg.what);
            seekBar.setProgress(msg.what);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SEARCH_REQCD && resultCode == RESULT_OK) {
            path = data.getStringExtra("SELECT_FILE");
            String[] pathArray = path.split("/");

            fileName.setText("NOW PLAYING : " + pathArray[pathArray.length - 1]);

            try{
                mPlayer = new MediaPlayer();
                //SDList で選択された曲のパスのデータを MediaPlayer にセットする
                mPlayer.setDataSource(MainActivity.this, Uri.parse(path));
                //CompletionListener の設定
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) { //曲の最後まで再生したら呼ばれる
                        Toast.makeText(MainActivity.this, "再生終了",Toast.LENGTH_SHORT).show();
                        playStart = true;
                        mPlayer.seekTo(0);
                        seekBar.setProgress(0);
                        mPlayer.stop();
                        setDefaultButtons();
                        txtCurrentTime.setText("00:00");
//                                setPlayingStateButtons();
                    }
                });
                mPlayer.prepare(); //MediaPlayer の準備(曲のロード)
            }catch (Exception e) {
                e.printStackTrace();
                Log.e("MainActivity", e.toString());
            }

            //再生中の曲の最大の長さを取得(msec)
            mTotalTime = mPlayer.getDuration();
            //シークバーの最大値を曲の長さにセット
            seekBar.setMax(mTotalTime);
            //再生中の曲の最大の長さを取得(msec)
            setDefaultButtons();
//            seekBar.setProgress(0);
            seekBar.setEnabled(true);
            mPlayer.seekTo(0);
            seekBar.setProgress(0);

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (mPlayer.isPlaying()) {
                            //現在の曲の再生位置
                            currentPosition = mPlayer.getCurrentPosition();
                            Log.i("aaa", "aaa + " + currentPosition);
//                                //スレッドからメインスレッドに currentPosition を渡すために
//                                //Message クラスを使う
                            Message msg = new Message();
                            msg.what = currentPosition;
                            //threadHandler で Message オブジェクトを渡す
                            threadHandler.sendMessage(msg);
                            Thread.sleep(100);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            startUpdateTimeThread();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length <= 0) {
            return;
        }
        switch (requestCode) {
            case EXTERNAL_STORAGE: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }else {
                    Toast.makeText(this, "アプリを起動できません", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
        return;
    }

    protected void onStop() {
        Log.i("aaa", "aaa " + "stop");
        super.onStop();
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        thread = null;
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        setDefaultButtons();
//    }

    class BtnEvent implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.btnPlay) {
                if(playStart) {
                    playStart = false;
                    btnPlay.setImageResource(R.drawable.pause);

//                    if(mPlayer == null) {
//                        Log.i("aaa", "aaa == null");
//                        setPlay();
//                    }else {
                        Log.i("aaa", "aaa != null");
                        mPlayer.start();
                        thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (mPlayer.isPlaying()) {
                                        //現在の曲の再生位置
                                        currentPosition = mPlayer.getCurrentPosition();
                                        Log.i("aaa", "aaa + " + currentPosition);
//                                //スレッドからメインスレッドに currentPosition を渡すために
//                                //Message クラスを使う
                                        Message msg = new Message();
                                        msg.what = currentPosition;
                                        //threadHandler で Message オブジェクトを渡す
                                        threadHandler.sendMessage(msg);
                                        Thread.sleep(100);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        thread.start();

                        btnPlay.setImageResource(R.drawable.pause);
                        setPlayingStateButtons();
                        startUpdateTimeThread();
//                    }
                }else {
                    if(mPlayer.isPlaying()) {
                        mPlayer.pause();
                        btnPlay.setImageResource(R.drawable.play);
                        updateCurrentTime();
                    }else {
                        mPlayer.start();
                        thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (mPlayer.isPlaying()) {
                                        //現在の曲の再生位置
                                        currentPosition = mPlayer.getCurrentPosition();
                                        Log.i("aaa", "aaa + " + currentPosition);
//                                //スレッドからメインスレッドに currentPosition を渡すために
//                                //Message クラスを使う
                                        Message msg = new Message();
                                        msg.what = currentPosition;
                                        //threadHandler で Message オブジェクトを渡す
                                        threadHandler.sendMessage(msg);
                                        Thread.sleep(100);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        thread.start();

                        btnPlay.setImageResource(R.drawable.pause);
                        startUpdateTimeThread();
                    }
                    setPlayingStateButtons();

                }


            }else if(view.getId() == R.id.btnStop) {
                playStart = true;
                btnPlay.setImageResource(R.drawable.play);
                mPlayer.seekTo(0);
                seekBar.setProgress(0);
                mPlayer.stop();

                setPlay();

                setDefaultButtons();
                txtCurrentTime.setText("00:00");
            }
        }
    }

    private void setDefaultButtons() {
        btnPlay.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void setPlayingStateButtons() {
        btnPlay.setEnabled(true);
        btnStop.setEnabled(true);
    }

    private void setPlay() {
                            try{
                        mPlayer = new MediaPlayer();
                        //SDList で選択された曲のパスのデータを MediaPlayer にセットする
                        mPlayer.setDataSource(MainActivity.this, Uri.parse(path));
                        //CompletionListener の設定
                        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) { //曲の最後まで再生したら呼ばれる
                                Toast.makeText(MainActivity.this, "再生終了",Toast.LENGTH_SHORT).show();
                                playStart = true;
                                mPlayer.seekTo(0);
                                seekBar.setProgress(0);
                                mPlayer.stop();
                                setDefaultButtons();
                                txtCurrentTime.setText("00:00");
//                                setPlayingStateButtons();
                            }
                        });
                        mPlayer.prepare(); //MediaPlayer の準備(曲のロード)
                    }catch (Exception e) {
                        e.printStackTrace();
                        Log.e("MainActivity", e.toString());
                    }

                    //再生中の曲の最大の長さを取得(msec)
                    mTotalTime = mPlayer.getDuration();
                    //シークバーの最大値を曲の長さにセット
                    seekBar.setMax(mTotalTime);
                    mPlayer.seekTo(0);
                    //再生
//                    mPlayer.start();
                    //再生中のボタンの有効・無効設定メソッドの呼び出し

//                    //再生中の曲の最大の長さを取得(msec)
//                    mTotalTime = mPlayer.getDuration();
//                    //シークバーの最大値を曲の長さにセット
//                    seekBar.setMax(mTotalTime);
                    //曲の長さに合わせてシークバーを動かす処理をするスレッドのを生成
                    thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                while (mPlayer.isPlaying()) {
                                    //現在の曲の再生位置
                                    currentPosition = mPlayer.getCurrentPosition();
                                    Log.i("aaa", "aaa + " + currentPosition);
//                                //スレッドからメインスレッドに currentPosition を渡すために
//                                //Message クラスを使う
                                    Message msg = new Message();
                                    msg.what = currentPosition;
                                    //threadHandler で Message オブジェクトを渡す
                                    threadHandler.sendMessage(msg);
                                    Thread.sleep(100);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                    startUpdateTimeThread();
    }
}