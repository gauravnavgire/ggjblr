package com.example.sidescroll;

import java.io.IOException;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicFactory;
import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.Engine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.IEntityMatcher;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.PathModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.modifier.PathModifier.IPathModifierListener;
import org.andengine.entity.modifier.PathModifier.Path;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.view.RenderSurfaceView;
import org.andengine.ui.activity.LayoutGameActivity;
import org.andengine.util.debug.Debug;

import android.opengl.GLES20;
import android.provider.Settings.System;
import android.view.KeyEvent;
import android.widget.Toast;

public class MainActivity extends LayoutGameActivity implements
		IOnSceneTouchListener {

	// Scene properties
	public enum SceneType {
		SPLASH, MENU, OPTIONS, GAME,
	}

	public SceneType currentScene = SceneType.SPLASH;

	// Camera
	private static final int CAMERA_WIDTH = 800;
	private static final int CAMERA_HEIGHT = 480;
	private Camera mCamera;

	// Scenes
	// 1. Splash Scene
	private Scene mSplashScene;
	private BitmapTextureAtlas mSplashTextureAtlas;
	private ITextureRegion mSplashTextureRegion;
	private Sprite mSplash;

	// 2. Menu Scene
	private Scene mMenuScene;

	// 3. Options Scene
	private Scene mOptionsScene;

	// 4. Game Scene
	private Scene mGameScene;
	private AnimatedSprite mPlayer;
	private AnimatedSprite mPet;

	// 5. Game Analog control
	private BitmapTextureAtlas mOnScreenControlTexture;
	private ITextureRegion mOnScreenControlBaseTextureRegion;
	private ITextureRegion mOnScreenControlKnobTextureRegion;

	private BuildableBitmapTextureAtlas mBitmapTextureAtlas;
	private BitmapTextureAtlas mPetTextureAtlas;

	private TiledTextureRegion mPlayerTextureRegion;
	private TiledTextureRegion mPetTextureRegion;
	private RepeatingSpriteBackground mGrassBackground;

	// 6. Game Elements
	private GameUpdateHandler mGameUpdateHandler;
	private Sound mySound;
	private Music myMusic;

	public float pLeftVolume = 1;

	public float pRightVolume = 1;

	@Override
	public EngineOptions onCreateEngineOptions() {
		mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(
						CAMERA_WIDTH, CAMERA_HEIGHT), mCamera);
		engineOptions.getAudioOptions().setNeedsMusic(true);
		engineOptions.getAudioOptions().setNeedsSound(true);
		return engineOptions;
	}

	@Override
	public void onCreateResources(
			OnCreateResourcesCallback pOnCreateResourcesCallback)
			throws Exception {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		SoundFactory.setAssetBasePath("mfx/");
		MusicFactory.setAssetBasePath("mfx/");
		loadSplashSceneResources();
		loadGameSceneResources();
		loadAudio();
		pOnCreateResourcesCallback.onCreateResourcesFinished();
	}

	private void loadAudio() {
		try {
			this.myMusic = MusicFactory.createMusicFromAsset(
					this.mEngine.getMusicManager(), this, "heartbeat.ogg");
			this.myMusic.setLooping(true);
		} catch (final IOException e) {
			Debug.e(e);
		}

	}

	private void loadGameSceneResources() {
		this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(
				this.getTextureManager(), 512, 512, TextureOptions.NEAREST);
		this.mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(this.mBitmapTextureAtlas, this,
						"player.png", 4, 4);

		try {
			this.mBitmapTextureAtlas
					.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(
							0, 0, 1));
			this.mBitmapTextureAtlas.load();
		} catch (TextureAtlasBuilderException e) {
			Debug.e(e);
		}
		// Pet resources
		this.mPetTextureAtlas = new BitmapTextureAtlas(
				this.getTextureManager(), 150, 200);
		this.mPetTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(this.mPetTextureAtlas, this, "pet.png",
						0, 0, 3, 4);
		this.mGrassBackground = new RepeatingSpriteBackground(CAMERA_WIDTH,
				CAMERA_HEIGHT, this.getTextureManager(),
				AssetBitmapTextureAtlasSource.create(this.getAssets(),
						"gfx/background_grass.png"),
				this.getVertexBufferObjectManager());
		this.mPetTextureAtlas.load();
		this.mBitmapTextureAtlas.load();

		// Analog control resources
		this.mOnScreenControlTexture = new BitmapTextureAtlas(
				this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mOnScreenControlTexture, this,
						"onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mOnScreenControlTexture, this,
						"onscreen_control_knob.png", 128, 0);
		this.mOnScreenControlTexture.load();
	}

	@Override
	public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
			throws Exception {
		initSplashScreen();
		pOnCreateSceneCallback.onCreateSceneFinished(this.mSplashScene);
	}

	@Override
	public void onPopulateScene(Scene pScene,
			OnPopulateSceneCallback pOnPopulateSceneCallback) throws Exception {
		mEngine.registerUpdateHandler(new TimerHandler(4f,
				new ITimerCallback() {

					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {
						mEngine.unregisterUpdateHandler(pTimerHandler);
						mSplash.detachSelf();
						setGameScene();
					}
				}));
		pOnPopulateSceneCallback.onPopulateSceneFinished();

	}

	protected void setGameScene() {
		mGameScene = new Scene();
		mGameScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
		mPlayer = new AnimatedSprite(250, 220, mPlayerTextureRegion,
				this.getVertexBufferObjectManager());
		long[] eachFrmTime = { 200, 200, 200, 200 };
		int[] eachFrm = { 8, 9, 10, 11 };
		mPlayer.animate(eachFrmTime, eachFrm);

		mGameScene.attachChild(mPlayer);
		final PhysicsHandler physicsHandler = new PhysicsHandler(mPlayer);
		mPlayer.registerUpdateHandler(physicsHandler);

		// Pet and Background
		mGameScene.setBackground(this.mGrassBackground);

		/*
		 * Calculate the coordinates for the face, so its centered on the
		 * camera.
		 */
		final float centerX = (CAMERA_WIDTH - this.mPetTextureRegion.getWidth()) / 2;
		final float centerY = (CAMERA_HEIGHT - this.mPetTextureRegion
				.getHeight()) / 2;

		/* Create the sprite and add it to the scene. */
		mPet = new AnimatedSprite(centerX, centerY, 24, 32,
				this.mPetTextureRegion, this.getVertexBufferObjectManager());

		final Path path = new Path(5).to(10, 10).to(10, CAMERA_HEIGHT - 74)
				.to(CAMERA_WIDTH - 58, CAMERA_HEIGHT - 74)
				.to(CAMERA_WIDTH - 58, 10).to(10, 10);

		final Path path2 = new Path(5).to(10, 10).to(10, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH/2 - 58, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH - 58, 10).to(10, 10);
		
		final Path path3 = new Path(5).to(10, 10).to(10, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH - 58, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH/2 - 58, 10).to(10, 10);
		
		final Path path4 = new Path(5).to(10, 10).to(10, CAMERA_HEIGHT/2 - 74)
		.to(CAMERA_WIDTH - 58, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH - 58, 10).to(10, 10);
		
		final Path path5 = new Path(5).to(10, 10).to(10, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH - 58, CAMERA_HEIGHT/2 - 74)
		.to(CAMERA_WIDTH - 58, 10).to(10, 10);
		
		final Path path6 = new Path(5).to(10, 10).to(10, CAMERA_HEIGHT - 74)
		.to(CAMERA_WIDTH - 58, CAMERA_HEIGHT/2 - 74)
		.to(CAMERA_WIDTH/2 - 58, 10).to(10, 10);
		
		mPet.registerEntityModifier(new LoopEntityModifier(new PathModifier(30,
				path6, null, new IPathModifierListener() {
					@Override
					public void onPathStarted(final PathModifier pPathModifier,
							final IEntity pEntity) {

					}

					@Override
					public void onPathWaypointStarted(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {
						switch (pWaypointIndex) {
						case 0:
							mPet.animate(new long[] { 200, 200, 200 }, 0, 2,
									true);
							break;
						case 1:
							mPet.animate(new long[] { 200, 200, 200 }, 3, 5,
									true);
							break;
						case 2:
							mPet.animate(new long[] { 200, 200, 200 }, 6, 8,
									true);
							break;
						case 3:
							mPet.animate(new long[] { 200, 200, 200 }, 9, 11,
									true);
							break;
						}
					}

					@Override
					public void onPathWaypointFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {

					}

					@Override
					public void onPathFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity) {

					}
				})));
		mGameScene.attachChild(mPet);

		// Analog control
		final AnalogOnScreenControl analogOnScreenControl = new AnalogOnScreenControl(
				0, CAMERA_HEIGHT
						- this.mOnScreenControlBaseTextureRegion.getHeight(),
				this.mCamera, this.mOnScreenControlBaseTextureRegion,
				this.mOnScreenControlKnobTextureRegion, 0.1f, 200,
				this.getVertexBufferObjectManager(),
				new IAnalogOnScreenControlListener() {
					@Override
					public void onControlChange(
							final BaseOnScreenControl pBaseOnScreenControl,
							final float pValueX, final float pValueY) {
						physicsHandler
								.setVelocity(pValueX * 100, pValueY * 100);
					}

					@Override
					public void onControlClick(
							final AnalogOnScreenControl pAnalogOnScreenControl) {
						mPlayer.registerEntityModifier(new SequenceEntityModifier(
								new ScaleModifier(0.25f, 1, 1.5f),
								new ScaleModifier(0.25f, 1.5f, 1)));
					}
				});
		analogOnScreenControl.getControlBase().setBlendFunction(
				GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		analogOnScreenControl.getControlBase().setAlpha(0.5f);
		analogOnScreenControl.getControlBase().setScaleCenter(0, 128);
		analogOnScreenControl.getControlBase().setScale(1.25f);
		analogOnScreenControl.getControlKnob().setScale(1.25f);
		analogOnScreenControl.refreshControlKnobPosition();

		mGameScene.setChildScene(analogOnScreenControl);
		mGameUpdateHandler = new GameUpdateHandler();
		mGameScene.registerUpdateHandler(mGameUpdateHandler);

		mEngine.setScene(mGameScene);
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this,
						"Its dark our there! You have save your Pet.",
						Toast.LENGTH_LONG).show();
			}
		});

		// start music
		this.myMusic.play();
		this.myMusic.setVolume(0.5f, 1.0f);
	}

	@Override
	protected int getLayoutID() {
		// TODO Auto-generated method stub
		return R.layout.activity_main;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		// TODO Auto-generated method stub
		return R.id.xmllayoutRenderSurfaceView;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (currentScene) {
			case OPTIONS:
			case GAME:
				// mEngine.setScene(mMenuScene);
				// currentScene = SceneType.MENU;
				// break;
			case MENU:
			case SPLASH:
				onBackPressed();
			default:
				break;
			}
		}
		return false;
	}

	private void loadSplashSceneResources() {
		// Splash screen
		mSplashTextureAtlas = new BitmapTextureAtlas(getTextureManager(), 350,
				256, TextureOptions.DEFAULT);
		mSplashTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(mSplashTextureAtlas, this, "splash.png", 0, 0);
		mSplashTextureAtlas.load();
	}

	private void initSplashScreen() {
		mSplashScene = new Scene();
		mSplash = new Sprite(0, 0, mSplashTextureRegion,
				getVertexBufferObjectManager()) {
			protected void preDraw(GLState pGLState, Camera pCamera) {
				super.preDraw(pGLState, pCamera);
				pGLState.enableDither();
			};
		};
		mSplash.setScale(1.5f);
		mSplash.setPosition(
				getCenter(CAMERA_WIDTH, mSplashTextureRegion.getWidth()),
				getCenter(CAMERA_HEIGHT, mSplashTextureRegion.getHeight()));
		mSplashScene.attachChild(mSplash);
	}

	public float getCenter(float total, float size) {
		return (total - size) / 2f;
	}

	static int i = 0;

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		if (pSceneTouchEvent.isActionDown()) {
			Sprite s = (Sprite) pScene.getChildByTag(mPlayer.getTag());
			s.setPosition(s.getX() + (i++), s.getY());
		}
		return false;
	}

	private class GameUpdateHandler implements IUpdateHandler {

		@Override
		public void onUpdate(float pSecondsElapsed) {
			if (mPlayer.collidesWith(mPet)) {
				// mGameScene.detachChild(mPet);
			}

			if (mGameScene.getChildByMatcher(new IEntityMatcher() {

				@Override
				public boolean matches(IEntity pEntity) {
					if (pEntity == mPet)
						return true;
					else
						return false;
				}
			}) == null) {
				// showGameOver();
			}

			float balance = (1 + (mPet.getX() - mPlayer.getX())
					/ (CAMERA_WIDTH / 2)) / 2;
			float distance = (float) Math.sqrt((mPet.getX() - mPlayer.getX())
					* (mPet.getX() - mPlayer.getX())
					+ ((mPet.getY() - mPlayer.getY()) * (mPet.getY() - mPlayer
							.getY())));
			if (distance < 200) {
				distance = distance / 200;
				myMusic.setVolume((pLeftVolume * (1 - balance))
						* (1 - distance), (pRightVolume * balance)
						* (1 - distance));
			} else {
				myMusic.setVolume(0, 0);
			}
		}

		@Override
		public void reset() {
			// TODO Auto-generated method stub

		}
	}

	private void showGameOver() {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this,
						"Yaay! You saved your Pet. Now enjoy!",
						Toast.LENGTH_LONG).show();
			}
		});

		// mGameScene.detachChildren();
		// mGameScene.unregisterUpdateHandler(mGameUpdateHandler);
	}

}
