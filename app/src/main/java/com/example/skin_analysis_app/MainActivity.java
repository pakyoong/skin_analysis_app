package com.example.skin_analysis_app;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mAlbumButton;
    private Button mCameraButton;
    private String picturePath = null;

    // 사진을 선택하고 결과를 받아오는 함수
    private ActivityResultLauncher<Intent> mStartForResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 버튼 초기화
        mAlbumButton = findViewById(R.id.albumButton);
        mCameraButton = findViewById(R.id.cameraButton);

        // 갤러리에서 이미지를 선택하고 결과를 받아오기 위해 액티비티 결과 런처를 등록
        mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                Uri selectedImage = data.getData();
                picturePath = getRealPathFromURI(selectedImage);
                Intent parsingIntent = new Intent(MainActivity.this, ParsingActivity.class);
                parsingIntent.putExtra("IMAGE_PATH", picturePath);
                startActivity(parsingIntent);
            }
        });

        // 앨범 버튼에 클릭 리스너 설정
        mAlbumButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            mStartForResult.launch(intent);
        });

        // 카메라 버튼에 클릭 리스너 설정
        mCameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraxActivity.class);
            startActivity(intent);
        });
    }

    // URI에서 실제 경로를 가져오는 함수
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
}
