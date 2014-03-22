package com.qd.recorder;

import android.os.Build;

import com.googlecode.javacv.cpp.avcodec;

public class RecorderParameters {

	private static boolean AAC_SUPPORTED  = Build.VERSION.SDK_INT >= 10;
	//private int videoCodec = avcodec.AV_CODEC_ID_H264;
	private int videoCodec = avcodec.AV_CODEC_ID_MPEG4;
	private int videoFrameRate = 15;
	//private int videoBitrate = 500 *1000;
	private int videoQuality = 12;
	private int audioCodec = AAC_SUPPORTED ? avcodec.AV_CODEC_ID_AAC : avcodec.AV_CODEC_ID_AMR_NB;
	private int audioChannel = 1;
	private int audioBitrate = 96000;//192000;//AAC_SUPPORTED ? 96000 : 12200; 
	private int videoBitrate = 1000000;
	private int audioSamplingRate = AAC_SUPPORTED ? 44100 : 8000; 
	private String videoOutputFormat = AAC_SUPPORTED ? "mp4"  : "3gp"; 
	
	
	
	public static boolean isAAC_SUPPORTED() {
		return AAC_SUPPORTED;
	}
	public static void setAAC_SUPPORTED(boolean aAC_SUPPORTED) {
		AAC_SUPPORTED = aAC_SUPPORTED;
	}
	public String getVideoOutputFormat() {
		return videoOutputFormat;
	}
	public void setVideoOutputFormat(String videoOutputFormat) {
		this.videoOutputFormat = videoOutputFormat;
	}
	public int getAudioSamplingRate() {
		return audioSamplingRate;
	}
	public void setAudioSamplingRate(int audioSamplingRate) {
		this.audioSamplingRate = audioSamplingRate;
	}
	
	public int getVideoCodec() {
		return videoCodec;
	}
	public void setVideoCodec(int videoCodec) {
		this.videoCodec = videoCodec;
	}
	public int getVideoFrameRate() {
		return videoFrameRate;
	}
	public void setVideoFrameRate(int videoFrameRate) {
		this.videoFrameRate = videoFrameRate;
	}
	
	
	
	public int getVideoQuality() {
		return videoQuality;
	}
	public void setVideoQuality(int videoQuality) {
		this.videoQuality = videoQuality;
	}
	public int getAudioCodec() {
		return audioCodec;
	}
	public void setAudioCodec(int audioCodec) {
		this.audioCodec = audioCodec;
	}
	public int getAudioChannel() {
		return audioChannel;
	}
	public void setAudioChannel(int audioChannel) {
		this.audioChannel = audioChannel;
	}
	public int getAudioBitrate() {
		return audioBitrate;
	}
	public void setAudioBitrate(int audioBitrate) {
		this.audioBitrate = audioBitrate;
	}
	public int getVideoBitrate() {
		return videoBitrate;
	}
	public void setVideoBitrate(int videoBitrate) {
		this.videoBitrate = videoBitrate;
	}
	
	
}
