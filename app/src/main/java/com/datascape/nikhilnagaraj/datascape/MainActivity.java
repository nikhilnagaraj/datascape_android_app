package com.datascape.nikhilnagaraj.datascape;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


import com.opencsv.CSVWriter;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import com.wonderkiln.camerakit.CameraViewLayout;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Policy;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class MainActivity extends Activity implements SensorEventListener, LocationListener{

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    //private MediaRecorder mediaRecorder ;
    private LocationManager locLocationManager;
    //private SurfaceHolder sHolder;
    //private Camera.Parameters parameters;
    //private CameraPreview mPreview;
    //private MediaRecorder mMediaRecorder;
   // private Camera mCamera;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static File imageTarget;
    private Button startButton;
    private Button stopButton;
    private CameraView camera;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    Intent takeVideoIntent;
    File dest_file; //video file

    boolean appRun;

    //Accelerometer Variables
    float accl_x;
    float accl_y;
    float accl_z;

    //GPS variables
    double lat_data;
    double long_data;
    double loc_acc;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.StartButton);
        stopButton = (Button) findViewById(R.id.StopButton);
        stopButton.setEnabled(false);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, 0);

        //camera = (CameraView) findViewById(R.id.camera);
    }

    @SuppressLint("MissingPermission")
    public void startOnClick(View view) {
       //Start Application

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        appRun = true;



        String parentPath = Environment.getExternalStorageDirectory() + File.separator + "JourneysFolder";
        Log.v("parentPath", parentPath);

        File parentFolder = new File(parentPath);

        if (!parentFolder.exists() && !parentFolder.isDirectory())
            Log.v("parentcreation", Boolean.toString(parentFolder.mkdir())); //Creates the parent journey folder is it doesn't exist

        //Find the number of journey subfolders i.e. the number of journeys already present
        File[] journeys = parentFolder.listFiles(new FileFilter() {
                                                     @Override
                                                     public boolean accept(File file) {
                                                         return file.isDirectory();
                                                     }
                                                 }

        );

        final File currentJourney = new File(parentFolder.getPath() + File.separator + Integer.toString(journeys.length + 1)); //Create Current journey folder
        currentJourney.mkdirs();


        //Getting Accelerometer data
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    printAccelerometerValue(currentJourney);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Handler().postDelayed(this, 1000);
            }
        }, 1000);

//        //Get Audio
//        MediaRecorderReady(currentJourney);
//        try {
//            mediaRecorder.prepare();
//            mediaRecorder.start();
//        } catch (IllegalStateException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        //Get Location

        locLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastKnownLocation != null){
            lat_data = lastKnownLocation.getLatitude();
            long_data = lastKnownLocation.getLongitude();
            loc_acc = lastKnownLocation.getAccuracy();
        }

        locLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    printLocationValue(currentJourney);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Handler().postDelayed(this, 1000);
            }
        }, 1000);

        Log.v("Location Done","");

        //Get Images
        Log.v("Where?","here");
        imageTarget = currentJourney;
//        imageTarget = new File(currentJourney.getPath() + File.separator + "Images");
//        if(!imageTarget.exists() && !imageTarget.isDirectory()) {
//            imageTarget.mkdirs();
//
//        }
        // Create an instance of Camera


//      Set Camera Parameters
//        Camera.Parameters params = mCamera.getParameters();
//        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        params.setSceneMode(Camera.Parameters.SCENE_MODE_ACTION);
//        List<Camera.Size> sizes = params.getSupportedPictureSizes();
//        Collections.sort(sizes, new Comparator<Camera.Size>() {
//
//            public int compare(final Camera.Size a, final Camera.Size b) {
//                return (b.width * b.height) - (a.width * a.height) ;
//            }
//        });
//
//
//        params.setPictureSize(sizes.get(sizes.size() - 2).width, sizes.get(sizes.size() - 2).height);
//
//
//        mCamera.setParameters(params);
//        mCamera = getCameraInstance();
//        mMediaRecorder = new MediaRecorder();
//        // Create our Preview view and set it as the content of our activity.
//        mPreview = new CameraPreview(this, mCamera);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        preview.addView(mPreview);


        //Uncomment below code to take pictures
