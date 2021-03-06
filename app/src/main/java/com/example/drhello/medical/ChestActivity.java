package com.example.drhello.medical;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.drhello.OnClickDoctorInterface;
import com.example.drhello.R;
import com.example.drhello.adapter.SliderAdapter;
import com.example.drhello.databinding.ActivityChatBinding;
import com.example.drhello.databinding.ActivityChestBinding;
import com.example.drhello.databinding.ActivityNumReactionBinding;
import com.example.drhello.fragment.HomeFragment;
import com.example.drhello.model.CommentModel;
import com.example.drhello.model.Posts;
import com.example.drhello.model.SliderItem;
import com.example.drhello.textclean.RequestPermissions;
import com.example.drhello.ui.writecomment.InsideCommentActivity;
import com.example.drhello.ui.writecomment.WriteCommentActivity;
import com.example.drhello.ui.writepost.NumReactionActivity;
import com.example.drhello.ui.writepost.WritePostsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChestActivity extends AppCompatActivity implements OnClickDoctorInterface {
    private ActivityChestBinding activityChestBinding;
    private ArrayList<SliderItem> sliderItems=new ArrayList<>();
    private String[] stringsChest = {"Covid19", "Lung Opacity","Normal", "Pneumonia"};
    private static final int Gallary_REQUEST_CODE = 1;
    PyObject main_program;
    public static ProgressDialog mProgress;
    String path = "";
    private Bitmap bitmap;
    private RequestPermissions requestPermissions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chest);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) ;
        }else{
            getWindow().setStatusBarColor(Color.WHITE);
        }

        requestPermissions = new RequestPermissions(ChestActivity.this,ChestActivity.this);

        mProgress = new ProgressDialog(ChestActivity.this);

        activityChestBinding = DataBindingUtil.setContentView(ChestActivity.this, R.layout.activity_chest);

        AsyncTaskD asyncTaskDownload = new AsyncTaskD(path,"first");
        asyncTaskDownload.execute();

        activityChestBinding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sliderItems.add(new SliderItem(R.drawable.normal_xray,"Normal"));
        sliderItems.add(new SliderItem(R.drawable.covid19,"Covid19"));
        sliderItems.add(new SliderItem(R.drawable.lung_opacity,"Lung Opacity"));
        sliderItems.add(new SliderItem(R.drawable.pneumonia,"Pneumonia"));

        SliderAdapter sliderAdapter=new SliderAdapter(sliderItems,ChestActivity.this,ChestActivity.this);

        activityChestBinding.viewPagerImageSlider.setAdapter(sliderAdapter);

        activityChestBinding.viewPagerImageSlider.startAutoScroll();

        activityChestBinding.viewPagerImageSlider.setLoopEnabled(true);
        activityChestBinding.viewPagerImageSlider.setCanTouch(true);

        activityChestBinding.selImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestPermissions.permissionStorageRead()) {
                    ActivityCompat.requestPermissions(ChestActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            Gallary_REQUEST_CODE);
                } else {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    String[] mimetypes = {"image/*", "video/*"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                    startActivityForResult(intent, Gallary_REQUEST_CODE);
                }
            }
        });

        activityChestBinding.result.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bitmap != null) {
                    if(!path.equals("")){
                        AsyncTaskD asyncTaskDownloadAudio = new AsyncTaskD(path,"");
                        asyncTaskDownloadAudio.execute();
                    }

                    bitmap = null;
                }else{
                    Toast.makeText(ChestActivity.this, "Please, Choose Image First!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Gallary_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(ChestActivity.this.getContentResolver(), data.getData());
                activityChestBinding.imgCorona.setImageBitmap(bitmap);
                File file = new File(getRealPathFromURI(getImageUri(getApplicationContext(),bitmap)));
                Log.e("file: ", file.getPath());
                path = file.getPath();
            } catch (IOException e) {
                Log.e("gallary exception: ", e.getMessage());
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // Toast.makeText(getBaseContext(), "Canceled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void OnClick(String spec) {

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri,
                null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        Log.e("result: " ,cursor.getString(idx)+"     1");
        String result = cursor.getString(idx);
        cursor.close();
        return result;
    }


    public class AsyncTaskD extends AsyncTask<String, String, String> {

        String path;
        String action;
        public AsyncTaskD(String path,String action){
            this.path = path;
            this.action = action;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress.setMessage("Image Processing..");
            mProgress.setCancelable(false);
            mProgress.show();
        }

        @Override
        protected String doInBackground(String... f_url) {
            if(action.equals("first")){
                if (! Python.isStarted()) {
                    Python.start(new AndroidPlatform(ChestActivity.this));//error is here!
                }
                final Python py = Python.getInstance();
                main_program = py.getModule("prolog");
            }else{
                String result = main_program.callAttr("model",path,"Corona").toString();
                String[] listResult = result.split("@");
                int prediction = Integer.parseInt(listResult[0]);
                String probStr = listResult[1].replace("[","")
                        .replace("]","")
                        .replace("\"","");
                String[] prop = probStr.split(" ");
                if (prediction == 0) {
                    activityChestBinding.txtResult.setText(stringsChest[0] + " :  " + String.format("%.2f", Float.parseFloat(prop[0]) * 100) );
                } else if (prediction == 1) {
                    activityChestBinding.txtResult.setText(stringsChest[1] + " :  " + String.format("%.2f", Float.parseFloat(prop[1]) * 100) );
                } else if (prediction == 2) {
                    activityChestBinding.txtResult.setText(stringsChest[2] + " :  " + String.format("%.2f", Float.parseFloat(prop[2]) * 100) );
                } else if (prediction == 3) {
                    activityChestBinding.txtResult.setText(stringsChest[3] + " :  " + String.format("%.2f", Float.parseFloat(prop[3]) * 100) );
                }
            }
            mProgress.dismiss();
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
        }
    }
}