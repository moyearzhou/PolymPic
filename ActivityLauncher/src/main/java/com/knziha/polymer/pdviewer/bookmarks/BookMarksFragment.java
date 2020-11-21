package com.knziha.polymer.pdviewer.bookmarks;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.GlobalOptions;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.knziha.polymer.Utils.CMN;
import com.knziha.polymer.databinding.BookmarksBinding;

import java.util.ArrayList;

public class BookMarksFragment extends DialogFragment {
	private BookmarksBinding bmView;
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if(bmView==null) {
			bmView = BookmarksBinding.inflate(getLayoutInflater(), null, false);
			ArrayList<Fragment> fragments = new ArrayList<>();
			fragments.add(new BookMarkFragment());
			FragAdapter adapterf = new FragAdapter(getChildFragmentManager(), fragments);
			
			ViewPager viewPager = bmView.viewpager;
			TabLayout mTabLayout = bmView.mTabLayout;
			
			viewPager.setAdapter(adapterf);
			
			String[] tabTitle = {"目录"};
			for (String s : tabTitle) {
				mTabLayout.addTab(mTabLayout.newTab().setText(s));
			}
			mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
				@Override
				public void onTabSelected(TabLayout.Tab tab) {
					bmView.viewpager.setCurrentItem(tab.getPosition());
				}
				@Override public void onTabUnselected(TabLayout.Tab tab) {}
				@Override public void onTabReselected(TabLayout.Tab tab) {}
			});
			
			mTabLayout.setSelectedTabIndicatorColor(ColorUtils.blendARGB(CMN.MainBackground, Color.BLACK, 0.28f));
			
			mTabLayout.setSelectedTabIndicatorHeight((int) (3.8* GlobalOptions.density));
			
			viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout) {
				@Override public void onPageSelected(int page) {
					Fragment fI = fragments.get(page);
					//bmView.viewpager.setOffscreenPageLimit(Math.max(bmView.viewpager.getOffscreenPageLimit(), Math.max(1+page, 1)));
					super.onPageSelected(page);
				}
			});
			
			viewPager.setCurrentItem(0);
			
			viewPager.setOffscreenPageLimit(1);
		}
		return bmView.root;
	}
	
	
	
	
}