//        final Camera.PictureCallback mPicture = new Camera.PictureCallback() {
//
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//
//                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, imageTarget);
//                if (pictureFile == null){
//                    return;
//                }
//
//                try {
//                    FileOutputStream fos = new FileOutputStream(pictureFile);
//                    fos.write(data);
//                    fos.close();
//                } catch (FileNotFoundException e) {
//                    Log.v("error", "File not found: " + e.getMessage());
//                } catch (IOException e) {
//                    Log.v("error", "Error accessing file: " + e.getMessage());
//                }
//            }
//        };
//
////        final Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
////            @Override
////            public void onAutoFocus(boolean success, Camera camera) {
////                camera.takePicture(null,null,mPicture);
////            }
////        };
//
//        camera.setJpegQuality(20);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                camera.captureImage(new CameraKitEventCallback<CameraKitImage>() {
//                    @Override
//                    public void callback(final CameraKitImage cameraKitImage) {
//
//                        AsyncTask.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                byte[] jpeg = cameraKitImage.getJpeg();
//                                Bitmap result = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
//                                File temp = getOutputMediaFile(MEDIA_TYPE_IMAGE);
//                                FileOutputStream out = null;
//                                try {
//                                    out = new FileOutputStream(temp);
//                                } catch (FileNotFoundException e) {
//                                    e.printStackTrace();
//                                }
//                                result.compress(Bitmap.CompressFormat.PNG, 100, out);
//                            }
//                        });
//                    }
//                });
//                new Handler().postDelayed(this, 100);
//            }
//        }, 100);

        //Take videos
//        boolean isRecording = false;
//
//        if (isRecording) {
//            // stop recording and release camera
//            mMediaRecorder.stop();  // stop the recording
//            releaseMediaRecorder(); // release the MediaRecorder object
//            mCamera.lock();         // take camera access back from MediaRecorder
//
//            // inform the user that recording has stopped
//            //setCaptureButtonText("Capture");
//            isRecording = false;
//        } else {
//
//            // initialize video camera
//            if (prepareVideoRecorder(mPreview)) {
//                // Camera is available and unlocked, MediaRecorder is prepared,
//                // now you can start recording
//                Log.d("Just before start","");
//
//                mMediaRecorder.start();
//
//
//                // inform the user that recording has started
//                //setCaptureButtonText("Stop");
//                startButton.setText("Recording");
//                isRecording = true;
//            } else {
//                // prepare didn't work, release the camera
//                releaseMediaRecorder();
//                startButton.setText("Not Recording");
//                // inform user
//            }


        //Capture Video
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        dest_file = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        Uri storeuri = FileProvider.getUriForFile(this,this.getApplicationContext().getPackageName() + ".com.datascape.nikhilnagaraj.datascape.provider",dest_file);
        takeVideoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,storeuri);
        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);







    }


        //T

