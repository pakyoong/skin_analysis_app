package com.example.skin_analysis_app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;


// ParsingActivity: 이미지를 파싱하고 분석하는 Activity
public class ParsingActivity extends AppCompatActivity {

    //<editor-fold desc="클래스 선언">
    // 클래스의 수를 상수로 선언
    private static final int N_CLASSES = 19;
    private static final int N_CLASSES2 = 2;
    private static final int TARGET_CLASS_INDEX =1;
    public static final String[] LABELS = {
            "background",
            "skin",
            "left_brow",
            "right_brow",
            "left_eye",
            "right_eye",
            "eye_glasses",
            "left_ear",
            "right_ear",
            "ear_ring",
            "nose",
            "mouth",
            "upper_lip",
            "lower_lip",
            "neck",
            "neck_lace",
            "cloth",
            "hair",
            "hat"
    };
    //</editor-fold>

    //<editor-fold desc="변수 선언">
    // 멤버 변수 선언
    private Module mModule = null;
    private Module mModule2 = null;
    private ImageView mImageView;
    private String picturePath = null;
    private Uri picturePathUri = null;
    private Bitmap mBitmap = null;
    private Bitmap finalBitmap = null;
    private Bitmap classBitmap = null;
    private Bitmap finalBitmap1 = null;
    private Bitmap classBitmap2 = null;
    private Bitmap newLeftBitmap = null;
    private Bitmap newRightBitmap = null;
    private Bitmap newLeftBitmap2 = null;
    private Bitmap newRightBitmap2 = null;
    private Bitmap unet_colorBitmap = null;
    private Bitmap unet_grayScaleBitmap =null;
    private Button mButtonParsing;
    private ProgressBar mProgressBar;
    private Button mBackButton;
    private Button mButtonRoI;
    private Button mUnetButton;

    //ROI 변수 선언

    public static boolean is_leye;
    public static boolean is_reye;
    public static boolean is_nose;
    public static boolean is_lear;
    public static boolean is_rear;
    public static boolean eye_error;

    int lear_ymin = -1;
    int lear_ymax = -1;
    int lear_xmin = -1;
    int lear_xmax = -1;
    int rear_ymin = -1;
    int rear_ymax = -1;
    int rear_xmin = -1;
    int rear_xmax = -1;
    int leye_ymin = -1;
    int leye_ymax = -1;
    int leye_xmin = -1;
    int leye_xmax = -1;
    int reye_ymin = -1;
    int reye_ymax = -1;
    int reye_xmin = -1;
    int reye_xmax = -1;
    int nose_ymin = -1;
    int nose_ymax = -1;
    int nose_xmin = -1;
    int nose_xmax = -1;
    int face_ymin = -1;
    int face_ymax = -1;
    int face_xmin = -1;
    int face_xmax = -1;
    int hair_ymin = -1;
    int hair_ymax = -1;
    int hair_xmin = -1;
    int hair_xmax = -1;
    int eye_ymin = -1;
    int eye_ymax = -1;
    int eye_xmin = -1;
    int eye_xmax = -1;
    int roi_ymin = -1;
    int roi_ymax = -1;
    int roi_xmin = -1;
    int roi_xmax = -1;
    Integer leye_w = -1;
    Integer leye_h = -1;
    Integer leye_x = -1;
    Integer leye_y = -1;
    Integer reye_w = -1;
    Integer reye_h = -1;
    Integer reye_x = -1;
    Integer reye_y = -1;
    Integer roi_w = -1;
    Integer roi_h = -1;
    //</editor-fold>

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

