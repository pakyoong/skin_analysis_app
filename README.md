# skin_analysis_app
## 참여 연구 과제명

- 유-수분 피부 분석 및 ICT 기반 AI 코칭이 가능한 어플리케이션 및 멀티 홈 뷰티 케어 디바이스 개발
    - **간단한 설명**: 이 연구 프로젝트는 피부 상태를 자동으로 분석하고, AI 코칭을 통해 개선 방안을 제시하는 어플리케이션과 홈 뷰티 케어 디바이스를 개발하는 것을 목표로 하고 있습니다.

## 지원 기관

- 중소기업청

## 해당 연구에서 맡은 역할

- **안드로이드 어플 개발 (초기 기능 구현)**
    - 연구실에서 학습한 딥러닝 모델을 안드로이드 어플에 적용
    - 프로젝트 초기 단계에서 모든 안드로이드 개발 작업을 단독 수행

## 해당 어플리케이션에 대한 설명

- 이 어플리케이션은 광열 패치를 개발하여 주름 개선을 목표로 하고 있습니다. 연구실에서 개발 중인 주름 인식 딥러닝 모델을 통해 사용자의 피부 상태를 분석하고 주름 개선 여부를 판별할 수 있습니다.
    - **사용자 이점**: 이 어플리케이션을 통해 사용자는 실시간으로 피부 상태를 체크하고 개선 방안을 얻을 수 있습니다.
    - **모델 학습**: 연구실에서는 주름 패턴과 광열 패치의 효과를 분석하여 딥러닝 모델을 학습시켰습니다.

## 사용한 기술 스택

- **개발 환경**: 안드로이드 스튜디오
    - 사용 언어: 코틀린, Java