@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();
            Log.d("file",videoUri.toString());

        }
    }

    private void printLocationValue(File targetFolder) throws IOException {
        //If app has not been stopped
        if(appRun){
            String locFilePath = targetFolder + File.separator + "Location_Data.csv";
            File locFile = new File(locFilePath);
            CSVWriter writer;
            // File exists
            if(locFile.exists() && !locFile.isDirectory()){
                FileWriter mFileWriter = null;
                try {
                    mFileWriter = new FileWriter(locFilePath, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writer = new CSVWriter(mFileWriter);
            }
            else {
                writer = new CSVWriter(new FileWriter(locFilePath));
                String[] headers = {"Latitude","Longitude","Accuracy","Timestamp"};
                //writer.writeNext(headers);
            }

            String[] data = {Double.toString(lat_data),Double.toString(long_data),Double.toString(loc_acc),new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())};

            writer.writeNext(data);

            writer.close();
        }else{
            //Do Nothing
        }



    }

    private void printAccelerometerValue(File targetFolder) throws IOException {
        //If app has not been stopped
        if(appRun){
            String acclnFilePath = targetFolder + File.separator + "egomotion.csv";
            File acclnFile = new File(acclnFilePath);
            CSVWriter writer;
            // File exists
            if(acclnFile.exists() && !acclnFile.isDirectory()){
                FileWriter mFileWriter = null;
                try {
                    mFileWriter = new FileWriter(acclnFilePath, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writer = new CSVWriter(mFileWriter);
            }
            else {
                writer = new CSVWriter(new FileWriter(acclnFilePath));
                String[] headers = {"X Axis","Y Axis","Z Axis","Timestamp"};
                writer.writeNext(headers);
            }

            String[] data = {Float.toString(accl_x),Float.toString(accl_y), Float.toString(accl_z),new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())};

            writer.writeNext(data);

            writer.close();
        }else{
            //Do Nothing
        }



    }


    public void stopOnClick(View view) {


        if(appRun){
            senSensorManager.unregisterListener(this, senAccelerometer);
            locLocationManager.removeUpdates(this);
            //FrameLayout preview =(FrameLayout) findViewById(R.id.camera_preview);
            //preview.removeAllViews();
            //releaseCamera();
            //mediaRecorder.stop();
            appRun = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }




    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
             accl_x = event.values[0];
             accl_y = event.values[1];
             accl_z = event.values[2];
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

//    public void MediaRecorderReady(File targetFolder){
//        //Setup Audio
//        mediaRecorder=new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
//        String audioFilePath = targetFolder + File.separator + "Audio.m4a";
//        mediaRecorder.setOutputFile(audioFilePath);
//    }


    @Override
    public void onLocationChanged(Location location) {
        lat_data = location.getLatitude();
        long_data = location.getLongitude();
        loc_acc = location.getAccuracy();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /** A safe way to get an instance of the Camera object. */
//    public static Camera getCameraInstance(){
//        Camera c = null;
//        try {
//            c = Camera.open(); // attempt to get a Camera instance
//            Log.v("Available Camera","");
//        }
//        catch (Exception e){
//            // Camera is not available (in use or does not exist)
//            Log.v("Unavailable Camera","");
//        }
//        return c; // returns null if camera is unavailable
//    }




//    /** Create a file Uri for saving an image or video */
//    private static Uri getOutputMediaFileUri(int type){
//        return Uri.fromFile(getOutputMediaFile());
//    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = imageTarget;
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.v("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "Video" + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

//    private boolean prepareVideoRecorder(CameraPreview mPreview1){
//
//
//
//
//        // Step 1: Unlock and set camera to MediaRecorder
//        /** A safe way to get an instance of the Camera object. */
//
//        //mCamera.setDisplayOrientation(90);
//        try {
//            mCamera.setPreviewDisplay(mPreview.getHolder());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        mCamera.startPreview();
//
//
//
//        mCamera.unlock();
//        startButton.setText("Here510");
//        mMediaRecorder.setCamera(mCamera);
//        startButton.setText("Here512");
//
//        // Step 2: Set sources
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        startButton.setText("Here517");
//        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
//        startButton.setText("Here520");
//        // Step 4: Set output file
//        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
//        startButton.setText("Here523");
//        // Step 5: Set the preview output
//        mMediaRecorder.setPreviewDisplay(this.mPreview.getHolder().getSurface());
//        startButton.setText("Here526");
//        // Step 6: Prepare configured MediaRecorder
//        try {
//            mMediaRecorder.prepare();
////        } catch (IllegalStateException e) {
////            Log.v(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
////            startButton.setText(e.getMessage());
////            releaseMediaRecorder();
////            return false;
////        } catch (IOException e) {
////            Log.v(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
////            startButton.setText(e.getMessage());
////            releaseMediaRecorder();
////            return false;
//        } catch (Exception e){
//            Log.d("TAGA", "failing here");
//            startButton.setText("Here");
//            return false;
//        }
//        return true;
//    }


//    protected void onPause() {
//        super.onPause();
//        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
//        releaseCamera();              // release the camera immediately on pause event
//    }
//
//    private void releaseMediaRecorder(){
//        if (mMediaRecorder != null) {
//            mMediaRecorder.reset();   // clear recorder configuration
//            mMediaRecorder.release(); // release the recorder object
//            mMediaRecorder = null;
//            mCamera.lock();           // lock camera for later use
//        }
//    }
//
//    private void releaseCamera(){
//        if (mCamera != null){
//            mCamera.release();        // release the camera for other applications
//            mCamera = null;
//        }
//   }
//@Override
//protected void onResume() {
//    super.onResume();
//    camera.start();
//}
//
//    @Override
//    protected void onPause() {
//       camera.stop();
//        super.onPause();
//    }
}



