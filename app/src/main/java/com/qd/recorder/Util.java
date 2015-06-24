package com.qd.recorder;

import static com.googlecode.javacv.cpp.opencv_highgui.cvCreateFileCapture;
import static com.googlecode.javacv.cpp.opencv_highgui.cvQueryFrame;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Video;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;
import com.qd.videorecorder.R;


public class Util {
	public static  ContentValues videoContentValues = null;
	public static String getRecordingTimeFromMillis(long millis)
	{
		String strRecordingTime = null;
		int seconds = (int) (millis / 1000);
		int minutes = seconds / 60;
		int hours = minutes / 60;

		if(hours >= 0 && hours < 10)
			strRecordingTime = "0" + hours + ":";
		else
			strRecordingTime = hours + ":";

		if(hours > 0)
			minutes = minutes % 60;

		if(minutes >= 0 && minutes < 10)
			strRecordingTime += "0" + minutes + ":";
		else
			strRecordingTime += minutes + ":";

		seconds = seconds % 60;

		if(seconds >= 0 && seconds < 10)
			strRecordingTime += "0" + seconds ;
		else
			strRecordingTime += seconds ;

		return strRecordingTime;

	}


	public static int determineDisplayOrientation(Activity activity, int defaultCameraId) {
		int displayOrientation = 0;
		if(Build.VERSION.SDK_INT >  Build.VERSION_CODES.FROYO)
		{
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(defaultCameraId, cameraInfo);

			int degrees  = getRotationAngle(activity);


			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				displayOrientation = (cameraInfo.orientation + degrees) % 360;
				displayOrientation = (360 - displayOrientation) % 360;
			} else {
				displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
			}
		}
		return displayOrientation;
	}

	public static int getRotationAngle(Activity activity)
	{
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees  = 0;

		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;

		case Surface.ROTATION_90:
			degrees = 90;
			break;

		case Surface.ROTATION_180:
			degrees = 180;
			break;

		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		return degrees;
	}

	public static int getRotationAngle(int rotation)
	{
		int degrees  = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;

		case Surface.ROTATION_90:
			degrees = 90;
			break;

		case Surface.ROTATION_180:
			degrees = 180;
			break;

		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		return degrees;
	}
	
	public static String createImagePath(Context context){
		long dateTaken = System.currentTimeMillis();
		String title = CONSTANTS.FILE_START_NAME + dateTaken;
		String filename = title + CONSTANTS.IMAGE_EXTENSION;
		
		String dirPath = Environment.getExternalStorageDirectory()+"/Android/data/" + context.getPackageName()+"/video";
		File file = new File(dirPath);
		if(!file.exists() || !file.isDirectory())
			file.mkdirs();
		String filePath = dirPath + "/" + filename;
		return filePath;
	}

	public static String createFinalPath(Context context)
	{
		long dateTaken = System.currentTimeMillis();
		String title = CONSTANTS.FILE_START_NAME + dateTaken;
		String filename = title + CONSTANTS.VIDEO_EXTENSION;
		String filePath = genrateFilePath(context,String.valueOf(dateTaken), true, null);
		
		ContentValues values = new ContentValues(7);
		values.put(Video.Media.TITLE, title);
		values.put(Video.Media.DISPLAY_NAME, filename);
		values.put(Video.Media.DATE_TAKEN, dateTaken);
		values.put(Video.Media.MIME_TYPE, "video/3gpp");
		values.put(Video.Media.DATA, filePath);
		videoContentValues = values;

		return filePath;
	}
	
	public static void deleteTempVideo(Context context){
		final String dirPath = Environment.getExternalStorageDirectory()+"/Android/data/" + context.getPackageName()+"/video";
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				File file = new File(dirPath);
				if(file != null && file.isDirectory()){
					for(File file2 :file.listFiles()){
						file2.delete();
					}
				}
			}
		}).start();
	}

	private static String genrateFilePath(Context context,String uniqueId, boolean isFinalPath, File tempFolderPath)
	{
		String fileName = CONSTANTS.FILE_START_NAME + uniqueId + CONSTANTS.VIDEO_EXTENSION;
		String dirPath = Environment.getExternalStorageDirectory()+"/Android/data/" + context.getPackageName()+"/video";
		if(isFinalPath)
		{
			File file = new File(dirPath);
			if(!file.exists() || !file.isDirectory())
				file.mkdirs();
		}
		else
			dirPath = tempFolderPath.getAbsolutePath();
		String filePath = dirPath + "/" + fileName;
		return filePath;
	}
	public static String createTempPath(Context context,File tempFolderPath )
	{
		long dateTaken = System.currentTimeMillis();
		String filePath = genrateFilePath(context,String.valueOf(dateTaken), false, tempFolderPath);
		return filePath;
	}



	public static File getTempFolderPath()
	{
		File tempFolder = new File(CONSTANTS.TEMP_FOLDER_PATH +"_" +System.currentTimeMillis());
		return tempFolder;
	}


	public static List<Camera.Size> getResolutionList(Camera camera)
	{ 
		Parameters parameters = camera.getParameters();
		List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();


		return previewSizes;
	}

	public static RecorderParameters getRecorderParameter(int currentResolution)
	{
		RecorderParameters parameters = new RecorderParameters();
		if(currentResolution ==  CONSTANTS.RESOLUTION_HIGH_VALUE)
		{
			parameters.setAudioBitrate(128000);
			parameters.setVideoQuality(0);
		}
		else if(currentResolution ==  CONSTANTS.RESOLUTION_MEDIUM_VALUE)
		{
			parameters.setAudioBitrate(128000);
			parameters.setVideoQuality(5);
		}
		else if(currentResolution == CONSTANTS.RESOLUTION_LOW_VALUE)
		{
			parameters.setAudioBitrate(96000);
			parameters.setVideoQuality(20);
		}
		return parameters;
	}

	public static int calculateMargin(int previewWidth, int screenWidth)
	{
		int margin = 0;
		if(previewWidth <= CONSTANTS.RESOLUTION_LOW)
		{
			margin = (int) (screenWidth*0.12);
		}
		else if(previewWidth > CONSTANTS.RESOLUTION_LOW && previewWidth <= CONSTANTS.RESOLUTION_MEDIUM)
		{
			margin = (int) (screenWidth*0.08);
		}
		else if(previewWidth > CONSTANTS.RESOLUTION_MEDIUM && previewWidth <= CONSTANTS.RESOLUTION_HIGH)
		{
			margin = (int) (screenWidth*0.08);
		}
		return margin;


	}

	public static int setSelectedResolution(int previewHeight)
	{
		int selectedResolution = 0;
		if(previewHeight <= CONSTANTS.RESOLUTION_LOW)
		{
			selectedResolution = 0;
		}
		else if(previewHeight > CONSTANTS.RESOLUTION_LOW && previewHeight <= CONSTANTS.RESOLUTION_MEDIUM)
		{
			selectedResolution = 1;
		}
		else if(previewHeight > CONSTANTS.RESOLUTION_MEDIUM && previewHeight <= CONSTANTS.RESOLUTION_HIGH)
		{
			selectedResolution = 2;
		}
		return selectedResolution;


	}

	public static class ResolutionComparator implements Comparator<Camera.Size> {
		@Override
		public int compare(Camera.Size size1, Camera.Size size2) {
			if(size1.height != size2.height)
				return size1.height -size2.height;
			else
				return size1.width - size2.width;
		}
	}


	public static void concatenateMultipleFiles(String inpath, String outpath)
	{
		File Folder = new File(inpath);
		File files[];
		files = Folder.listFiles();

		if(files.length>0)
		{
			for(int i = 0;i<files.length; i++){
				Reader in = null;
				Writer out = null;
				try {
					in =   new FileReader(files[i]);
					out = new FileWriter(outpath , true); 
					in.close();
					out.close(); 
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	public static String getEncodingLibraryPath(Context paramContext)
	{
		return paramContext.getApplicationInfo().nativeLibraryDir + "/libencoding.so";
	}

	private static HashMap<String, String> getMetaData()
	{
		HashMap<String, String> localHashMap = new HashMap<String, String>();
		localHashMap.put("creation_time", new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSSZ").format(new Date()));
		return localHashMap;
	}

	public static int getTimeStampInNsFromSampleCounted(int paramInt)
	{
		return (int)(paramInt / 0.0441D);
	}



	public static void saveReceivedFrame(SavedFrames frame)
	{
		File cachePath = new File(frame.getCachePath());
		BufferedOutputStream bos;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(cachePath));
			if(bos != null)
			{
				bos.write(frame.getFrameBytesData());
				bos.flush();
				bos.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			cachePath = null;
		} catch (IOException e) {
			e.printStackTrace();
			cachePath = null;
		}
	}

	public static Toast showToast(Context context, String textMessage, int timeDuration)
	{
		if (null == context)
		{
			return null;
		}
		textMessage = (null == textMessage ? "Oops! " : textMessage.trim());
		Toast t = Toast.makeText(context, textMessage, timeDuration);
		t.show();
		return t;
	}
	
	/**
	 * 公共弹窗
	 * 
	 * @param context
	 *            :Context 传入当前调用该方法的activity实例
	 * @param msg
	 *            :String 要显示的显示文字
	 * @param type
	 *            :int 显示类型1：仅为确定，2：有“确定”、“取消”两个操作
	 * @param handler
	 *            :Handler 传入的需要回调的handler信息，可作为回调方法是用，msg.what = 1时为操作完成状态符
	 */
	public static void showDialog(Context context, String title, String content, int type, final Handler handler){
		final Dialog dialog = new Dialog(context, R.style.Dialog_loading);
		dialog.setCancelable(true);
		// 设置像是内容模板
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.global_dialog_tpl, null);
		Button confirmButton = (Button) view
				.findViewById(R.id.setting_account_bind_confirm);// 确认
		Button cancelButton = (Button) view
				.findViewById(R.id.setting_account_bind_cancel);// 取消
		TextView dialogTitle = (TextView) view
				.findViewById(R.id.global_dialog_title);// 标题
		View line_hori_center = view.findViewById(R.id.line_hori_center);// 中竖线
		confirmButton.setVisibility(View.GONE);// 默认隐藏取消按钮
		line_hori_center.setVisibility(View.GONE);
		TextView textView = (TextView) view.findViewById(R.id.setting_account_bind_text);

		// 设置对话框的宽度
		Window dialogWindow = dialog.getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		lp.width = (int) (context.getResources().getDisplayMetrics().density*288);
		dialogWindow.setAttributes(lp);

		// 设置显示类型
		if(type != 1 && type != 2){
			type = 1;
		}
		dialogTitle.setText(title);// 设置标题
		textView.setText(content); // 设置提示内容

		// 确认按钮操作
		if(type == 1 || type == 2){
			confirmButton.setVisibility(View.VISIBLE);
			confirmButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v){
					if(handler != null){
						Message msg = handler.obtainMessage();
						msg.what = 1;
						handler.sendMessage(msg);
					}
					dialog.dismiss();
				}
			});
		}
		// 取消按钮事件
		if(type == 2){
			cancelButton.setVisibility(View.VISIBLE);
			line_hori_center.setVisibility(View.VISIBLE);
			cancelButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v){
					if(handler != null){
						Message msg = handler.obtainMessage();
						msg.what = 0;
						handler.sendMessage(msg);
					}
					dialog.dismiss();
				}
			});
		}
		dialog.addContentView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		dialog.setCancelable(true);// 点击返回键关闭
		dialog.setCanceledOnTouchOutside(true);// 点击外部关闭
		dialog.show();
	}
	
	public IplImage getFrame(String filePath){
		CvCapture capture = cvCreateFileCapture(filePath);
		IplImage image = cvQueryFrame(capture);
		return image;
	}
}
