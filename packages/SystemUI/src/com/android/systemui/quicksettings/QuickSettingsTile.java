package com.android.systemui.quicksettings;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsTileView;

public class QuickSettingsTile implements OnClickListener {

    protected final Context mContext;
    protected QuickSettingsContainerView mContainer;
    protected QuickSettingsTileView mTile;
    protected OnClickListener mOnClick;
    protected OnLongClickListener mOnLongClick;
    protected final int mTileLayout;
    protected int mDrawable;
    protected String mLabel;
    protected int mTileTextSize;
    protected int mTileTextPadding;
    protected int mTileTextColor;
    protected Drawable mRealDrawable;
    protected PhoneStatusBar mStatusbarService;
    protected QuickSettingsController mQsc;
    protected SharedPreferences mPrefs;

    private Handler mHandler = new Handler();

    protected Vibrator mVibrator;

    // Flip
    protected final static int FLIP_RIGHT = 0;
    protected final static int FLIP_LEFT = 1;
    protected final static int FLIP_UP = 2;
    protected final static int FLIP_DOWN = 3;

    public QuickSettingsTile(Context context, QuickSettingsController qsc) {
        this(context, qsc, R.layout.quick_settings_tile_basic);
    }

    public QuickSettingsTile(Context context, QuickSettingsController qsc, int layout) {
        
        mContext = context;
        mDrawable = R.drawable.ic_notifications;
        mRealDrawable = null;
        mLabel = mContext.getString(R.string.quick_settings_label_enabled);
        mStatusbarService = qsc.mStatusBarService;
        mQsc = qsc;
        mTileLayout = layout;
        mPrefs = mContext.getSharedPreferences("quicksettings", Context.MODE_PRIVATE);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setupQuickSettingsTile(LayoutInflater inflater,
            QuickSettingsContainerView container) {
        container.updateResources();
        mTileTextColor = container.getTileTextColor();
        mTileTextSize = container.getTileTextSize();
        mTileTextPadding = container.getTileTextPadding();
        mTile = (QuickSettingsTileView) inflater.inflate(
                R.layout.quick_settings_tile, container, false);
        mTile.setContent(mTileLayout, inflater);
        mContainer = container;
        mContainer.addView(mTile);
        onPostCreate();
        updateQuickSettings();
        mTile.setOnClickListener(this);
        mTile.setOnLongClickListener(mOnLongClick);
    }

    public void switchToRibbonMode() {
        TextView tv = getLabelView();
        if (tv != null) {
            tv.setVisibility(View.GONE);
        }
        View image = getImageView();
        if (image != null) {
            MarginLayoutParams params = (MarginLayoutParams) image.getLayoutParams();
            int margin = mContext.getResources().getDimensionPixelSize(
                    R.dimen.qs_tile_ribbon_icon_margin);
            params.topMargin = params.bottomMargin = margin;
            image.setLayoutParams(params);
        }
    }

    public void switchToSmallIcons() {
        TextView tv = getLabelView();
        if (tv != null) {
            tv.setText(mLabel);
            tv.setTextSize(mTileTextSize);
            int dpi = mContext.getResources().getDisplayMetrics().densityDpi;
            if (dpi > DisplayMetrics.DENSITY_HIGH) {
                tv.setPadding(0, mTileTextPadding, 0, 0);
            }
        }
    }

    protected View getImageView() {
        return mTile.findViewById(R.id.image);
    }

    protected TextView getLabelView() {
        return (TextView) mTile.findViewById(R.id.text);
    }

    void onPostCreate() {}

    public void onDestroy() {}

    public void onReceive(Context context, Intent intent) {}

    public void onChangeUri(ContentResolver resolver, Uri uri) {}

    public void updateResources() {
        if (mTile != null) {
            updateQuickSettings();
        }
    }

    void updateQuickSettings() {
        TextView tv = getLabelView();
        if (tv != null) {
            tv.setText(mLabel);
            tv.setTextSize(mTileTextSize);
            int dpi = mContext.getResources().getDisplayMetrics().densityDpi;
            if (dpi > DisplayMetrics.DENSITY_HIGH) {
                tv.setPadding(0, mTileTextPadding, 0, 0);
                if (mTileTextColor != -2) {
                    tv.setTextColor(mTileTextColor);
                }
            }
        }
        View image = getImageView();
        if (image != null && image instanceof ImageView) {
            if (mRealDrawable == null) {
                ((ImageView) image).setImageResource(mDrawable);
            } else {
                ((ImageView) image).setImageDrawable(mRealDrawable);
            }
        }
    }

    public boolean isVibrationEnabled() {
        return (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES_VIBRATE, 1) == 1);
    }

    public void vibrateTile(int duration) {
        if (!isVibrationEnabled()) {
            return;
        }
        if (mVibrator != null) {
            if (mVibrator.hasVibrator()) {
                mVibrator.vibrate(duration);
            }
        }
    }

    public boolean isFlipTilesEnabled() {
        return (Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES_FLIP, 1) == 1);
    }

    public void flipTile(int delay, int flipId) {
        if (!isFlipTilesEnabled()) {
            return;
        }
        int animId;
        switch (flipId) {
            default:
            case FLIP_RIGHT:
                animId = R.anim.flip_right;
                break;
            case FLIP_LEFT:
                animId = R.anim.flip_left;
                break;
            case FLIP_UP:
                animId = R.anim.flip_up;
                break;
            case FLIP_DOWN:
                animId = R.anim.flip_down;
                break;
        }
        final AnimatorSet anim = (AnimatorSet) AnimatorInflater.loadAnimator(
                mContext, animId);
        anim.setTarget(mTile);
        anim.setDuration(200);
        anim.addListener(new AnimatorListener() {

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

        });

        Runnable doAnimation = new Runnable() {
            @Override
            public void run() {
                anim.start();
            }
        };

        mHandler.postDelayed(doAnimation, delay);
    }

    void startSettingsActivity(String action) {
        Intent intent = new Intent(action);
        startSettingsActivity(intent);
    }

    void startSettingsActivity(Intent intent) {
        startSettingsActivity(intent, true);
    }

    private void startSettingsActivity(Intent intent, boolean onlyProvisioned) {
        if (onlyProvisioned && !mStatusbarService.isDeviceProvisioned()) return;
        try {
            // Dismiss the lock screen when Settings starts.
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
            // ignored
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        mStatusbarService.animateCollapsePanels();
    }

    public void setColors(int bgColor, int presColor, float bgAlpha) {
        if (mTile != null) {
            mTile.setColors(bgColor, presColor, bgAlpha);
        }
    }

    @Override
    public void onClick(View v) {
        if (mOnClick != null) {
            mOnClick.onClick(v);
        }

        final ContentResolver resolver = mContext.getContentResolver();
        boolean shouldCollapse = Settings.System.getIntForUser(resolver,
                Settings.System.QS_COLLAPSE_PANEL, 0, UserHandle.USER_CURRENT) == 1;
        if (shouldCollapse) {
            mQsc.mBar.collapseAllPanels(true);
        }

        vibrateTile(30);
        flipTile(0, FLIP_DOWN);
    }
}
