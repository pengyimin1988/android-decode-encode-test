package com.example.decodeencodetest;

/**
 * Created by DW on 2016/6/29.
 */

import android.hardware.Camera;
import android.os.Parcel;
import android.os.Parcelable;

//package com.example.SimpleCapture.Utils;

import android.hardware.Camera;
import android.os.Parcel;
import android.os.Parcelable;
//import com.example.SimpleCapture.Constant;

/**
 * Created by lookatmeyou on 2016/3/26.
 */
public class VideoEntities {
    public static class VideoData {
        public byte[] data;
        public long pts;
    }

    public static class VideoSizes {
        public int mViewX;
        public int mViewY;
        public int mViewWidth;
        public int mViewHeight;
        public int mVideoWidth = 64;
        public int mVideoHeight = 64;
        public int mVideoSurfaceWidth = 64;
        public int mVideoSurfaceHeight = 64;
    }

    public static class VideoConfig {
        public int mEncodeWidth;
        public int mEncodeHeight;
        public int mPreviewWidth;
        public int mPreviewHeight;
        public int mFrameRate;
        public int mBitRate;
        public int mCameraFacing;
        public boolean videoStabilization;

        public VideoConfig() {}

        public VideoConfig(int encodeWidth, int encodeHeight, int previewWidth, int previewHeight, int frameRate, int bitRate, int cameraFacing) {
            mEncodeWidth = encodeWidth;
            mEncodeHeight = encodeHeight;
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFrameRate = frameRate;
            mBitRate = bitRate;
            mCameraFacing = cameraFacing;
        }
    }

//    public static class WaterMark {
//        public byte[] rgba32data;
//        public int width;
//        public int height;
//        public int offsetx;
//        public int offsety;
//        public Constant.WaterMarkOrigin origin;
//    }

    public static class ResolutionItem {
        public int width;
        public int height;
        public String showText;

        public ResolutionItem(int width, int height) {
            this.width = width;
            this.height = height;
            showText = "" + width + "x" + height;
        }

        @Override
        public String toString() {
            return showText;
        }
    }

    public static class BitRateItem {
        public int bitrateInKbps;
        public String showText;

        public BitRateItem(int bitrateInKbps) {
            this.bitrateInKbps = bitrateInKbps;
            showText = "" + bitrateInKbps + "Kbps";
        }

        @Override
        public String toString() {
            return showText;
        }
    }

    public static class FrameRateItem {
        public int frameRate;
        public String showText;

        public FrameRateItem(int frameRate) {
            this.frameRate = frameRate;
            showText = "" + frameRate + "fps";
        }

        @Override
        public String toString() {
            return showText;
        }
    }

    public static class CodecItem {
        public enum CodecId {
            H264, H265
        }
        public CodecId codecId;
        public String showText;

        public CodecItem(CodecId codecId) {
            this.codecId = codecId;
            showText = "" + codecId;
        }

        @Override
        public String toString() {
            return showText;
        }
    }

    public static class CameraItem {
        public int cameraFacing;
        public String showText;

        public CameraItem(int cameraFacing) {
            this.cameraFacing = cameraFacing;
            if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                showText = "前置";
            }
            else {
                showText = "后置";
            }
        }

        @Override
        public String toString() {
            return showText;
        }
    }

    public static class VideoParameters implements Parcelable {
        public int width = 720;
        public int height = 1280;
        public int frameRate = 24;
        public int bitRateInKbps = 1200;
        public CodecItem.CodecId codecId = CodecItem.CodecId.H264;
        public boolean videoStabilization = true;
        public int cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        public boolean saveVideoToFile = false;
        public String videoFileName = "/storage/sdcard0/SimpleCapture.Video.H264";

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(width);
            parcel.writeInt(height);
            parcel.writeInt(frameRate);
            parcel.writeInt(bitRateInKbps);
            parcel.writeInt(cameraFacing);
            parcel.writeString(videoFileName);
            parcel.writeValue(videoStabilization);
            parcel.writeValue(saveVideoToFile);
            parcel.writeValue(codecId);
        }

        public VideoParameters() {
        }

        public VideoParameters(Parcel parcel) {
            width = parcel.readInt();
            height = parcel.readInt();
            frameRate = parcel.readInt();
            bitRateInKbps = parcel.readInt();
            cameraFacing = parcel.readInt();
            videoFileName = parcel.readString();
            videoStabilization = (Boolean)parcel.readValue(null);
            saveVideoToFile = (Boolean)parcel.readValue(null);
            codecId = (CodecItem.CodecId)parcel.readValue(null);
        }

        public static final Parcelable.Creator<VideoParameters> CREATOR
                = new Parcelable.Creator<VideoParameters>() {
            public VideoParameters createFromParcel(Parcel in) {
                return new VideoParameters(in);
            }

            public VideoParameters[] newArray(int size) {
                return new VideoParameters[size];
            }
        };
    }

    public static class VideoInfo {
        public int width = 0;
        public int height = 0;
        public double frameRate = 0;
        public double bitRateInKbps = 0;
        public boolean videoStabilization = false;
    }
}

