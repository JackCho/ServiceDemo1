package com.example.servicedemo;

import java.io.File;

import com.example.servicedemo.MusicService.MyBinder;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	
	private MusicService service;
	
	private TextView mTotalTime;
	private TextView mPlayTime;
	
	private Button mStartBtn;
	private Button mPauseBtn;
	
	private Dialog mDialog;
	private String musicPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.music.end");
		filter.addAction("com.music.duration");
		filter.addAction("com.music.current");
		registerReceiver(mReceiver, filter);
		
		mTotalTime = (TextView) findViewById(R.id.music_total_time);
		mPlayTime = (TextView) findViewById(R.id.music_play_time);
		mStartBtn = (Button) findViewById(R.id.start_music);
		mPauseBtn = (Button) findViewById(R.id.pause_music);
		mStartBtn.setOnClickListener(this);
		mPauseBtn.setOnClickListener(this);
		
		mDialog = new Dialog(this);
		mDialog.setTitle("正在查找音乐文件");
		mDialog.setCancelable(false);
		
		new Thread(){
			public void run() {
				getFirstMusic(Environment.getExternalStorageDirectory().getAbsolutePath());
				mHandler.sendEmptyMessage(0);
			};
		}.start();
		
		Intent intent = new Intent(this, MusicService.class);
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
	}
	
	private ServiceConnection conn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e("test", "onServiceDisconnected");
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.e("test", "onServiceConnected");
			service = ((MyBinder)binder).getService();
		}
	};
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				mDialog.dismiss();
				break;
			case 1:
				if (service.isPlaying()) {
					mPlayTime.setText("播放时间:" + service.getCurrentPosition() / 1000 + "s");
					mHandler.sendEmptyMessageDelayed(1, 1000);
				}
				break;

			default:
				break;
			}
		};
	};

	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacksAndMessages(null);
		unbindService(conn);
		unregisterReceiver(mReceiver);
	};
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_music:
			service.playMusic(musicPath);
			mTotalTime.setText("音乐总时间：" + service.getDuration() / 1000 + "s");
			mHandler.sendEmptyMessageDelayed(1, 1000);
			break;
		case R.id.pause_music:
			service.pauseMusic();
			break;

		default:
			break;
		}
	}
	
	private void getFirstMusic(String path) {
		File file = new File(path);
		if (file.isFile()) {
			if (file.getName().endsWith(".mp3")) {
				musicPath = file.getAbsolutePath();
			}
		} else {
			File[] childs = file.listFiles();
			if (childs != null && childs.length > 0) {
				for (File f : childs) {
					getFirstMusic(f.getAbsolutePath());
				}
			}
		}
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("com.music.end")) {
				Toast.makeText(MainActivity.this, intent.getCharSequenceExtra("hint"), 2000).show();
			} else if (action.equals("com.music.duration")) {
				mTotalTime.setText("音乐总时间：" + intent.getIntExtra("duration", 0) / 1000 + "s");
			} else if (action.equals("com.music.current")) {
				mPlayTime.setText("播放时间:" + intent.getIntExtra("current", 0) / 1000 + "s");
			}
		}
	};

}
