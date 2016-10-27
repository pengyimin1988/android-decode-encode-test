/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Environment;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@TargetApi(18)
public class EncodeYuvTest extends AndroidTestCase {

    public static final String OUTPUT_VIDEO_MIME_H264_TYPE = "video/avc"; // H.264 Advanced Video Coding
    public static final String OUTPUT_VIDEO_MIME_H265_TYPE = "video/hevc"; // H.264 Advanced Video Coding

    private static final String TAG = EncodeYuvTest.class.getSimpleName();
    private static final boolean VERBOSE = true; // lots of logging

    /** How long to wait for the next buffer to become available. */
    private static final int TIMEOUT_USEC = 10000;

    /** Where to output the test files. */
    private static final File OUTPUT_FILENAME_DIR = Environment.getExternalStorageDirectory();

    // parameters for the video encoder
    private static final int OUTPUT_VIDEO_BIT_RATE = 2000000;//2000000; // 2Mbps //by ppym
    private static final int OUTPUT_VIDEO_FRAME_RATE = 30; //15; // 15fps  //by ppym
    private static final int OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int OUTPUT_VIDEO_COLOR_FORMAT =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

    /** Width of the output frames. */
    private int mWidth = -1;
    /** Height of the output frames. */
    private int mHeight = -1;
    /** Width of the input frames. */
    private int mInputWidth = -1;
    /** Height of the input frames. */

    private int mInputHeight = -1;

    private int mFrameRate = OUTPUT_VIDEO_FRAME_RATE;
    private int mBitRate = OUTPUT_VIDEO_BIT_RATE;

    /** The destination file for the encoded output. */
    private String mOutputFile = null;
    private String mInputFile = null;
    private FileInputStream mInputStream = null;
    private FileOutputStream mOutputStream = null;

    private String mOutputVideoMimeType = OUTPUT_VIDEO_MIME_H265_TYPE;

    public void testEncodeYuvFile(String outType, int fps, int bitRate) throws Throwable {
        //openYuvFile and get buffer frame by frame
        openYuvFile();
        setSize(1920,1080);

        mOutputVideoMimeType = outType;
        mFrameRate = fps;
        mBitRate = bitRate;

        TestWrapper.runTest(this);
    }

    /** Wraps EncodeYuvTest() */
    private static class TestWrapper implements Runnable {
        private Throwable mThrowable;
        private EncodeYuvTest mTest;

        private TestWrapper(EncodeYuvTest test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.encodeYuvFile();
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /**
         * Entry point.
         */
        public static void runTest(EncodeYuvTest test) throws Throwable {
            test.setOutputFile();
            TestWrapper wrapper = new TestWrapper(test);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            th.join();
            Log.d(TAG,"=runTest thread end return");
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    /**
     * Sets the desired frame size.
     */
    private void setSize(int width, int height) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
    }

    private void setOutputFile() throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append(OUTPUT_FILENAME_DIR.getAbsolutePath());
        sb.append("/");

        sb.append("yuv-");
        sb.append(mFrameRate);
        sb.append("fps");

        sb.append("-");
        sb.append(mBitRate/1000);
        sb.append("Kbps");

        assertTrue("should have called setSize() first", mWidth != -1);
        assertTrue("should have called setSize() first", mHeight != -1);
        sb.append('-');
        sb.append(mWidth);
        sb.append('x');
        sb.append(mHeight);

        if(mInputFile != null){
            sb.append("-");
            String name = mInputFile.substring(mInputFile.lastIndexOf('/') + 1, mInputFile.lastIndexOf('.'));
            sb.append(name);
        }

        if(mOutputVideoMimeType.equals(OUTPUT_VIDEO_MIME_H264_TYPE)){
            sb.append(".h264");
        }
        else{
            sb.append(".h265");
        }
        mOutputFile = sb.toString();

        File f = new File(mOutputFile);
        mOutputStream = new FileOutputStream(f);
    }

