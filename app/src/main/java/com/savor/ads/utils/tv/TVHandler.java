package com.savor.ads.utils.tv;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.savor.ads.utils.LogUtils;

/**
 * handler for message
 * @author Meifk
 *
 */
public class TVHandler extends Handler {
	
	public static final int WHAT_TIME 		= 0x01;
	public static final int WHAT_PFBAR 		= 0x02;
	public static final int WHAT_VOLUME 	= 0x03;
	public static final int WHAT_ERROR	 	= 0x04;
	public static final int WHAT_PLAY		= 0x05;
	public static final int WHAT_CATEGORY	= 0x06;
	public static final int WHAT_PROGRAM	= 0x07;
	public static final int WHAT_DIALOG		= 0x08;
	public static final int WHAT_FPS		= 0x09; 
	public static final int WHAT_START		= 0x0a; 
	public static final int WHAT_UPDATE		= 0x0b;
	public static final int WHAT_READY		= 0x0c;
	public static final int WHAT_CHANNEL	= 0x0d;
	public static final int WHAT_LOADING	= 0x0e;
	public static final int WHAT_TIP		= 0x0f;
	public static final int WHAT_NOTUPDATE	= 0xa1;
	public static final int WHAT_HKCATEGORY  = 0x14;
	public static final int WHAT_HKPLAY		= 0x15;
	public static final int WHAT_INIT		= 0x16;
	public static final int WHAT_SEEKBAR	= 0x17;
	public static final int WHAT_SEEK		= 0x18;
	public static final int WHAT_VOLUMEBAR 	= 0x19;
	public static final int WHAT_ENDHKPLAY	= 0x20;
	public static final int WHAT_UPDATECHANNEL	= 0x21;
	public static final int WHAT_UPDATECHANNELOK = 0x22;
	
	public static final int WHAT_TVOK = 0x23;
	public static final int WHAT_SOUTVOK = 0x24;
	public static final int WHAT_SOUTVPER = 0x25;
	
	private final long DELAY_TV_TO_AD		= 3000;
	private final long DELAY_TIP			= 5000;
	private final long DELAY_HIDE 			= 8000;
	private final long DELAY_CANCEL 		= 10000;
	private final long DELAY_HIDE2 			= 15000;
	
	public TVHandler(Callback callback) {
		super(callback);
	}
	
	public void handlePfbar() {
		if(hasMessages(WHAT_PFBAR)) removeMessages(WHAT_PFBAR);
		sendEmptyMessageDelayed(WHAT_PFBAR, DELAY_HIDE2);
	}
	
	public void handleVolume() {
		if(hasMessages(WHAT_VOLUME)) removeMessages(WHAT_VOLUME);
		sendEmptyMessageDelayed(WHAT_VOLUME, DELAY_HIDE);
	}
	
	public void handleCategory() {
		if(hasMessages(WHAT_CATEGORY)) removeMessages(WHAT_CATEGORY);
		sendEmptyMessageDelayed(WHAT_CATEGORY, DELAY_HIDE2);
	}
	
	public void handleUpdateOK(){
		LogUtils.i("handleUpdateOK");
		Message msg = new Message();
		msg.what = WHAT_UPDATECHANNELOK;
		sendMessage(msg);
	}
	public void handleSeekBar() {
		if(hasMessages(WHAT_SEEKBAR)) removeMessages(WHAT_SEEKBAR);
		sendEmptyMessageDelayed(WHAT_SEEKBAR, DELAY_HIDE2);
	}
	
	public void handleTvOK() {
		if(hasMessages(WHAT_TVOK)) removeMessages(WHAT_TVOK);
		sendEmptyMessageDelayed(WHAT_TVOK, DELAY_TV_TO_AD);
	}
	public void handleSouTvOK() {
		if(hasMessages(WHAT_SOUTVOK)) removeMessages(WHAT_SOUTVOK);
		sendEmptyMessageDelayed(WHAT_SOUTVOK, 100);
	}
	public boolean handleSeek() {
		return hasMessages(WHAT_SEEK); 
	}
	
	public void handleSouTvPer(int per) {
		Message msg = new Message();
		msg.what = WHAT_SOUTVPER;
		msg.arg1 = per;
		sendMessage(msg);
	}
	public void handlePlay(int position) {
		Message msg = new Message();
		msg.what = WHAT_PLAY;
		msg.arg1 = position;
		sendMessage(msg);
	}
	public void handleInit() {
		Message msg = new Message();
		msg.what = WHAT_INIT;
		sendMessage(msg);
	}
	public void handleHKPlay(int position) {
		Message msg = new Message();
		msg.what = WHAT_HKPLAY;
		msg.arg1 = position;
		sendMessage(msg);
	}
	public  void handleEndHKPlay() {
		Message msg = new Message();
		msg.what = WHAT_ENDHKPLAY;
		sendMessage(msg);
	}
	public  void handleUpdateChannel() {
		Message msg = new Message();
		msg.what = WHAT_UPDATECHANNEL;
		sendMessage(msg);
	}
	public void handleLoading() {
		if(hasMessages(WHAT_LOADING)) removeMessages(WHAT_LOADING);
		sendEmptyMessageDelayed(WHAT_LOADING, DELAY_CANCEL);
	}
	
	public void handleTip() {
		if(hasMessages(WHAT_TIP)) removeMessages(WHAT_TIP);
		sendEmptyMessageDelayed(WHAT_TIP, DELAY_TIP);
	}
}
