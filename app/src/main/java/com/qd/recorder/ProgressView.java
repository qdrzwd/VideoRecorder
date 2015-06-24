package com.qd.recorder;

import java.util.Iterator;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class ProgressView extends View
{

	public ProgressView(Context context) {
		super(context);
		init(context);
	}

	public ProgressView(Context paramContext, AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
		init(paramContext);

	}

	public ProgressView(Context paramContext, AttributeSet paramAttributeSet,
						int paramInt) {
		super(paramContext, paramAttributeSet, paramInt);
		init(paramContext);
	}

	private Paint progressPaint, firstPaint, threePaint,breakPaint;//三个颜色的画笔
	private float firstWidth = 4f, threeWidth = 1f;//断点的宽度
	private LinkedList<Integer> linkedList = new LinkedList<Integer>();
	private float perPixel = 0l;
	private float countRecorderTime = 6000;//总的录制时间

	public void setTotalTime(float time){
		countRecorderTime = time;
	}

	private void init(Context paramContext) {

		progressPaint = new Paint();
		firstPaint = new Paint();
		threePaint = new Paint();
		breakPaint = new Paint();

		// 背景
		setBackgroundColor(Color.parseColor("#19000000"));

		// 主要进度的颜色
		progressPaint.setStyle(Paint.Style.FILL);
		progressPaint.setColor(Color.parseColor("#19e3cf"));

		// 一闪一闪的黄色进度
		firstPaint.setStyle(Paint.Style.FILL);
		firstPaint.setColor(Color.parseColor("#ffcc42"));

		// 3秒处的进度
		threePaint.setStyle(Paint.Style.FILL);
		threePaint.setColor(Color.parseColor("#12a899"));

		breakPaint.setStyle(Paint.Style.FILL);
		breakPaint.setColor(Color.parseColor("#000000"));

		DisplayMetrics dm = new DisplayMetrics();
		((Activity)paramContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
		perPixel = dm.widthPixels/countRecorderTime;

		perSecProgress = perPixel;

	}

	/**
	 * 绘制状态
	 * @author QD
	 *
	 */
	public static enum State {
		START(0x1),PAUSE(0x2);

		static State mapIntToValue(final int stateInt) {
			for (State value : State.values()) {
				if (stateInt == value.getIntValue()) {
					return value;
				}
			}
			return PAUSE;
		}

		private int mIntValue;

		State(int intValue) {
			mIntValue = intValue;
		}

		int getIntValue() {
			return mIntValue;
		}
	}


	private volatile State currentState = State.PAUSE;//当前状态
	private boolean isVisible = true;//一闪一闪的黄色区域是否可见
	private float countWidth = 0;//每次绘制完成，进度条的长度
	private float perProgress = 0;//手指按下时，进度条每次增长的长度
	private float perSecProgress = 0;//每毫秒对应的像素点
	private long initTime;//绘制完成时的时间戳
	private long drawFlashTime = 0;//闪动的黄色区域时间戳

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		long curTime = System.currentTimeMillis();
		//Log.i("recorder", curTime  - initTime + "");
		countWidth = 0;
		//每次绘制都将队列中的断点的时间顺序，绘制出来
		if(!linkedList.isEmpty()){
			float frontTime = 0;
			Iterator<Integer> iterator = linkedList.iterator();
			while(iterator.hasNext()){
				int time = iterator.next();
				//求出本次绘制矩形的起点位置
				float left = countWidth;
				//求出本次绘制矩形的终点位置
				countWidth += (time-frontTime)*perPixel;
				//绘制进度条
				canvas.drawRect(left, 0,countWidth,getMeasuredHeight(),progressPaint);
				//绘制断点
				canvas.drawRect(countWidth, 0,countWidth + threeWidth,getMeasuredHeight(),breakPaint);
				countWidth += threeWidth;

				frontTime = time;
			}
			//绘制三秒处的断点
			if(linkedList.getLast() <= 3000)
				canvas.drawRect(perPixel*3000, 0,perPixel*3000+threeWidth,getMeasuredHeight(),threePaint);
		}else//绘制三秒处的断点
			canvas.drawRect(perPixel*3000, 0,perPixel*3000+threeWidth,getMeasuredHeight(),threePaint);//绘制三秒处的矩形

		//当手指按住屏幕时，进度条会增长
		if(currentState == State.START){
			perProgress += perSecProgress*(curTime - initTime );
			if(countWidth + perProgress <= getMeasuredWidth())
				canvas.drawRect(countWidth, 0,countWidth + perProgress,getMeasuredHeight(),progressPaint);
			else
				canvas.drawRect(countWidth, 0,getMeasuredWidth(),getMeasuredHeight(),progressPaint);
		}
		//绘制一闪一闪的黄色区域，每500ms闪动一次
		if(drawFlashTime==0 || curTime - drawFlashTime >= 500){
			isVisible = !isVisible;
			drawFlashTime = System.currentTimeMillis();
		}
		if(isVisible){
			if(currentState == State.START)
				canvas.drawRect(countWidth + perProgress, 0,countWidth + firstWidth + perProgress,getMeasuredHeight(),firstPaint);
			else
				canvas.drawRect(countWidth, 0,countWidth + firstWidth,getMeasuredHeight(),firstPaint);
		}
		//结束绘制一闪一闪的黄色区域
		initTime = System.currentTimeMillis();
		invalidate();
	}

	/**
	 * 设置进度条的状态
	 * @param state
	 */
	public void setCurrentState(State state){
		currentState = state;
		if(state == State.PAUSE)
			perProgress = perSecProgress;
	}

	/**
	 * 手指抬起时，将时间点保存到队列中
	 * @param time:ms为单位
	 */
	public void putProgressList(int time) {
		linkedList.add(time);
	}
}