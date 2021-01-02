package com.knziha.polymer.browser;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.GlobalOptions;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jess.ui.TwoWayAdapterView;
import com.jess.ui.TwoWayGridView;
import com.knziha.filepicker.model.DialogConfigs;
import com.knziha.filepicker.model.DialogProperties;
import com.knziha.filepicker.model.DialogSelectionListener;
import com.knziha.filepicker.view.FilePickerDialog;
import com.knziha.polymer.PDocViewerActivity;
import com.knziha.polymer.R;
import com.knziha.polymer.Utils.CMN;
import com.knziha.polymer.databinding.BrowseMainBinding;
import com.knziha.polymer.databinding.TaskItemsBinding;
import com.knziha.polymer.toolkits.MyX509TrustManager;
import com.knziha.polymer.widgets.DescriptiveImageView;
import com.knziha.polymer.widgets.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import static com.knziha.polymer.browser.BrowseFieldHandler.dateToStr;
import static com.knziha.polymer.qrcode.QRActivity.StaticTextExtra;
import static com.knziha.polymer.widgets.Utils.RequsetUrlFromCamera;

/** BrowseActivity Is Not The Browser. It is a work station for resource sniffers.
 * 		The extraction logic is extracted and is not included.
 * 		It is extracted as a field in the db.	*/
public class BrowseActivity extends Activity implements View.OnClickListener {
	BrowseMainBinding UIData;
	
	WebView webview_Player;
	com.tencent.smtt.sdk.WebView x5_webview_Player;
	
	WebBrowseListener listener;
	WebBrowseListenerX5 listenerX5;
	
	BrowseTaskExecuter taskExecuter;
	private File download_path;
	private SharedPreferences opt;
	
