package com.example.hackathonfilemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int PERMISSIONS_COUNT = 2;
    private static final String TAG = "MainActivity";
    private boolean isFileManagerInitialised;
    private boolean[] selection, cutSelection;
    private File[] files, copyFiles;
    private FileAdapter adapter;
    private List<String> filesList, copyFilesList;
    private String rootPath, currentPath;
    private File dir;
    private TextView path;
    private ListView listView;
    private int filesFoundCount, selectedItemIndex;
    private ImageButton  backButton, refreshButton, newFolderButton;
    private ImageButton renameButton, cutButton, pasteButton, deleteButton, copyButton;
    private boolean isLongClick, isCut;
    private String copyPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        path = findViewById(R.id.textViewDirectory);
        path.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        path.setText("File Manager");
        path.setGravity(Gravity.LEFT);
        listView = findViewById(R.id.fileListView);
        refreshButton = findViewById(R.id.buttonRefresh);
        renameButton = findViewById(R.id.buttonRename);
        backButton = findViewById(R.id.buttonBack);
        deleteButton = findViewById(R.id.buttonDelete);
        newFolderButton = findViewById(R.id.buttonNewFolder);
        copyButton = findViewById(R.id.buttonCopy);
        pasteButton = findViewById(R.id.buttonPaste);
        cutButton = findViewById(R.id.buttonCut);
        filesList = new ArrayList<>();
        copyFilesList = new ArrayList<>();
        selection = new boolean[filesFoundCount];
        adapter = new FileAdapter(filesList, selection, this);

    }

    private void updatePath(){
        dir = new File(currentPath);
        files = dir.listFiles();
        if(files == null){
            return;
        }
        filesFoundCount = files.length;
        Log.d(TAG, "updatePath: "+filesFoundCount);
        selection = new boolean[filesFoundCount];
        adapter.setSelection(selection);
        for(int i=0; i<filesFoundCount; i++){
            filesList.add(String.valueOf(files[i].getAbsolutePath()));
        }
        adapter.setFileNames(filesList);
    }

    public void refresh(){

        filesList.clear();
        if(currentPath.equals(rootPath)) {
            path.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            path.setText("File Manager");
            path.setGravity(Gravity.LEFT);
        }else{
            path.setText(currentPath.substring(currentPath.lastIndexOf('/') + 1));
            path.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            path.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        updatePath();
    }

    public static String get_mime_type(String url){
        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
        String mime = null;
        if(ext != null){
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }
        return mime;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if(!isFileManagerInitialised) {
            currentPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
            rootPath = currentPath;
            listView.setAdapter(adapter);
            refresh();

            refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    findViewById(R.id.buttonPanel).setVisibility(View.GONE);
                    refresh();
                }
            });

            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //change directory
                    if(currentPath.equals(rootPath)){
                        Toast.makeText(getApplicationContext(), "Can't go back. Viewing root directory already!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentPath = currentPath.substring(0,currentPath.lastIndexOf('/'));
                    Log.d(TAG, "onClick: "+currentPath+" Root "+rootPath);
                    refresh();
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                           if(!isLongClick){

                               if(files[position].isDirectory()) {
                                   currentPath = files[position].getAbsolutePath();
                                   refresh();
                               }else{
                                   File file = files[position];

                                   // Get URI and MIME type of file
                                   Uri uri = FileProvider.getUriForFile(MainActivity.this,
                                           getApplicationContext().getPackageName() + ".provider", file);
                                   String mime = get_mime_type(uri.toString());

                                   try {
                                       // Open file with user selected app
                                       Intent intent = new Intent();
                                       intent.setAction(Intent.ACTION_VIEW);
                                       intent.setDataAndType(uri, mime);
                                       intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                       startActivity(intent);
                                   }catch (Exception e){
                                       Toast.makeText(MainActivity.this, e.getMessage(),Toast.LENGTH_LONG).show();
                                   }
                               }
                           }
                        }
                    },50);
                    Log.d(TAG, "onItemClick: "+isLongClick);
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    isLongClick = true;
                    selectedItemIndex = position;
                    selection[position] = !selection[position];
                    Log.d(TAG, "onItemLongClick: CLICK SUCCESSFUL");
                    adapter.setSelection(selection);
                    checkButtonPanelDisplay();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isLongClick = false;
                        }
                    },1000);
                    Log.d(TAG, "onItemLongClick: "+isLongClick);
                    return false;
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete File");
                    deleteDialog.setMessage("Do you really want to delete the selected files");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for(int i=0; i<files.length; i++){
                                if(selection[i]){
                                    deleteFileOrFolder(files[i]);
                                    selection[i]=false;
                                }
                            }
                            findViewById(R.id.buttonPanel).setVisibility(View.GONE);
                            refresh();
                        }
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();

                        }
                    });
                    deleteDialog.show();
                }
            });


            newFolderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder createDialog = new AlertDialog.Builder(MainActivity.this);
                    createDialog.setTitle("New Folder");
                    final EditText folderName = new EditText(MainActivity.this);
                    folderName.setHint("Enter folder name");
                    folderName.setInputType(InputType.TYPE_CLASS_TEXT);
                    createDialog.setView(folderName);
                    createDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final File newFolder = new File(currentPath+"/"+folderName.getText());
                            if(!newFolder.exists()){
                                newFolder.mkdir(); //create folder
                                refresh();
                            }else{
                                Toast.makeText(getApplicationContext(),"Folder already exists",
                                        Toast.LENGTH_SHORT).show();
                                refresh();
                            }
                        }
                    });
                    createDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    createDialog.show();
                }
            });

            renameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder renameDialog = new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename to");
                    final EditText name = new EditText(MainActivity.this);
                    name.setHint("Enter new name");
                    name.setInputType(InputType.TYPE_CLASS_TEXT);
                    final String renamePath = filesList.get(selectedItemIndex);
                    name.setText(renamePath.substring(renamePath.lastIndexOf('/') + 1));
                    renameDialog.setView(name);
                    renameDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String s = new File(renamePath).getParent() + "/" + name.getText();
                            File newFile = new File(s);
                            new File(renamePath).renameTo(newFile);
                            findViewById(R.id.layoutRename).setVisibility(View.GONE);
                            findViewById(R.id.buttonPanel).setVisibility(View.GONE);
                            refresh();
                        }
                    });
                    renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            findViewById(R.id.layoutRename).setVisibility(View.GONE);
                            findViewById(R.id.buttonPanel).setVisibility(View.GONE);
                            refresh();
                        }
                    });
                    renameDialog.show();
                }
            });

            copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: "+copyPath);
                    int size=0;
                    cutSelection = new boolean[files.length];
                    for(int i=0,j=0; i<files.length; i++) {
                        cutSelection[i]=selection[i];
                        if (selection[i]) {
                            size++;
                        }
                    }
                    copyFiles = new File[size];
                    for(int i=0,j=0; i<files.length; i++) {
                            if (selection[i]) {
                            copyFilesList.add(filesList.get(i));
                            copyFiles[j] = files[i];
                            j++;
                        }
                        selection[i]=false;
                    }
                    adapter.setSelection(selection);
                    findViewById(R.id.layoutCopy).setVisibility(View.GONE);
                    findViewById(R.id.layoutCut).setVisibility(View.GONE);
                    findViewById(R.id.layoutPaste).setVisibility(View.VISIBLE);
                }
            });

            pasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String destPath;
                    //Log.d(TAG, "onClick: "+destPath);
                    for(int i=0; i<copyFiles.length; i++){
                            copyPath = copyFilesList.get(i);
                            destPath = currentPath  + copyPath.substring(copyPath.lastIndexOf('/'));
                            copy(new File(copyPath), new File(destPath), currentPath);
                            if(isCut){
                                deleteFileOrFolder(copyFiles[i]);
                                cutSelection[i]=false;
                            }

                    }

                    isCut = false;
                    copyFilesList.clear();
                    findViewById(R.id.layoutCopy).setVisibility(View.VISIBLE);
                    findViewById(R.id.layoutCut).setVisibility(View.VISIBLE);
                    findViewById(R.id.layoutPaste).setVisibility(View.GONE);
                    checkButtonPanelDisplay();
                    refresh();

                }
            });

            cutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isCut = true;
                    copyButton.callOnClick();
                }
            });
            isFileManagerInitialised = true;
        }else{
            refresh();
        }
    }

    private void copy(File src, File dest, String pastePath){
        try {
            if(src.isDirectory()){
                pastePath = pastePath + "/"+src.getName();
                File newFolder = new File(pastePath);
                newFolder.mkdir(); //create folder
                refresh();
                Log.d(TAG, "Paste Path: "+pastePath);
                Log.d(TAG, "Created Directory Paste");
                for(int i=0; i<src.listFiles().length; i++){
                    copyPath = src.listFiles()[i].toString();
                    Log.d(TAG, "copy: "+copyPath);
                    copy(new File(copyPath), new File(pastePath), pastePath);
                }

            }else {
                if(dest.isDirectory()){
                    String path = dest.getPath();
                    path += "/"+src.getName();
                    dest = new File(path);
                }
                InputStream in = new FileInputStream(src);
                OutputStream out = new FileOutputStream(dest);
                byte[] buff = new byte[1024];
                int len;
                while ((len = in.read(buff)) > 0) {
                    out.write(buff, 0, len);
                }
                Log.d(TAG, "Created File Paste");
                out.close();
                in.close();
                refresh();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("NewApi")
    private boolean arePermissionsDenied(){
            int p = 0;
            while (p<PERMISSIONS_COUNT){
                if(checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED){
                    return true;
                }
                p++;
            }

        return false;
    }

    private void deleteFileOrFolder(File fileOrFolder){
        if(fileOrFolder.isDirectory()){
            if(fileOrFolder.list().length == 0){
                fileOrFolder.delete();
            }else{
                //delete all files within a folder recursively
                String files[] = fileOrFolder.list();
                for(String t : files){
                    File fileToDelete = new File(fileOrFolder, t);
                    deleteFileOrFolder(fileToDelete);
                }
                if(fileOrFolder.list().length==0){
                    fileOrFolder.delete();
                }
            }
        }else{
            fileOrFolder.delete();
        }
    }

    private void checkButtonPanelDisplay(){
        int selectionCount=0;
        for(boolean s : selection){
            if(s) {
                selectionCount++;
            }
        }
        LinearLayout layout = findViewById(R.id.buttonPanel);
        if(selectionCount>0){
            layout.setVisibility(View.VISIBLE);
            if(selectionCount==1){
                findViewById(R.id.layoutRename).setVisibility(View.VISIBLE);
            }else{
                findViewById(R.id.layoutRename).setVisibility(View.GONE);
            }
            layout.setVisibility(View.VISIBLE);
        }else{
            layout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSIONS &&  grantResults.length>0){
            if(arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }
    }
}