package com.qd.recorder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

public class VideoPlayTextureView extends TextureView implements
		TextureView.SurfaceTextureListener, OnPreparedListener,
		OnCompletionListener {

	private MediaPlayer mediaPlayer;
	private Surface surface;
	private MediaStateLitenser mediaStateLitenser;
	private MediaState currentMediaState = MediaState.RESET;
	private boolean isChange = true;//当加载完视频文件时，判断当前SurfaceView是否还是之前的SurfaceView
	
	public void setChange(boolean change){
		isChange = change;
	}
	
	public boolean isChange(){
		return isChange;
	}

	public VideoPlayTextureView(Context context) {
		super(context);
		init(context);
	}

	public VideoPlayTextureView(Context context, AttributeSet paramAttributeSet) {
		super(context, paramAttributeSet);
		init(context);
	}

	public VideoPlayTextureView(Context paramContext,
			AttributeSet paramAttributeSet, int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
		init(paramContext);
	}

	/**
	 * 初始化数据
	 * @param context
	 */
	private void init(Context context) {
		mediaPlayer = new MediaPlayer();
		setSurfaceTextureListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
	}

	protected void onMeasure(int paramInt1, int paramInt2) {
		int i = View.MeasureSpec.getSize(paramInt1);
		setMeasuredDimension(i, i);
	}

	/**
	 * 播放当前的视频，如果已经在播放则暂停，否则都开始播放
	 */
	public void play() {
		if(currentMediaState ==  MediaState.PLAY){
			currentMediaState = MediaState.PAUSE;
			if (mediaPlayer != null)
				mediaPlayer.pause();
			if(mediaStateLitenser != null)
				mediaStateLitenser.OnPauseListener();
		}else{
			currentMediaState = MediaState.PLAY;
			if(mediaStateLitenser != null)
				mediaStateLitenser.OnPlayListener();
			if (mediaPlayer != null && !mediaPlayer.isPlaying())
				mediaPlayer.start();
		}
	}

	/**
	 * 暂停播放
	 */
	public void pause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()){
			currentMediaState = MediaState.PAUSE;
			mediaPlayer.pause();
			if(mediaStateLitenser != null)
				mediaStateLitenser.OnPauseListener();
		}
	}

	/**
	 * 停止播放，暂时没使用
	 */
	public void stop() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			currentMediaState = MediaState.RESET;
			mediaPlayer.stop();
			mediaPlayer.release();
		}
	}
	
	/**
	 * 重置当前mediaPlyer，在listView重用该控件时调用
	 */
	public void reset(){
		currentMediaState = MediaState.RESET;
		mediaPlayer.reset();
	}

	/**
	 * 准备播放之前额准备动作，同步调用，适合于文件
	 * 如果是流，调用asyncPrepare()
	 * @param path
	 */
	public void prepare(String path) {
		try {
			currentMediaState = MediaState.PREPARE;
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(path);
			mediaPlayer.setSurface(surface);
			mediaPlayer.prepare();
		} catch (Exception e) {
		}
	}
	
	public MediaState getMediaState(){
		return currentMediaState;
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		surface = new Surface(arg0);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {

	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {

	}

	/**
	 * 设置播放状态的监听
	 * @param mediaStateLitenser
	 */
	public void setMediaStateLitenser(MediaStateLitenser mediaStateLitenser) {
		this.mediaStateLitenser = mediaStateLitenser;
	}

	/**
	 * 播放状态
	 * @author QD
	 *
	 */
	public enum MediaState {
		RESET(0x5),PREPARE(0x1), COMPLETE(0x2), PLAY(0x3), PAUSE(0x4);
		static MediaState mapIntToValue(final int stateInt) {
			for (MediaState value : MediaState.values()) {
				if (stateInt == value.getIntValue()) {
					return value;
				}
			}
			return RESET;
		}

		private int mIntValue;

		MediaState(int intValue) {
			mIntValue = intValue;
		}

		int getIntValue() {
			return mIntValue;
		}
	}
	
	/**
	 * 开始视频文件下载视调用
	 */
	public void OnDownLoadingListener(){
		if (mediaStateLitenser != null)
			mediaStateLitenser.OnDownLoadingListener();
	}

	public interface MediaStateLitenser {
		public void OnCompletionListener();

		public void OnPrepareListener();

		public void OnPauseListener();

		public void OnPlayListener();
		
		public void OnDownLoadingListener();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (mediaStateLitenser != null)
			mediaStateLitenser.OnCompletionListener();
		currentMediaState = MediaState.COMPLETE;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (mediaStateLitenser != null)
			mediaStateLitenser.OnPrepareListener();
	}
}