	static {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				WebView.setDataDirectorySuffix("st");
			}
			com.tencent.smtt.sdk.WebView.setDataDirectorySuffix("st");
		} catch (Exception e) {
			CMN.Log(e);
		}
	}
	
	private PowerManager.WakeLock mWakeLock;
	
	private void initX5WebStation() {
		x5_webview_Player = new com.tencent.smtt.sdk.WebView(this);
		Utils.addViewToParent(x5_webview_Player, UIData.webStation);
		listenerX5 = new WebBrowseListenerX5(this, x5_webview_Player);
	}
	
	private void initStdWebStation() {
		webview_Player = new WebView(this);
		Utils.addViewToParent(webview_Player, UIData.webStation);
		listener = new WebBrowseListener(this, webview_Player);
	}
	
	BrowseDBHelper tasksDB;
	
	AlarmManager alarmManager;
	
	private String task_action = "knziha.task";
	
	private TextPaint menu_grid_painter;
	
	BrowseFieldHandler editHandler;
	
	FileOutputStream fout;
	private static BrowseReceiver receiver;
	private EnchanterReceiver locationReceiver;
	
	ArrayList<String> menuList = new ArrayList<>();
	RecyclerView.Adapter adapter;
	private Cursor cursor = Utils.EmptyCursor;
	
	DisplayMetrics dm;
	private int selectionPos;
	private long selectionRow;
	
	Map<Long, DownloadTask> taskMap = Collections.synchronizedMap(new HashMap<>());
	Map<Long, AtomicBoolean> runningMap = Collections.synchronizedMap(new HashMap<>());
	Map<Long, Integer> lifesMap = Collections.synchronizedMap(new HashMap<>());
	Map<Long, ScheduleTask> scheduleMap = Collections.synchronizedMap(new HashMap<>());
	Map<Long, PendingIntent> intentMap = Collections.synchronizedMap(new HashMap<>());
	Map<Long, ViewHolder> viewMap = new HashMap<>();
	private boolean checkResumeQRText;
	
	Random random = new Random();
	
	public boolean MainMenuListVis;
	
	@Override
	public void onBackPressed() {
		if(MainMenuListVis) {
			setMainMenuList(false);
		} else {
			super.onBackPressed();
		}
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "servis:SimpleTimer");
		UIData = DataBindingUtil.setContentView(this, R.layout.browse_main);
		opt = getSharedPreferences("browse", 0);
		String path_download = opt.getString("path", null);
		if(path_download!=null) {
			download_path = new File(path_download);
			download_path.mkdir();
			if(!download_path.exists()) {
				download_path = null;
			}
		}
		if(download_path==null) {
			download_path = getExternalFilesDir(null);
		}
		try {
			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(null, new TrustManager[]{new MyX509TrustManager()}, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
		} catch (Exception e) {
			CMN.Log(e);
		}
		Resources mResources = getResources();
		dm = mResources.getDisplayMetrics();
		GlobalOptions.density = dm.density;
		menuList.add("开始/停止");
		menuList.add("附加字段");
		menuList.add("新增项目");
		menuList.add("下载至…");
		menuList.add("计划任务");
		menuList.add("编辑命令");
		menu_grid_painter = DescriptiveImageView.createTextPainter();
		TwoWayGridView mainMenuLst = UIData.mainMenuLst;
		mainMenuLst.setHorizontalSpacing(0);
		mainMenuLst.setVerticalSpacing(0);
		mainMenuLst.setHorizontalScroll(true);
		mainMenuLst.setStretchMode(GridView.NO_STRETCH);
		MenuAdapter menuAda = new MenuAdapter();
		mainMenuLst.setAdapter(menuAda);
		mainMenuLst.setOnItemClickListener(menuAda);
		mainMenuLst.setScrollbarFadingEnabled(false);
		mainMenuLst.setSelector(mResources.getDrawable(R.drawable.listviewselector0));
		mainMenuLst.post(() -> mainMenuLst.setTranslationY(mainMenuLst.getHeight()));
		if (true) {
			startService(new Intent(this, ServiceEnhancer.class));
			locationReceiver = new EnchanterReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction("plodlock");
			registerReceiver(locationReceiver, filter);
		}
		tasksDB = BrowseDBHelper.connectInstance(this);
		alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		RecyclerView recyclerView = UIData.tasks;
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setItemAnimator(null);
		recyclerView.setRecycledViewPool(Utils.MaxRecyclerPool(35));
		recyclerView.setHasFixedSize(true);
		LayoutInflater layoutInflater = LayoutInflater.from(this);
		recyclerView.setAdapter(adapter = new RecyclerView.Adapter() {
			@NonNull
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				return new ViewHolder(TaskItemsBinding.inflate(layoutInflater, parent, false));
			}
			
			@Override
			public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
				if(position<0) return;
				cursor.moveToPosition(position);
				ViewHolder viewHolder = (ViewHolder) holder;
				viewMap.remove(viewHolder.lastBoundRow);
				long rowID = cursor.getLong(0);
				viewMap.put(viewHolder.lastBoundRow=rowID, viewHolder);
				String title = cursor.getString(1);
				viewHolder.taskItemData.title.setText(TextUtils.isEmpty(title)?"Untitled":title);
				viewHolder.taskItemData.url.setText(cursor.getString(2));
				
				TextView number = viewHolder.taskItemData.number;
				number.setText(""+position);
				if(taskMap.containsKey(rowID)) {
					number.setTextColor(Color.GREEN);
				} else if(taskExecuter!=null && taskExecuter.inQueue(rowID)) {
					number.setTextColor(Color.GRAY);
				} else {
					number.setTextColor(Color.BLACK);
				}
			}
			
			@Override
			public int getItemCount() {
				return cursor.getCount();
			}
		});
		updateTaskList();
		try {
			if(true) {
				fout = new FileOutputStream("/sdcard/myFolder/browser.log", true);
				write(fout, "启动..."+new Date()+"\n\n");
			}
		} catch (IOException e) {
			CMN.Log(e);
		}
		if(Utils.littleCat) {
			initX5WebStation();
		} else {
			initStdWebStation();
		}
		CMN.Log("启动...");
		UIData.switchWidget.setOnClickListener(this);
		Window window = getWindow();
		if(Build.VERSION.SDK_INT>=21) {
			window.setStatusBarColor(0xff8f8f8f);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(checkResumeQRText && StaticTextExtra!=null) {
			editHandler.setText(StaticTextExtra);
			StaticTextExtra = null;
		}
	}
	
	void updateTaskList() {
		if(cursor!=null) {
			cursor.close();
		}
		cursor = tasksDB.getCursor();
		if(cursor.getCount()==0) {
			tasksDB.insertNewEntry();
			cursor = tasksDB.getCursor();
		}
		adapter.notifyDataSetChanged();
	}
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id==R.id.switchWidget) {
			View ca = UIData.webs.getChildAt(0);
			if(ca!=null) {
				UIData.webs.removeView(ca);
				UIData.webs.addView(ca, 1);
			}
		} else {
			ViewHolder vh = (ViewHolder) v.getTag();
			selectionPos = vh.getLayoutPosition();
			selectionRow = vh.lastBoundRow;
			if(id==R.id.itemRoot) {
				setMainMenuList(true);
			} else {
				int idx = getFieldIndex(id);
				if(idx>=0) {
					cursor.moveToPosition(selectionPos);
					setFieldRowAndIndex(((TextView)v).getText().toString()
							, cursor.getLong(0), idx);
				}
			}
		}
	}
	
	private void setFieldRowAndIndex(String text, long rowID, int idx) {
		if(editHandler==null) {
			editHandler = new BrowseFieldHandler(this);
		}
		editHandler.setText(text);
		if(idx<100) {
			editHandler.setGotoQR(true);
		}
		editHandler.setVis(true);
		editHandler.setFieldCxt(rowID, idx);
	}
	
	private int getFieldIndex(int id) {
		switch (id) {
			case R.id.title:
			return 0;
			case R.id.url:
			return 1;
		}
		return -1;
	}
	
	
	public void stopWebView() {
		boolean x5 = Utils.littleCat;
		if(x5) {
			if(listenerX5!=null)
				listenerX5.stopWebview();
		} else {
			if(listener!=null)
				listener.stopWebview();
		}
	}
	
	public void clearWebview() {
		boolean x5 = Utils.littleCat;
		if(x5) {
			if(listenerX5!=null)
				listenerX5.clearWebview();
		} else {
			if(listener!=null)
				listener.clearWebview();
		}
	}
	
	public void updateTitleForRow(long id, String newTitle) {
//		ViewHolder item = viewMap.get(id);
//		if(item!=null && item.lastBoundRow==id) {
//			item.taskItemData.title.set(newTitle);
//		}
		updateTaskList();
	}
	
	public void refreshViewForRow(long id) {
		ViewHolder view = viewMap.get(id);
		if(view!=null) {
			UIData.root.post(() -> adapter.onBindViewHolder(view, view.getLayoutPosition()));
		}
	}
	
	public boolean taskRunning(long id) {
		return getRunningFlagForRow(id).get();
	}
	
	public void markTaskEnded(long id) {
		getRunningFlagForRow(id).set(false);
	}
	
	public void batRenWithPat(String path, String pattern, String replace) {
		File f = new File(path);
		if(f.isDirectory()) {
			File[] files = f.listFiles();
			if(files!=null) {
				File newF;
				String newName, suffix;
				Matcher m;
				Pattern p = Pattern.compile(pattern);
				for(File fI:files) {
					if(fI.isFile() && fI.length()>0 && (m=p.matcher(fI.getName())).find()) {
						newName = m.replaceAll(replace);
						suffix = "";
						int  suffix_idx = newName.lastIndexOf(".");
						if(suffix_idx>=0) {
							suffix = newName.substring(suffix_idx);
							newName = newName.substring(0, suffix_idx);
						}
						int cc=0;
						while(true) {
							String fn = cc==0?(newName+suffix):(newName+cc+suffix);
							newF = new File(f, fn);
							if(!newF.exists()) {
								break;
							}
						}
						CMN.Log("renameTo", fI.renameTo(newF), fI, newF);
					}
				}
			}
		}
	}
	
	class ViewHolder extends RecyclerView.ViewHolder {
		final TaskItemsBinding taskItemData;
		public Long lastBoundRow;
		
		public ViewHolder(TaskItemsBinding taskItemData) {
			super(taskItemData.itemRoot);
			taskItemData.itemRoot.setTag(this);
			taskItemData.itemRoot.setOnClickListener(BrowseActivity.this);
			taskItemData.title.setTag(this);
			taskItemData.title.setOnClickListener(BrowseActivity.this);
			taskItemData.url.setTag(this);
			taskItemData.url.setOnClickListener(BrowseActivity.this);
			this.taskItemData = taskItemData;
		}
	}
	
	public void setMainMenuList(boolean vis) {
		int TargetTransY = 0;
		TwoWayGridView animMenu = UIData.mainMenuLst;
		if(MainMenuListVis&&vis) {
			animMenu.setTranslationY(8* GlobalOptions.density);
		}
		if(MainMenuListVis = vis) {
			animMenu.setVisibility(View.VISIBLE);
		} else {
			TargetTransY = TargetTransY + animMenu.getHeight();
		}
		animMenu
				.animate()
				.translationY(TargetTransY)
				.setDuration(220)
				.start();
	}
	
	//for menu list
	public class MenuAdapter extends BaseAdapter implements TwoWayAdapterView.OnItemClickListener
	{
		@Override
		public int getCount() {
			return menuList.size();
		}
		
		@Override
		public View getItem(int position) {
			return null;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			PDocViewerActivity.MenuItemViewHolder holder;
			if(convertView==null) {
				convertView = getLayoutInflater().inflate(R.layout.menu_item, parent, false);
				convertView.setTag(holder=new PDocViewerActivity.MenuItemViewHolder(convertView));
				holder.tv.textPainter = menu_grid_painter;
			} else {
				holder = (PDocViewerActivity.MenuItemViewHolder) convertView.getTag();
			}
			holder.tv.setText(menuList.get(position));
			return convertView;
		}
		
		@Override
		public void onItemClick(TwoWayAdapterView<?> parent, View view, int position, long id) {
			switch (position) {
				case 0:{ //start
					cursor.moveToPosition(selectionPos);
					long rowID = cursor.getLong(0);
					DownloadTask task = taskMap.get(rowID);
					if(task!=null) {
						task.stop();
						taskMap.remove(rowID);
					}
					AtomicBoolean runFlag = getRunningFlagForRow(rowID);
					boolean start=!runFlag.get();
					runFlag.set(start);
					if(start) {
						queueTaskForDB(rowID, true);
						//startTaskForDB(cursor);
					} else {
						setTaskDelayed(rowID, -1, false);
						if(task!=null && taskExecuter!=null && taskExecuter.token==task.abort) {
							taskExecuter.interrupt();
						}
					}
					Toast.makeText(BrowseActivity.this, start?"开始":"终止", Toast.LENGTH_LONG).show();
					setMainMenuList(false);
					refreshViewForRow(id);
				} break;
				case 1:{ //ext
					cursor.moveToPosition(selectionPos);
					String ext1 = cursor.getString(7);
					if(ext1==null) {
						//ext1 = "{ext1:\""+yourScript+"\"}";
					}
					setFieldRowAndIndex(ext1
							, cursor.getLong(0), 7);
					editHandler.setGotoQR(false);
				} break;
				case 2: { //add
					tasksDB.insertNewEntry();
					updateTaskList();
					setMainMenuList(false);
				} break;
				case 3: { //folder
					//CMN.Log(fileChooserParams.getAcceptTypes());
					DialogProperties properties = new DialogProperties();
					properties.selection_mode = DialogConfigs.SINGLE_MODE;
					properties.selection_type = DialogConfigs.DIR_SELECT;
					properties.root = new File("/");
					properties.error_dir = Environment.getExternalStorageDirectory();
					properties.offset = download_path;
					properties.opt_dir=new File(getExternalFilesDir(null), "favorite_dirs");
					properties.opt_dir.mkdirs();
					properties.title_id = 0;
					properties.isDark = false;
					FilePickerDialog dialog = new FilePickerDialog(BrowseActivity.this, properties);
					dialog.setDialogSelectionListener(new DialogSelectionListener() {
						@Override
						public void onSelectedFilePaths(String[] files, String currentPath) {
							if(files.length>0) {
								download_path = new File(files[0]);
								opt.edit().putString("path", download_path.getPath()).apply();
							}
						}
						@Override
						public void onEnterSlideShow(Window win, int delay) { }
						@Override
						public void onExitSlideShow() { }
						@Override
						public Activity getDialogActivity() {
							return BrowseActivity.this;
						}
						@Override
						public void onDismiss() { }
					});
					dialog.show();
					dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
				} break;
				case 4: { //schedule
					cursor.moveToPosition(selectionPos);
					String ext=cursor.getString(7);
					String url = cursor.getString(2);
					ScheduleTask task = new ScheduleTask(
							BrowseActivity.this, id
							, url
							, download_path
							, cursor.getString(1)
							, 0
							, ext);
					
					//scheduleMap.put(selectionRow, task);
					//scheduleMap.put(selectionRow, new int[]{50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50});
					
					setFieldRowAndIndex(dateToStr(new Date(CMN.now()+1000*25)), selectionRow, 115);
					
					editHandler.taskToSchedule = task;
				} break;
				case 5: { //mingling
					cursor.moveToPosition(selectionPos);
					long rowID = cursor.getLong(0);
					String ext1 = cursor.getString(7);
					setFieldRowAndIndex(ext1, rowID, 7);
				} break;
			}
		}
	}
	
	public void thriveIfNeeded(long id) {
		Integer val = lifesMap.get(id);
		if(val==null) {
			//if(scheduleMap.get(id)!=null)
			{
				DownloadTask task = taskMap.get(id);
				lifesMap.put(id, task==null?5:task.lives);
				CMN.Log("thriving...", task.lives);
			}
		}
	}
	
	void queueTaskForDB(long rowID, boolean proliferate) {
		BrowseTaskExecuter taskExecutor = acquireExecutor();
		taskExecutor.run(this, rowID);
		taskExecutor.start();
		if(proliferate) {
			lifesMap.put(rowID, null);
		}
	}
	
	private BrowseTaskExecuter acquireExecutor() {
		synchronized (this) {
			if(taskExecuter!=null && taskExecuter.acquire()) {
				return taskExecuter;
			}
			if(taskExecuter!=null) {
				taskExecuter.stop();
			}
			return taskExecuter = new BrowseTaskExecuter(this);
		}
	}
	
	// 任务启动，添加记录至图
	DownloadTask startTaskForDB(Cursor cursor) {
		boolean x5 = Utils.littleCat;
		long id = cursor.getLong(0);
		DownloadTask task = taskMap.get(id);
		if(task!=null) {
			task.abort();
		}
		String ext=cursor.getString(7);
		String url = cursor.getString(2);
		task = new DownloadTask(
				this, id
				, url
				, download_path
				, cursor.getString(1)
				, 0
				, ext);
		taskMap.put(id, task);
		DownloadTask finalTask = task;
		boolean started=false;
		//CMN.Log("启动...", task.ext1);
		if(ext!=null) {
			if(task.ext1!=null || task.ext2!=null) {
				UIData.root.post(() -> {
					if(x5) {
						x5_webview_Player.loadUrl(url);
						x5_webview_Player.setTag(finalTask);
					} else {
						webview_Player.loadUrl(url);
						webview_Player.setTag(finalTask);
					}
				});
				started=true;
			}
		}
		if(!started) {
			taskMap.remove(id);
			//task=null;
		}
		return task;
	}
	
	
	public void scheduleNxtAuto(long id) {
		if(!respawnTask(id)) {
			ScheduleTask task = scheduleMap.get(id);
			Integer[] schedule = task.scheduleSeq;
			if(schedule!=null) {
				if(++task.scheduleIter<schedule.length) {
					int delay = schedule[task.scheduleIter];
					delay = (int) (delay*60*1000+random.nextFloat()*1*30*1000);
					setTaskDelayed(id, delay, true);
				} else {
					scheduleMap.remove(id);
				}
			}
		}
	}
	
	void setTaskDelayed(long id, int delay, boolean schedule) {
		if(schedule) {
			getRunningFlagForRow(id).set(true);
		}
		PendingIntent pendingIntent = intentMap.get(id);
		if(pendingIntent!=null) {
			alarmManager.cancel(pendingIntent);
		}
		
		if(delay<0) {
			scheduleMap.remove(id);
			return;
		}
		
		CMN.Log("setTaskDelayed...", delay/1000/60.f);
		Intent intent = new Intent();
		intent.setClass(this, BrowseActivity.class);
		intent.putExtra("task", id);
		intent.putExtra("pro", schedule);
		intent.setData(Uri.parse(""+id));
		
		pendingIntent = PendingIntent.getActivity(getApplicationContext(),
				110, intent,
				0);
		
		intentMap.put(id, pendingIntent);
		
		//alarmManager.cancel(pendingIntent);
		alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+delay, pendingIntent);
	}
	
	/** respawn from end of downloading
	 * respawn on failure of extraction
	 * respawn on extraction timeout
	 *
	 * @return whether the task is respawned*/
	public boolean respawnTask(long id) {
		CMN.Log("respawnTask", taskRunning(id), lifesMap.get(id));
		if(!taskRunning(id)) {
			return false;
		}
		int lifeSpan = getIntValue(lifesMap.get(id));
		if(--lifeSpan>0) {
			// queue the next analogous task.
			CMN.Log("queue nxt respawner");
			setTaskDelayed(id, (int) (1.5*5*1180*Math.max(0.65, random.nextFloat())), false);
			//a.queueTaskForDB(id, false);
			lifesMap.put(id, lifeSpan);
			return true;
		}
		return false;
	}
	
	private int getIntValue(Integer val) {
		return val==null?0:val;
	}
	
	public static class EnchanterReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String intentAction = intent.getAction();
			if ("plodlock".equals(intentAction)) {
				CMN.Log("plodlock!!!");
			}
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		//CMN.Log("onNewIntent");
		CMN.Log("接收到任务：", intent);
		long task = intent.getLongExtra("task", -1);
		if(taskRunning(task)) {
			if(task!=-1) {
				boolean schedule = intent.getBooleanExtra("pro", false);
				CMN.Log("接收到任务：", task, CMN.id(this), schedule);
				queueTaskForDB(task, schedule);
			}
			mWakeLock.acquire();
			mWakeLock.release();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		CMN.Log("onActivityResult::", data, requestCode, resultCode, StaticTextExtra);
		if(requestCode==RequsetUrlFromCamera&&editHandler!=null)
		{
			String text = data==null?null:data.getStringExtra(Intent.EXTRA_TEXT);
			if(Utils.littleCat) {
				checkResumeQRText = true;
			}
			if(text!=null) {
				editHandler.setText(text);
			}
		}
	}
	
	static AtomicBoolean stop = new AtomicBoolean(false);
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		stop.set(true);
		for(Long kI: runningMap.keySet()) {
			AtomicBoolean task = runningMap.get(kI);
			if(task!=null) {
				task.set(false);
			}
		}
		for(Long kI:taskMap.keySet()) {
			DownloadTask task = taskMap.get(kI);
			if(task!=null) {
				task.stop();
			}
		}
		taskMap.clear();
		if(fout!=null) {
			write(fout, "终止..."+new Date()+"\n\n");
			try {
				fout.close();
			} catch (IOException e) {
				CMN.Log(e);
			}
		}
		if(taskExecuter!=null) {
			taskExecuter.stop();
		}
	}
	
	synchronized public AtomicBoolean getRunningFlagForRow(long id) {
		AtomicBoolean ret = runningMap.get(id);
		if(ret==null) {
			runningMap.put(id, ret = new AtomicBoolean());
		}
		return ret;
	}
	
	public void onUrlExtracted(DownloadTask task, String url) {
		if(task!=null) {
			if(taskRunning(task.id)) {
				if(!TextUtils.isEmpty(url) && !task.abort.get()) {
					task.download(url);
				} else {
					task.abort();
					// fail to extract url and start download. schedule nxt.
					scheduleNxtAuto(task.id);
					taskMap.remove(task.id);
				}
			}
			BrowseTaskExecuter taskExecuter = this.taskExecuter;
			if(taskExecuter !=null && taskExecuter.token==task.abort) {
				taskExecuter.interrupt();
			}
		}
	}
	
	private void write(FileOutputStream fout, String val) {
		try {
			fout.write(val.getBytes());
			fout.flush();
		} catch (IOException e) {
			CMN.Log(e);
		}
	}
	
	static HashMap<String, String> defaultHeaders(String referer, String cookie) {
		//a reasonable UA
		String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36";
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "*/*");
		headers.put("Accept-Language", "en-US,en;q=0.5");
		headers.put("User-Agent", ua);
		//if referer is not None:
		//headers.update({'Referer': referer})
		//if cookie is not None:
		//headers.update({'Cookie': cookie})
		return headers;
	}
	
	@JavascriptInterface
	public String httpGet(String url, String[] headerArr) {
		//if(true) return "x";
		CMN.Log("httpGet", url, headerArr);
		HashMap<String, String> headers;
		if(headerArr!=null) {
			headers = new HashMap<>();
			for (int i = 0; i+1 < headerArr.length; i+=2) {
				headers.put(headerArr[i], headerArr[i+1]);
			}
		} else {
			headers = defaultHeaders(null, null);
		}
		return get_content_std(url, headers);
	}
	
	public static String get_content_std(String httpurl, HashMap<String, String> headers) {
		HttpURLConnection httpConnect = null;
		InputStream is = null;
		BufferedReader br = null;
		String result = null;// 返回结果字符串
		try {
			URL url = new URL(httpurl);
			httpConnect = (HttpURLConnection) url.openConnection();
			httpConnect.setRequestMethod("GET");
			httpConnect.setConnectTimeout(15000);
			httpConnect.setReadTimeout(60000);
			if(headers!=null) {
				for(String key:headers.keySet()) {
					httpConnect.setRequestProperty(key, headers.get(key));
				}
			}
			httpConnect.connect();
			if (httpConnect.getResponseCode() == 200) {
				is = httpConnect.getInputStream();
				br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				StringBuffer sbf = new StringBuffer();
				String temp = null;
				while ((temp = br.readLine()) != null) {
					sbf.append(temp);
					sbf.append("\r\n");
				}
				result = sbf.toString();
			}
		} catch (Exception e) {
			CMN.Log(e);
		} finally {
			// 关闭资源
			if (null != br) {
				try {
					br.close();
				} catch (IOException ignored) { }
			}
			if (null != is) {
				try {
					is.close();
				} catch (IOException ignored) { }
			}
			httpConnect.disconnect();
		}
		return result;
	}
	
	/** https://www.cnblogs.com/hhhshct/p/8523697.html */
	public static String get_content(String url, HashMap<String, String> headers) {
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		String result = "";
		try {
			CMN.Log("result?1?", result);
			httpClient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader("Authorization", "Bearer da3efcbf-0845-4fe3-8aba-ee040be542c0");
			if(headers!=null) {
				for(String key:headers.keySet()) {
					httpGet.setHeader(key, headers.get(key));
				}
			}
			RequestConfig requestConfig =
					RequestConfig.custom()
							.setConnectTimeout(35000)// 连接主机服务超时时间
							.setConnectionRequestTimeout(35000)// 请求超时时间
							.setSocketTimeout(60000)// 数据读取超时时间
							.build();
			httpGet.setConfig(requestConfig);
			response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			result = EntityUtils.toString(entity);
		} catch (Exception e) {
			CMN.Log(e);
		} finally {
			// 关闭资源
			if (null != response) {
				try {
					response.close();
				} catch (IOException ignored) { }
			}
			if (null != httpClient) {
				try {
					httpClient.close();
				} catch (IOException ignored) { }
			}
		}
		return result;
	}
}
