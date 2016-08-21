package com.andyken.draggablegridview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.andyken.draggablegridview.views.DraggableGridView;

import java.util.Random;

public class DraggableGridViewSampleActivity extends Activity {
	private static final String TAG = "DraggableGridView";
	static Random random = new Random();
	static String[] words = "the of and a to in is be that was he for it with as his I on have at by not they this had are but from or she an which you one we all were her would there their will when who him been has more if no out do so can what up said about other into than its time only could new them man some these then two first may any like now my such make over our even most me state after also made many did must before back see through way where get much go well your know should down work year because come people just say each those take day good how long Mr own too little use US very great still men here life both between old under last never place same another think house while high right might came off find states since used give against three himself look few general hand school part small American home during number again Mrs around thought went without however govern don't does got public United point end become head once course fact upon need system set every war put form water took".split(" ");
	DraggableGridView dgv;
	Button button1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        dgv = ((DraggableGridView)findViewById(R.id.vgv));
//        dgv.setEnabled(false);
        button1 = ((Button)findViewById(R.id.button1));

        setListeners();


		initViews();

    }

	private void initViews() {


        Bitmap bitmap = getIndicatorThumb("啥啥啥");
        ImageView indicator = new ImageView(DraggableGridViewSampleActivity.this);
        indicator.setImageBitmap(bitmap);
        dgv.addIndicatorView(indicator);

        for (int i=0;i<20;i++){

			String word = words[random.nextInt(words.length)];
			ImageView view = new ImageView(DraggableGridViewSampleActivity.this);
			view.setImageBitmap(getThumb(word));
			dgv.addChildView(view);

		}

	}

	private void setListeners() {
    	dgv.setOnRearrangeListener(new DraggableGridView.OnRearrangeListener() {
			public void onRearrange(int oldIndex, int newIndex) {
				//当一个item位置被改动
				Log.i(TAG,"item "+oldIndex+" was moved to "+newIndex);
			}
		});
    	dgv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				//一个item被点击
				dgv.removeChildViewAt(arg2);
			}
		});
    	button1.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				//点击 button 增加一个item
				String word = words[random.nextInt(words.length)];
				ImageView view = new ImageView(DraggableGridViewSampleActivity.this);
				view.setImageBitmap(getThumb(word));
				dgv.addChildView(view);
			}
		});
    }

    private Bitmap getThumb(String s){
        Bitmap bmp = Bitmap.createBitmap(150, 150, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        paint.setColor(Color.rgb(random.nextInt(128), random.nextInt(128), random.nextInt(128)));
        paint.setTextSize(24);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        canvas.drawRect(new Rect(0, 0, 150, 150), paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(s, 75, 75, paint);

        return bmp;
    }

    private Bitmap getIndicatorThumb(String s){
        Bitmap bmp = Bitmap.createBitmap(150, 150, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        canvas.drawRect(new Rect(0, 0, 150, 150), paint);
        paint.setColor(Color.RED);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(s, 75, 75, paint);

        return bmp;
    }



}