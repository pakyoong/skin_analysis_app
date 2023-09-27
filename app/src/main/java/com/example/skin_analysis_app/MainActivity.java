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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    // 변수 선언: 앨범 버튼, 사진 경로, 사용자 이름 입력 필드, 확인 버튼, 이름 변경 버튼
    private Button mAlbumButton;
    private String picturePath = null;


    private Button mConfirmNameButton;
    private Button mChangeNameButton;

    private String userName;
    private EditText mUserNameEditText;


    // 사진을 선택하고 결과를 받아오는 함수의 결과 런처
    private ActivityResultLauncher<Intent> mStartForResult;

    // 액티비티 생성 시 호출되는 함수
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 각 버튼과 사용자 이름 입력 필드를 레이아웃에서 찾아 변수에 할당
        mConfirmNameButton = findViewById(R.id.confirmNameButton);
        mUserNameEditText = findViewById(R.id.userNameEditText);
        mChangeNameButton = findViewById(R.id.changeNameButton);

        // 확인 버튼 클릭 리스너 설정
        mConfirmNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 사용자 이름 입력 필드를 수정 불가능하게 설정
                mUserNameEditText.setFocusable(false);
                mUserNameEditText.setFocusableInTouchMode(false);
                mUserNameEditText.setClickable(false);

                // 확인 버튼을 숨기고 이름 변경 버튼을 보이게 함
                mConfirmNameButton.setVisibility(View.GONE);
                mChangeNameButton.setVisibility(View.VISIBLE);
            }
        });

        // 이름 변경 버튼 클릭 리스너 설정
        mChangeNameButton.setOnClickListener(new View.OnClickListener() { // 추가
            @Override
            public void onClick(View v) {
                // 사용자 이름 입력 필드를 다시 수정 가능하게 설정
                mUserNameEditText.setFocusable(true);
                mUserNameEditText.setFocusableInTouchMode(true);
                mUserNameEditText.setClickable(true);
                mUserNameEditText.requestFocus();

                // 이름 변경 버튼을 숨기고 확인 버튼을 보이게 함
                mChangeNameButton.setVisibility(View.GONE);
                mConfirmNameButton.setVisibility(View.VISIBLE);
            }
        });

        // 앨범 버튼 초기화
        mAlbumButton = findViewById(R.id.albumButton);

        // 앨범 버튼 클릭 시 실행될 코드 설정
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

        // 카메라 버튼 찾기 및 클릭 리스너 설정
        final Button buttonCamera = findViewById(R.id.cameraButton);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText userNameEditText = findViewById(R.id.userNameEditText);
                String userName = userNameEditText.getText().toString();

                // 카메라 액티비티로 이동하면서 사용자 이름 전달
                Intent intent = new Intent(MainActivity.this, CameraxActivity.class);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);

            }
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
