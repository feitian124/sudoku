package com.yunnuy.sudoku;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import net.youmi.android.AdManager;
import net.youmi.android.AdView;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Game extends Activity implements OnClickListener{

	public static final String KEY_DIFFICULTY = "com.yunnuy.sudoku.difficulty";
	public static final int DIFFICULTY_EASY = 0;
	public static final int DIFFICULTY_NORMAL = 1;
	public static final int DIFFICULTY_MEDIUM = 2;
	public static final int DIFFICULTY_HARD = 3;
	public static final int DIFFICULTY_VERY_HARD = 4;
	public static final int DIFFICULTY_CONTINUE = -1;
	public static final int REQ_GET_SAVED = 0;
	public static String[] difficulties;
	public static int[] difficulty_values;
	public static final String PREF_PUZZLE = "puzzle";
	public static final String PREF_SAVED_PUZZLE = "saved_puzzles";
	private static final int NAME_ENTRY = 0;
	private static final String TAG = "Sudoku";

	private SharedPreferences puzzles_preferences;
	private Stack<Tile> undoStack;
	private Stack<Tile> redoStack;
	private int puzzle[];
	private int puzzle_original[];

	private static final String DEFAULT_PUZZLE = "360000000004230800000004200"
			+ "070460003820000014500013020" + "001900000007048300000000045";

	private PuzzleView puzzleView;
	private Button load_btn;
	private Button save_btn;
	private Button undo_btn;
	private Button redo_btn;
	private TextView game_info;
	/** ��ʱ�� */
	private Chronometer chronometer;
	private int current_difficulty = 0;
	
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate");

      setContentView(R.layout.play);
      puzzleView = (PuzzleView)findViewById(R.id.play);
      puzzleView.requestFocus();
      
      difficulties = getResources().getStringArray(R.array.difficulty);
      difficulty_values = getResources().getIntArray(R.array.difficulty_value);
      
      load_btn = (Button)findViewById(R.id.load_btn);
      save_btn = (Button)findViewById(R.id.save_btn);
      undo_btn = (Button)findViewById(R.id.undo_btn);
      redo_btn = (Button)findViewById(R.id.redo_btn);
      save_btn.setOnClickListener(this);
      load_btn.setOnClickListener(this);
      undo_btn.setOnClickListener(this);
      redo_btn.setOnClickListener(this);
      
      game_info = (TextView)findViewById(R.id.game_info);
      game_info.setText(difficulties[Prefs.getDifficluty(this)]);
      chronometer = (Chronometer)findViewById(R.id.chronometer);
      chronometer.start();
      
      undoStack = new Stack<Tile>();
  	  redoStack = new Stack<Tile>();
      puzzles_preferences = getSharedPreferences(Game.PREF_SAVED_PUZZLE, MODE_PRIVATE);
      
      int diff = getIntent().getIntExtra(KEY_DIFFICULTY,DIFFICULTY_CONTINUE);
      puzzle = getPuzzle(diff);
      puzzle_original = puzzle.clone();
      calculateUsedTiles();
      
      // If the activity is restarted, do a continue next chronometer
      getIntent().putExtra(KEY_DIFFICULTY, DIFFICULTY_CONTINUE);
      
     
      int width = chronometer.getWidth() + game_info.getWidth();
      
      AdManager.init(this, "d05bf29da9d6f9fd", "a08fe64e01135cb9", 31, false);
      AdView adView = new AdView(this,Color.GRAY, Color.WHITE, 100);
      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width,
      FrameLayout.LayoutParams.WRAP_CONTENT);
      params.bottomMargin=0;
      params.rightMargin=0;
      params.gravity=Gravity.TOP|Gravity.RIGHT;
      LinearLayout adViewLayout = (LinearLayout) findViewById(R.id.adViewLayout);
		adViewLayout.addView(adView, params);