        OpenCVLoader.initDebug();

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
        }
        else {
            // MainActivity에서 이미지 경로를 가져옴
            picturePath = intent.getStringExtra("IMAGE_PATH");
            picturePathUri = Uri.parse(intent.getStringExtra("IMAGE_PATH"));
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


        // PyTorch 모델(Pt)을 로드
        try {
            mModule = Module.load(ParsingActivity.assetFilePath(getApplicationContext(), "face.pt"));
        } catch (IOException e) {
            Log.e("ImageParsing", "Error reading assets", e);
            finish();
        }

        // 파싱 버튼에 클릭 리스너 설정
        mButtonParsing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // 버튼을 비활성화하고 프로그레스 바를 표시
                mButtonParsing.setEnabled(false);
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                mButtonParsing.setText(getString(R.string.run_model));

                // ParsingRunnable 사용
                Thread parsingThread = new Thread(new ParsingRunnable(ParsingActivity.this));
                parsingThread.start();
            }
        });

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

        mButtonRoI = findViewById(R.id.roiButton);
        mButtonRoI.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                get_RoI(classBitmap);
                Log.d("RoI", "ROI(xmin, ymin, xmax, ymax): " + roi_xmin + " " + roi_ymin + " " + roi_xmax + " " + roi_ymax);

                roi_w = roi_xmax - roi_xmin;
                roi_h = roi_ymax - roi_ymin;
                Log.d("RoI", "ROI(xmin, ymin, width, height):" + roi_xmin + " " + roi_ymin + " " + roi_w + " " + roi_h);
                get_eyes(classBitmap);
                Log.d("RoI", "(" + leye_x + "," + leye_y + "," + leye_w + "," + leye_h + "), (" + reye_x + "," + reye_y + "," + reye_w + "," + reye_h + ")");

                // finalBitmap을 Mat 객체로 변환
                Mat imgMat = new Mat();
                Bitmap bmp32 = finalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, imgMat);

                Rect rect = new Rect(roi_xmin, roi_ymin, roi_xmax - roi_xmin, roi_ymax - roi_ymin);
                // OpenCV를 사용하여 사각형 그리기
                Imgproc.rectangle(imgMat, rect, new Scalar(255, 0, 0), 3);

                // Mat 객체를 Bitmap으로 변환하여 ImageView에 표시
                Utils.matToBitmap(imgMat, finalBitmap);
                mImageView.setImageBitmap(finalBitmap);
                if (is_leye && !eye_error) {
                    Imgproc.ellipse(imgMat, new org.opencv.core.Point(leye_x, leye_y), new Size(leye_w + 20, leye_h + 20), 0, 0, 360, new Scalar(255, 255, 255, 255), -1);
                }
                if (is_reye && !eye_error) {
                    Imgproc.ellipse(imgMat, new org.opencv.core.Point(reye_x, reye_y), new Size(reye_w + 20, reye_h + 20), 0, 0, 360, new Scalar(255, 255, 255, 255), -1);
                }

                Utils.matToBitmap(imgMat, finalBitmap);

                // mBitmap을 새 Bitmap 객체로 복사
                Bitmap newBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Bitmap newBitmap2 = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                // 새로운 Bitmap을 Mat 객체로 변환
                Mat newImgMat = new Mat();
                Utils.bitmapToMat(newBitmap, newImgMat);
                Mat newImgMat2 = new Mat();
                Utils.bitmapToMat(newBitmap2, newImgMat2);

                // 조건에 따라 타원을 그리기
                if (is_leye && !eye_error) {
                    Imgproc.ellipse(newImgMat, new org.opencv.core.Point(leye_x, leye_y), new Size(leye_w + 40, leye_h + 40), 0, 0, 360, new Scalar(255, 255, 255, 255), -1);
                }
                if (is_reye && !eye_error) {
                    Imgproc.ellipse(newImgMat, new org.opencv.core.Point(reye_x, reye_y), new Size(reye_w + 40, reye_h + 40), 0, 0, 360, new Scalar(255, 255, 255, 255), -1);
                }

                logMatSize(newImgMat, "newImgMat");  // newImgMat의 해상도 로그 출력
                saveMatToBinary(newImgMat, "newImgMat");

                int cropXmin = roi_xmin;
                int cropXmax = roi_xmax;
                int cropYmin = roi_ymin;
                int cropYmax = roi_ymax;
                int cropW = cropXmax - cropXmin;
                int cropH = cropYmax - cropYmin;

                Rect cropRect = new Rect(cropXmin, cropYmin, cropW, cropH);
                Mat cropNewImgMat = new Mat(newImgMat, cropRect);
                Mat cropNewImgMat2 = new Mat(newImgMat2, cropRect);
                saveMatToBinary(cropNewImgMat, "cropNewImgMat");
                logMatSize(cropNewImgMat, "cropNewImgMat");  // cropNewImgMat의 해상도 로그 출력

                // cropNewImgMat 크기와 반 크기 계산
                int width = cropNewImgMat.cols();
                int height = cropNewImgMat.rows();
                int halfWidth = width / 2;

                // 왼쪽 영역 정의
                Rect leftRect = new Rect(0, 0, halfWidth, height);
                Mat cropNewLeftMat = new Mat(cropNewImgMat, leftRect);
                Mat cropNewLeftMat2 = new Mat(cropNewImgMat2, leftRect);

                // 오른쪽 영역 정의
                Rect rightRect = new Rect(halfWidth, 0, halfWidth, height);
                Mat cropNewRightMat = new Mat(cropNewImgMat, rightRect);
                Mat cropNewRightMat2 = new Mat(cropNewImgMat2, rightRect);

                // letterboxImage 함수를 사용하여 이미지 크기 조정
                Mat newLeftMat = letterboxImage(cropNewLeftMat, new Size(640, 640));
                Mat newRightMat = letterboxImage(cropNewRightMat, new Size(640, 640));
                Mat newLeftMat2 = letterboxImage(cropNewLeftMat2, new Size(640, 640));
                Mat newRightMat2 = letterboxImage(cropNewRightMat2, new Size(640, 640));


                // 크롭된 Mat 객체를 다시 Bitmap으로 변환
                Bitmap cropNewImg = Bitmap.createBitmap(cropNewImgMat.cols(), cropNewImgMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropNewImgMat, cropNewImg);
                Bitmap cropNewImg2 = Bitmap.createBitmap(cropNewImgMat2.cols(), cropNewImgMat2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropNewImgMat2, cropNewImg2);

                // Mat 객체를 다시 Bitmap으로 변환
                newLeftBitmap = Bitmap.createBitmap(newLeftMat.cols(), newLeftMat.rows(), Bitmap.Config.ARGB_8888);
                newRightBitmap = Bitmap.createBitmap(newRightMat.cols(), newRightMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(newLeftMat, newLeftBitmap);
                Utils.matToBitmap(newRightMat, newRightBitmap);


                newLeftBitmap2 = Bitmap.createBitmap(newLeftMat2.cols(), newLeftMat2.rows(), Bitmap.Config.ARGB_8888);
                newRightBitmap2 = Bitmap.createBitmap(newRightMat2.cols(), newRightMat2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(newLeftMat2, newLeftBitmap2);
                Utils.matToBitmap(newRightMat2, newRightBitmap2);

                // 파일로 저장
                saveBitmapToBinary(newLeftBitmap, "newLeftBitmap");

                // ImageView에 새로운 Bitmap을 설정합니다.
                mImageView.setImageBitmap(newLeftBitmap2);
                // newLeftBitmap의 가로와 세로 길이를 얻기
                int bitwidth = newLeftBitmap.getWidth();
                int bitheight = newLeftBitmap.getHeight();

                // 로그 메시지로 크기 출력
                Log.d("BitmapSize", "newLeftBitmap: Width = " + bitwidth + ", Height = " + bitheight);
            }
        });

        // PyTorch 모델(Pt)을 로드
        try {
            mModule2 = Module.load(ParsingActivity.assetFilePath(getApplicationContext(), "unet_model2.pt"));
        } catch (IOException e) {
            Log.e("ImageParsing", "Error reading assets", e);
            finish();
        }


        mUnetButton = findViewById(R.id.UnetButton);
        mUnetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // UnetRunnable 사용
                Thread unetThread = new Thread(new UnetRunnable(ParsingActivity.this));
                unetThread.start();
            }
        });

    }
    // Mat 객체의 해상도를 로그로 출력하는 코드
    private void logMatSize(Mat mat, String matName) {
        int width = mat.cols();  // Mat 객체의 너비
        int height = mat.rows(); // Mat 객체의 높이

        // 로그 메시지 출력
        Log.d("MatSize", matName + " size: Width = " + width + ", Height = " + height);
    }
    // ParsingRunnable 클래스
    private class ParsingRunnable implements Runnable {
        private final WeakReference<ParsingActivity> activityReference;

        public ParsingRunnable(ParsingActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            ParsingActivity activity = activityReference.get();
            if (activity != null) {
                // 이미지를 리사이즈 하는 부분(createScaledBitmap)
                //        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, 512, 512, true);

                // 이미지를 리사이즈 하는 부분(OpenCV)
                Mat mat = new Mat();
                Utils.bitmapToMat(mBitmap, mat); // Bitmap을 Mat으로 변환

                Mat resizedMat = new Mat();
                Imgproc.resize(mat, resizedMat, new Size(512, 512), 0, 0, Imgproc.INTER_NEAREST); // INTER_NEAREST 사용

                Bitmap resizedBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(resizedMat, resizedBitmap); // 리사이즈된 Mat을 다시 Bitmap으로 변환


                // 이미지를 전처리 하는 부분
                final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
//                saveTensorToFile(inputTensor, "inputTensor");
                Log.d("TensorSize", "Tensor shape: " + Arrays.toString(inputTensor.shape()));

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
                int[] ColorValues = new int[width * height]; // 색상 값이 저장될 배열
                int[] classValues = new int[width * height]; // 클래스 번호가 저장될 배열

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
                        ColorValues[j * width + k] = getColorForClass(maxClass);
                        int grayValue = maxClass & 0xFF;  // 가정: maxClass는 0에서 255 사이의 값을 가집니다.
                        classValues[j * width + k] = 0xFF000000 | (grayValue << 16) | (grayValue << 8) | grayValue;
                    }
                }
                final Bitmap outBitmap = Bitmap.createBitmap(ColorValues, width, height, Bitmap.Config.ARGB_8888);
                final Bitmap out2Bitmap = Bitmap.createBitmap(classValues, width, height, Bitmap.Config.ARGB_8888);

//
//                        // Bitmap 파일 저장
//                        saveBitmapAsPNG(outBitmap, "outBitmap_512");
//                        saveBitmapAsPNG(out2Bitmap, "out2Bitmap_512");
//
//                        // 원본 이미지 크기로 결과 이미지 스케일링(createScaledBitmap)
//                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(outBitmap, mBitmap.getWidth(), mBitmap.getHeight(), true);
//                        Bitmap scaled2Bitmap = Bitmap.createScaledBitmap(out2Bitmap, mBitmap.getWidth(), mBitmap.getHeight(), true);

                // 원본 이미지 크기로 결과 이미지 스케일링
                Mat outMat = new Mat();
                Utils.bitmapToMat(outBitmap, outMat);
                Mat scaledOutMat = new Mat();
                Imgproc.resize(outMat, scaledOutMat, new Size(mBitmap.getWidth(), mBitmap.getHeight()), 0, 0, Imgproc.INTER_NEAREST);

                Bitmap scaledBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(scaledOutMat, scaledBitmap);


                Mat out2Mat = new Mat();
                Utils.bitmapToMat(out2Bitmap, out2Mat);
                Mat scaledOut2Mat = new Mat();
                Imgproc.resize(out2Mat, scaledOut2Mat, new Size(mBitmap.getWidth(), mBitmap.getHeight()), 0, 0, Imgproc.INTER_NEAREST);

                Bitmap scaled2Bitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(scaledOut2Mat, scaled2Bitmap);

                if (scaledBitmap.getWidth() > scaledBitmap.getHeight()) {
                    finalBitmap = rotateBitmap(scaledBitmap, ExifInterface.ORIENTATION_ROTATE_90);
                } else {
                    finalBitmap = scaledBitmap;
                }

                if (scaled2Bitmap.getWidth() > scaled2Bitmap.getHeight()) {
                    classBitmap = rotateBitmap(scaled2Bitmap, ExifInterface.ORIENTATION_ROTATE_90);
                } else {
                    classBitmap = scaled2Bitmap;
                }

//                        // classBitmap 에서 class 번호가 잘 출력 되는지 Pixels 값 테스트
//                        int[] pixels = new int[classBitmap.getWidth() * classBitmap.getHeight()];
//                        classBitmap.getPixels(pixels, 0, classBitmap.getWidth(), 0, 0, classBitmap.getWidth(), classBitmap.getHeight());
//
//                        for (int i = 0; i < pixels.length; i++) {
//                            int blue = pixels[i] & 0xFF; // 클래스 번호가 블루 채널에 저장될 것으로 예상
//                            if (blue != 0) {
//                                Log.d("PARSING_ACTIVITY", "Pixel " + i + ": Class Number = " + blue);
//                            }
//                        }

                runOnUiThread(() -> {
                    mImageView.setImageBitmap(finalBitmap);
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    mButtonParsing.setText(R.string.parsing);
                    mButtonParsing.setEnabled(true);
                });

                //        // Bitmap 파일 저장 Test
                //        saveBitmapAsPNG(finalBitmap, "finalBitmap");
                //        saveBitmapAsPNG(classBitmap, "classBitmap");
            }
        }
    }

    // UnetRunnable 클래스
    private class UnetRunnable implements Runnable {
        private final WeakReference<ParsingActivity> activityReference;

        private int classOnePixelCount = 0;
        public UnetRunnable(ParsingActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            ParsingActivity activity = activityReference.get();
            if (activity != null) {

                // 첫 번째 이미지 처리
                final Tensor inputTensor1 = TensorImageUtils.bitmapToFloat32Tensor(newLeftBitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

                final Tensor outputTensor1 = mModule2.forward(IValue.from(inputTensor1)).toTensor();
                Log.d("TensorSize", "Tensor shape: " + Arrays.toString(outputTensor1.shape()));


//                Bitmap tensorBitmap1 = tensorToBitmap(inputTensor1);
//                saveBitmapAsPNG(tensorBitmap1, "tensorBitmap1");
//                Log.d("TensorSize", "Tensor shape: " + Arrays.toString(inputTensor1.shape()));

                // 두 번째 이미지 처리
                final Tensor inputTensor2 = TensorImageUtils.bitmapToFloat32Tensor(newRightBitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

                final Tensor outputTensor2 = mModule2.forward(IValue.from(inputTensor2)).toTensor();
                Log.d("TensorSize", "Tensor shape: " + Arrays.toString(outputTensor2.shape()));

//                Bitmap tensorBitmap2 = tensorToBitmap(inputTensor2);
//                saveBitmapAsPNG(tensorBitmap2, "tensorBitmap2");
//                Log.d("TensorSize", "Tensor shape: " + Arrays.toString(inputTensor2.shape()));

                // 모델 출력을 사용하여 softmax 점수 계산
                double[][][] unet_score1 = calculateScores(outputTensor1, newLeftBitmap.getWidth(), newLeftBitmap.getHeight());
                double[][][] unet_score2 = calculateScores(outputTensor2, newRightBitmap.getWidth(), newRightBitmap.getHeight());

                // 확률을 파일로 저장
                saveProbabilitiesToFile(unet_score1, "unet_score1");
                saveProbabilitiesToFile(unet_score2, "unet_score2");

                int[] leftEyeClassOnePixelCount = new int[1];
                int[] rightEyeClassOnePixelCount = new int[1];
                int highlightColor = Color.BLUE; // 클래스 1을 강조할 색상

                // 세그먼테이션 이미지 생성
//                Bitmap segmentationBitmap1 = createSegmentationImage(outputTensor1, newLeftBitmap.getWidth(), newLeftBitmap.getHeight(), leftEyeClassOnePixelCount);
//                Bitmap segmentationBitmap2 = createSegmentationImage(outputTensor2, newRightBitmap.getWidth(), newRightBitmap.getHeight(), rightEyeClassOnePixelCount);
                Bitmap segmentationBitmap1 = createSegmentationImage2(outputTensor1, newLeftBitmap.getWidth(), newLeftBitmap.getHeight(), leftEyeClassOnePixelCount, highlightColor);
                Bitmap segmentationBitmap2 = createSegmentationImage2(outputTensor2, newRightBitmap.getWidth(), newRightBitmap.getHeight(), rightEyeClassOnePixelCount, highlightColor);

                // 세그먼테이션 이미지 저장
                saveBitmapAsPNG(segmentationBitmap1, "segmentation1");
                saveBitmapAsPNG(segmentationBitmap2, "segmentation2");
//
//
                Bitmap overlayBitmap = applyOverlayOnImage(newLeftBitmap2, segmentationBitmap1);
//                Bitmap overlayBitmap2 = applyOverlayOnImage(newRightBitmap2, segmentationBitmap1);
//                saveBitmapToBinary(overlayBitmap1, "overlayBitmap1");

                // 결과 이미지 화면에 표시
                runOnUiThread(() -> {
//                    mImageView.setImageBitmap(segmentationBitmap1);
                    mImageView.setImageBitmap(overlayBitmap);
//                    mImageView.setImageBitmap(overlayBitmap2);
                    TextView textView2 = activity.findViewById(R.id.textView2);
                    String pixelCountText = "좌측 눈가 주름 픽셀 수: " + leftEyeClassOnePixelCount[0] +
                            "\n우측 눈가 주름 픽셀 수: " + rightEyeClassOnePixelCount[0];
                    textView2.setText(pixelCountText);

                    // mImageView2.setImageBitmap(overlayBitmap2); // 다른 이미지 뷰에 두 번째 이미지 결과 표시
                });
            }
        }
        private double[][][] calculateScores(Tensor outputTensor, int width, int height) {
            final float[] scores = outputTensor.getDataAsFloatArray();
            double[][][] classScores = new double[height][width][N_CLASSES2];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int c = 0; c < N_CLASSES2; c++) {
                        float score = scores[(c * width * height) + (y * width) + x];
                        classScores[y][x][c] = score;
                    }
                }
            }
            return classScores;
        }

        // 모델 출력을 사용하여 세그먼테이션 이미지 생성하는 함수
        private Bitmap createSegmentationImage(Tensor outputTensor, int width, int height, int[] classOnePixelCount) {
            final float[] scores = outputTensor.getDataAsFloatArray();
            int[] ColorValues = new int[width * height]; // 색상 값이 저장될 배열
            int[] classValues = new int[width * height]; // 클래스 번호가 저장될 배열

            classOnePixelCount[0] = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int maxClassIndex = 0;
                    float maxnum = -Float.MAX_VALUE;
                    // 각 클래스에 대한 확률을 비교하여 최대 확률을 가진 클래스 선택
                    for (int c = 0; c < N_CLASSES2; c++) {
                        float score = scores[(c * width * height) + (y * width) + x];
                        if (score > maxnum) {
                            maxnum = score;
                            maxClassIndex = c;
                        }
                    }
                    // 클래스 1인 픽셀의 수를 세는 로직 추가
                    if (maxClassIndex == 1) {
                        classOnePixelCount[0]++;
                    }
                    ColorValues[y * width + x] = getColorForClass2(maxClassIndex);
                    int grayValue = maxClassIndex & 0xFF;
                    classValues[y * width + x] = 0xFF000000 | (grayValue << 16) | (grayValue << 8) | grayValue;
                }
            }
            Log.d("ClassOnePixelCount", "Number of pixels in class 1: " + classOnePixelCount);
            unet_colorBitmap = Bitmap.createBitmap(ColorValues, width, height, Bitmap.Config.ARGB_8888);
            unet_grayScaleBitmap = Bitmap.createBitmap(classValues, width, height, Bitmap.Config.ARGB_8888);
            final Bitmap segmentationBitmap =unet_colorBitmap;
            return segmentationBitmap;
        }

        // 모델 출력을 사용하여 세그먼테이션 이미지 생성하는 함수 (수정됨)
        private Bitmap createSegmentationImage2(Tensor outputTensor, int width, int height, int[] classOnePixelCount, int highlightColor) {
            final float[] scores = outputTensor.getDataAsFloatArray();
            int[] segmentationValues = new int[width * height]; // 세그먼테이션 값이 저장될 배열

            classOnePixelCount[0] = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int maxClassIndex = 0;
                    float maxnum = -Float.MAX_VALUE;
                    for (int c = 0; c < N_CLASSES2; c++) {
                        float score = scores[(c * width * height) + (y * width) + x];
                        if (score > maxnum) {
                            maxnum = score;
                            maxClassIndex = c;
                        }
                    }
                    // 클래스 1인 픽셀의 수를 세는 로직 추가
                    if (maxClassIndex == 1) {
                        classOnePixelCount[0]++;
                        segmentationValues[y * width + x] = highlightColor; // 클래스 1에 해당하는 픽셀을 특정 색상으로 표시
                    } else {
                        segmentationValues[y * width + x] = Color.TRANSPARENT; // 그 외는 투명 처리
                    }
                }
            }

            return Bitmap.createBitmap(segmentationValues, width, height, Bitmap.Config.ARGB_8888);
        }


    }
        private int getColorForClass2(int classIndex) {
        // 클래스 인덱스에 따라 그레이스케일 값 반환
        // 예: 클래스 0은 검은색, 클래스 1은 흰색
        if (classIndex == 0) {
            return Color.BLACK; // 검은색
        } else if (classIndex == 1) {
            return Color.WHITE; // 흰색
        } else {
            return Color.GRAY; // 기타 클래스는 회색
        }
    }

    // 정규화된 텐서를 원본 이미지 값으로 변환하는 함수
    public Bitmap tensorToBitmap(Tensor tensor) {
        // 텐서의 데이터를 float 배열로 가져옵니다.
        float[] normalizedTensor = tensor.getDataAsFloatArray();
        int height = (int) tensor.shape()[2];
        int width = (int) tensor.shape()[3];

        // PyTorch의 TensorImageUtils에서 사용된 평균 및 표준편차 값을 정의합니다.
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};

        // RGB 채널 별로 denormalize합니다.
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelIndex = y * width + x;
                int rIndex = pixelIndex;
                int gIndex = pixelIndex + width * height;
                int bIndex = pixelIndex + 2 * width * height;

                // 픽셀 값 denormalize
                int r = (int) ((normalizedTensor[rIndex] * std[0] + mean[0]) * 255);
                int g = (int) ((normalizedTensor[gIndex] * std[1] + mean[1]) * 255);
                int b = (int) ((normalizedTensor[bIndex] * std[2] + mean[2]) * 255);

                // 픽셀 값 범위 확인
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                // ARGB 값 생성
                pixels[pixelIndex] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    // 세그먼테이션 이미지를 원본 이미지에 오버레이하는 함수
    private Bitmap applyOverlayOnImage(Bitmap originalBitmap, Bitmap segmentationBitmap) {
        Bitmap overlayBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlayBitmap);
        canvas.drawBitmap(originalBitmap, 0, 0, null);
        canvas.drawBitmap(segmentationBitmap, 0, 0, null); // 여기서 segmentationBitmap은 클래스 1에 해당하는 영역을 강조한 이미지
        return overlayBitmap;
    }

    // 정규화된 텐서를 원본 이미지 값으로 변환하는 함수
    public Bitmap denormalizeTensor(Tensor tensor, float[] mean, float[] std) {
        float[] normalizedTensor = tensor.getDataAsFloatArray();
        int height = (int) tensor.shape()[2];
        int width = (int) tensor.shape()[3];

        // RGB 채널 별로 denormalize
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelIndex = y * width + x;
                int rIndex = pixelIndex;
                int gIndex = pixelIndex + width * height;
                int bIndex = pixelIndex + 2 * width * height;

                // 픽셀 값 denormalize
                int r = (int) ((normalizedTensor[rIndex] * std[0] + mean[0]) * 255);
                int g = (int) ((normalizedTensor[gIndex] * std[1] + mean[1]) * 255);
                int b = (int) ((normalizedTensor[bIndex] * std[2] + mean[2]) * 255);

                // 픽셀 값 범위 확인
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                // ARGB 값 생성
                pixels[pixelIndex] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
    private void saveBitmapToBinary(Bitmap bitmap, String filename) {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(storageDir, filename + ".bin");

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);
                    bos.write(pixelToByteArray(pixel));
                }
            }

            bos.flush();

            Log.d("SaveBitmapToBinary", "File saved successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SaveBitmapToBinary", "Error saving file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void saveProbabilitiesToFile(double[][][] softmaxScores, String fileName) {
        try {
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(storageDir, fileName + ".txt");

            // 파일 작성을 위한 FileWriter 생성
            FileWriter writer = new FileWriter(file);

            // softmax 점수를 파일에 작성
            for (int y = 0; y < softmaxScores.length; y++) {
                for (int x = 0; x < softmaxScores[0].length; x++) {
                    String line = y + "," + x;
                    for (int c = 0; c < softmaxScores[0][0].length; c++) {
                        line += "," + softmaxScores[y][x][c];
                    }
                    writer.write(line + "\n");
                }
            }
            Log.d("SaveBitmapToBinary", "File saved successfully: " + file.getAbsolutePath());
            // 파일 닫기
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FileSaveError", "Error in saving file: " + e.getMessage());
        }
    }

    private byte[] pixelToByteArray(int pixel) {
        byte[] byteArray = new byte[3]; // 알파 채널을 제외한 3바이트 배열
        byteArray[0] = (byte) ((pixel >> 16) & 0xFF); // red
        byteArray[1] = (byte) ((pixel >> 8) & 0xFF);  // green
        byteArray[2] = (byte) (pixel & 0xFF);         // blue
        return byteArray;
    }

    private void saveMatToBinary(Mat mat, String filename) {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(storageDir, filename + ".bin");

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            int width = mat.cols();
            int height = mat.rows();
            int type = mat.type();

            // 각 픽셀을 바이트 배열로 변환
            byte[] buffer = new byte[width * height * CvType.channels(type)];
            mat.get(0, 0, buffer);

            // 파일에 바이트 배열 기록
            bos.write(buffer);
            bos.flush();

            Log.d("SaveMatToBinary", "File saved successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SaveMatToBinary", "Error saving file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void saveTensorToFile(Tensor tensor, String filename) {
        // 텐서 데이터를 float 배열로 가져오기
        float[] data = tensor.getDataAsFloatArray();

        // 로그 출력
        Log.d("TensorDataLength", "Data length: " + data.length);

        // 저장할 디렉토리 설정
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(storageDir, filename + ".bin");

        // 파일 스트림 생성 및 데이터 기록
        try (FileOutputStream fos = new FileOutputStream(file);
             DataOutputStream dos = new DataOutputStream(fos)) {
            // 배열 데이터 기록
            for (float f : data) {
                dos.writeFloat(f);
            }
            // 로그 메시지 추가
            Log.d("SaveTensor", "File saved successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            // 파일 저장 실패 시 로그 메시지 추가
            Log.e("SaveTensor", "Error saving file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkSavedTensorFile(String filename) {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(storageDir, filename + ".bin");

        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {

            ArrayList<Float> data = new ArrayList<>();
            while (dis.available() > 0) {
                data.add(dis.readFloat());
            }

            Log.d("CheckSavedTensorFile", "Read data length: " + data.size());
        } catch (IOException e) {
            Log.e("CheckSavedTensorFile", "Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Bitmap 파일 저장 함수
    private void saveBitmapAsPNG(Bitmap bitmap, String filename) {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String originalFileName = new File(picturePath).getName().replaceFirst("[.][^.]+$", "");
        File imageFile = new File(storageDir, originalFileName + "_" + filename + ".png");

        try (FileOutputStream outStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            // 성공 로그 추가
            Log.d("SaveBitmap", "Bitmap saved successfully: " + imageFile.getAbsolutePath());
        } catch (IOException e) {
            // 실패 로그 추가
            Log.e("SaveBitmap", "Error saving bitmap: " + e.getMessage());
            e.printStackTrace();
        }
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

    public class Point {
        public int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private int h;
    private int w;

    private List<Point> where(int[] pixels, int value) {
        List<Point> points = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixels[y * w + x] & 0xFF;  // Extracting the blue channel
                if (pixel == value) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

    private List<Point> whereMultiple(int[] pixels, int... values) {
        List<Point> points = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixels[y * w + x] & 0xFF;  // Extracting the blue channel
                for (int value : values) {
                    if (pixel == value) {
                        points.add(new Point(x, y));
                        break;
                    }
                }
            }
        }
        return points;
    }


    private int min(List<Point> points, char axis) {
        return points.stream().mapToInt(p -> (axis == 'x') ? p.x : p.y).min().orElse(0);
    }

    private int max(List<Point> points, char axis) {
        return points.stream().mapToInt(p -> (axis == 'x') ? p.x : p.y).max().orElse(0);
    }


    private int[] get_RoI(Bitmap bitmap) {

        eye_error = false;
        int half_w = mBitmap.getWidth() / 2;
        int ymax = finalBitmap.getHeight() - 1;
        int xmax = finalBitmap.getWidth() - 1;
        w = bitmap.getWidth();
        h = bitmap.getHeight();


        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        List<Point> face = where(pixels, 1);
//        // 픽셀 값들을 출력
//        for (int i = 0; i < pixels.length; i++) {
//            int classNumber = pixels[i] & 0xFF; // blue 채널에서 클래스 번호 추출
//            if (classNumber != 0) {
//                Log.d("PARSING_ACTIVITY", "Pixel " + i + ": Class Number = " + classNumber);
//            }
//        }

        if (!face.isEmpty()) {
            face_ymin = min(face, 'y');
            face_ymax = max(face, 'y');
            face_xmin = min(face, 'x');
            face_xmax = max(face, 'x');
        } else {
            Log.d("MyApp", "No face detected!");
        }

        List<Point> hair = where(pixels, 17);

        if (!hair.isEmpty()) {
            hair_ymin = min(hair, 'y');
            hair_ymax = max(hair, 'y');
            hair_xmin = min(hair, 'x');
            hair_xmax = max(hair, 'x');
        }

        List<Point> eyes = whereMultiple(pixels, 4, 5);
        List<Point> leye = where(pixels, 4);
        List<Point> reye = where(pixels, 5);
        List<Point> nose = where(pixels, 10);
        List<Point> lear = where(pixels, 7);
        List<Point> rear = where(pixels, 8);

        is_leye = false;
        is_reye = false;

        // For the left eye
        if (!leye.isEmpty()) {
            Log.d("MyApp", "Left eye appeared.");
            is_leye = true;
            leye_ymin = min(leye, 'y');
            leye_ymax = max(leye, 'y');
            leye_xmin = min(leye, 'x');
            leye_xmax = max(leye, 'x');
            if (leye_xmin < half_w && leye_xmax > half_w) {
                eye_error = true;
                Log.d("MyApp", "Eye Detection Error!");
            }
            Log.d("MyApp", "Left eye: " + leye_xmin + ", " + leye_ymin + ", " + leye_xmax + ", " + leye_ymax);
        }

        // For the right eye
        if (!reye.isEmpty()) {
            Log.d("MyApp", "Right eye appeared.");
            is_reye = true;
            reye_ymin = min(reye, 'y');
            reye_ymax = max(reye, 'y');
            reye_xmin = min(reye, 'x');
            reye_xmax = max(reye, 'x');
            if (reye_xmin < half_w && reye_xmax > half_w) {
                eye_error = true;
                Log.d("MyApp", "Eye Detection Error!");
            }
            Log.d("MyApp", "Right eye: " + reye_xmin + ", " + reye_ymin + ", " + reye_xmax + ", " + reye_ymax);
        }

        // For the nose
        if (!nose.isEmpty()) {
            Log.d("MyApp", "Nose appeared.");
            is_nose = true;
            nose_ymin = min(nose, 'y');
            nose_ymax = max(nose, 'y');
            nose_xmin = min(nose, 'x');
            nose_xmax = max(nose, 'x');
        } else {
            is_nose = false;
        }

        is_lear = false;
        is_rear = false;

        // For the left ear
        if (!lear.isEmpty()) {
            Log.d("MyApp", "Left ear appeared: " + min(lear, 'x') + ", " + max(lear, 'x') + ", " + min(lear, 'y') + ", " + max(lear, 'y'));

            if (min(lear, 'x') < mBitmap.getWidth() / 2) {
                if (max(lear, 'x') > mBitmap.getWidth() / 2) {
                    Log.d("MyApp", "Left ear error!");
                    is_lear = false;
                } else {
                    Log.d("MyApp", "Changed to right ear.");
                    is_rear = true;
                    rear_ymin = min(lear, 'y');
                    rear_ymax = max(lear, 'y');
                    rear_xmin = min(lear, 'x');
                    rear_xmax = max(lear, 'x');
                    Log.d("MyApp", "rear: " + rear_xmin + ", " + rear_xmax + ", " + rear_ymin + ", " + rear_ymax);
                }
            } else {
                is_lear = true;
                lear_ymin = min(lear, 'y');
                lear_ymax = max(lear, 'y');
                lear_xmin = min(lear, 'x');
                lear_xmax = max(lear, 'x');
                Log.d("MyApp", "lear: " + lear_xmin + ", " + lear_xmax + ", " + lear_ymin + ", " + lear_ymax);
            }
        }

        // For the right ear
        if (!rear.isEmpty()) {
            Log.d("MyApp", "Right ear appeared: " + min(rear, 'x') + ", " + max(rear, 'x') + ", " + min(rear, 'y') + ", " + max(rear, 'y'));

            if (max(rear, 'x') > mBitmap.getWidth() / 2) {
                if (min(rear, 'x') < mBitmap.getWidth() / 2) {
                    Log.d("MyApp", "Right ear error!");
                    is_rear = false;
                } else {
                    Log.d("MyApp", "Changed to left ear.");
                    is_lear = true;
                    lear_ymin = min(rear, 'y');
                    lear_ymax = max(rear, 'y');
                    lear_xmin = min(rear, 'x');
                    lear_xmax = max(rear, 'x');
                    Log.d("MyApp", "lear: " + lear_xmin + ", " + lear_xmax + ", " + lear_ymin + ", " + lear_ymax);
                }
            } else {
                is_rear = true;
                rear_ymin = min(rear, 'y');
                rear_ymax = max(rear, 'y');
                rear_xmin = min(rear, 'x');
                rear_xmax = max(rear, 'x');
                Log.d("MyApp", "rear: " + rear_xmin + ", " + rear_xmax + ", " + rear_ymin + ", " + rear_ymax);
            }
        }

        if (is_lear && is_rear && lear_xmin < rear_xmin) {
            Log.d("MyApp", "Left and right ear exchanged.");
            int temp;

            temp = lear_ymin;
            lear_ymin = rear_ymin;
            rear_ymin = temp;

            temp = lear_ymax;
            lear_ymax = rear_ymax;
            rear_ymax = temp;

            temp = lear_xmin;
            lear_xmin = rear_xmin;
            rear_xmin = temp;

            temp = lear_xmax;
            lear_xmax = rear_xmax;
            rear_xmax = temp;
        }

        if (is_reye && is_leye) {
            eye_ymin = min(eyes, 'y');
            eye_ymax = max(eyes, 'y');
            eye_xmin = min(eyes, 'x');
            eye_xmax = max(eyes, 'x');
            if (is_nose) {
                roi_ymin = Math.min(eye_ymin, nose_ymin);
                roi_ymax = (eye_ymax + nose_ymax) / 2;
            } else {
                roi_ymin = eye_ymin;
                roi_ymax = Math.min(eye_ymax + 2 * Math.max(reye_ymax - reye_ymin, leye_ymax - leye_ymin), face_ymax);
            }
            if (is_rear) {
                roi_xmin = rear_xmax;
            } else {
                roi_xmin = face_xmin;
            }
            if (is_lear) {
                roi_xmax = lear_xmin;
            } else {
                roi_xmax = Math.min(eye_xmax + 2 * (eye_ymax - eye_ymin), face_xmax);
            }
        } else if (is_reye && !is_leye) {
            if (is_nose) {
                roi_ymin = Math.min(reye_ymin, nose_ymin);
                roi_ymax = (reye_ymax + nose_ymax) / 2;
            } else {
                roi_ymin = reye_ymin;
                roi_ymax = Math.min(reye_ymax + 2 * (reye_ymax - reye_ymin), face_ymax);
            }

            if (is_rear) {
                roi_xmin = rear_xmax;
            } else {
                roi_xmin = face_xmin;
            }
            if (is_lear) {
                roi_xmax = lear_xmin;
            } else {
                roi_xmax = face_xmax;
            }
        } else if (!is_reye && is_leye) {
            if (is_nose) {
                roi_ymin = Math.min(leye_ymin, nose_ymin);
                roi_ymax = (leye_ymax + nose_ymax) / 2;
            } else {
                roi_ymin = leye_ymin;
                roi_ymax = Math.min(reye_ymax + 2 * (leye_ymax - leye_ymin), face_ymax);
            }
            if (is_rear) {
                roi_xmin = rear_xmax;
            } else {
                roi_xmin = face_xmin;
            }
            if (is_lear) {
                roi_xmax = lear_xmin;
            } else {
                roi_xmax = face_xmax;
            }
        } else {
            if (is_nose) {
                if (is_lear && is_rear) {
                    roi_ymin = Math.min(nose_ymin, Math.min(lear_ymin, rear_ymin));
                    roi_ymax = Math.min(nose_ymax, Math.min(lear_ymax, rear_ymax));
                    roi_xmin = rear_xmax;
                    roi_xmax = lear_xmin;
                } else if (is_rear) {
                    roi_ymin = Math.min(nose_ymin, rear_ymin);
                    roi_ymax = Math.min(nose_ymax, rear_ymax);
                    roi_xmin = rear_xmax;
                    roi_xmax = face_xmax;
                } else if (is_lear) {
                    roi_ymin = Math.min(nose_ymin, lear_ymin);
                    roi_ymax = Math.min(nose_ymax, lear_ymax);
                    roi_xmin = face_xmin;
                    roi_xmax = lear_xmin;
                } else {
                    roi_ymin = Math.min(nose_ymin, (hair_ymin + face_ymax) / 2);
                    roi_ymax = (2 * nose_ymin + 3 * nose_ymax) / 5;
                    roi_xmin = face_xmin;
                    roi_xmax = face_xmax;
                }
            } else { // no nose
                if (is_lear && is_rear) {
                    roi_ymin = Math.min(lear_ymin, rear_ymin);
                    roi_ymax = Math.min(lear_ymax, rear_ymax);
                    roi_xmin = rear_xmax;
                    roi_xmax = lear_xmin;
                } else if (is_rear) {
                    roi_ymin = rear_ymin;
                    roi_ymax = rear_ymax;
                    roi_xmin = rear_xmax;
                    roi_xmax = face_xmax;
                } else if (is_lear) {
                    roi_ymin = lear_ymin;
                    roi_ymax = lear_ymax;
                    roi_xmin = face_xmin;
                    roi_xmax = lear_xmin;
                } else {
                    return new int[]{face_xmin, face_ymin, face_xmax, (face_ymin + face_ymax) / 2};
                }
            }
        }
        return new int[]{roi_xmin, roi_ymin, roi_xmax, roi_ymax};
    }

    private int[][] get_eyes(Bitmap bitmap) {

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        List<Point> leye = where(pixels, 4);
        List<Point> reye = where(pixels, 5);

        if (!leye.isEmpty()) {
            leye_ymin = min(leye, 'y');
            leye_ymax = max(leye, 'y');
            leye_xmin = min(leye, 'x');
            leye_xmax = max(leye, 'x');
            leye_w = (leye_xmax - leye_xmin) / 2;
            leye_h = (leye_ymax - leye_ymin) / 2;
            leye_x = leye_xmin + leye_w;
            leye_y = leye_ymin + leye_h;
        } else {
            leye_w = null;
            leye_h = null;
            leye_x = null;
            leye_y = null;
        }

        if (!reye.isEmpty()) {
            reye_ymin = min(reye, 'y');
            reye_ymax = max(reye, 'y');
            reye_xmin = min(reye, 'x');
            reye_xmax = max(reye, 'x');
            reye_w = (reye_xmax - reye_xmin) / 2;
            reye_h = (reye_ymax - reye_ymin) / 2;
            reye_x = reye_xmin + reye_w;
            reye_y = reye_ymin + reye_h;
        } else {
            reye_w = null;
            reye_h = null;
            reye_x = null;
            reye_y = null;
        }
        int[][] result = new int[2][4];

        if (leye_x != null && leye_y != null && leye_w != null && leye_h != null) {
            result[0][0] = leye_x;
            result[0][1] = leye_y;
            result[0][2] = leye_w;
            result[0][3] = leye_h;
        } else {
            result[0][0] = -1;
            result[0][1] = -1;
            result[0][2] = -1;
            result[0][3] = -1;
        }

        if (reye_x != null && reye_y != null && reye_w != null && reye_h != null) {
            result[1][0] = reye_x;
            result[1][1] = reye_y;
            result[1][2] = reye_w;
            result[1][3] = reye_h;
        } else {
            result[1][0] = -1;
            result[1][1] = -1;
            result[1][2] = -1;
            result[1][3] = -1;
        }

        return result;
    }

    public Mat letterboxImage(Mat image, Size expectedSize) {
        int ih = image.rows();
        int iw = image.cols();
        int eh = (int) expectedSize.height;
        int ew = (int) expectedSize.width;

        double scale = Math.min((double) eh / ih, (double) ew / iw);
        int nh = (int) (ih * scale);
        int nw = (int) (iw * scale);

        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(nw, nh), 0, 0, Imgproc.INTER_NEAREST);

        // 변경된 부분: 모든 픽셀을 불투명한 검은색으로 초기화
        Mat newImage = new Mat(eh, ew, image.type(), new Scalar(255, 255, 255, 255)); // 검은색 배경, 불투명 (A값 255)

        int top = (eh - nh) / 2;
        int left = (ew - nw) / 2;

        Mat subMat = newImage.submat(top, top + nh, left, left + nw);
        resizedImage.copyTo(subMat);

        return newImage;
    }
}