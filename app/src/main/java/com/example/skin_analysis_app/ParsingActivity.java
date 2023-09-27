package com.example.skin_analysis_app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// ParsingActivity: 이미지를 파싱하고 분석하는 Activity
public class ParsingActivity extends AppCompatActivity implements Runnable{

    // 클래스의 수를 상수로 선언
    private static final int N_CLASSES = 19;

    // 멤버 변수 선언
    private Module mModule = null;
    private ImageView mImageView;
    private String picturePath = null;
    private Bitmap mBitmap = null;

    private Button mButtonParsing;
    private ProgressBar mProgressBar;

    private Button mBackButton;

    // assetFilePath: Asset 폴더에서 파일 경로를 가져오는 함수
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

    // onCreate: Activity가 생성될 때 호출되는 메서드
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parsing);

        // 이미지 뷰를 참조
        mImageView = findViewById(R.id.imageView);

        // 인텐트에서 이미지 URI를 가져옴
        Intent intent = getIntent();
        String imageUri = intent.getStringExtra("image_uri");

        // 이미지 URI가 null이 아니면 CameraxActivity에서 온 것
        if (imageUri != null) {
            try {
                // Load image from URI
                InputStream inputStream = getContentResolver().openInputStream(Uri.parse(imageUri));
                mBitmap = BitmapFactory.decodeStream(inputStream);
                mBitmap = rotateImageIfRequired(mBitmap, Uri.parse(imageUri).getPath()); // Add this line
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // MainActivity에서 이미지 경로를 가져옴
            picturePath = intent.getStringExtra("IMAGE_PATH");
            mBitmap = BitmapFactory.decodeFile(picturePath);
            try {
                mBitmap = rotateImageIfRequired(mBitmap, picturePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 이미지를 이미지 뷰에 설정
        mImageView.setImageBitmap(mBitmap);

        // 파싱 버튼과 프로그레스 바를 참조
        mButtonParsing = findViewById(R.id.parsingButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        // 파싱 버튼에 클릭 리스너 설정
        mButtonParsing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // 버튼을 비활성화하고 프로그레스 바를 표시
                mButtonParsing.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mButtonParsing.setText(getString(R.string.run_model));

                // 새 스레드를 시작하여 이미지 파싱을 수행
                Thread thread = new Thread(ParsingActivity.this);
                thread.start();
            }
        });

        // PyTorch 모델(Pt)을 로드
        try {
            mModule = Module.load(ParsingActivity.assetFilePath(getApplicationContext(), "face.pt"));
        } catch (IOException e) {
            Log.e("ImageParsing", "Error reading assets", e);
            finish();
        }

        // 뒤로 가기 버튼을 참조하고 클릭 리스너를 설정
        mBackButton = findViewById(R.id.backButton);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // MainActivity로 명시적 인텐트를 사용하여 이동
                Intent intent = new Intent(ParsingActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 이 플래그를 사용하여 MainActivity를 새로 생성하지 않고 기존의 MainActivity로 돌아갑니다.
                startActivity(intent);
            }
        });

    }

    // run: 이미지를 처리하고 파싱하는 주요 함수
    @Override
    public void run() {
        // 이미지를 리사이즈 하는 부분
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, 512, 512, true);

        // 이미지를 전처리 하는 부분
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        final float[] inputs = inputTensor.getDataAsFloatArray();

        // 모델을 사용하여 예측하는 부분
        final long startTime = SystemClock.elapsedRealtime();
        IValue[] outputs = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputs[0].toTensor();
        Log.d("parsing", "Output tensor shape: " + java.util.Arrays.toString(outputTensor.shape()));

        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Log.d("parsing", "inference time (ms): " + inferenceTime);

        final float[] scores = outputTensor.getDataAsFloatArray();

        // 결과를 처리하고 화면에 표시하는 부분
        int width = 512;
        int height = 512;
        int[] intValues = new int[width * height];
        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                int maxClass = 0;
                double maxnum = -Double.MAX_VALUE;
                for (int i = 0; i < N_CLASSES; i++) {
                    float score = scores[(i * width * height) + (j * width) + k];
                    if (score > maxnum) {
                        maxnum = score;
                        maxClass = i;
                    }
                }
                intValues[j * width + k] = getColorForClass(maxClass);
            }
        }
        final Bitmap outBitmap = Bitmap.createBitmap(intValues, width, height, Bitmap.Config.ARGB_8888);

        // 원본 이미지 크기로 결과 이미지 스케일링
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(outBitmap, mBitmap.getWidth(), mBitmap.getHeight(), true);


        final Bitmap finalBitmap;
        if (scaledBitmap.getWidth() > scaledBitmap.getHeight()) {
            finalBitmap = rotateBitmap(scaledBitmap, ExifInterface.ORIENTATION_ROTATE_90);
        } else {
            finalBitmap = scaledBitmap;
        }


        runOnUiThread(() -> {
            mImageView.setImageBitmap(finalBitmap);
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mButtonParsing.setText(R.string.parsing);
            mButtonParsing.setEnabled(true);
        });
    }

    // getColorForClass: 클래스 인덱스에 따라 색상을 반환하는 함수
    private int getColorForClass(int classIndex) {
        // 예제 색상 코드
        int[][] colors = {{255, 0, 0}, {255, 85, 0}, {255, 170, 0}, {255, 0, 85}, {255, 0, 170}, {0, 255, 0}, {85, 255, 0}, {170, 255, 0}, {0, 255, 85}, {0, 255, 170}, {0, 0, 255}, {85, 0, 255}, {170, 0, 255}, {0, 85, 255}, {0, 170, 255}, {255, 255, 0}, {255, 255, 85}, {255, 255, 170}, {255, 0, 255}, {255, 85, 255}, {255, 170, 255}, {0, 255, 255}, {85, 255, 255}, {170, 255, 255}};
        int[] color = colors[classIndex % colors.length];
        return 0xFF000000 | (color[0] << 16) | (color[1] << 8) | color[2];
    }

    // rotateImageIfRequired: 필요한 경우 이미지를 회전시키는 함수
    private Bitmap rotateImageIfRequired(Bitmap img, String imagePath) throws IOException {
        ExifInterface ei = new ExifInterface(imagePath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        return rotateBitmap(img, orientation);
    }

    // rotateBitmap: 지정된 방향으로 비트맵을 회전시키는 함수
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
