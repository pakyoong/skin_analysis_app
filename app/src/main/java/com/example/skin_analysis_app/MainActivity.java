package com.example.skin_analysis_app;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {


    private Button mConfirmNameButton;
    private Button mChangeNameButton;
    private Button mAlbumButton;
    private String picturePath = null;
    private Bitmap mBitmap = null;

    // Asset 폴더에서 파일 경로를 가져오는 함수
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    // 사진을 선택하고 결과를 받아오는 함수
    private ActivityResultLauncher<Intent> mStartForResult;


    private String userName;
    private EditText mUserNameEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConfirmNameButton = findViewById(R.id.confirmNameButton);
        mUserNameEditText = findViewById(R.id.userNameEditText);
        mChangeNameButton = findViewById(R.id.changeNameButton);

        mConfirmNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EditText를 수정 불가능하게 설정
                mUserNameEditText.setFocusable(false);
                mUserNameEditText.setFocusableInTouchMode(false);
                mUserNameEditText.setClickable(false);

                // 선택적으로, 확인 버튼을 숨기고, 이름 변경 버튼을 보이게 함
                mConfirmNameButton.setVisibility(View.GONE);
                mChangeNameButton.setVisibility(View.VISIBLE);
            }
        });

        mChangeNameButton.setOnClickListener(new View.OnClickListener() { // 추가
            @Override
            public void onClick(View v) {
                // EditText를 다시 수정 가능하게 설정
                mUserNameEditText.setFocusable(true);
                mUserNameEditText.setFocusableInTouchMode(true);
                mUserNameEditText.setClickable(true);
                mUserNameEditText.requestFocus();

                // 이름 변경 버튼을 숨기고, 확인 버튼을 보이게 함
                mChangeNameButton.setVisibility(View.GONE);
                mConfirmNameButton.setVisibility(View.VISIBLE);
            }
        });

        mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                Uri selectedImage = data.getData();
                picturePath = getRealPathFromURI(selectedImage);
                mBitmap = BitmapFactory.decodeFile(picturePath);
                try {
                    mBitmap = rotateImageIfRequired(mBitmap, picturePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mImageView.setImageBitmap(mBitmap);
            }
        });

        mAlbumButton = findViewById(R.id.albumButton);
        mAlbumButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                mStartForResult.launch(intent);
            }
        });
        final Button buttonCamera = findViewById(R.id.cameraButton);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText userNameEditText = findViewById(R.id.userNameEditText);
                String userName = userNameEditText.getText().toString();

                Intent intent = new Intent(MainActivity.this, CameraxActivity.class);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);

            }
        });
    }

    // URI로부터 이미지의 절대 경로를 반환하는 함수
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    // 필요한 경우 이미지를 회전시키는 함수
    private Bitmap rotateImageIfRequired(Bitmap img, String imagePath) throws IOException {
        ExifInterface ei = new ExifInterface(imagePath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        return rotateBitmap(img, orientation);
    }

    // 지정된 방향으로 비트맵을 회전시키는 함수
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }


        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
