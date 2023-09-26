package com.example.skin_analysis_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ParsingActivity extends AppCompatActivity {

    private ImageView mImageView;
    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parsing);

        mImageView = findViewById(R.id.imageView);

        // Intent를 통해 이미지 파일 경로 받아오기
        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("IMAGE_PATH");

        // 이미지 파일 경로를 사용하여 Bitmap 생성
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        // ImageView에 Bitmap 표시
        mImageView.setImageBitmap(bitmap);
    }
}
