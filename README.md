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
