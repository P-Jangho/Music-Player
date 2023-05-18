package jp.ac.jec.cm0135.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mPlayer;
    private Button btnPlay;
    private Button btnPause;
    private Button btnStop;
    private Button btnSDList;
    private TextView fileName;
    String path;
    private final int SEARCH_REQCD = 123;
    private static final int EXTERNAL_STORAGE = 1;
    private SeekBar seekBar;
    private int mTotalTime;
    private int currentPosition;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnPlay.setOnClickListener(new BtnEvent());
        btnPause.setOnClickListener(new BtnEvent());
        btnStop.setOnClickListener(new BtnEvent());

        btnPlay.setEnabled(false);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //シークバーが操作されたら、その位置に MediaPlayer の再生箇所を移動する
                if(fromUser) {
                    Log.i("MainActivity", "progress = " + progress);
                    mPlayer.seekTo(progress);
                    seekBar.setProgress(progress);
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
                    mPlayer.stop();
                }

                Intent i = new Intent(MainActivity.this, SDListActivity.class);
//                startActivity(i);
                startActivityForResult(i, SEARCH_REQCD);
            }
        });
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

            fileName.setText(path);

            setDefaultButtons();
            seekBar.setProgress(0);
            seekBar.setEnabled(true);
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
                try{
                    mPlayer = new MediaPlayer();
                    //SDList で選択された曲のパスのデータを MediaPlayer にセットする
                    mPlayer.setDataSource(MainActivity.this, Uri.parse(path));
                    //CompletionListener の設定
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) { //曲の最後まで再生したら呼ばれる
                        Toast.makeText(MainActivity.this, "再生終了",Toast.LENGTH_SHORT).show();
                        mPlayer.seekTo(0);
                        seekBar.setProgress(0);
                        mPlayer.stop();
                        setDefaultButtons();
                        }
                    });
                    mPlayer.prepare(); //MediaPlayer の準備(曲のロード)
                }catch (Exception e) {
                    e.printStackTrace();
                    Log.e("MainActivity", e.toString());
                }
                mPlayer.seekTo(0);
                //再生
                mPlayer.start();
                //再生中のボタンの有効・無効設定メソッドの呼び出し
                setPlayingStateButtons();

                //再生中の曲の最大の長さを取得(msec)
                mTotalTime = mPlayer.getDuration();
                //シークバーの最大値を曲の長さにセット
                seekBar.setMax(mTotalTime);
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
            }else if(view.getId() == R.id.btnPause) {
                if(mPlayer.isPlaying()) {
                    mPlayer.pause();
                }else {
                    mPlayer.start();
                }
                setPlayingStateButtons();
            }else if(view.getId() == R.id.btnStop) {
//                mPlayer.seekTo(0);
                mPlayer.seekTo(0);
                seekBar.setProgress(0);
                mPlayer.stop();
                setDefaultButtons();
            }
        }
    }

    private void setDefaultButtons() {
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
    }

    private void setPlayingStateButtons() {
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
    }
}