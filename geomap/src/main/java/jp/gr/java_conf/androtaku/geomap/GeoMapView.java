package jp.gr.java_conf.androtaku.geomap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by takuma on 2015/07/18.
 */
public class GeoMapView extends ImageView{
    private List<CountrySection> countrySections;
    private Context context;
    private Paint defaultPaint;
    private Thread prepareThread, thread;
    private HashMap<String, Paint> countryPaints;
    private OnInitializedListener listener;

    public GeoMapView(Context context){
        super(context);
        this.context = context;
        countryPaints = new HashMap<>();
        initialize();
    }
    public GeoMapView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        this.context = context;
        countryPaints = new HashMap<>();
        initialize();
    }

    private void initialize(){
        defaultPaint = new Paint();
        defaultPaint.setColor(Color.BLACK);
        defaultPaint.setStyle(Paint.Style.STROKE);
        defaultPaint.setAntiAlias(true);

        final Handler handler = new Handler();

        prepareThread = new Thread(new Runnable() {
            @Override
            public void run() {
                countrySections = SVGParser.getCountrySections(context);
                final Bitmap bitmap = Bitmap.createBitmap(GeoMapView.this.getWidth(),
                        GeoMapView.this.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawMap(canvas);
                //run on main thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        GeoMapView.this.setImageBitmap(bitmap);
                        listener.onInitialized(GeoMapView.this);
                    }
                });
            }
        });
        prepareThread.start();
    }

    private void drawMap(Canvas canvas){
        float ratio = (float)canvas.getWidth() / SVGParser.xMax;

        for(CountrySection countrySection : countrySections){
            List<List<Float>> xPathList = countrySection.getXPathList();
            List<List<Float>> yPathList = countrySection.getYPathList();
            int numList = xPathList.size();
            for (int i = 0; i < numList; ++i) {
                Path path = new Path();
                path.moveTo(xPathList.get(i).get(0) * ratio, yPathList.get(i).get(0) * ratio);
                int numPoint = xPathList.get(i).size();
                for (int j = 1; j < numPoint; ++j) {
                    path.lineTo(xPathList.get(i).get(j) * ratio, yPathList.get(i).get(j) * ratio);
                }
                Paint paint = countryPaints.get(countrySection.getCountryCode());
                if(paint != null){
                    canvas.drawPath(path, paint);
                }
                canvas.drawPath(path, defaultPaint);
            }
        }
    }

    public void setCountryColor(String countryCode, String color){
        Paint paint = new Paint();
        paint.setColor(Color.parseColor(color));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        countryPaints.put(countryCode, paint);
    }
    public void setCountryColor(String countryCode, int red, int green, int blue){
        Paint paint = new Paint();
        paint.setColor(Color.rgb(red, green, blue));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        countryPaints.put(countryCode, paint);
    }

    public void removeCountryColor(String countryCode){
        countryPaints.remove(countryCode);
    }

    public void refresh(){
        final Handler handler = new Handler();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = Bitmap.createBitmap(GeoMapView.this.getWidth(),
                        GeoMapView.this.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawMap(canvas);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        GeoMapView.this.setImageBitmap(bitmap);
                    }
                });
            }
        });
        thread.start();
    }

    public void destroy(){
        prepareThread = null;
        thread = null;
    }

    public void setOnInitializedListener(OnInitializedListener listener){
        this.listener = listener;
    }
}
