package com.yunnuy.sudoku;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PuzzleManager extends Activity {

	public static final String SPLIT = ",";
	private List<String> puzzles;
	private List<String> names;
	private ListView listView;
	private OnItemClickListener itemClickListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_view);
		listView = (ListView) findViewById(R.id.list_view);

		itemClickListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				String s = (String) parent.getItemAtPosition(position);
				setResult(RESULT_OK,
						(new Intent(PuzzleManager.this, Game.class))
								.setAction(s));
				finish();
			}
		};
		
		puzzles = new LinkedList<String>();
		names = new LinkedList<String>();
		getPuzzleInfo();
		listView.setAdapter(new PuzzleAdapter());
		listView.setOnItemClickListener(itemClickListener);
		registerForContextMenu(listView);
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		menu.setHeaderTitle(R.string.menu_head_title);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	  AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	  switch (item.getItemId()) {
	  case R.id.cancel:
	    deletePuzzle(info.position);
	    listView.setAdapter(new PuzzleAdapter());
	    return true;
	  default:
	    return super.onContextItemSelected(item);
	  }
	}
	
	private void deletePuzzle(int position){
		String name = names.get(position);
		SharedPreferences pref = getSharedPreferences(Game.PREF_SAVED_PUZZLE,
				MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(name).commit();
		names.remove(position);
		puzzles.remove(position);
		Log.d("deletePuzzle", name);
	}
	
	
	private void getPuzzleInfo() {
		SharedPreferences pref = getSharedPreferences(Game.PREF_SAVED_PUZZLE,
				MODE_PRIVATE);
		Iterator iterator = pref.getAll().entrySet().iterator();
		while (iterator.hasNext()) {
			Entry entry = (Entry) iterator.next();
			puzzles.add((String) entry.getValue());
			names.add((String) entry.getKey());
		}
		if(puzzles.size()==0){
			Toast.makeText(this, R.string.no_saved_puzzle, Toast.LENGTH_LONG).show();
		}
	}
	
	
	/**
	 * 
	 * @author ming_peng
	 * @date 2011-5-22
	 */
	public class PuzzleAdapter extends BaseAdapter {
		
		public int getCount() {
			return puzzles.size();
		}

		public String getItem(int position) {
			return puzzles.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			String[] ss = puzzles.get(position).split(SPLIT);
			
			View itemView = getLayoutInflater().inflate(R.layout.list_item, null);
			ImageView imageView = (ImageView)itemView.findViewById(R.id.item_image);
			imageView.setImageResource(R.drawable.puzzle);
			
			TextView name  = (TextView)itemView.findViewById(R.id.item_name);
			name.setText(names.get(position));
			
			TextView size  = (TextView)itemView.findViewById(R.id.item_size);
			size.setText(Game.difficulties[Integer.valueOf(ss[1])] + "    " + ss[2]);
		
			return itemView;
		}
	}
}