    void openYuvFile() throws FileNotFoundException {
        File f = new File(OUTPUT_FILENAME_DIR + "/gamelive_1920x1080_30.yuv");
        mInputWidth = 1920;
        mInputHeight = 1080;
        mInputStream = new FileInputStream(f);
    }

    byte[] getYuvFileOneFrame() throws IOException {
        if (mInputStream != null) {
            byte b[] = new byte[(int)(mInputWidth*mInputHeight*1.5)];
            int ret = mInputStream.read(b);
            if(ret == -1){
                mVideoExtractorDone = true;
                return null;
            }
            assertEquals("mInputStream read not equals",b.length,ret);
            mVideoExtractedFrameCount++;
            return b;
        }
        return null;
    }

    public String getOutputFile() {
        return mOutputFile;
    }

    private void saveEncodeOutput(int index, MediaCodec.BufferInfo info) {

        ByteBuffer encoderOutputBuffer = mVideoEncoder.getOutputBuffer(index);
        if (encoderOutputBuffer == null) {
            throw new RuntimeException("encoderOutputBuffer " + index +
                    " was null");
        }
        // adjust the ByteBuffer values to match BufferInfo (not needed?)
        encoderOutputBuffer.position(info.offset);
        encoderOutputBuffer.limit(info.offset + info.size);

        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            if (VERBOSE) Log.d(TAG, "video encoder: codec config buffer");

            byte[] buf = new byte[info.size];
            encoderOutputBuffer.get(buf);
            if (null != mOutputStream) {
                try {
                    mOutputStream.write(buf);
                    mOutputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            mVideoEncoder.releaseOutputBuffer(index, false);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "video encoder: returned buffer for time "
                    + info.presentationTimeUs);
        }
        if (info.size != 0) {
            if(mOutputStream != null){
                byte[] buf = new byte[info.size];
                encoderOutputBuffer.get(buf);
                try {
                    mOutputStream.write(buf);
                    mOutputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mVideoEncoder.releaseOutputBuffer(index, false);
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE) Log.d(TAG, "video encoder: EOS");
            synchronized (this) {
                mVideoEncoderDone = true;
                notifyAll();
            }
        }
        logState();
    }

    private MediaCodec mVideoEncoder = null;

    /**
     * Tests encoding and subsequently decoding video from frames generated into a buffer.
     * <p>
     * We encode several frames of a video test pattern using MediaCodec, then decode the output
     * with MediaCodec and do some simple checks.
     */
    private void encodeYuvFile() throws Exception {
        // Exception that may be thrown during release.
        Exception exception = null;

        mEncoderOutputVideoFormat = null;

        mVideoExtractorDone = false;
        mVideoEncoderDone = false;

        mVideoExtractedFrameCount = 0;
        mVideoEncodedFrameCount = 0;

        MediaCodecInfo videoCodecInfo = selectCodec(mOutputVideoMimeType);
        if (videoCodecInfo == null) {
            // Don't fail CTS if they don't have an AVC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + mOutputVideoMimeType);
            return;
        }
        if (VERBOSE) Log.d(TAG, "video found codec: " + videoCodecInfo.getName());

        try {
            // We avoid the device-specific limitations on width and height by using values
            // that are multiples of 16, which all tested devices seem to be able to handle.
            MediaFormat outputVideoFormat =
                    MediaFormat.createVideoFormat(mOutputVideoMimeType, mWidth, mHeight);

            // Set some properties. Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            outputVideoFormat.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
            outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            outputVideoFormat.setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);
            if (VERBOSE) Log.d(TAG, "video format: " + outputVideoFormat);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties. Required YUV Buffer to use for input.
            mVideoEncoder = createVideoEncoder(videoCodecInfo, outputVideoFormat);

            awaitEncode();
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing extractor, encoder, and filestream");
            // Try to release everything we acquired, even if one of the releases fails, in which
            // case we save the first exception we got and re-throw at the end (unless something
            // other exception has already been thrown). This guarantees the first exception thrown
            // is reported as the cause of the error, everything is (attempted) to be released, and
            // all other exceptions appear in the logs.
            try {
                if (mVideoEncoder != null) {
                    mVideoEncoder.stop();
                    mVideoEncoder.release();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing videoEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (mInputStream != null) {
                    mInputStream.close();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing mInputStream", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
            } catch(Exception e) {
                Log.e(TAG, "error while releasing mOutputStream", e);
                if (exception == null) {
                    exception = e;
                }
            }
            mVideoEncoder = null;
            mInputStream = null;
            mOutputStream = null;
        }
        if (exception != null) {
            throw exception;
        }
    }


    /**
     * Creates an encoder for the given format using the specified codec
     * @param codecInfo of the codec to use
     * @param format of the stream to be produced
     */
    private MediaCodec createVideoEncoder(
            MediaCodecInfo codecInfo,
            MediaFormat format) throws IOException {
        MediaCodec encoder = MediaCodec.createByCodecName(codecInfo.getName());

        encoder.setCallback(new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                Log.d("MediaCodecCallback","encode Errror mess: " + e);
                Log.d("MediaCodecCallback","encode Errror diag: " + e.getDiagnosticInfo());
                Log.d("MediaCodecCallback","encode Errror rec: " + e.isRecoverable());
                Log.d("MediaCodecCallback","encode Errror tra: " + e.isTransient());
            }
            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                if (VERBOSE) Log.d(TAG, "video encoder: output format changed");

                mEncoderOutputVideoFormat = codec.getOutputFormat();
                //setupMuxer();
            }
            public void onInputBufferAvailable(MediaCodec codec, int index) {
                byte[] buf = new byte[0];
                try {
                    buf = getYuvFileOneFrame();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(buf == null){
                    if (VERBOSE) Log.d(TAG, "getYuvFileOneFrame: EOS");
                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    logState();
                    return;
                }

                if (VERBOSE) Log.d(TAG, "video encoder onInputBufferAvailable " + buf.length);
                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                inputBuffer.put(buf, 0, buf.length);
                codec.queueInputBuffer(index, 0, buf.length, 0, 0);
                logState();
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned output buffer: " + index);
                    Log.d(TAG, "video encoder: returned buffer of size " + info.size);
                }
                saveEncodeOutput(index,info);
                mVideoEncodedFrameCount++;
            }
        });
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // Must be called before start() is.
        encoder.start();
        //from google official
        //Each codec maintains a set of input and output buffers that are referred to by a buffer-ID in API calls. After a successful call to start() the client "owns" neither input nor output buffers.
//        inputBuffers = encoder.getInputBuffers();
//        if (VERBOSE) {
//            Log.d(TAG, "get video encoder Buffers: " + inputBuffers + " size " + inputBuffers.length);
//        }
        return encoder;
    }


