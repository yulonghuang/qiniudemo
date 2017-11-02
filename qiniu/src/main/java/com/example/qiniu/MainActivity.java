package com.example.qiniu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.KeyGenerator;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.storage.persistent.FileRecorder;
import com.qiniu.android.utils.UrlSafeBase64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity
{

    private byte[] mBuffer;
    private ProgressBar mPr;
    private boolean isCancle;
    private UploadManager uploadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPr = (ProgressBar) findViewById(R.id.pr);
        //断点上传
        String dirPath = "/storage/emulated/0/Download";
        Recorder recorder = null;
        try{
            File f = File.createTempFile("qiniu_xxxx", ".tmp");
            Log.d("qiniu", f.getAbsolutePath().toString());
            dirPath = f.getParent();
            recorder = new FileRecorder(dirPath);
        } catch(Exception e) {
            e.printStackTrace();
        }

        final String dirPath1 = dirPath;
        //默认使用 key 的url_safe_base64编码字符串作为断点记录文件的文件名。
        //避免记录文件冲突（特别是key指定为null时），也可自定义文件名(下方为默认实现)：
        KeyGenerator keyGen = new KeyGenerator(){
            public String gen(String key, File file){
                // 不必使用url_safe_base64转换，uploadManager内部会处理
                // 该返回值可替换为基于key、文件内容、上下文的其它信息生成的文件名
                String path = key + "_._" + new StringBuffer(file.getAbsolutePath()).reverse();
                Log.d("qiniu", path);
                File f = new File(dirPath1, UrlSafeBase64.encodeToString(path));
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(f));
                    String tempString = null;
                    int line = 1;
                    try {
                        while ((tempString = reader.readLine()) != null) {
                            //                          System.out.println("line " + line + ": " + tempString);
                            Log.d("qiniu", "line " + line + ": " + tempString);
                            line++;
                        }

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        try{
                            reader.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return path;
            }
        };

        Configuration config = new Configuration.Builder()
                .chunkSize(512 * 1024)        // 分片上传时，每片的大小。 默认256K
                .putThreshhold(1024 * 1024)   // 启用分片上传阀值。默认512K
                .connectTimeout(10)           // 链接超时。默认10秒
                .useHttps(true)               // 是否使用https上传域名
                .responseTimeout(60)          // 服务器响应超时。默认60秒
                .recorder(recorder)           // recorder分片上传时，已上传片记录器。默认null
                .recorder(recorder, keyGen)   // keyGen 分片上传时，生成标识符，用于片记录器区分是那个文件的上传记录
                .zone(FixedZone.zone0)        // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
                .build();

        // 重用uploadManager。一般地，只需要创建一个uploadManager对象
      uploadManager = new UploadManager(config);


    }

    public void putFile(View view){

        try
        {
            InputStream open = getAssets().open("test.jpg");
            int available = open.available();
            mBuffer = new byte[available];
            //将文件中的数据读到byte数组中
            open.read(mBuffer);
        } catch (IOException e)
        {

        }
        UploadOptions uploadOptions = new UploadOptions(null, null, false, new UpProgressHandler()
        {
            @Override
            public void progress(String key, double percent)
            {
                Log.d("tag",""+percent);
                mPr.setSecondaryProgress((int) percent);
            }
        }, new UpCancellationSignal()
        {
            @Override
            public boolean isCancelled()
            {
                return isCancle;
            }
        });
        uploadManager.put(mBuffer, "test.jpg", "5pOmga8a9VzJRhuEi1P-5hhE6Ylm23NhMCxzw6qR:LOw0saT4ZvM58teM72Eos8NzK3k=:eyJzY29wZSI6InJ2MmdvLXRlc3QiLCJkZWFkbGluZSI6MTUwOTYzNzg5OX0=", new UpCompletionHandler()
        {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response)
            {
                if(info.isOK()){
                    Log.d("tag",key);
                }

            }
        },uploadOptions);

    }

    public void putvideo(View view){

        try
        {
            InputStream open = getAssets().open("video.mp4");
            int available = open.available();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            byte[] b = new byte[1000];
            int n;
            while ((n = open.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            open.close();
            bos.close();

             mBuffer= bos.toByteArray();
            // mBuffer = new byte[available];
            //将文件中的数据读到byte数组中
            //open.read(mBuffer);
        } catch (IOException e)
        {

        }
        UploadOptions uploadOptions = new UploadOptions(null, null, false, new UpProgressHandler()
        {
            @Override
            public void progress(String key, double percent)
            {
                Log.d("tag",""+percent);
                  mPr.setSecondaryProgress((int) percent);
            }
        }, new UpCancellationSignal()
        {
            @Override
            public boolean isCancelled()
            {
                return isCancle;
            }
        });
        uploadManager.put(mBuffer, "video.mp4", "5pOmga8a9VzJRhuEi1P-5hhE6Ylm23NhMCxzw6qR:LOw0saT4ZvM58teM72Eos8NzK3k=:eyJzY29wZSI6InJ2MmdvLXRlc3QiLCJkZWFkbGluZSI6MTUwOTYzNzg5OX0=", new UpCompletionHandler()
        {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response)
            {
                if(info.isOK()){
                    Toast.makeText(MainActivity.this, key+"上传成功", Toast.LENGTH_SHORT).show();
                }

            }
        },uploadOptions);

    }

    public void cancle(View view){
        isCancle=true;
    }

    public void continue2(View view){
        isCancle=false;
}


}
