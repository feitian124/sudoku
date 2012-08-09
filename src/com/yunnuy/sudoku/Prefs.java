package com.yunnuy.sudoku;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Prefs extends PreferenceActivity implements
	OnSharedPreferenceChangeListener {
   // Option names and default values
   private static final String OPT_MUSIC = "music";
   private static final boolean OPT_MUSIC_DEF = true;
   private static final String OPT_HINTS = "hints";
   private static final boolean OPT_HINTS_DEF = true;
   private static final String OPT_DIFFICLUTY = "difficulty";
   private static SharedPreferences settings;
   private ListPreference diffculty;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings);
      
      diffculty = (ListPreference) findPreference(OPT_DIFFICLUTY);
      setDifficultySummer();
      
      //注册监听器，使得ListPreference可以响应onSharedPreferenceChanged函数
      settings = getPreferenceManager().getSharedPreferences();
      settings.registerOnSharedPreferenceChangeListener(this);
   }

   /** Get the current value of the music option */
   
   public static boolean getMusic(Context context) {
      return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(OPT_MUSIC, OPT_MUSIC_DEF);
   }
   
   
   /** Get the current value of the hints option */
   public static boolean getHints(Context context) {
      return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(OPT_HINTS, OPT_HINTS_DEF);
   }
   
   public static int getDifficluty(Context context){
	   String val = PreferenceManager.getDefaultSharedPreferences(context)
	   		.getString(OPT_DIFFICLUTY, "0");
	   return Integer.valueOf(val);
   }
   
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
           String key) {
		if (OPT_DIFFICLUTY.equals(key) ){
			setDifficultySummer();
		} 
	}
   
   //根据当前难度设置summer
	private void setDifficultySummer() {
		String prefix = getResources().getString(R.string.diff_sum_prefix);
		CharSequence[] entries = diffculty.getEntries();
		String val = diffculty.getValue(); // difficulty is 0,1,2...
		int index = (null == val) ? 0 : Integer.valueOf(val);
		String sum = prefix + " " + entries[index];
		diffculty.setSummary(sum);
	}

}
