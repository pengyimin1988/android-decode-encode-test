/*
 * Copyright (C) 2016 Martin Storsjo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.decodeencodetest;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "H264toH265";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;

    Spinner mResolutionSpinner;
    Spinner mFrameRateSpinner;
    Spinner mBitRateSpinner;
    Spinner mCodecSpinner;
    TextView mVideoFileNameTextView;
    TextView mVideoEncodeTime;
    TextView mInputFileNameTextView;

    private String mFilePath = null;
    private String videoType = ExtractDecodeEditEncodeMuxTest.OUTPUT_VIDEO_MIME_H264_TYPE;
    private int mWidth = 1280;
    private int mHeight = 720;
    private int mFrameRate = 25;
    private int mBitRate = 2000000;
    private String mOutputFile = null;
    private long t1 = 0, t2, t = 0;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 1) {
                mVideoFileNameTextView.setText((String)msg.obj);
            }

            if(msg.what == 2) {
                mVideoEncodeTime.setText((String)msg.obj);
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResolutionSpinner = (Spinner)findViewById(R.id.resolutionSpinner);
        mFrameRateSpinner = (Spinner)findViewById(R.id.frameRateSpinner);
        mBitRateSpinner = (Spinner)findViewById(R.id.bitRateSpinner);
        mCodecSpinner = (Spinner)findViewById(R.id.codecSpinner);
        mVideoFileNameTextView = (TextView)findViewById(R.id.videoFileNameTextView);
        mVideoEncodeTime = (TextView)findViewById(R.id.videoDecodeEncodeTime);
        mInputFileNameTextView = (TextView)findViewById(R.id.inputFileNameTextView);

        //开始按钮
        findViewById(R.id.start_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mFilePath == null) {
                    mInputFileNameTextView.setText("请先选择编码文件再开始编码");
                    return;
                }

                mVideoFileNameTextView.setTextColor(Color.BLUE);
                mVideoFileNameTextView.setText("编码中...");

                Thread th = new Thread() {
                    public void run() {
                        ExtractDecodeEditEncodeMuxTest test = new ExtractDecodeEditEncodeMuxTest();
                        test.setContext(MainActivity.this);
                        try {
                            t1 = System.currentTimeMillis();
                            test.testExtractDecodeEditEncodeMuxVideo(mFilePath,mWidth,mHeight,videoType, mFrameRate,mBitRate,true);
                            t2 = System.currentTimeMillis();
                            long t = t2-t1;
                            Log.d(TAG,"==================testExtractDecodeEditEncodeMuxAudioVideo end return " + t );

                            mOutputFile = test.getOutputFile();
                            Message msg = mHandler.obtainMessage();
                            msg.what = 1;
                            msg.obj = mOutputFile;
                            msg.sendToTarget();

                            Message msg2 = mHandler.obtainMessage();
                            msg2.what = 2;
                            msg2.obj = Long.toString(t);
                            msg2.sendToTarget();

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }


//                        EncodeYuvTest test = new EncodeYuvTest();
//                        test.setContext(MainActivity.this);
//                        try {
//                            test.testEncodeYuvFile(EncodeYuvTest.OUTPUT_VIDEO_MIME_H264_TYPE, 25, 4000000);
//                            Log.d(TAG,"==================testExtractDecodeEditEncodeMuxAudioVideo end return");
//
//                            mOutputFile = test.getOutputFile();
//                            Message msg = mHandler.obtainMessage();
//                            msg.what = 1;
//                            msg.obj = mOutputFile;
//                            msg.sendToTarget();
//
//                        } catch (Throwable t) {
//                            t.printStackTrace();
//                        }
                    }
                };
                th.start();
            }
        });

        //选择编码文件按钮
        findViewById(R.id.select_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), REQUEST_CODE_PICK);
            }
        });

        //编码分辨率
        ArrayAdapter<VideoEntities.ResolutionItem> cadp = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, getSupportedResolutions());
        cadp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mResolutionSpinner.setAdapter(cadp);
        mResolutionSpinner.setSelection(0);
        mResolutionSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                VideoEntities.ResolutionItem selectedItem = (VideoEntities.ResolutionItem)adapterView.getSelectedItem();
                mWidth = selectedItem.width;
                mHeight = selectedItem.height;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //编码码率
        ArrayAdapter<VideoEntities.BitRateItem> bitRateItemArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, getSupportedBitRates());
        bitRateItemArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBitRateSpinner.setAdapter(bitRateItemArrayAdapter);
        mBitRateSpinner.setSelection(2);
        mBitRateSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                VideoEntities.BitRateItem selectedItem = (VideoEntities.BitRateItem)adapterView.getSelectedItem();
                mBitRate = selectedItem.bitrateInKbps * 1000;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //编码帧率
        ArrayAdapter<VideoEntities.FrameRateItem> frameRateItemArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, getSupportedFrameRates());
        frameRateItemArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFrameRateSpinner.setAdapter(frameRateItemArrayAdapter);
        mFrameRateSpinner.setSelection(1);
        mFrameRateSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                VideoEntities.FrameRateItem selectedItem = (VideoEntities.FrameRateItem)adapterView.getSelectedItem();
                mFrameRate = selectedItem.frameRate;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //编码类型
        mCodecSpinner = (Spinner)findViewById(R.id.codecSpinner);
        ArrayAdapter<VideoEntities.CodecItem> codecItemArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item, getSupportedCodecs());
        codecItemArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCodecSpinner.setAdapter(codecItemArrayAdapter);
        mCodecSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                VideoEntities.CodecItem selectedItem = (VideoEntities.CodecItem)adapterView.getSelectedItem();
                if(selectedItem.codecId  == VideoEntities.CodecItem.CodecId.H264) {
                    videoType = ExtractDecodeEditEncodeMuxTest.OUTPUT_VIDEO_MIME_H264_TYPE;
                }
                else {
                    videoType = ExtractDecodeEditEncodeMuxTest.OUTPUT_VIDEO_MIME_H265_TYPE;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private List<VideoEntities.BitRateItem> getSupportedBitRates() {
        List<VideoEntities.BitRateItem> ls = new ArrayList<>();

        ls.add(new VideoEntities.BitRateItem(4000));
        ls.add(new VideoEntities.BitRateItem(3000));
        ls.add(new VideoEntities.BitRateItem(2000));
        ls.add(new VideoEntities.BitRateItem(1500));
        ls.add(new VideoEntities.BitRateItem(1200));
        ls.add(new VideoEntities.BitRateItem(1000));
        ls.add(new VideoEntities.BitRateItem(800));
        ls.add(new VideoEntities.BitRateItem(400));
        ls.add(new VideoEntities.BitRateItem(200));
        return ls;
    }

    private List<VideoEntities.FrameRateItem> getSupportedFrameRates() {
        List<VideoEntities.FrameRateItem> ls = new ArrayList<>();
        ls.add(new VideoEntities.FrameRateItem(30));
        ls.add(new VideoEntities.FrameRateItem(25));
        ls.add(new VideoEntities.FrameRateItem(20));
        ls.add(new VideoEntities.FrameRateItem(15));
        ls.add(new VideoEntities.FrameRateItem(10));
        return ls;
    }

    private List<VideoEntities.CodecItem> getSupportedCodecs() {
        List<VideoEntities.CodecItem> ls = new ArrayList<>();
        ls.add(new VideoEntities.CodecItem(VideoEntities.CodecItem.CodecId.H264));
        ls.add(new VideoEntities.CodecItem(VideoEntities.CodecItem.CodecId.H265));
        return ls;
    }

    private List<VideoEntities.ResolutionItem> getSupportedResolutions() {
        List<VideoEntities.ResolutionItem> ls = new ArrayList<>();
        ls.add(new VideoEntities.ResolutionItem(1920, 1088));
        ls.add(new VideoEntities.ResolutionItem(1920, 1080));
        ls.add(new VideoEntities.ResolutionItem(1280, 720));
        ls.add(new VideoEntities.ResolutionItem(640, 480));
        ls.add(new VideoEntities.ResolutionItem(640, 386));
        ls.add(new VideoEntities.ResolutionItem(320, 240));
        return ls;
    }

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK: {
                final File file;
                if (resultCode == RESULT_OK) {
                    String mFilePathTemp = Uri.decode(data.getDataString());
                    //通过data.getDataString()得到的路径如果包含中文路径，则会出现乱码现象
                    mFilePath = mFilePathTemp.substring(7, mFilePathTemp.length());
                    mFilePath = "/sdcard/output_cqp1.mp4";

                    data.getData();
                    Log.d(TAG,"get file : " + mFilePath + data.getData());

                    mInputFileNameTextView.setText( mFilePath);
                    mVideoFileNameTextView.setTextColor(Color.BLACK);
                    mVideoFileNameTextView.setText("点击开始编码");
                }

            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
