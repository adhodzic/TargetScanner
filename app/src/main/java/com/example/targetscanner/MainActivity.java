package com.example.targetscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity{

    Button btn;
    TextView msg;
    Mat mat1;
    BaseLoaderCallback baseLoaderCallback;
    final static int PERMISSION_CODE = 1000;
    final static int SECOND_ACTIVITY = 0;
    final static int IMAGE_CAPTURE_CODE = 1001;
    Uri imageUri;
    private File perStorageDir;
    private String currentImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        mat1 = new Mat();
        setContentView(R.layout.activity_main);
        btn = (Button)findViewById(R.id.button_camera);
        msg = (TextView)findViewById(R.id.textView);
        btn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(checkSelfPermission(Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);
                    }else{
                        openCamera();
                    }
                }else {
                    openCamera();
                }
            }
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(cameraIntent.resolveActivity(getPackageManager())!=null)
        {
            File imageFile = null;
            try {
                imageFile = getImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(imageFile!=null)
            {
                imageUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imageFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
            }

        }
    }
    public File getImageFile() throws IOException {
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmsss").format(new Date());
        String imageName = "jpg_temp_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        storageDir.mkdirs();
        perStorageDir = storageDir;
        File imageFile = File.createTempFile(imageName,".jpg", storageDir);
        currentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_CODE:{
                if(grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }else{
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SECOND_ACTIVITY && resultCode == 2){
            msg.setVisibility(View.VISIBLE);
            msg.setText("Target not found. Try again!!!");
        }
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK){
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = null;
            try {
                bmp = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(),
                        imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Mat obj = new Mat();
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            Utils.bitmapToMat(bmp, obj);
            long addr = obj.getNativeObjAddr();
            Intent i = new Intent(MainActivity.this, ProcImage.class);
            i.putExtra("mat", addr);
            startActivityForResult(i, SECOND_ACTIVITY);
        }
    }
}