    // We will get these from the encoders when notified of a format change.
    private MediaFormat mEncoderOutputVideoFormat = null;
    private MediaFormat mEncoderOutputAudioFormat = null;

    // Whether things are done on the video side.
    private boolean mVideoExtractorDone = false;
    private boolean mVideoDecoderDone = false;
    private boolean mVideoEncoderDone = false;

    private boolean mMuxing = false;

    private int mVideoExtractedFrameCount = 0;
    private int mVideoDecodedFrameCount = 0;
    private int mVideoEncodedFrameCount = 0;

    private void logState() {
        if (VERBOSE) {
            Log.d(TAG, String.format(
                    "loop: "
                    + "{"
                    + "extracted:%d(done:%b) "
                    + "encoded:%d(done:%b)} ",
                    mVideoExtractedFrameCount, mVideoExtractorDone,
                    mVideoEncodedFrameCount, mVideoEncoderDone));
        }
    }

    private void awaitEncode() {
        synchronized (this) {
            while (!mVideoEncoderDone) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
        }

        // Basic sanity checks.
//        assertEquals("encoded and decoded video frame counts should match",
//                mVideoDecodedFrameCount, mVideoEncodedFrameCount);
//        assertTrue("decoded frame count should be less than extracted frame count",
//                mVideoDecodedFrameCount <= mVideoExtractedFrameCount);

        // TODO: Check the generated output file.
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no match was
     * found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

}
