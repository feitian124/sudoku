package com.yunnuy.sudoku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class Sudoku extends Activity implements OnClickListener {
   private static final String TAG = "Sudoku";
   
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      // Set up click listeners for all the buttons
      View continueButton = findViewById(R.id.continue_button);
      continueButton.setOnClickListener(this);
      View newButton = findViewById(R.id.new_button);
      newButton.setOnClickListener(this);
      View settingsButton = findViewById(R.id.settings_button);
      settingsButton.setOnClickListener(this);
      View aboutButton = findViewById(R.id.about_button);
      aboutButton.setOnClickListener(this);
      View exitButton = findViewById(R.id.exit_button);
      exitButton.setOnClickListener(this);
   }

   @Override
   protected void onResume() {
      super.onResume();
      Music.play(this, R.raw.main);
   }

   @Override
   protected void onPause() {
      super.onPause();
      Music.stop(this);
   }

   public void onClick(View v) {
      switch (v.getId()) {
      case R.id.continue_button:
         startGame(Game.DIFFICULTY_CONTINUE);
         break;
      case R.id.about_button:
         Intent i = new Intent(this, About.class);
         startActivity(i);
         break;
      case R.id.new_button:
    	  startGame(Prefs.getDifficluty(this));
         break;
      case R.id.settings_button:
    	  startActivity(new Intent(this, Prefs.class));
          break;
      case R.id.exit_button:
         finish();
         break;
      }
   }

   /** Start a new game with the given difficulty level */
   private void startGame(int difficulty) {
      Log.d(TAG, "start Game, diffculty:" + difficulty);
      Intent intent = new Intent(this, Game.class);
      intent.putExtra(Game.KEY_DIFFICULTY, difficulty);
      startActivity(intent);
   }
}