package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher.ViewFactory;

import com.aviary.android.feather.Constants;
import com.aviary.android.feather.FilterManager.FeatherContext;
import com.aviary.android.feather.R;
import com.aviary.android.feather.async_tasks.AsyncImageManager;
import com.aviary.android.feather.async_tasks.AsyncImageManager.OnImageLoadListener;
import com.aviary.android.feather.graphics.PluginDividerDrawable;
import com.aviary.android.feather.graphics.RepeatableHorizontalDrawable;
import com.aviary.android.feather.headless.filters.INativeFilter;
import com.aviary.android.feather.headless.filters.NativeFilter;
import com.aviary.android.feather.headless.moa.Moa;
import com.aviary.android.feather.headless.moa.MoaAction;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaResult;
import com.aviary.android.feather.library.content.FeatherIntent;
import com.aviary.android.feather.library.filters.BorderFilter;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.graphics.drawable.FakeBitmapDrawable;
import com.aviary.android.feather.library.plugins.FeatherExternalPack;
import com.aviary.android.feather.library.plugins.FeatherInternalPack;
import com.aviary.android.feather.library.plugins.FeatherPack;
import com.aviary.android.feather.library.plugins.PluginFactory;
import com.aviary.android.feather.library.plugins.PluginFactory.ExternalPlugin;
import com.aviary.android.feather.library.plugins.PluginFactory.FramePlugin;
import com.aviary.android.feather.library.plugins.PluginFactory.ICDSPlugin;
import com.aviary.android.feather.library.plugins.PluginFactory.IPlugin;
import com.aviary.android.feather.library.plugins.PluginFactory.InternalPlugin;
import com.aviary.android.feather.library.plugins.UpdateType;
import com.aviary.android.feather.library.services.CDSService;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.EffectContext;
import com.aviary.android.feather.library.services.ImageCacheService;
import com.aviary.android.feather.library.services.ImageCacheService.SimpleCachedRemoteBitmap;
import com.aviary.android.feather.library.services.PluginService;
import com.aviary.android.feather.library.services.PluginService.OnUpdateListener;
import com.aviary.android.feather.library.services.PluginService.PluginException;
import com.aviary.android.feather.library.services.PreferenceService;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.ImageLoader;
import com.aviary.android.feather.library.utils.SystemUtils;
import com.aviary.android.feather.library.utils.UserTask;
import com.aviary.android.feather.widget.ArrayAdapterExtended;
import com.aviary.android.feather.widget.EffectThumbLayout;
import com.aviary.android.feather.widget.HorizontalVariableListView;
import com.aviary.android.feather.widget.HorizontalVariableListView.OnItemClickedListener;
import com.aviary.android.feather.widget.IapDialog;
import com.aviary.android.feather.widget.IapDialog.OnCloseListener;
import com.aviary.android.feather.widget.ImageSwitcher;

public class BordersPanel extends AbstractContentPanel implements ViewFactory, OnUpdateListener, OnImageLoadListener, OnScrollChangedListener, OnItemClickedListener, OnItemSelectedListener {

	private static final int TAG_EXTERNAL_VIEW = 4000;
	
	private final int mPluginType;

	/** view flipper for switching between lists */
	private ViewFlipper mViewFlipper;

	/** thumbnail horizontal listview */
	protected HorizontalVariableListView mHList;

	/** Panel is rendering. */
	protected volatile Boolean mIsRendering = false;

	/** Panel is animating */
	private volatile boolean mIsAnimating;

	/** The current rendering task. */
	private RenderTask mCurrentTask;

	protected PluginService mPluginService;
	
	protected CDSService mCDSService;

	protected ImageCacheService mCacheService;

	private PreferenceService mPreferenceService;

	/** The main image switcher. */
	protected ImageSwitcher mImageSwitcher;

	/** external plugins enabled */
	private boolean mExternalPacksEnabled = true;

	/** A reference to the effect applied */
	protected MoaActionList mActions = null;

	protected String mRenderedEffect;

	protected String mRenderedPackName;

	/** create a reference to the update alert dialog. This to prevent multiple alert messages */
	private AlertDialog mUpdateDialog;

	/** default width of each effect thumbnail */
	private int mFilterCellWidth = 80;

	private int mThumbBitmapSize;

	private List<String> mInstalledPackages;

	/** thumbnail cache manager */
	private AsyncImageManager mImageManager;

	/** thumbnail for effects */
	protected Bitmap mThumbBitmap;

	/** current selected position */
	protected int mSelectedPosition = -1;

	/** first position allowed in selection */
	private static int FIRST_POSITION = -1;

	/** total number of available plugins */
	private int mAvailablePacks = 0;

	/** show the first get more button */
	private boolean mShowFirstGetMore = true;

	private boolean mEnableEffectAnimation = false;

	protected Bitmap updateArrowBitmap;

	/** hlist scrolled */
	private boolean mScrollChanged;

	private boolean mConfigurationChanged;

	private boolean mShowIapNotificationAndValue;

	// thumbnail properties
	private static int mRoundedBordersPixelSize = 16;
	private static int mShadowRadiusPixelSize = 4;
	private static int mShadowOffsetPixelSize = 2;
	private static int mRoundedBordersPaddingPixelSize = 5;
	private static int mRoundedBordersStrokePixelSize = 3;
	private static int mItemsGapPixelsSize = 2;
	private int mExternalThumbPadding = 0;

	/** default title for featured divider */
	private String mFeaturedDefaultTitle;

	/** max number of featured elements to display */
	private int mFeaturedCount;

	// don't display the error dialog more than once
	private static boolean mUpdateErrorHandled = false;

	private boolean mFirstTimeRenderer;

	/** options used to decode cached images */
	private static BitmapFactory.Options mThumbnailOptions;

	public BordersPanel( EffectContext context ) {
		this( context, FeatherIntent.PluginType.TYPE_BORDER );
	}

