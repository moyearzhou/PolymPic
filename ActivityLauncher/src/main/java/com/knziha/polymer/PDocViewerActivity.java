package com.knziha.polymer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.knziha.polymer.R;
import com.knziha.polymer.Toastable_Activity;
import com.knziha.polymer.Utils.CMN;
import com.knziha.polymer.Utils.Options;
import com.knziha.polymer.WeakReferenceHelper;
import com.knziha.polymer.databinding.BookmarksBinding;
import com.knziha.polymer.databinding.ImageviewDebugBinding;
import com.knziha.polymer.pdviewer.PDocView;
import com.knziha.polymer.pdviewer.PDocument;
import com.knziha.polymer.pdviewer.bookmarks.BookMarkFragment;
import com.knziha.polymer.pdviewer.bookmarks.BookMarksFragment;
import com.knziha.polymer.pdviewer.bookmarks.FragAdapter;
import com.knziha.polymer.widgets.AppIconsAdapter;
import com.knziha.polymer.widgets.Utils;

import java.io.File;
import java.util.ArrayList;

import static com.knziha.polymer.BrowserActivity.GoogleTranslate;

public class PDocViewerActivity extends Toastable_Activity implements View.OnClickListener {
	ImageviewDebugBinding UIData;
	private boolean hidingContextMenu;
	public PDocView currentViewer;
	
	
	@Override
	public void onBackPressed() {
		if(currentViewer.tryClearSelection()) {
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Window win = getWindow();
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
		//getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		boolean transit = Options.getTransitSplashScreen();
		if(!transit) setTheme(R.style.AppThemeRaw);
		
		UIData = DataBindingUtil.setContentView(this, R.layout.imageview_debug);
		root=UIData.root;
		currentViewer = UIData.wdv;
		
		try {
			currentViewer.dm=dm;
			
			currentViewer.a=this;
			
			
			currentViewer.setContextMenuView(UIData.contextMenu);
			
			if(transit){
				//root.setAlpha(0);
				closeSplashScreen();
			}
			
			currentViewer.setSelectionPaintView(UIData.sv);
			
			MenuBuilder context_menu = new MenuBuilder(this);
			getMenuInflater().inflate(R.menu.context_menu, context_menu);
			SpannableStringBuilder text = new SpannableStringBuilder();
			for (int i = 0; i < context_menu.size(); i++) {
				int start = text.length();
				MenuItem item = context_menu.getItem(i);
				text.append(item.getTitle());
				text.setSpan(new ClickableSpan() {
					@Override
					public void updateDrawState(TextPaint ds) {
					
					}
					@Override
					public void onClick(@NonNull View widget) {
						OnMenuClicked(item);
					}},start,text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				text.append("  ");
			}
			UIData.contextMenu.setText(text, TextView.BufferType.SPANNABLE);
			
			
			UIData.contextMenu.setOnClickListener(CMN.XYTouchRecorder());
			
			UIData.contextMenu.setOnTouchListener(CMN.XYTouchRecorder());
			
			Utils.setOnClickListenersOneDepth(UIData.bottombar2, this, 1, null);
			
			if(transit){
				currentViewer.setImageReadyListener(() -> {
					root.post(this::closeSplashScreen);
					currentViewer.setImageReadyListener(null);
				});
			}
			
			currentViewer.setImageReadyListener(() -> {
				root.post(() -> UIData.mainProgressBar.setVisibility(View.GONE));
				currentViewer.setImageReadyListener(null);
			});
			
			processIntent(getIntent());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void processIntent(Intent intent) {
		Uri uri = intent.getData();
		if(uri!=null) {
			String path = uri.getPath();
			if (path != null) {
				currentViewer.setDocumentPath(path);
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
					ActivityManager.TaskDescription taskDesc = new ActivityManager.TaskDescription(
							new File(path).getName(),//title
							BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),//图标
							ResourcesCompat.getColor(getResources(), R.color.colorPrimary,
									getTheme()));
					setTaskDescription(taskDesc);
				}
			}
		} else { //tg
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/Gpu Pro 1.pdf");
			
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/YotaSpec02.pdf"); // √
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/sample.pdf");
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/sig-notes.pdf");
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/sig-notes-new-txt.pdf");
			
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/sig-notes-t.pdf");
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/tmp.pdf");
			//currentViewer.setDocumentPath("/storage/emulated/0/myFolder/sig-notes-new-txt-page0.pdf");
			currentViewer.setDocumentPath("/storage/emulated/0/myFolder/1.pdf");
		}
		
	}
	
	private void closeSplashScreen() {
		root.setAlpha(0);
		ObjectAnimator fadeInContents = ObjectAnimator.ofFloat(root, "alpha", 0, 1);
		fadeInContents.setInterpolator(new AccelerateDecelerateInterpolator());
		fadeInContents.setDuration(350);
		fadeInContents.addListener(new Animator.AnimatorListener() {
			@Override public void onAnimationStart(Animator animation) { }
			@Override public void onAnimationEnd(Animator animation) {
				getWindow().setBackgroundDrawable(null);
			}
			@Override public void onAnimationCancel(Animator animation) { }
			@Override public void onAnimationRepeat(Animator animation) { }
		});
		root.post(fadeInContents::start);
	}
	
	
	@SuppressLint("NonConstantResourceId")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.browser_widget10: {
				currentViewer.pdoc.prepareBookMarks();
				BookMarksFragment bmks = new BookMarksFragment();
				bmks.width=(int) (dm.widthPixels-2*getResources().getDimension(R.dimen.diagMarginHor));
				bmks.mMaxH=(int) (dm.heightPixels-2*getResources().getDimension(R.dimen.diagMarginVer));
				bmks.height=-2;
				bmks.show(getSupportFragmentManager(), "bkmks");
			} break;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(hidingContextMenu) {
			currentViewer.showContextMenuView();
			hidingContextMenu=false;
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(PDocument.SavingScheme==PDocument.SavingScheme_AlwaysSaveOnPause) {
			currentViewer.checkDoc(false, true);
		}
		if(hidingContextMenu) {
			currentViewer.hideContextMenuView();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(PDocument.SavingScheme==PDocument.SavingScheme_SaveOnClose) {
			currentViewer.checkDoc(false, false);
		}
		currentViewer.setDocumentPath(null);
	}
	
	@SuppressLint("NonConstantResourceId")
	public void OnMenuClicked(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ctx_copy:{
				String text = getSelection();
				if(text!=null) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
					clipboard.setPrimaryClip(ClipData.newPlainText("POLYM", text));
					showT("已复制！");
					currentViewer.clearSelection();
				}
			} break;
			case R.id.ctx_hightlight:{
				currentViewer.highlightSelection();
			} break;
			case R.id.ctx_enlarge:{
				currentViewer.enlargeSelection();
			} break;
			case R.id.ctx_share:{
				shareUrlOrText(getSelection());
			} break;
			case R.id.ctx_dictionay:{
				if(currentViewer.shouldDrawSelection()) {
					Intent intent = new Intent("colordict.intent.action.SEARCH");
					intent.putExtra("EXTRA_QUERY", getSelection());
					hidingContextMenu=true;
					startActivity(intent);
				}
			} break;
			case R.id.ctx_translation:{
				if(currentViewer.shouldDrawSelection()) {
					boolean processText = true;
					String Action=processText?Intent.ACTION_PROCESS_TEXT:Intent.ACTION_SEND;
					String Extra=processText?Intent.EXTRA_PROCESS_TEXT:Intent.EXTRA_TEXT;
					Intent intent = new Intent(Action);
					intent.setType("text/plain");
					try {
						intent.setPackage(GoogleTranslate);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(Extra, getSelection());
						startActivity(intent);
					} catch (Exception e) {
						showT(R.string.gt_no_inst);
					}
				}
			} break;
		}
	}
	
	private String getSelection() {
		String ret = currentViewer.getSelection();
		if(true && ret!=null) {
			ret = ret.replace("\r\n", " ");
		}
		return ret;
	}
	
	private void shareUrlOrText(String selection) {
		//CMN.Log("menu_icon6menu_icon6");
		//CMN.rt("分享链接……");
		int id = WeakReferenceHelper.share_dialog;
		BottomSheetDialog dlg = (BottomSheetDialog) getReferencedObject(id);
		if(dlg==null) {
			putReferencedObject(id, dlg=new AppIconsAdapter(this).shareDialog);
		}
		//CMN.pt("新建耗时：");
		AppIconsAdapter shareAdapter = (AppIconsAdapter) dlg.tag;
		shareAdapter.pullAvailableApps(this, null, selection);
		//CMN.pt("拉取耗时：");
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if(intent.getData()!=null) {
			showT("新的来啦！");
		}
	}
}