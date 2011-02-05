package com.chanshuikay.otousan;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class LiveWallpaperService extends WallpaperService {
 

	@Override
    public Engine onCreateEngine() {
        return new OtousanEngine();
    }
 
    @Override
    public void onCreate() {
        super.onCreate();
    }
 
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    /**
     * Engine class
     */
    public class OtousanEngine extends Engine implements OnCompletionListener, Runnable {
    	
    	private static final int EPSILON = 50;
    	
    	private static final int TOP_LEFT = 0;
    	private static final int TOP_RIGHT = 1;
    	private static final int BOTTOM_LEFT = 2;
    	private static final int BOTTOM_RIGHT = 3;
    	
    	private final int[] lookingImagesResIds = {R.drawable.image04_1, R.drawable.image05_1, R.drawable.image04_2, R.drawable.image05_2 };    	
    	private final int[] talkingImages = {R.drawable.image01, R.drawable.image02_1, R.drawable.image02_2, R.drawable.image02_3, R.drawable.image03_1, R.drawable.image03_2};
    	private final int[] quotes = {R.raw.sound0, R.raw.sound1,R.raw.sound2,R.raw.sound3,R.raw.sound4,R.raw.sound5,R.raw.sound6,R.raw.sound7,R.raw.sound8,R.raw.sound9};
    	private final Bitmap[] imageCache = new Bitmap[talkingImages.length];
    	private final Bitmap[] lookingImagesCache = new Bitmap[lookingImagesResIds.length];
    	private final MediaPlayer[] quoteCache = new MediaPlayer[quotes.length];
    	
    	private int currentImage = 0;
    	private boolean talking = false;
    	private float touchDownX = -1;
    	private float touchDownY = -1;
    	private final Random quoteGenerator = new Random(System.currentTimeMillis());
		private boolean visible;
		private Rect screenRect;
    	
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
    		drawImage(loadBitmap());
    		screenRect = surfaceHolder.getSurfaceFrame();
        }
        
        @Override
        public void onVisibilityChanged(boolean visible) {
        	this.visible = visible;
        	if( visible ) {
        		drawImage(loadBitmap());
        	}
        }
        
        @Override
        public void onDestroy() {
        	for( Bitmap b : imageCache ) {
        		if (b != null ) {
        			b.recycle();
        		}
        	}
        	
        	for( MediaPlayer m : quoteCache ) {
        		if( m!=null ){
        			m.release();
        		}
        	}
        }
        
        public void onTouchEvent(MotionEvent event) {
        	Log.i("OTOUSAN", "Mouse event received " + event);
        	if( event.getAction() == MotionEvent.ACTION_DOWN) {
        		touchDownX = event.getX();
        		touchDownY = event.getY();
        		Log.d("OTOUSAN", "DOWN touchX=" + touchDownX + ", touchY=" + touchDownY);
        	}
        	
        	else if( event.getAction() == MotionEvent.ACTION_UP) {
        		Log.d("OTOUSAN", "UP touchX=" + event.getX() + ", touchY=" +  event.getY());
        		if( visible && !isSilentOrVibrateMode() && almostTheSame(touchDownX,event.getX()) && almostTheSame(touchDownY,event.getY()) ) {
        			otousanSpeaks();
        		}
        	}
        	else if ( event.getAction() == MotionEvent.ACTION_MOVE ) {
        		if( aroundCentre(event) ){
        			drawImage(loadBitmap());
        		}
        		else if( inTopLHS(event) ) {
        			drawImage(loadLookingBitmap(TOP_LEFT));
        		} 
        		else if ( inTopRHS(event)) {
        			drawImage(loadLookingBitmap(TOP_RIGHT));
        		}
        		else if ( inBottomLHS(event)) {
        			drawImage(loadLookingBitmap(BOTTOM_LEFT));
        		}
        		else if ( inBottomRHS(event)) {
        			drawImage(loadLookingBitmap(BOTTOM_RIGHT));
        		}
        	}
        }
        
        private boolean aroundCentre(MotionEvent event) {
			return (event.getX() > (screenRect.centerX() - EPSILON)) &&
			(event.getX() < (screenRect.centerX() + EPSILON)) &&
			(event.getY() > (screenRect.centerY() - EPSILON)) &&
			(event.getY() < (screenRect.centerY() + EPSILON)); 
		}

		private boolean inBottomRHS(MotionEvent event) {
        	return event.getX() > screenRect.centerX() && event.getY() > screenRect.centerY();
		}

		private boolean inBottomLHS(MotionEvent event) {
        	return event.getX() < screenRect.centerX() && event.getY() > screenRect.centerY();
		}

		private boolean inTopRHS(MotionEvent event) {
        	return event.getX() > screenRect.centerX() && event.getY() < screenRect.centerY();
		}

		private boolean inTopLHS(MotionEvent event) {
			return event.getX() < screenRect.centerX() && event.getY() < screenRect.centerY();
		}

		private boolean almostTheSame(float f1, float f2) {
        	return Math.abs(f1 - f2) < EPSILON;
        }
        
        private boolean isSilentOrVibrateMode() {
        	AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        	return AudioManager.RINGER_MODE_NORMAL != am.getRingerMode();
        }
        
        private void otousanSpeaks() {
        	if( talking ) {
        		return;
        	} 
        	else {
        		talking = true;
        	}
        	
        	MediaPlayer mp = loadRandomQuote();
        	mp.setOnCompletionListener(this);
            mp.start();
            
            // start the talking animation
            new Thread(this).start();

    	}

		private MediaPlayer loadRandomQuote() {
			int quoteId = randomQuote();
			if( quoteCache[quoteId] == null ) {
				quoteCache[quoteId] = MediaPlayer.create(getApplicationContext(), quotes[quoteId]);
			}
			return quoteCache[quoteId];
		}
        
        private int randomQuote() {
			return quoteGenerator.nextInt(10);
		}

		public void run() {
            while(talking) {
            	currentImage = ++currentImage % talkingImages.length;
            	drawImage(loadBitmap());
            	Thread.yield();
            	try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
            }
        }

		private Bitmap loadBitmap() {
			if( imageCache[currentImage] == null ) {
				imageCache[currentImage] = BitmapFactory.decodeResource(getResources(), talkingImages[currentImage]);
			}
			return imageCache[currentImage];
		}
		
		private Bitmap loadLookingBitmap(int direction) {
			if( lookingImagesCache[direction] == null ) {
				lookingImagesCache[direction] = BitmapFactory.decodeResource(getResources(), lookingImagesResIds[direction]);
			}
			return lookingImagesCache[direction];
		}

		@Override
		public void onCompletion(MediaPlayer paramMediaPlayer) {
			talking = false;
//			paramMediaPlayer.release();
		}
        	
        

		private void drawImage(Bitmap imageToDraw) {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
//                	Bitmap background = BitmapFactory.decodeResource(getResources(), resourceId);
                	c.drawBitmap(imageToDraw, 0, 0, null);
//                	background.recycle();
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }
		}
        
    }
}