	protected BordersPanel( EffectContext context, int type ) {
		super( context );
		mPluginType = type;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate( Bitmap bitmap ) {
		super.onCreate( bitmap );

		mImageManager = new AsyncImageManager( 1 );

		mThumbnailOptions = new Options();
		mThumbnailOptions.inPreferredConfig = Config.RGB_565;

		mPluginService = getContext().getService( PluginService.class );
		mCDSService = getContext().getService( CDSService.class );
		mCacheService = getContext().getService( ImageCacheService.class );
		mPreferenceService = getContext().getService( PreferenceService.class );

		if ( mPluginType == FeatherIntent.PluginType.TYPE_FILTER )
			mExternalPacksEnabled = Constants.getExternalEffectsEnabled();
		else
			mExternalPacksEnabled = Constants.getExternalFramesEnabled();

		mViewFlipper = (ViewFlipper) getOptionView().findViewById( R.id.flipper );
		mHList = (HorizontalVariableListView) getOptionView().findViewById( R.id.list );

		mPreview = BitmapUtils.copy( mBitmap, Bitmap.Config.ARGB_8888 );

		// ImageView Switcher setup
		mImageSwitcher = (ImageSwitcher) getContentView().findViewById( R.id.switcher );
		initContentImage( mImageSwitcher );

		// Horizontal list setup
		mHList.setOnScrollListener( this );

		View content = getOptionView().findViewById( R.id.background );
		content.setBackgroundDrawable( RepeatableHorizontalDrawable.createFromView( content ) );

		try {
			updateArrowBitmap = BitmapFactory.decodeResource( getContext().getBaseContext().getResources(), R.drawable.feather_update_arrow );
		} catch ( Throwable t ) {}

		mEnableEffectAnimation = Constants.ANDROID_SDK > android.os.Build.VERSION_CODES.GINGERBREAD && SystemUtils.getCpuMhz() >= Constants.MHZ_CPU_FAST;
		// mSelectedPosition = 0;
	}

	@SuppressLint("NewApi")
	@Override
	public void onActivate() {
		super.onActivate();

		ConfigService config = getContext().getService( ConfigService.class );

		// new method, using the panel height dinamically
		mFilterCellWidth = (int) ( ( getOptionView().findViewById( R.id.background ).getHeight() - getOptionView().findViewById( R.id.bottom_background_overlay ).getHeight() ) * 0.9 );
		mThumbBitmapSize = (int) ( mFilterCellWidth * 0.85 );

		// mFilterCellWidth = config.getDimensionPixelSize( R.dimen.feather_effects_cell_width );
		// mFilterCellWidth = (int) ( ( Constants.SCREEN_WIDTH / UIUtils.getScreenOptimalColumnsPixels( mFilterCellWidth ) ) );

		mThumbBitmap = generateThumbnail( mBitmap, mThumbBitmapSize, mThumbBitmapSize );

		mInstalledPackages = Collections.synchronizedList( new ArrayList<String>() );

		mRoundedBordersPixelSize = config.getDimensionPixelSize( R.dimen.feather_effects_panel_thumb_rounded_border );
		mRoundedBordersPaddingPixelSize = config.getDimensionPixelSize( R.dimen.feather_effects_panel_thumb_padding );
		mShadowOffsetPixelSize = config.getDimensionPixelSize( R.dimen.feather_effects_panel_thumb_shadow_offset );
		mShadowRadiusPixelSize = config.getDimensionPixelSize( R.dimen.feather_effects_panel_thumb_shadow_radius );
		mRoundedBordersStrokePixelSize = config.getDimensionPixelSize( R.dimen.feather_effects_panel_thumb_stroke_size );
		mItemsGapPixelsSize = config.getDimensionPixelSize( R.dimen.feather_effects_panel_items_gap );
		mExternalThumbPadding = config.getDimensionPixelSize( R.dimen.feather_effects_external_thumb_padding );

		mFeaturedDefaultTitle = config.getString( R.string.feather_featured );
		mFeaturedCount = config.getInteger( R.integer.feather_featured_count );

		mHList.setGravity( Gravity.BOTTOM );
		mHList.setOverScrollMode( HorizontalVariableListView.OVER_SCROLL_ALWAYS );
		mHList.setEdgeGravityY( Gravity.BOTTOM );
		mHList.setOnItemSelectedListener( this );
		mHList.setOnItemClickedListener( this );

		mImageManager.setOnLoadCompleteListener( this );

		getContentView().setVisibility( View.VISIBLE );
		onPostActivate();
	}

	protected void initContentImage( ImageSwitcher imageView ) {
		if ( null != imageView ) {
			imageView.setFactory( this );
			
			@SuppressWarnings ( "unused" ) Matrix matrix = getContext().getCurrentImageViewMatrix();
			imageView.setImageBitmap( mBitmap, null );
			imageView.setAnimateFirstView( false );
		}
	}

	protected final int getPluginType() {
		return mPluginType;
	}

	private void showUpdateAlert( final CharSequence packageName, final CharSequence label, final int error, String exceptionMessage, boolean fromUseClick ) {
		if ( error != PluginService.ERROR_NONE ) {

			String errorString = getError( error, exceptionMessage );

			if ( error == PluginService.ERROR_PLUGIN_TOO_OLD || error == PluginService.ERROR_PLUGIN_CORRUPTED || error == PluginService.ERROR_DOWNLOAD ) {

				OnClickListener yesListener = new OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						Tracker.recordTag( "PluginNeedsUpdate: " + label );
						getContext().downloadPlugin( (String) packageName, mPluginType );
					}
				};
				
				onGenericMessage( label, errorString, R.string.feather_update, yesListener, android.R.string.cancel, null );

			} else if ( error == PluginService.ERROR_PLUGIN_TOO_NEW ) {
				OnClickListener yesListener = new OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						String pname = getContext().getBaseContext().getPackageName();
						Tracker.recordTag( "EditorNeedsUpdate" );
						getContext().downloadPlugin( pname, mPluginType );
					}
				};
				onGenericMessage( label, errorString, R.string.feather_update, yesListener, android.R.string.cancel, null );
			} else {
				onGenericMessage( label, errorString, android.R.string.ok, null );
			}
			return;
		}
	}

	/**
	 * Create a popup alert dialog when multiple plugins need to be updated
	 * 
	 * @param error
	 */
	private void showUpdateAlertMultiplePlugins( final int error, boolean fromUserClick ) {

		if ( error != PluginService.ERROR_NONE ) {
			final String errorString = getError( error, null );

			if ( error == PluginService.ERROR_PLUGIN_TOO_OLD  || error == PluginService.ERROR_PLUGIN_CORRUPTED || error == PluginService.ERROR_DOWNLOAD ) {
				OnClickListener yesListener = new OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						getContext().searchPlugin( mPluginType );
					}
				};
				onGenericError( errorString, R.string.feather_update, yesListener, android.R.string.cancel, null );

			} else if ( error == PluginService.ERROR_PLUGIN_TOO_NEW ) {
				OnClickListener yesListener = new OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						String pname = getContext().getBaseContext().getPackageName();
						getContext().downloadPlugin( pname, mPluginType );
					}
				};
				onGenericError( errorString, R.string.feather_update, yesListener, android.R.string.cancel, null );
			} else {
				onGenericError( errorString, android.R.string.ok, null );
			}
		}
	}

	/**
	 * Multiple items and different errors
	 * @param pkgname
	 * @param set
	 */
	private void showUpdateAlertMultipleItems( final String pkgname, Set<Integer> set ) {
		if ( null != set ) {
			final String errorString = getContext().getBaseContext().getResources().getString( R.string.feather_effects_error_update_multiple );

			OnClickListener yesListener = new OnClickListener() {

				@Override
				public void onClick( DialogInterface dialog, int which ) {
					getContext().searchPlugin( mPluginType );
				}
			};
			onGenericError( errorString, R.string.feather_update, yesListener, android.R.string.cancel, null );
		}
	}

	protected String getError( int error, String message ) {

		mLogger.info( "getError for " + error );
		
		int resId = R.string.feather_effects_error_loading_packs;

		switch ( error ) {
			case PluginService.ERROR_UNKNOWN:
				resId = R.string.feather_effects_unknown_errors;
				break;

			case PluginService.ERROR_PLUGIN_TOO_OLD:
				resId = R.string.feather_effects_error_update_packs;
				break;

			case PluginService.ERROR_PLUGIN_TOO_NEW:
				resId = R.string.feather_effects_error_update_editors;
				break;
				
			case PluginService.ERROR_DOWNLOAD:
				resId = R.string.feather_plugin_error_download;
				break;
				
			case PluginService.ERROR_PLUGIN_CORRUPTED:
				resId = R.string.feather_plugin_error_corrupted;
				break;
				
			case PluginService.ERROR_STORAGE_NOT_AVAILABLE:
				resId = R.string.feather_plugin_error_storage_not_available;
				break;
				
			default:
				if( null != message ) {
					return message;
				}
				resId = R.string.feather_effects_unknown_errors;
				break;
		}

		return getContext().getBaseContext().getString( resId );
	}

	protected void onPostActivate() {
		// register for plugins updates
		mPluginService.registerOnUpdateListener( this );
		updateInstalledPacks( true );
		contentReady();
	}

	@Override
	public void onDestroy() {
		mPluginService = null;
		mCDSService = null;
		mCacheService = null;
		mPreferenceService = null;
		super.onDestroy();
	}

	@Override
	public void onDeactivate() {
		onProgressEnd();
		mPluginService.removeOnUpdateListener( this );
		mImageManager.setOnLoadCompleteListener( null );

		mHList.setOnScrollListener( null );
		mHList.setOnItemSelectedListener( null );
		mHList.setOnItemClickedListener( null );

		super.onDeactivate();
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig, Configuration oldConfig ) {

		// TODO: we really need this?
		// mImageManager.clearCache();

		mConfigurationChanged = true;

		if ( mIapDialog != null ) {

			ViewGroup parent = (ViewGroup) mIapDialog.getParent();

			if ( null != parent ) {
				ExternalPlugin currentPlugin = mIapDialog.getPlugin();

				int index = parent.indexOfChild( mIapDialog );
				parent.removeView( mIapDialog );
				mIapDialog = (IapDialog) LayoutInflater.from( getContext().getBaseContext() ).inflate( R.layout.feather_iap_dialog, parent, false );
				mIapDialog.setLayoutAnimation( null );
				parent.addView( mIapDialog, index );
				updateIapDialog( currentPlugin );
				setApplyEnabled( false );
			}
		}

		// TODO: we don't really need to update everything...
		// updateInstalledPacks( false );

		super.onConfigurationChanged( newConfig, oldConfig );
	}

	@Override
	public void onScrollChanged() {
		mHList.setOnScrollListener( null );
		mScrollChanged = true;
		mEnableEffectAnimation = false;
	}

	@Override
	protected void onDispose() {

		mHList.setAdapter( null );

		if ( null != mImageManager ) {
			mImageManager.clearCache();
			mImageManager.shutDownNow();
		}

		if ( mThumbBitmap != null && !mThumbBitmap.isRecycled() ) {
			mThumbBitmap.recycle();
		}
		mThumbBitmap = null;

		if ( null != updateArrowBitmap && !updateArrowBitmap.isRecycled() ) {
			updateArrowBitmap.recycle();
		}
		updateArrowBitmap = null;

		super.onDispose();
	}

	@Override
	protected void onGenerateResult() {
		mLogger.info( "onGenerateResult. isRendering: " + mIsRendering );
		if ( mIsRendering ) {
			GenerateResultTask task = new GenerateResultTask();
			task.execute();
		} else {
			onComplete( mPreview, mActions );
		}
	}

	@Override
	protected void onComplete( Bitmap bitmap, MoaActionList actions ) {

		if ( null != mRenderedEffect ) {
			Tracker.recordTag( mRenderedEffect + ": Applied" );
			mTrackingAttributes.put( "filterName", mRenderedEffect );
		}

		if ( null != mRenderedPackName ) {
			try {
				if ( mRenderedPackName.equals( getContext().getBaseContext().getPackageName() ) ) {
					mRenderedPackName = "com.aviary.android.feather";
				}
			} catch ( Throwable t ) {}

			mTrackingAttributes.put( "packName", mRenderedPackName );
		}

		super.onComplete( bitmap, actions );
	}

	@Override
	public boolean onBackPressed() {
		if ( backHandled() ) return true;
		return super.onBackPressed();
	}

	@Override
	public void onCancelled() {
		killCurrentTask();
		mIsRendering = false;
		super.onCancelled();
	}

	@Override
	public boolean getIsChanged() {
		return super.getIsChanged() || mIsRendering == true;
	}

	@Override
	public void onUpdate( Bundle delta ) {
		if ( isActive() && mExternalPacksEnabled ) {
			if ( mUpdateDialog != null && mUpdateDialog.isShowing() ) {
				// another update alert is showing, skip new alerts
				return;
			}

			if ( validDelta( delta ) ) {

				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						updateInstalledPacks( true );
					}
				};

				mUpdateDialog = new AlertDialog.Builder( getContext().getBaseContext() ).setMessage( R.string.filter_pack_updated ).setNeutralButton( android.R.string.ok, listener ).setCancelable( false ).create();

				mUpdateDialog.show();

			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onLoadComplete( final ImageView view, Bitmap bitmap, int tag ) {

		if ( !isActive() ) return;

		View parent = (View) view.getParent();

		if ( null != bitmap ) {
			view.setImageDrawable( new BitmapDrawable( bitmap ) );
		} else {
			view.setImageResource( R.drawable.feather_iap_dialog_image_na );
		}

		if ( parent != null && parent.findViewById( R.id.progress ) != null ) {
			parent.findViewById( R.id.progress ).setVisibility( View.GONE );
		}

		if ( !mEnableEffectAnimation || mConfigurationChanged || tag == TAG_EXTERNAL_VIEW ) {
			view.setVisibility( View.VISIBLE );
			return;
		}

		if ( null != view && parent != null ) {

			if ( !mScrollChanged ) {
				if ( parent.getLeft() < mHList.getRight() ) {
					
					Animation anim;
					
					// anim = new AlphaAnimation( 0.0f, 1.0f );
					anim = new ScaleAnimation( 0, 1, 0, 1, Animation.RELATIVE_TO_SELF, (float) 0.5, Animation.RELATIVE_TO_SELF, (float) 0.5 );
					
					anim.setAnimationListener( new AnimationListener() {

						@Override
						public void onAnimationStart( Animation animation ) {
							view.setVisibility( View.VISIBLE );
						}

						@Override
						public void onAnimationRepeat( Animation animation ) {}

						@Override
						public void onAnimationEnd( Animation animation ) {}
					} );

					anim.setDuration( 100 );
					anim.setStartOffset( mHList.getScreenPositionForView( view ) * 100 );
					view.startAnimation( anim );
					view.setVisibility( View.INVISIBLE );
					return;
				}
			}

			view.setVisibility( View.VISIBLE );
		}
	}

	/**
	 * bundle contains a list of all updates applications. if one meets the criteria ( is a filter apk ) then return true
	 * 
	 * @param bundle
	 *           the bundle
	 * @return true if bundle contains a valid filter package
	 */
	private boolean validDelta( Bundle bundle ) {
		if ( null != bundle ) {
			if ( bundle.containsKey( "delta" ) ) {
				try {
					@SuppressWarnings("unchecked")
					ArrayList<UpdateType> updates = (ArrayList<UpdateType>) bundle.getSerializable( "delta" );
					if ( null != updates ) {
						for ( UpdateType update : updates ) {
							if ( FeatherIntent.PluginType.isTypeOf( update.getPluginType(), mPluginType ) ) {
								return true;
							}

							if ( FeatherIntent.ACTION_PLUGIN_REMOVED.equals( update.getAction() ) ) {
								// if it's removed check against current listed packs
								if ( mInstalledPackages.contains( update.getPackageName() ) ) {
									return true;
								}
							}
						}
						return false;
					}
				} catch ( ClassCastException e ) {
					return true;
				}
			}
		}
		return true;
	}

	@Override
	public View makeView() {
		ImageViewTouch view = new ImageViewTouch( getContext().getBaseContext(), null );
		view.setBackgroundColor( 0x00000000 );
		view.setDoubleTapEnabled( false );
		view.setScaleEnabled( false );
		view.setScrollEnabled( false );
		view.setDisplayType( DisplayType.FIT_IF_BIGGER );
		view.setLayoutParams( new ImageSwitcher.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
		return view;
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.feather_native_effects_content, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.feather_effects_panel, parent, false );
	}

	@Override
	public Matrix getContentDisplayMatrix() {
		return ( (ImageViewTouch) mImageSwitcher.getCurrentView() ).getDisplayMatrix();
	}

	protected Bitmap generateThumbnail( Bitmap input, final int width, final int height ) {
		return ThumbnailUtils.extractThumbnail( input, width, height );
	}

	/**
	 * Update the installed plugins
	 */
	protected void updateInstalledPacks( boolean firstTime ) {

		mIsAnimating = true;

		if ( mViewFlipper.getDisplayedChild() != 0 ) {
			mViewFlipper.setDisplayedChild( 0 );
		}

		// now try to install every plugin...
		if ( firstTime ) {
			mHList.setAdapter( null );
		}
		new PluginInstallTask().execute( mPluginType );
	}

	/**
	 * Creates and returns the default adapter for the frames listview
	 * 
	 * @param context
	 * @param result
	 * @return
	 */
	protected FramesListAdapter createListAdapter( Context context, List<EffectPack> result ) {
		return new FramesListAdapter( context, R.layout.feather_effect_thumb, R.layout.feather_effect_external_thumb, R.layout.feather_stickers_pack_divider_empty, R.layout.feather_getmore_stickers_thumb,
				R.layout.feather_getmore_stickers_thumb_inverted, result );
	}

	/**
	 * Called after the {@link PluginInstallTask} completed
	 * 
	 * @param result
	 *           - list of installed packs
	 * @param errors
	 *           - list of errors
	 */
	private void onEffectListUpdated( List<EffectPack> result, List<EffectPackError> errors, int totalCount ) {

		// we had errors during installation
		if ( null != errors && errors.size() > 0 ) {
			if ( !mUpdateErrorHandled ) {
				handleErrors( errors );
			}
		}

		FIRST_POSITION = ( mExternalPacksEnabled && mShowFirstGetMore ) ? 1 : 0;

		FramesListAdapter adapter = createListAdapter( getContext().getBaseContext(), result );
		mHList.setAdapter( adapter );

		if ( mViewFlipper.getDisplayedChild() != 1 ) {
			mViewFlipper.setDisplayedChild( 1 );
		}

		if ( mFirstTimeRenderer ) {
			mHList.setSelectedPosition( mSelectedPosition, false );
		} else if ( mSelectedPosition != FIRST_POSITION ) {
			mHList.setSelectedPosition( FIRST_POSITION, false );
		}

		if ( mFirstTimeRenderer ) {
			onItemSelected( mHList, null, mSelectedPosition, -1 );
		}
		mFirstTimeRenderer = true;

		// show the alert only the first time!!
		if ( totalCount < 1 && mExternalPacksEnabled && mPluginType == FeatherIntent.PluginType.TYPE_BORDER ) {

			if ( !mPreferenceService.containsValue( this.getClass().getSimpleName() + "-install-first-time" ) ) {

				OnClickListener listener = new OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						getContext().downloadPlugin( PluginService.FREE_BORDERS_PACKAGENAME, FeatherIntent.PluginType.TYPE_BORDER );
						dialog.dismiss();
					}
				};

				AlertDialog dialog = new AlertDialog.Builder( getContext().getBaseContext() ).setMessage( R.string.feather_borders_dialog_first_time ).setPositiveButton( android.R.string.ok, listener )
						.setNegativeButton( android.R.string.cancel, null ).create();

				mPreferenceService.putBoolean( this.getClass().getSimpleName() + "-install-first-time", true );

				dialog.show();
			}
		}
	}

	// ///////////////
	// IAP - Dialog //
	// ///////////////

	protected IapDialog mIapDialog;

	protected final IapDialog createIapDialog( final ExternalPlugin plugin ) {
		ViewGroup container = ( (FeatherContext) getContext().getBaseContext() ).activatePopupContainer();
		IapDialog dialog = (IapDialog) container.findViewById( R.id.main_iap_dialog );
		if ( dialog == null ) {
			LayoutInflater.from( getContext().getBaseContext() ).inflate( R.layout.feather_iap_dialog, container, true );
			dialog = (IapDialog) container.findViewById( R.id.main_iap_dialog );
			dialog.setFocusable( true );
			dialog.setPlugin( plugin, mPluginType, getContext().getBaseContext() );
			dialog.setOnCloseListener( new OnCloseListener() {

				@Override
				public void onClose() {
					removeIapDialog();
				}
			} );
		}
		setApplyEnabled( false );
		return dialog;
	}

	private void updateIapDialog( ExternalPlugin plugin ) {
		if ( null != mIapDialog && null != plugin ) {
			mIapDialog.setPlugin( plugin, mPluginType, getContext().getBaseContext() );
		}
	}

	private boolean removeIapDialog() {
		setApplyEnabled( true );
		if ( null != mIapDialog ) {
			mIapDialog.setOnCloseListener( null );
			mIapDialog.hide();
			mIapDialog = null;
			return true;
		}
		return false;
	}

	@SuppressLint("UseSparseArrays")
	private void handleErrors( List<EffectPackError> mErrors ) {

		if ( mErrors == null || mErrors.size() < 1 ) return;

		// first get the total number of errors
		HashMap<Integer, String> hash = new HashMap<Integer, String>();

		for ( EffectPackError item : mErrors ) {
			int error = item.mError;
			hash.put( error, (String) item.mPackageName );
		}

		// now manage the different cases
		// 1. just one type of error
		if ( hash.size() == 1 ) {

			// get the first error
			EffectPackError item = mErrors.get( 0 );

			if ( mErrors.size() == 1 ) {
				showUpdateAlert( item.mPackageName, item.mLabel, item.mError, item.mErrorMessage, false );
			} else {
				showUpdateAlertMultiplePlugins( item.mError, false );
			}
		} else {
			// 2. here we must handle different errors type
			showUpdateAlertMultipleItems( getContext().getBaseContext().getPackageName(), hash.keySet() );
		}

		mUpdateErrorHandled = true;
	}

	private void renderEffect( EffectPack item, int position ) {

		String label = (String) item.getItemAt( position );
		mLogger.log( "renderEffect: " + label );

		killCurrentTask();
		mCurrentTask = createRenderTask( position );
		mCurrentTask.execute( item );

		if ( null != item ) {
			Tracker.recordTag( item.getItemAt( position ) + ": Selected" );
		}

	}

	protected RenderTask createRenderTask( int position ) {
		return new RenderTask( position );
	}

	boolean killCurrentTask() {
		if ( mCurrentTask != null ) {
			onProgressEnd();
			return mCurrentTask.cancel( true );
		}
		return false;
	}

	protected INativeFilter loadNativeFilter( final EffectPack pack, int position, final CharSequence label, boolean hires ) {
		BorderFilter filter = (BorderFilter) FilterLoaderFactory.get( Filters.BORDERS );
		filter.setBorderName( label );
		filter.setHiRes( hires );

		IPlugin plugin = pack.mPluginRef;
		if ( null != plugin ) {
			if ( plugin instanceof FramePlugin ) {
				filter.setSourceApp( ( (FramePlugin) plugin ).getSourceDir() );

				// border size
				int[] sizes = ( (FramePlugin) plugin ).listBordersWidths();
				position -= pack.getIndex();

				if ( null != sizes && sizes.length > ( position - 1 ) && position > 0 ) {
					int borderSize = sizes[position - 1];
					filter.setSize( (double) borderSize / 100.0 );
				}
			}
		}
		return filter;
	}

	boolean backHandled() {
		if ( mIsAnimating ) return true;
		if ( null != mIapDialog ) {
			removeIapDialog();
			return true;
		}
		killCurrentTask();
		return false;
	}

	static class ViewHolder {

		TextView text;
		ImageView image;
	}

	class FramesListAdapter extends ArrayAdapterExtended<EffectPack> {

		private int mLayoutResId;
		private int mExternalLayoutResId;
		private int mAltLayoutResId;
		private int mAltLayout2ResId;
		private int mDividerLayoutResId;
		private int mCount = -1;
		private List<EffectPack> mData;
		private LayoutInflater mLayoutInflater;
		private int mDefaultHeight;
		protected BitmapDrawable mExternalFolderIcon;

		static final int TYPE_GET_MORE_FIRST = 0;
		static final int TYPE_GET_MORE_LAST = 1;
		static final int TYPE_NORMAL = 2;
		static final int TYPE_EXTERNAL = 3;
		static final int TYPE_DIVIDER = 4;

		public FramesListAdapter( Context context, int mainResId, int externalResId, int dividerResId, int altResId, int altResId2, List<EffectPack> objects ) {
			super( context, mainResId, objects );
			mLayoutResId = mainResId;
			mExternalLayoutResId = externalResId;
			mAltLayoutResId = altResId;
			mAltLayout2ResId = altResId2;
			mDividerLayoutResId = dividerResId;
			mData = objects;
			mLayoutInflater = LayoutInflater.from( context );
			mExternalFolderIcon = getExternalBackgroundDrawable( context );
			mDefaultHeight = getOptionView().findViewById( R.id.background ).getHeight() - getOptionView().findViewById( R.id.bottom_background_overlay ).getHeight();
		}

		protected BitmapDrawable getExternalBackgroundDrawable( Context context ) {
			return (BitmapDrawable) context.getResources().getDrawable( R.drawable.feather_frames_pack_background );
		}

		protected void finalize() throws Throwable {
			Log.i( "effects-adapter", "finalize" );
		};

		@Override
		public int getCount() {
			if ( mCount == -1 ) {
				int total = 0; // first get more
				for ( EffectPack pack : mData ) {
					if ( null == pack ) {
						total++;
						continue;
					}
					pack.setIndex( total );
					total += pack.size;
				}
				// return total;
				mCount = total;
			}
			return mCount;
		}

		@Override
		public void notifyDataSetChanged() {
			mCount = -1;
			super.notifyDataSetChanged();
		}

		@Override
		public void notifyDataSetAdded() {
			mCount = -1;
			super.notifyDataSetAdded();
		}

		@Override
		public void notifyDataSetRemoved() {
			mCount = -1;
			super.notifyDataSetRemoved();
		}

		@Override
		public void notifyDataSetInvalidated() {
			mCount = -1;
			super.notifyDataSetInvalidated();
		}

		@Override
		public int getViewTypeCount() {
			return 5;
		}

		@Override
		public int getItemViewType( int position ) {

			if ( !mExternalPacksEnabled ) return TYPE_NORMAL;

			EffectPack item = getItem( position );
			if ( null == item ) {
				if ( position == 0 )
					return TYPE_GET_MORE_FIRST;
				else
					return TYPE_GET_MORE_LAST;
			}

			if ( item.isDivider ) return TYPE_DIVIDER;
			if ( item.isExternal ) return TYPE_EXTERNAL;
			return TYPE_NORMAL;
		}

		@Override
		public EffectPack getItem( int position ) {
			for ( int i = 0; i < mData.size(); i++ ) {
				EffectPack pack = mData.get( i );
				if ( null == pack ) continue;

				if ( position >= pack.index && position < pack.index + pack.size ) {
					return pack;
				}
			}
			return null;
		}

		@SuppressWarnings("deprecation")
		@Override
		public View getView( final int position, final View convertView, final ViewGroup parent ) {

			View view;
			ViewHolder holder = null;
			int type = getItemViewType( position );
			final EffectPack item = getItem( position );

			int layoutWidth;
			int layoutHeight = LayoutParams.MATCH_PARENT;

			if ( convertView == null ) {
				holder = new ViewHolder();
				if ( type == TYPE_GET_MORE_FIRST ) {
					view = mLayoutInflater.inflate( mAltLayoutResId, parent, false );
					layoutHeight = mDefaultHeight;
					layoutWidth = mFilterCellWidth;

				} else if ( type == TYPE_GET_MORE_LAST ) {
					view = mLayoutInflater.inflate( mAltLayout2ResId, parent, false );
					layoutHeight = mDefaultHeight;
					layoutWidth = mFilterCellWidth;

					// hide the last "get more" button if there's no need
					View lastChild = parent.getChildAt( parent.getChildCount() - 1 );
					if ( null != lastChild ) {
						if ( lastChild.getRight() < parent.getRight() ) {
							layoutWidth = 0;
						}
					}

				} else if ( type == TYPE_NORMAL ) {
					view = mLayoutInflater.inflate( mLayoutResId, parent, false );
					holder.text = (TextView) view.findViewById( R.id.text );
					holder.image = (ImageView) view.findViewById( R.id.image );
					holder.image.setImageDrawable( new BitmapDrawable( mThumbBitmap ) );
					view.setTag( holder );

					LayoutParams params = holder.image.getLayoutParams();
					params.width = params.height = mThumbBitmapSize;
					holder.image.setLayoutParams( params );
					holder.image.requestLayout();

					layoutHeight = LayoutParams.WRAP_CONTENT;
					layoutWidth = mThumbBitmapSize + mItemsGapPixelsSize;

				} else if ( type == TYPE_EXTERNAL ) {
					view = mLayoutInflater.inflate( mExternalLayoutResId, parent, false );
					holder.text = (TextView) view.findViewById( R.id.text );
					holder.image = (ImageView) view.findViewById( R.id.image );
					view.setTag( holder );

					LayoutParams params = holder.image.getLayoutParams();
					params.width = mThumbBitmapSize + ( mExternalThumbPadding * 2 );
					params.height = mThumbBitmapSize + mExternalThumbPadding;
					holder.image.setImageDrawable( mExternalFolderIcon );
					holder.image.setLayoutParams( params );
					holder.image.requestLayout();

					layoutHeight = LayoutParams.WRAP_CONTENT;
					layoutWidth = mThumbBitmapSize + mItemsGapPixelsSize + ( mExternalThumbPadding * 2 );

				} else {
					// TYPE_DIVIDER
					view = mLayoutInflater.inflate( mDividerLayoutResId, parent, false );
					holder.image = (ImageView) view.findViewById( R.id.image );
					view.setTag( holder );

					layoutWidth = EffectThumbLayout.LayoutParams.WRAP_CONTENT;
					layoutHeight = mDefaultHeight;
				}
				view.setLayoutParams( new EffectThumbLayout.LayoutParams( layoutWidth, layoutHeight ) );
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}

			Callable<Bitmap> executor;

			if ( type == TYPE_NORMAL ) {
				holder.text.setText( item.getLabelAt( position ) );

				final String effectName = (String) item.getItemAt( position );
				executor = createContentCallable( item, position, effectName );
				
				mImageManager.execute( executor, position + "/" + effectName, holder.image );

			} else if ( type == TYPE_EXTERNAL ) {
				holder.text.setText( item.mTitle );

				ExternalPlugin plugin = (ExternalPlugin) item.mPluginRef;
				if ( null != plugin ) {
					executor = createExternalContentCallable( plugin.getIconUrl() );
					mImageManager.execute( executor, plugin.getIconUrl(), holder.image, TAG_EXTERNAL_VIEW );
				}

			} else if ( type == TYPE_DIVIDER ) {

				Drawable drawable = holder.image.getDrawable();

				if ( drawable instanceof PluginDividerDrawable ) {
					( (PluginDividerDrawable) drawable ).setTitle( item.mTitle.toString() );
				} else {
					PluginDividerDrawable d = new PluginDividerDrawable( drawable, item.mTitle.toString() );
					holder.image.setImageDrawable( d );
				}

			} else {
				// get more
				if ( mShowIapNotificationAndValue ) {
					TextView totalText = (TextView) view.findViewById( R.id.text01 );
					totalText.setText( String.valueOf( mAvailablePacks ) );
				}
			}

			return view;
		}

		protected Callable<Bitmap> createContentCallable( final EffectPack item, int position, final String effectName ) {
			return new BorderThumbnailCallable( mCacheService, (InternalPlugin) item.mPluginRef, effectName, mFilterCellWidth );
		}

		protected Callable<Bitmap> createExternalContentCallable( final String iconUrl ) {
			return new ExternalFramesThumbnailCallable( iconUrl, mCacheService, mExternalFolderIcon, this.getContext().getResources(), R.drawable.feather_iap_dialog_image_na );
		}
	}

	static MoaActionList actionsForRoundedThumbnail( final boolean isValid, INativeFilter filter ) {

		MoaActionList actions = MoaActionFactory.actionList();
		if( null != filter ) actions.addAll( filter.getActions() );
		
		MoaAction action = MoaActionFactory.action( "ext-roundedborders" );
		action.setValue( "padding", mRoundedBordersPaddingPixelSize );
		action.setValue( "roundPx", mRoundedBordersPixelSize );
		action.setValue( "strokeColor", 0xffa6a6a6 );
		action.setValue( "strokeWeight", mRoundedBordersStrokePixelSize );

		if ( !isValid ) action.setValue( "overlaycolor", 0x99000000 );
		actions.add( action );

		// shadow
		action = MoaActionFactory.action( "ext-roundedshadow" );
		action.setValue( "color", 0x99000000 );
		action.setValue( "radius", mShadowRadiusPixelSize );
		action.setValue( "roundPx", mRoundedBordersPixelSize );
		action.setValue( "offsetx", mShadowOffsetPixelSize );
		action.setValue( "offsety", mShadowOffsetPixelSize );
		action.setValue( "padding", mRoundedBordersPaddingPixelSize );
		
		actions.add( action );
		return actions;
	}

	// ////////////////////////
	// OnItemClickedListener //
	// ////////////////////////

	@Override
	public boolean onItemClick( AdapterView<?> parent, View view, int position, long id ) {

		mLogger.info( "onItemClick: " + position );

		if ( isActive() ) {
			if ( mHList.getAdapter() == null ) return false;
			int viewType = mHList.getAdapter().getItemViewType( position );

			if ( viewType == FramesListAdapter.TYPE_NORMAL ) {

				EffectPack item = (EffectPack) mHList.getAdapter().getItem( position );

				if ( item != null && item.mStatus == PluginService.ERROR_NONE ) {
					return true;
				} else {
					showUpdateAlert( item.mPackageName, item.mTitle, item.mStatus, item.mError, true );
					return false;
				}

			} else if ( viewType == FramesListAdapter.TYPE_GET_MORE_FIRST || viewType == FramesListAdapter.TYPE_GET_MORE_LAST ) {

				if ( position == 0 ) {
					Tracker.recordTag( "LeftGetMoreEffects : Selected" );
				} else {
					Tracker.recordTag( "RightGetMoreEffects : Selected" );
				}
				getContext().searchPlugin( mPluginType );
				return false;

			} else if ( viewType == FramesListAdapter.TYPE_EXTERNAL ) {
				EffectPack item = (EffectPack) mHList.getAdapter().getItem( position );
				if ( null != item ) {

					if ( android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.FROYO && SystemUtils.getApplicationTotalMemory() >= Constants.APP_MEMORY_SMALL ) {
						ExternalPlugin externalPlugin = (ExternalPlugin) item.mPluginRef;

						if ( externalPlugin == null ) return false;

						if ( null == mIapDialog ) {
							mIapDialog = createIapDialog( externalPlugin );
						} else {
							updateIapDialog( externalPlugin );
						}
					} else {
						Tracker.recordTag( "Unpurchased(" + item.mTitle + ") : StoreButtonClicked" );
						getContext().downloadPlugin( item.mPackageName.toString(), mPluginType );
					}
				}
			}
		}
		return false;
	}

	// /////////////////////////
	// OnItemSelectedListener //
	// /////////////////////////

	@Override
	public void onItemSelected( AdapterView<?> parent, View view, int position, long id ) {
		mLogger.info( "onItemSelected: " + position );

		mSelectedPosition = position;

		if ( isActive() ) {

			if ( mHList.getAdapter() == null ) return;
			int viewType = mHList.getAdapter().getItemViewType( position );

			if ( viewType == FramesListAdapter.TYPE_NORMAL ) {

				EffectPack item = (EffectPack) mHList.getAdapter().getItem( position );

				if ( item == null ) return;

				if ( item.mStatus == PluginService.ERROR_NONE ) {
					// so we assume the view is already selected and so let's selected the "original" effect by default
					if ( !item.isExternal ) {
						renderEffect( item, position );
					}
				}
			}
		}
	}

	@Override
	public void onNothingSelected( AdapterView<?> parent ) {
		mLogger.info( "onNothingSelected" );

		if ( parent.getAdapter() != null ) {
			EffectPack item = ( (FramesListAdapter) parent.getAdapter() ).getItem( FIRST_POSITION );
			if ( null != item && item.size > 0 ) {
				renderEffect( item, FIRST_POSITION );
			} else {
				return;
			}

			if ( null != getHandler() ) {
				getHandler().postDelayed( new Runnable() {

					@Override
					public void run() {
						mHList.setSelectedPosition( FIRST_POSITION, false );

					}
				}, 200 );
			}
		}
	}

	static class ExternalFramesThumbnailCallable implements Callable<Bitmap> {

		String mUri;
		int mFallbackResId;
		BitmapDrawable mFolder;
		SoftReference<ImageCacheService> cacheServiceRef;
		SoftReference<Resources> resourcesRef;

		public ExternalFramesThumbnailCallable( final String uri, ImageCacheService cacheService, final BitmapDrawable folderBackground, Resources resources, final int fallbackResId ) {
			mUri = uri;
			mFallbackResId = fallbackResId;
			cacheServiceRef = new SoftReference<ImageCacheService>( cacheService );
			resourcesRef = new SoftReference<Resources>( resources );
			mFolder = folderBackground;
		}

		@Override
		public Bitmap call() throws Exception {

			if ( mUri == null || mUri.length() < 1 ) {
				return mFolder.getBitmap();
			}

			Bitmap bitmap = null;
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;

			ImageCacheService cache = cacheServiceRef.get();
			if ( null == cache ) {
				return mFolder.getBitmap();
			}

			SimpleCachedRemoteBitmap request;

			try {
				request = cache.requestRemoteBitmap( PluginService.CONTENT_DEFAULT_URL + "/" + mUri );
				bitmap = request.getBitmap( options );
			} catch ( Exception e ) {
				e.printStackTrace();
			}

			// fallback icon
			if ( null == bitmap ) {
				if ( null != resourcesRef.get() ) {
					try {
						bitmap = BitmapFactory.decodeResource( resourcesRef.get(), mFallbackResId );
					} catch ( Throwable t ) {}
				}
			}

			if ( null != bitmap ) {
				Bitmap result = generateBitmap( bitmap );
				if ( result != bitmap ) {
					bitmap.recycle();
					bitmap = null;
					bitmap = result;
				}
				return bitmap;
			} else {
				return mFolder.getBitmap();
			}
		}

		Bitmap generateBitmap( Bitmap icon ) {
			return icon;
		}
	}

	/**
	 * Downloads and renders the sticker thumbnail
	 * 
	 * @author alessandro
	 * 
	 */
	static class BorderThumbnailCallable implements Callable<Bitmap> {

		InternalPlugin mPlugin;
		int mFinalSize;
		String mUrl;
		SoftReference<ImageCacheService> cacheRef;

		public BorderThumbnailCallable( ImageCacheService cacheService, final InternalPlugin plugin, final String srcUrl, final int size ) {
			mPlugin = plugin;
			mFinalSize = size;
			mUrl = srcUrl;
			cacheRef = new SoftReference<ImageCacheService>( cacheService );
		}

		@Override
		public Bitmap call() throws Exception {

			ImageCacheService cache = cacheRef.get();
			Bitmap bitmap;

			if ( null != cache ) {
				bitmap = cache.getBitmap( mPlugin.getType() + "-" + mUrl, mThumbnailOptions );
				if ( null != bitmap ) return bitmap;
			}

			try {
				bitmap = ImageLoader.getPluginItemBitmap( mPlugin, mUrl, null, mFinalSize, mFinalSize );
			} catch ( Exception e ) {
				return null;
			}

			if ( null != bitmap ) {
				NativeFilter filter = new NativeFilter( "undefined" );
				actionsForRoundedThumbnail( true, filter );
				Bitmap result = filter.execute( bitmap, null, 1, 1 );

				bitmap.recycle();
				bitmap = result;

				if ( null != bitmap && null != cache ) {
					cache.putBitmap( mPlugin.getType() + "-" + mUrl, bitmap );
				}
				return bitmap;
			}

			return null;
		}
	}

	protected CharSequence[] getOptionalEffectsValues() {
		return new CharSequence[] { "original" };
	}

	protected CharSequence[] getOptionalEffectsLabels() {
		return new CharSequence[] { "Original" };
	}

	/**
	 * Install all the
	 * 
	 * @author alessandro
	 * 
	 */
	class PluginInstallTask extends AsyncTask<Integer, Void, List<EffectPack>> {

		List<EffectPackError> mErrors;
		private int mInstalledCount = 0;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mErrors = Collections.synchronizedList( new ArrayList<EffectPackError>() );
			mImageManager.clearCache();
		}

		@Override
		protected List<EffectPack> doInBackground( Integer... params ) {

			// List of installed plugins available on the device
			final int pluginType = params[0];
			long sharedUpdateTime = 0, lastUpdateTime = 0;
			FeatherInternalPack installedPacks[];
			FeatherPack availablePacks[];

			if ( mExternalPacksEnabled ) {

				while ( !mPluginService.isUpdated() ) {
					try {
						Thread.sleep( 50 );
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
					mLogger.log( "waiting for plugin service..." );
				}

				installedPacks = mPluginService.getInstalled( getContext().getBaseContext(), pluginType );
				availablePacks = mPluginService.getAvailable( pluginType );
			} else {
				if ( pluginType == FeatherIntent.PluginType.TYPE_FILTER )
					installedPacks = new FeatherInternalPack[] { FeatherInternalPack.getDefault( getContext().getBaseContext() ) };
				else
					installedPacks = new FeatherInternalPack[] {};
				availablePacks = new FeatherExternalPack[] {};
			}

			mInstalledCount = 0;

			if ( null != mPreferenceService && mExternalPacksEnabled ) sharedUpdateTime = mPreferenceService.getLong( this.getClass().getName() + "-plugins-update-date", 0 );

			if ( null != mPluginService ) lastUpdateTime = mPluginService.getLastUpdateTime();

			// List of the available plugins online
			mAvailablePacks = availablePacks.length;

			List<EffectPack> result = Collections.synchronizedList( new ArrayList<EffectPack>() );
			mInstalledPackages.clear();

			if ( mExternalPacksEnabled ) {
				if ( mPluginType == FeatherIntent.PluginType.TYPE_BORDER ) {
					mShowFirstGetMore = !( ( installedPacks.length == 0 && availablePacks.length == 1 ) || ( installedPacks.length == 1 && availablePacks.length == 0 ) );
				}
				if ( mShowFirstGetMore ) 
					result.add( null );
			}

			int index = 0;
			
			for ( FeatherPack pack : installedPacks ) {
				if ( pack instanceof FeatherInternalPack ) {
					InternalPlugin plugin = (InternalPlugin) PluginFactory.create( getContext().getBaseContext(), pack, mPluginType );
					final CharSequence packagename = plugin.getPackageName();
					final CharSequence label = plugin.getPackageLabel();

					int status = PluginService.ERROR_NONE;
					String errorMessage = null;
					
					List<Pair<String, String>> pluginItems = null;
					List<Long> pluginIds = null;
					
					// install process
					if ( plugin.installed() ) { // ok, the plugin is on the device
						mLogger.info( "**** " + packagename + " ****" );
						if( plugin instanceof ICDSPlugin ) {
							try {
								if( !( (ICDSPlugin) plugin ).installAndLoad( getContext().getBaseContext(), mPluginService ) ) {
									status = PluginService.ERROR_INSTALL;
								} else {
									status = PluginService.ERROR_NONE;
								}
							} catch( PluginException e ) {
								e.printStackTrace();
								status = e.getErrorCode();
								errorMessage = e.getMessage();
							} catch( Throwable t ) {
								t.printStackTrace();
								status = PluginService.ERROR_UNKNOWN;
								errorMessage = t.getMessage();
							}
						}
					} else {
						status = PluginService.ERROR_PLUGIN_NOT_FOUND;
						errorMessage = "Plugin not installed";
					}

					if ( status != PluginService.ERROR_NONE ) {
						// Some errors loading the plugin!

						// if the plugin is not the default one then display the error
						if( !getContext().getBaseContext().getPackageName().equals( plugin.getPackageName() ) ) {
							pluginItems = new ArrayList<Pair<String,String>>();
							pluginItems.add( Pair.create( "-1", (String) plugin.getPackageLabel() ) );
							pluginIds = new ArrayList<Long>();
							pluginIds.add( -1L );
							
							EffectPackError error = new EffectPackError( packagename, label, status, errorMessage );
							mErrors.add( error );
						}
					} else {
						pluginItems = loadPluginItems( plugin );
						pluginIds = loadPluginIds( plugin );
						
						if ( index == 0 ) {
							CharSequence[] f = getOptionalEffectsValues();
							CharSequence[] n = getOptionalEffectsLabels();
							if ( null != f && null != n && f.length == n.length ) {
								
								for( int i = 0; i < f.length; i++ ) {
									pluginItems.add( 0, Pair.create( (String) f[i], (String) n[i] ) );
									if( null != pluginIds ) pluginIds.add( 0, -1L );
								}
							}
						}
						mInstalledCount++;
					}

					if( null != pluginItems && null != pluginIds ) {
						final EffectPack effectPack = new EffectPack( packagename, label, pluginItems, pluginIds, status, errorMessage, plugin, false );
						mInstalledPackages.add( packagename.toString() );
						
						if( result.size() > 0 && result.get( result.size() - 1 ) != null ) {
							// first add the label item
							result.add( new EffectPack( label.toString() ) );
						}
						
						// then add the item pack
						result.add( effectPack );
					}

					index++;
				}
			}

			if ( mExternalPacksEnabled ) {

				if ( availablePacks.length > 0 && mInstalledCount > 0 ) {
					// "featured" divider
					result.add( new EffectPack( mFeaturedDefaultTitle ) );
				}

				index = 0;
				for ( FeatherPack pack : availablePacks ) {
					if ( index >= mFeaturedCount ) break;
					ExternalPlugin plugin = (ExternalPlugin) PluginFactory.create( getContext().getBaseContext(), pack, mPluginType );
					final CharSequence packagename = plugin.getPackageName();
					final CharSequence label = plugin.getPackageLabel();

					final EffectPack effectPack = new EffectPack( packagename, label, null, null, PluginService.ERROR_NONE, null, plugin, true );

					if ( isActive() ) {
						result.add( effectPack );
					}
					index++;
				}
			}

			if ( mInstalledPackages != null && mInstalledCount > 0 ) {

				if ( mExternalPacksEnabled && mShowFirstGetMore ) {
					result.add( null );
				}
			}

			if ( mExternalPacksEnabled ) {
				if ( sharedUpdateTime != lastUpdateTime ) {
					mLogger.log( "lastUpdateTime: " + lastUpdateTime + " != sharedUpdateTime: " + sharedUpdateTime );
					if ( mPreferenceService != null ) mPreferenceService.putLong( this.getClass().getName() + "-plugins-update-date", lastUpdateTime );
					mShowIapNotificationAndValue = mAvailablePacks > 0;
				} else {
					mShowIapNotificationAndValue = false;
				}
			}
			
			if ( mInstalledCount == 0 ) {
				mEnableEffectAnimation = false;
			}

			return result;
		}

		@Override
		protected void onPostExecute( List<EffectPack> result ) {
			super.onPostExecute( result );

			onEffectListUpdated( result, mErrors, mInstalledCount );
			mIsAnimating = false;
		}
	}
	
	protected List<Pair<String, String>> loadPluginItems( InternalPlugin plugin ) {
		List<Pair<String, String>> result = new ArrayList<Pair<String,String>>();
		if( plugin instanceof FramePlugin ) {
			String[] items = ((FramePlugin) plugin).listBorders();
			for( int i = 0; i < items.length; i++ ) {
				String label = (String) plugin.getResourceLabel( items[i] );
				result.add( Pair.create( items[i], label ) );
			}
		}
		return result;
	}
	
	protected List<Long> loadPluginIds( InternalPlugin plugin ) {
		if( plugin instanceof FramePlugin ) {
			return new ArrayList<Long>( ((FramePlugin)plugin).size() );
		}
		return new ArrayList<Long>();
	}

	class EffectPackError {

		CharSequence mPackageName;
		CharSequence mLabel;
		int mError;
		String mErrorMessage;

		public EffectPackError( CharSequence packagename, CharSequence label, int error, String errorString ) {
			mPackageName = packagename;
			mLabel = label;
			mError = error;
			mErrorMessage = errorString;
		}
	}

	static class EffectPack {

		CharSequence mPackageName;
		List<Pair<String, String>> mValues;
		List<Long> mIds;
		CharSequence mTitle;
		int mStatus;
		String mError;
		IPlugin mPluginRef;
		int size = 0;
		int index = 0;
		boolean isExternal;
		boolean isDivider;

		public EffectPack( final String label ) {
			isDivider = true;
			size = 1;
			mStatus = PluginService.ERROR_NONE;
			mTitle = label;
		}

		public EffectPack( CharSequence packageName, CharSequence pakageTitle, List<Pair<String, String>> values, List<Long> ids, int status, String errorMsg, IPlugin plugin, boolean external ) {
			mPackageName = packageName;
			mStatus = status;
			mTitle = pakageTitle;
			mPluginRef = plugin;
			isExternal = external;
			isDivider = false;
			mValues = values;
			mIds = ids;
			mError = errorMsg;

			if ( null != values ) {
				size = values.size();
			} else {
				size = 1;
			}
		}

		public int getCount() {
			return size;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex( int value ) {
			index = value;
		}

		public CharSequence getItemAt( int position ) {
			return mValues.get(position - index).first;
		}
		
		public long getItemIdAt( int position ) {
			if( null != mIds )
				return mIds.get( position - index );
			return -1;
		}

		public CharSequence getLabelAt( int position ) {
			return mValues.get( position - index ).second;
		}

		public int getItemIndex( int position ) {
			return position - index;
		}

		@Override
		protected void finalize() throws Throwable {
			mPluginRef = null;
			super.finalize();
		}
	}

	/**
	 * Render the selected effect
	 */
	protected class RenderTask extends UserTask<EffectPack, Bitmap, Bitmap> implements OnCancelListener {

		int mPosition;
		String mError;
		MoaResult mMoaMainExecutor;
		MoaResult mMoaPreviewExecutor;

		/**
		 * Instantiates a new render task.
		 * 
		 * @param tag
		 */
		public RenderTask( final int position ) {
			mPosition = position;
		}

		@Override
		public void onPreExecute() {
			super.onPreExecute();
			onProgressStart();
		}

		private INativeFilter initFilter( EffectPack pack, int position, String label ) {
			final INativeFilter filter;
			
			try {
				filter = loadNativeFilter( pack, position, label, true );
			} catch( Throwable t ) {
				t.printStackTrace();
				return null;
			}

			mActions = (MoaActionList) filter.getActions().clone();

			if ( filter instanceof BorderFilter ) ( (BorderFilter) filter ).setHiRes( false );

			try {
				mMoaMainExecutor = filter.prepare( mBitmap, mPreview, 1, 1 );
			} catch ( JSONException e ) {
				e.printStackTrace();
				mMoaMainExecutor = null;
				return null;
			}
			return filter;
		}

		protected MoaResult initPreview( INativeFilter filter ) {
			return null;
		}

		/**
		 * Process the preview bitmap while executing in background the full image
		 */
		public void doSmallPreviewInBackground() {
			// rendering the small preview
			if ( mMoaPreviewExecutor != null ) {

				mMoaPreviewExecutor.execute();
				if ( mMoaPreviewExecutor.active > 0 ) {
					publishProgress( mMoaPreviewExecutor.outputBitmap );
				}
			}
		}

		public void doFullPreviewInBackground( final String effectName ) {
			// rendering the full preview
			mMoaMainExecutor.execute();
		}

		@Override
		public Bitmap doInBackground( final EffectPack... params ) {

			if ( isCancelled() ) return null;

			final EffectPack pack = params[0];
			mRenderedEffect = (String) pack.getLabelAt( mPosition );
			mRenderedPackName = (String) pack.mPackageName;
			
			final String mEffect = (String) pack.getItemAt( mPosition );
			INativeFilter filter = initFilter( pack, mPosition, mEffect );
			if ( null != filter ) {
				mMoaPreviewExecutor = initPreview( filter );
			} else {
				return null;
			}

			mIsRendering = true;

			// render small preview if required
			doSmallPreviewInBackground();

			if ( isCancelled() ) return null;

			// rendering the full preview
			try {
				doFullPreviewInBackground( mEffect );
			} catch ( Exception exception ) {
				mError = exception.getMessage();
				exception.printStackTrace();
				return null;
			}

			mLogger.log( "	complete. isCancelled? " + isCancelled(), mEffect );

			if ( !isCancelled() ) {
				return mMoaMainExecutor.outputBitmap;
			} else {
				return null;
			}
		}

		@Override
		public void onProgressUpdate( Bitmap... values ) {
			super.onProgressUpdate( values );

			// we're using a FakeBitmapDrawable just to upscale the small bitmap
			// to be rendered the same way as the full image
			final Bitmap preview = values[0];
			if ( null != preview ) {
				final FakeBitmapDrawable drawable = new FakeBitmapDrawable( preview, mBitmap.getWidth(), mBitmap.getHeight() );
				mImageSwitcher.setImageDrawable( drawable, null );
			}
		}

		@Override
		public void onPostExecute( final Bitmap result ) {
			super.onPostExecute( result );

			if ( !isActive() ) return;

			mPreview = result;

			if ( result == null || mMoaMainExecutor == null || mMoaMainExecutor.active == 0 ) {

				onRestoreOriginalBitmap();

				if ( mError != null ) {
					onGenericError( mError, android.R.string.ok, null );
				}

				setIsChanged( false );
				mActions = null;

			} else {
				onApplyNewBitmap( result );
				setIsChanged( true );
			}

			onProgressEnd();

			mIsRendering = false;
			mCurrentTask = null;
		}

		protected void onApplyNewBitmap( final Bitmap result ) {
			if ( SystemUtils.isHoneyComb() ) {
				Moa.notifyPixelsChanged( result );
			}
			mImageSwitcher.setImageBitmap( result, null );
		}

		protected void onRestoreOriginalBitmap() {
			// restore the original bitmap...
			mImageSwitcher.setImageBitmap( mBitmap, null );
		}

		@Override
		public void onCancelled() {
			super.onCancelled();

			if ( mMoaMainExecutor != null ) {
				mMoaMainExecutor.cancel();
			}

			if ( mMoaPreviewExecutor != null ) {
				mMoaPreviewExecutor.cancel();
			}

			mIsRendering = false;
		}

		@Override
		public void onCancel( DialogInterface dialog ) {
			cancel( true );
		}
	}

	/**
	 * Used to generate the Bitmap result. If user clicks on the "Apply" button when an effect is still rendering, then starts this
	 * task.
	 */
	class GenerateResultTask extends AsyncTask<Void, Void, Void> {

		ProgressDialog mProgress = new ProgressDialog( getContext().getBaseContext() );

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgress.setTitle( getContext().getBaseContext().getString( R.string.feather_loading_title ) );
			mProgress.setMessage( getContext().getBaseContext().getString( R.string.effect_loading_message ) );
			mProgress.setIndeterminate( true );
			mProgress.setCancelable( false );
			mProgress.show();
		}

		@Override
		protected Void doInBackground( Void... params ) {

			mLogger.info( "GenerateResultTask::doInBackground", mIsRendering );

			while ( mIsRendering ) {
				mLogger.log( "waiting...." );
			}

			return null;
		}

		@Override
		protected void onPostExecute( Void result ) {
			super.onPostExecute( result );

			mLogger.info( "GenerateResultTask::onPostExecute" );

			if ( getContext().getBaseActivity().isFinishing() ) return;
			if ( mProgress.isShowing() ) mProgress.dismiss();

			onComplete( mPreview, mActions );
		}
	}

}