- 딥러닝 모델
    - face-parsing.PyTorch
        - https://github.com/zllrunning/face-parsing.PyTorch
    - ****CameraX Extensions API****
        - [**Official Documentation**](https://developer.android.com/training/camerax/extensions-api?hl=ko)
        - [**GitHub Repository**](https://github.com/android/camera-samples/tree/main/CameraXExtensions)
    - 그 외 연구실 자체 개발 모델 사용

## 안드로이드 테스트 앱 설계 절차

- MobileNetV2 기반 객체 인식 모델 구현 및 테스트
    - https://github.com/d-li14/mobilenetv2.pytorch
- DeeplabV3 기반 Segmantic Segmentation 모델 구현 및 테스트
    - [**PyTorch DeeplabV3**](https://pytorch.org/hub/pytorch_vision_deeplabv3_resnet101/)
    - [**Android Implementation**](https://pytorch.org/tutorials/beginner/deeplabv3_on_android.html)
- DeeplabV3 + 카메라 기능 구현 및 테스트
    - [**개인 GitHub**](https://github.com/pakyoong/Image_segmentation)
- 사용자 이름 저장 카메라앱 구현 및 테스트(기능연습)
    - 
- face-parsing.Pytorch 모델 구현 및 테스트
    - 

## 문제 해결

### **이미지 회전 및 좌우 반전 문제 해결**

- **문제 상황**: 카메라 기능 구현 후, 이미지를 불러오는 과정에서 회전 및 좌우 반전 문제가 발생
    - 참고 자료 : https://stickyny.tistory.com/95
    - 참고 자료 : [https://bugloss-chestnut.tistory.com/entry/Android-imageView-%EC%82%AC%EC%A7%84-%ED%9A%8C%E[…]C%80%EA%B2%BD%EB%A1%9C%EB%AA%85-%EC%B0%BE%EA%B8%B0JAVA](https://bugloss-chestnut.tistory.com/entry/Android-imageView-%EC%82%AC%EC%A7%84-%ED%9A%8C%EC%A0%84-%ED%98%84%EC%83%81-%ED%95%B4%EA%B2%B0%ED%95%98%EA%B8%B0-%EC%82%AC%EC%A7%84%EC%9D%98-%EC%A0%88%EB%8C%80%EA%B2%BD%EB%A1%9C%EB%AA%85-%EC%B0%BE%EA%B8%B0JAVA)
- 해결 코드
    
    ```jsx
    // 이 코드 블록은 이미지가 잘못 회전되거나 좌우반전 되는 문제를 해결하기 위한 것입니다.
    
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
    ```
    

**원리**: 카메라나 갤러리에서 이미지를 불러올 때, 이미지의 META 데이터에 저장된 **`ExifInterface`** 정보를 활용하여 이미지의 회전 정보를 얻습니다. 얻은 정보를 바탕으로 이미지를 올바르게 회전 시킵니다.

- **`getRealPathFromURI(Uri contentURI)`**: URI에서 이미지의 절대 경로를 얻습니다.
- **`rotateImageIfRequired(Bitmap img, String imagePath)`**: **`ExifInterface`**를 활용해 이미지가 회전 되었는지 정보를 확인하고, 필요한 경우에 이미지를 회전 시킵니다.
- **`rotateBitmap(Bitmap bitmap, int orientation)`**: 얻은 회전 정보를 바탕으로 실제로 이미지를 회전 시키는 함수입니다.

### 이미지 리사이즈 방식에 따른 Parsing문제

**문제 상황**:

- 사용 중인 **`face.pt`** 모델에 대해 PC 환경에서는 영상에서 ROI 영역이 정상적으로 파싱되었으나, 안드로이드 환경에서는 ROI 영역 파싱에 문제가 발생하였다.

**원인**:

- PC(Python)에서의 이미지 리사이징 방식과 안드로이드에서의 이미지 리사이징 방식에 차이가 있었다.
- 안드로이드에서 사용되는 **`Bitmap.createScaledBitmap`** 함수는 bilinear interpolation 방식을 사용한다.
- 반면, PC(Python) 환경에서는 OpenCV의 **`resize`** 함수를 사용하여 INTER_NEAREST (Nearest neighbor interpolation) 방식을 사용하고 있다.

**해결 방법**:

- 안드로이드 환경에서 OpenCV 라이브러리를 추가한 뒤, 안드로이드에서도 OpenCV의 **`resize`** 함수를 사용하여 이미지를 INTER_NEAREST 방식으로 리사이징하였다.

**변경 사항**:

- 기존 코드:
    
    ```java
    Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, 512, 512, true);
    ```
    
- 변경 후 코드:
    
    ```java
    Mat imgMat = new Mat();
    Utils.bitmapToMat(mBitmap, imgMat);
    
    Mat resizedMat = new Mat();
    Imgproc.resize(imgMat, resizedMat, new Size(512, 512), 0, 0, Imgproc.INTER_NEAREST);
    
    Bitmap resizedBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
    Utils.matToBitmap(resizedMat, resizedBitmap); // 리사이즈된 Mat을 다시 Bitmap으로 변환
    ```
    

**결론**:

- 이미지 리사이징 방식의 차이가 파싱 결과에 큰 영향을 미치는 것으로 확인되었다.
- 동일한 이미지 처리 방식을 여러 환경에서 적용하여 일관된 결과를 얻는 것이 중요하다.

### **이미지 보간법의 원리**

이미지를 리사이징할 때 특정 픽셀 위치에 대한 새로운 값이 필요합니다. 이 픽셀 값은 주변 픽셀 값들을 기반으로 계산됩니다. 이러한 계산 방법을 '보간법'이라고 합니다. 주요 보간법에는 다음과 같은 것들이 있습니다:

1. **Nearest Neighbor Interpolation (최근접 이웃 보간법)**
    - 가장 간단한 방법으로, 새로운 픽셀의 값은 가장 가까운 원본 픽셀의 값으로 설정됩니다.
    - 이 방법은 계산 속도는 빠르지만, 리사이징된 이미지에서 계단 현상(jagging)이 발생할 수 있습니다.
2. **Bilinear Interpolation (양선형 보간법)**
    - 새로운 픽셀의 값은 가장 가까운 4개의 픽셀 값(2x2 그리드)을 기반으로 가중 평균을 취하여 계산됩니다.
    - 결과 이미지는 부드러운 경계를 갖지만, 더 많은 계산이 필요합니다.
3. **Bicubic Interpolation (양삼차 보간법)**
    - 새로운 픽셀의 값은 가장 가까운 16개의 픽셀 값(4x4 그리드)을 기반으로 계산됩니다.
    - 보다 정교한 결과를 제공하지만, 계산 복잡도가 높습니다.

### **보간법의 차이로 인한 결과의 영향**

- **Nearest Neighbor** 보간법은 원본 이미지의 픽셀 값을 변경하지 않기 때문에, 이미지의 선명한 특징이나 경계를 그대로 유지할 수 있습니다. 하지만 리사이징된 이미지에서 계단 현상이 발생할 수 있어, 이미지가 덜 부드러워 보일 수 있습니다.
- **Bilinear** 보간법은 주변 픽셀 값을 평균화하기 때문에 이미지가 더 부드럽게 보입니다. 그러나 선명한 특징이나 경계가 약간 흐려질 수 있습니다.

따라서, 이미지의 세부 특징이나 경계를 파악하는 데 중요한 역할을 하는 모델 (예: 세그멘테이션, 경계 검출)의 경우, 보간법의 선택이 결과에 큰 영향을 미칠 수 있습니다. 특히, 모델 학습 시 사용한 리사이징 방법과 실제 사용 시의 리사이징 방법이 다른 경우, 결과의 차이가 발생할 수 있습니다.
