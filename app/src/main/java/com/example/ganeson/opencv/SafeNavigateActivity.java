package com.example.ganeson.opencv;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SafeNavigateActivity extends FragmentActivity implements OnMapReadyCallback ,CameraBridgeViewBase.CvCameraViewListener2{

    private GoogleMap mMap;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static String TAG = "MainActivity";
    Mat rgba,gray ;
    JavaCameraView javaCameraView ;
    CascadeClassifier faceDetector ;
    CascadeClassifier eyeDetector ;
    double absolutu_face_size ;
    BaseLoaderCallback mLoaderCallBack =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS :{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }

            //super.onManagerConnected(status);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_navigate);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        javaCameraView = (JavaCameraView) findViewById(R.id.idForJavaCameraViewV);
        javaCameraView.setCameraIndex(new Integer(1));
        javaCameraView.setVisibility(View.VISIBLE);

        javaCameraView.setCvCameraViewListener(this) ;

        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface.xml");

            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if(faceDetector.empty())
            {
                Log.v("MyActivity","--(!)Error loading A\n");
                return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load cascade. Exception thrown: " + e);
        }
        //Loading Cascade For Eye

        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_eye_tree_eyeglasses.xml");

            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            eyeDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if(faceDetector.empty())
            {
                Log.v("MyActivity","--(!)Error loading A\n");
                return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load cascade. Exception thrown: " + e);
        }

    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }else{
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9,this,mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgba = new Mat(height,width, CvType.CV_8SC4);
        gray = new Mat(height,width,CvType.CV_8SC1);
        absolutu_face_size = (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
        rgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgba  = inputFrame.rgba();
        Imgproc.cvtColor(rgba,gray,Imgproc.COLOR_BGR2GRAY);
        MatOfRect faces = new MatOfRect();
        if(faceDetector==null){
            Log.i(TAG,"NULL is coming");
            return gray;
        }

        faceDetector.detectMultiScale(gray,faces,1.3,1,2,new Size(absolutu_face_size,absolutu_face_size),new Size());
        Rect[] rect_of_array = faces.toArray();
        for(Rect r:rect_of_array){
            Core.rectangle(rgba,new Point(r.x,r.y),new Point(r.x+r.width,r.y+r.height),new Scalar(0,255,0),new Integer(2));
            Rect roi = new Rect((int)r.tl().x,(int)r.tl().y,(int)(r.tl().x+r.width),(int)(r.tl().y+r.height));
            if(roi.tl().x >= 0 && roi.tl().x+roi.width < gray.width()  && roi.tl().y >=0 && roi.tl().y+roi.height < gray.height() ) {
                Mat face_mat = gray.submat(roi);
                MatOfRect eyes = new MatOfRect();
                eyeDetector.detectMultiScale(face_mat, eyes, 1.5, 2, 2, new Size(absolutu_face_size * 0.4, absolutu_face_size * 0.4), new Size());
                if(eyes.toArray().length < 2){
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone ringtoneing = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        ringtoneing.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("Notifier","Cant open ringtone soory");
                    }
                }
                for (Rect reye : eyes.toArray()) {
                    Core.rectangle(rgba, new Point(r.x + reye.x, r.y + reye.y), new Point(r.x + reye.x + reye.width, r.y + reye.y + reye.width), new Scalar(255, 0, 0), new Integer(2));
                    Rect roi_eye = new Rect((int)(r.x + reye.x),(int)(r.y + reye.y),(int)(r.x + reye.x + reye.width),(int)(r.y + reye.y + reye.width)) ;
                    if(roi_eye.tl().x >= 0 && roi_eye.tl().x+roi_eye.width < gray.width()  && roi_eye.tl().y >=0 && roi_eye.tl().y+roi_eye.height < gray.height()) {
                        Mat eye_mat = gray.submat(roi_eye);
                        Imgproc.resize(eye_mat, eye_mat, new Size(24, 24));
                        eye_mat = eye_mat.reshape(1, eye_mat.rows() * eye_mat.cols());
                        byte[] eye_arr = new byte[(int) eye_mat.total() * eye_mat.channels()];
                    }
                }
            }
        }
        return  rgba;
    }
}