//      addContentView(adView, params);
   }

   @Override
   protected void onResume() {
      super.onResume();
      Music.play(this, R.raw.game);
   }

   @Override
   protected void onPause() {
      super.onPause();
      Log.d(TAG, "onPause");
      Music.stop(this);

      // Save the current puzzle
      String willContinue = toPuzzleString(puzzle) 
      					  + PuzzleManager.SPLIT 
      					  + String.valueOf(current_difficulty)
      					  + PuzzleManager.SPLIT 
      					  +chronometer.getText().toString();
      getPreferences(MODE_PRIVATE).edit().putString(PREF_PUZZLE,
            willContinue).commit();
   }
   

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.undo_btn:
			undo();
			break;
		case R.id.redo_btn:
			redo();
			break;
		case R.id.save_btn:
			showDialog(NAME_ENTRY);
			break;
		case R.id.load_btn:
			startActivityForResult(new Intent(Game.this, PuzzleManager.class), REQ_GET_SAVED);
			break;
		}
		calculateUsedTiles();
		puzzleView.invalidate();
	}

	//ֻ��dialog��һ�δ���ʱ���ã���������޸�dialog��ֵ������onPrepareDialog
	protected Dialog onCreateDialog(int id){
		switch(id){
		case NAME_ENTRY:
			LayoutInflater factory = LayoutInflater.from(this);
	        final View textEntryView = factory.inflate(R.layout.save_name_entry, null);
	        final EditText et = (EditText)textEntryView.findViewById(R.id.save_name_edit);
	        et.setText(getFileName());
	        et.selectAll(); //�����ı�Ϊȫѡ�������û�ɾ��
	        return new AlertDialog.Builder(Game.this)
	            .setIcon(android.R.drawable.ic_dialog_alert)
	            .setTitle(R.string.save_name_view_Title)
	            .setView(textEntryView)
	            .setPositiveButton(R.string.save_name_ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	savePuzzle(puzzles_preferences,
	                			et.getText().toString(), 
	                			toPuzzleString(puzzle), 
	                			String.valueOf(current_difficulty), 
	                			chronometer.getText().toString());
	                }
	            })
	            .setNegativeButton(R.string.save_name_cancel, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	
	                    /* User clicked cancel so do some stuff */
	                }
	            })
	            .create();
		}
		return null;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		  switch(id) {
	      case (NAME_ENTRY) :
	         AlertDialog d = (AlertDialog)dialog;
	         EditText t = (EditText)d.findViewById(R.id.save_name_edit);
	         t.setText(getFileName());
	         t.selectAll();
	         break;
	      }
		super.onPrepareDialog(id, dialog);
	}
	
	
   /**
    * This method is called when the sending activity has finished, with the
    * result it supplied.
    * 
    * @param requestCode The original request code as given to
    *                    startActivity().
    * @param resultCode From sending activity as per setResult().
    * @param data From sending activity as per setResult().
    */
   @Override
	protected void onActivityResult(int requestCode, int resultCode,
		Intent data) {
       // You can use the requestCode to select between multiple child
       // activities you may have started.  Here there is only one thing
       // we launch.
       if (requestCode == REQ_GET_SAVED) {

           // This is a standard resultCode that is sent back if the
           // activity doesn't supply an explicit result.  It will also
           // be returned if the activity failed to launch.
			if (resultCode == RESULT_OK) {
				if (data != null) {
					redoStack.clear();
					undoStack.clear();
					String s = data.getAction();
					puzzle = fromPuzzleString(loadPuzzle(s));
					Log.d("onActivityResult",s);
					calculateUsedTiles();
					puzzleView.invalidate();
					Toast.makeText(this, R.string.load_game_success, Toast.LENGTH_SHORT).show();
				}
			}

       }
   }
   
   /** Given a difficulty level, come up with a new puzzle */
   private int[] getPuzzle(int diff) {
	  XmlPuzzle xp = new XmlPuzzle();
      String puz = null;
      if(diff == DIFFICULTY_CONTINUE){
    	  String continued = getPreferences(MODE_PRIVATE).getString(PREF_PUZZLE,DEFAULT_PUZZLE);
    	  puz = loadPuzzle(continued);
      }else{
    	  puz = xp.getRandmoPuzzle(diff);
    	  current_difficulty = diff;
      }
      return fromPuzzleString(puz);
   }

   /** Convert an array into a puzzle string */
   static private String toPuzzleString(int[] puz) {
      StringBuilder buf = new StringBuilder();
      for (int element : puz) {
         buf.append(element);
      }
      return buf.toString();
   }

   /** Convert a puzzle string into an array */
   static protected int[] fromPuzzleString(String string) {
      int[] puz = new int[string.length()];
      for (int i = 0; i < puz.length; i++) {
         puz[i] = string.charAt(i) - '0';
      }
      return puz;
   }

   /** Return the tile at the given coordinates */
   private int getTile(int x, int y) {
      return puzzle[y * 9 + x];
   }

   /** Change the tile at the given coordinates */
   private void setTile(int x, int y, int value) {
      puzzle[y * 9 + x] = value;
   }

   /** Return a string for the tile at the given coordinates */
   protected String getTileString(int x, int y) {
      int v = getTile(x, y);
      if (v == 0)
         return "";
      else
         return String.valueOf(v);
   }

   /** Change the tile only if it's a valid move */
   protected boolean setTileIfValid(int x, int y, int value) {
      int tiles[] = getUsedTiles(x, y);
      if (value != 0) {
         for (int tile : tiles) {
            if (tile == value)
               return false;
         }
      }
      setTile(x, y, value);
      calculateUsedTiles();
      undoStack.push(new Tile(x, y, value));
      return true;
   }

   /** Open the keypad if there are any valid moves */
   protected void showKeypadOrError(int x, int y) {
      int tiles[] = getUsedTiles(x, y);
      if (tiles.length == 9) {
         Toast toast = Toast.makeText(this,
               R.string.no_moves_label, Toast.LENGTH_SHORT);
         toast.setGravity(Gravity.CENTER, 0, 0);
         toast.show();
      } else {
         Log.d(TAG, "showKeypad: used=" + toPuzzleString(tiles));
         Dialog v = new Keypad(this, tiles, puzzleView);
         v.show();
      }
   }

   /** Cache of used tiles */
   private final int used[][][] = new int[9][9][];

   /** Return cached used tiles visible from the given coords */
   protected int[] getUsedTiles(int x, int y) {
      return used[x][y];
   }

   /** Compute the two dimensional array of used tiles 
    * ÿ��puzzle��ֵ�ı䶼��Ҫ���� 
    */
   private void calculateUsedTiles() {
      for (int x = 0; x < 9; x++) {
         for (int y = 0; y < 9; y++) {
            used[x][y] = calculateUsedTiles(x, y);
            // Log.d(TAG, "used[" + x + "][" + y + "] = "
            // + toPuzzleString(used[x][y]));
         }
      }
   }

   /** Compute the used tiles visible from this position */
   private int[] calculateUsedTiles(int x, int y) {
      int c[] = new int[9];
      // horizontal
      for (int i = 0; i < 9; i++) {
         if (i == x)
            continue;
         int t = getTile(i, y);
         if (t != 0)
            c[t - 1] = t;
      }
      // vertical
      for (int i = 0; i < 9; i++) {
         if (i == y)
            continue;
         int t = getTile(x, i);
         if (t != 0)
            c[t - 1] = t;
      }
      // same cell block
      int startx = (x / 3) * 3;
      int starty = (y / 3) * 3;
      for (int i = startx; i < startx + 3; i++) {
         for (int j = starty; j < starty + 3; j++) {
            if (i == x && j == y)
               continue;
            int t = getTile(i, j);
            if (t != 0)
               c[t - 1] = t;
         }
      }
      // compress
      int nused = 0;
      for (int t : c) {
         if (t != 0)
            nused++;
      }
      int c1[] = new int[nused];
      nused = 0;
      for (int t : c) {
         if (t != 0)
            c1[nused++] = t;
      }
      return c1;
   }

	private void redo() {
		if(redoStack.empty()) {
			Toast.makeText(this, R.string.no_redo, Toast.LENGTH_SHORT).show();
			return;
		}
		
		Tile redo = redoStack.pop();	
		puzzleView.select(redo.getX(), redo.getY());
		undoStack.push(redo);
		
		int x = redo.getX(), y = redo.getY(), v=redo.getValue();
		setTile(x, y, v);
	}
		
	private void undo() {
		if(undoStack.empty()) {
			Toast.makeText(this, R.string.no_undo, Toast.LENGTH_SHORT).show();
			return;
		}
		
		Tile undoed = undoStack.pop();	
		puzzleView.select(undoed.getX(), undoed.getY());
		redoStack.push(undoed);
		
		//�ҵ����tile֮ǰ��ֵ
		for(int i = undoStack.size()-1; i>=0; i--){
			Tile t = undoStack.get(i);
			if(t.getX() == undoed.getX()  && t.getY() == undoed.getY()){
				setTile(t.getX(), t.getY(), t.getValue());
				return;
			}
		}
		//���forѭ����û�ҵ�������ʾԭʼ��Ŀ
		int x = undoed.getX(), y = undoed.getY();
		setTile(x, y, puzzle_original[9*y+x]);
	}
	
	private String getFileName(){
		SimpleDateFormat   format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date(System.currentTimeMillis());
        String time = format.format(date);
        return "game_"+time;
	}
	
	/** ��puzzl����*/
	private boolean savePuzzle(SharedPreferences pref, String name, String puzzle, String lvl, String time){
		if(null==name || "".equals(name)){
			Toast.makeText(Game.this, R.string.pls_input_name, Toast.LENGTH_SHORT).show();
			return false;
		}
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(name,puzzle + PuzzleManager.SPLIT + lvl + PuzzleManager.SPLIT + time);
		boolean b = editor.commit();
		if(b){
			Toast.makeText(Game.this, R.string.save_puzzle_success, Toast.LENGTH_SHORT).show();
		}else{
			Toast.makeText(Game.this, R.string.save_puzzle_failure, Toast.LENGTH_SHORT).show();
		}
		return b;
	}
	
	/** ��֮ǰ�����puzzle��load�������ѶȺ�ʱ�䣬����puzzle�ַ� */
	private String loadPuzzle(String saved){
		String[] ss = saved.split(PuzzleManager.SPLIT);
		
		//�����Ѷ�
		current_difficulty = Integer.valueOf(ss[1]);
		game_info.setText(difficulties[current_difficulty]);
		
		//��ss[2]�е�hh:mm:ssת��Ϊ����
		String[] tmp = ss[2].split(":");
		long ms = 0; //����
		if(tmp.length>=3) ms += Long.valueOf(tmp[tmp.length-3])*3600*1000;
		ms += Long.valueOf(tmp[tmp.length-2])*60*1000;
		ms += Long.valueOf(tmp[tmp.length-1])*1000;
		chronometer.setBase(SystemClock.elapsedRealtime()-ms);
		return ss[0];
	}
	

	   //��xml�ļ��ж�ȡ�����Ŀ
	   //�����Դhttp://code.google.com/p/klsudoku/
		class XmlPuzzle {
			public String getRandmoPuzzle(int difficulty) {
				XmlPullParser xpp = null;
				String puzzle = null;
				try {
					switch(difficulty){
					case DIFFICULTY_EASY:
						xpp = getResources().getXml(R.xml.l1);
						break;
					case DIFFICULTY_NORMAL:
						xpp = getResources().getXml(R.xml.l2);
						break;
					case DIFFICULTY_MEDIUM:
						xpp = getResources().getXml(R.xml.l3);
						break;
					case DIFFICULTY_HARD:
						xpp = getResources().getXml(R.xml.l4);
						break;
					default:
						xpp = getResources().getXml(R.xml.l5);
					}
					
					int count = 0;
					int rand = (int)Math.round(Math.random() * 200);
					while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
						if (xpp.getEventType() == XmlPullParser.START_TAG) {
							String tagName = xpp.getName();
							if (tagName.startsWith("KLSUDOKU") && rand == count++) {
								puzzle = xpp.getName().substring(8);
								break;
							}
						}
						xpp.next();
					}
				} catch (Throwable t) {
					Log.e("xmlpuzzle", "constrator", t);
				}
				Log.d("xmlpuzzle", "get puzzle from xml:\n"+puzzle+",length:"+puzzle.length());
				return puzzle;
			}
			
		}
}


/**
 * pojo,��ʾ���(x,y)��������value
 * @author ming_peng
 * @since 2011-5-20
 * @version 0.1
 */
class Tile{
	private int x,y,value;
	public Tile(int x, int y, int value) {
		this.x = x;
		this.y = y;
		this.value = value;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getValue() {
		return value;
	}
}
