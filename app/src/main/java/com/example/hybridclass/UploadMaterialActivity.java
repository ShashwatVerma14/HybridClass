package com.example.hybridclass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;


public class UploadMaterialActivity extends AppCompatActivity {

    private EditText fileName;

    private Button uploadMaterial;
    private TextView fileStatus;
    private ImageView uploadFile;
    private final int REQ=1;

    private Uri pdfData;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    String downloadUrl="";
    String pdfName="";
    String title="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_material);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        fileName = findViewById(R.id.etMaterialName);
        fileStatus = findViewById(R.id.tvSelectedFile);
        uploadMaterial = findViewById(R.id.btUploadMaterial);
        uploadFile = findViewById(R.id.addPDF);

        uploadFile.setOnClickListener((view -> { openGallery(); }));

        uploadMaterial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                title = fileName.getText().toString();
                if(title.isEmpty())
                {
                    fileName.setError("Empty Name");
                    fileName.requestFocus();
                }

                if(pdfData==null)
                {
                    Toast.makeText(UploadMaterialActivity.this,"Please Upload File",Toast.LENGTH_LONG).show();
                }
                
                uploadPDF();
            }

        });


    }

    private void uploadPDF()
    {
        StorageReference reference = storageReference.child("pdf/"+pdfName+"-"+System.currentTimeMillis()+".pdf");
        reference.putFile(pdfData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while(!uriTask.isComplete());
                Uri uri = uriTask.getResult();
                uploadData(String.valueOf(uri));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadMaterialActivity.this,"Exception : "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadData(String downloadUrl) {

        String uniqueKey = databaseReference.child("pdf").push().getKey();
        HashMap data = new HashMap();
        data.put("pdfName",title);
        data.put("pdfUrl",downloadUrl);
        databaseReference.child("pdf").child(uniqueKey).setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(UploadMaterialActivity.this,"Uploaded Successfully",Toast.LENGTH_LONG).show();
                fileName.setText("");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadMaterialActivity.this,"Exception : "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openGallery(){
        Intent i = new Intent();
        i.setType("application/pdf");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i,"Select PDF File"),REQ);
    }

    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ && resultCode == RESULT_OK)
        {
            pdfData=data.getData();

            if(pdfData.toString().startsWith("content://"))
            {
                Cursor cursor = null;
                try {
                    cursor = UploadMaterialActivity.this.getContentResolver().query(pdfData,null,null,null,null);
                    if(cursor!=null && cursor.moveToFirst())
                    {
                        pdfName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(pdfData.toString().startsWith("file://"))
            {
                    pdfName = new File(pdfData.toString()).getName();
            }

            fileStatus.setText(pdfName);
        }
    }
}