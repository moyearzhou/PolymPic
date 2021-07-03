package com.knziha.polymer.webstorage;

import org.adrianwalker.multilinestring.Multiline;

public class WebOptions {
	public final static int StorageSettings=1;
	public final static int BackendSettings=2;
	public final static int ImmersiveSettings=3;
	public final static int TextSettings=4;
	public static long tmpFlag;
//	@Multiline(flagPos=2) public static boolean getForbidLocalStorage(long flag){ flag=flag; throw new RuntimeException(); }
//	@Multiline(flagPos=2) public static boolean toggleForbidLocalStorage(long flag){ flag=flag; throw new IllegalArgumentException(); }
	@Multiline(flagPos=2, shift=1) public static boolean getRecordHistory(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=3, shift=1) public static boolean getUseCookie(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=4, shift=1) public static boolean getUseDomStore(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=5, shift=1) public static boolean getUseDatabase(long flag){ flag=flag; throw new RuntimeException(); }
	
	@Multiline(flagPos=7) public static boolean getPCMode(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=7) public static boolean togglePCMode(){ tmpFlag=tmpFlag; throw new IllegalArgumentException(); }
	@Multiline(flagPos=8, shift=1) public static boolean getEnableJavaScript(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=9) public static boolean getNoAlerts(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=10) public static boolean getNoCORSJump(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=12) public static boolean getNoNetworkImage(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=13) public static boolean getPremature(long flag){ flag=flag; throw new RuntimeException(); }
	
	// 14
	
	@Multiline(flagPos=15, shift=1) public static boolean getImmersiveScrollEnabled(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=15, shift=1) public static void setImmersiveScrollEnabled(boolean val){ tmpFlag=tmpFlag; throw new RuntimeException(); }
	@Multiline(flagPos=16, shift=1) public static boolean getImmersiveScroll_HideTopBar(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=17, shift=0) public static boolean getImmersiveScroll_HideBottomBar(long flag){ flag=flag; throw new RuntimeException(); }
	
	// 21
	
	@Multiline(flagPos=18, shift=1) public static boolean getTextTurboEnabled(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=18, shift=1) public static void setTextTurboEnabled(boolean val){ tmpFlag=tmpFlag; throw new RuntimeException(); }
	@Multiline(flagPos=19, shift=1) public static boolean getForcePageZoomable(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=20, shift=1) public static boolean getForceTextWrap(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=22, shift=1) public static boolean getOverviewMode(long flag){ flag=flag; throw new RuntimeException(); }
	
	@Multiline(flagPos=23, flagSize=9, shift=110) public static int getTextZoom(long flag){ flag=flag; throw new RuntimeException(); }
	@Multiline(flagPos=23, flagSize=9, shift=110) public static void setTextZoom(int val){ tmpFlag=tmpFlag; throw new RuntimeException(); }
	
	
}
