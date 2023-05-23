package jp.ac.jec.cm0135.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class SDListActivity extends AppCompatActivity {

//    private ArrayAdapter<String> adapter;
    private RowModelAdapter adapter;
    private TextView currentPathTextView;
    private String currentPath;
    private Button moveUpBtn;
    private int count = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdlist);

        currentPathTextView = findViewById(R.id.currentPathTextView);

        moveUpBtn = findViewById(R.id.moveUpBtn);
        btnStatus();
//        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
//        adapter.add("ドラえもん音頭");
//        adapter.add("日本電子専門学校校歌");

        adapter = new RowModelAdapter(this);
//        adapter.add(new RowModel("aaa", 1111111L));

        File path = Environment.getExternalStorageDirectory();
        final File[] files = path.listFiles();
        if(files != null) {
            for (int i = 0; i < files.length ; i++) {
                adapter.add(new RowModel(files[i]));
            }
        }

        ListView list = findViewById(R.id.sdList);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                count += 1;
                btnStatus();
                ListView list = (ListView) parent;
                RowModel item = (RowModel) list.getItemAtPosition(position);
                Toast.makeText(SDListActivity.this, item.getFile().getAbsolutePath(), Toast.LENGTH_SHORT).show();

//                currentPathTextView.setText(item.getFile().getAbsolutePath());
                if (item.getFile().isDirectory()) {
                    currentPath = item.getFile().getAbsolutePath();
                    updateCurrentPathText();
                    updateAdapter(currentPath);
                } else {
                    currentPathTextView.setText(item.getFile().getAbsolutePath());

                    Intent intent = getIntent();
                    intent.putExtra("SELECT_FILE", item.getFile().getAbsolutePath());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        // 上位層へ移動するボタンのクリックリスナーを設定
        moveUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 上位層へ移動する
                count -= 1;
                btnStatus();
                File currentDir = new File(currentPath);
                File parentDir = currentDir.getParentFile();
                if (parentDir != null) {
                    currentPath = parentDir.getAbsolutePath();
                    updateAdapter(currentPath);
                    updateCurrentPathText();
                }
            }
        });
    }

    private void updateAdapter(String path) {
        adapter.clear();
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                adapter.add(new RowModel(file));
            }
        }
    }

    private void updateCurrentPathText() {
        currentPathTextView.setText(currentPath);
    }

    private void btnStatus() {
        if(count == 0) {
            moveUpBtn.setEnabled(false);
        }else {
            moveUpBtn.setEnabled(true);
        }
    }

    class RowModelAdapter extends ArrayAdapter<RowModel> {
        public RowModelAdapter(@NonNull Context context) {
            super(context, R.layout.row_item);
        }
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            RowModel item = getItem(position);
            Log.i("MusicPlayer", "RowModelAdapter getView position:" + position);
            if(convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.row_item, null);
            }

            if(item != null) {
                TextView txt1 = convertView.findViewById(R.id.txtListFileName);
                if(txt1 != null) {
                    String fileName = item.getFileName();
                    if (item.getFile().isDirectory()) {
                        fileName += "/";
                    }
                    txt1.setText(fileName);
                }
                TextView txt2 = convertView.findViewById(R.id.txtListFileSize);
                if(txt2 != null) {
                    txt2.setText(String.valueOf(item.getFileSize()));
                }

                // 파일인 경우 텍스트 색상을 파랑으로 변경
                if (!item.getFile().isDirectory()) {
                    txt1.setTextColor(Color.BLUE);
                    txt2.setTextColor(Color.BLUE);
                } else {
                    txt1.setTextColor(Color.BLACK);
                    txt2.setTextColor(Color.BLACK);
                }
            }
            return convertView;
        }
    }
}