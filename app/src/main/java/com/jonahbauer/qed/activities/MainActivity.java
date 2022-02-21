package com.jonahbauer.qed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.*;
import androidx.customview.widget.Openable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.jonahbauer.qed.BuildConfig;
import com.jonahbauer.qed.MainDirections;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.ActivityMainBinding;
import com.jonahbauer.qed.layoutStuff.CustomActionMode;
import com.jonahbauer.qed.networking.NetworkListener;
import com.jonahbauer.qed.networking.login.QEDLogout;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NetworkListener, NavController.OnDestinationChangedListener {
    private static final String LOG_TAG = MainActivity.class.getName();

    private ActivityMainBinding mBinding;

    private final IntSet mTopLevelDestinations = IntSet.of(
            R.id.nav_chat, R.id.nav_chat_log, R.id.nav_chat_db,
            R.id.nav_database_persons, R.id.nav_database_events,
            R.id.nav_gallery,
            R.id.nav_login
    );
    private AppBarConfiguration mAppBarConfiguration;
    private NavController mNavController;

    private WindowInsetsControllerCompat mWindowInsetsController;

    private ActionMode mActionMode;
    private ViewPropertyAnimatorCompat mActionModeFade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App_NoActionBar);
        super.onCreate(savedInstanceState);

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        setSupportActionBar(mBinding.toolbar);

        DrawerLayout drawerLayout = mBinding.drawerLayout;
        NavigationView navigationView = mBinding.navView;

        mNavController = Navigation.findNavController(this, R.id.nav_host);

        mAppBarConfiguration = new AppBarConfiguration.Builder(mTopLevelDestinations)
                .setOpenableLayout(drawerLayout)
                .build();
        NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, mNavController);

        mNavController.addOnDestinationChangedListener(this);

        navigationView.getMenu()
                      .findItem(R.id.nav_logout)
                      .setOnMenuItemClickListener(this::onLogout);

        // double tap to exit
        getOnBackPressedDispatcher().addCallback(this, new BackPressedCallback());

        mWindowInsetsController = new WindowInsetsControllerCompat(getWindow(), mBinding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private Fragment getFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (fragment instanceof NavHostFragment) {
            NavHostFragment navHostFragment = (NavHostFragment) fragment;
            List<Fragment> fragments = navHostFragment.getChildFragmentManager().getFragments();
            if (fragments.size() > 0) {
                return fragments.get(0);
            }
        }
        return null;
    }

    private boolean onLogout(MenuItem item) {
        QEDLogout.logoutAsync(success -> {
            if (!success) {
                Log.e(LOG_TAG, "An error occured during logout.");
            } else {
                Snackbar.make(mBinding.getRoot(), R.string.logout_success, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        mNavController.navigate(MainDirections.login());
        ViewParent viewParent = mBinding.navView.getParent();
        if (viewParent instanceof Openable) {
            ((Openable) viewParent).close();
        }
        return true;
    }

    public WindowInsetsControllerCompat getWindowInsetsController() {
        return mWindowInsetsController;
    }

    public AppBarLayout getAppBarLayout() {
        return mBinding.appBarLayout;
    }

    //<editor-fold desc="Action Modes" defaultstate="collapsed">
    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(@NonNull ActionMode.Callback callback) {
        Objects.requireNonNull(callback);

        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }

        var toolbar = mBinding.actionModeToolbar;
        var appBarLayout = mBinding.actionModeAppBarLayout;

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mBinding.actionModeToolbar.setNavigationOnClickListener(v -> finishActionMode());

        var actionMode = new CustomActionMode(
                mBinding.actionModeToolbar,
                new ActionModeCallbackWrapper(callback)
        );

        if (callback.onCreateActionMode(actionMode, actionMode.getMenu())) {
            if (mActionModeFade != null) mActionModeFade.cancel();
            appBarLayout.setAlpha(0f);
            mActionModeFade = ViewCompat.animate(appBarLayout).alpha(1f);
            mActionModeFade.setListener(new ViewPropertyAnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(View view) {
                    appBarLayout.setVisibility(View.VISIBLE);
                    ViewCompat.requestApplyInsets(appBarLayout);
                }

                @Override
                public void onAnimationEnd(View view) {
                    appBarLayout.setAlpha(1f);
                    mActionModeFade.setListener(null);
                    mActionModeFade = null;
                }
            });
            mActionModeFade.start();

            appBarLayout.setVisibility(View.VISIBLE);
            onSupportActionModeStarted(actionMode);
            mActionMode = actionMode;
        }

        return mActionMode;
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }
    //</editor-fold>

    //<editor-fold desc="Network Listener" defaultstate="collapsed">
    /**
     * Relays a network connection failure event to the content mFragment
     */
    @Override
    public void onConnectionFail() {
        Fragment fragment = getFragment();
        if (fragment instanceof NetworkListener) {
            ((NetworkListener) fragment).onConnectionFail();
        }
    }

    /**
     * Relays a network connection regain event to the content mFragment
     */
    @Override
    public void onConnectionRegain() {
        Fragment fragment = getFragment();
        if (fragment instanceof NetworkListener) {
            ((NetworkListener) fragment).onConnectionFail();
        }
    }
    //</editor-fold>

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Navigation.findNavController(this, R.id.nav_host).handleDeepLink(intent);
    }

    @Override
    public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Destination changed to " + getResources().getResourceName(destination.getId()));
        }
        DrawerSelection drawerSelection = DrawerSelection.byMenuItemId(destination.getId(), null);
        if (drawerSelection != null) {
            mBinding.navView.setCheckedItem(drawerSelection.getMenuItemId());
        }

        var lockMode = mTopLevelDestinations.contains(destination.getId())
                ? DrawerLayout.LOCK_MODE_UNLOCKED
                : DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        mBinding.drawerLayout.setDrawerLockMode(lockMode);
    }

    @Getter
    public enum DrawerSelection {
        CHAT(R.id.nav_chat),
        CHAT_LOG(R.id.nav_chat_log),
        CHAT_DATABASE(R.id.nav_chat_db),
        DATABASE_PEOPLE(R.id.nav_database_persons),
        DATABASE_EVENTS(R.id.nav_database_events),
        GALLERY(R.id.nav_gallery);

        @IdRes
        private final int menuItemId;

        DrawerSelection(int menuItemId) {
            this.menuItemId = menuItemId;
        }

        public static DrawerSelection byMenuItemId(@IdRes int menuItemId, DrawerSelection defaultValue) {
            for (DrawerSelection value : values()) {
                if (menuItemId == value.getMenuItemId()) {
                    return value;
                }
            }
            return defaultValue;
        }
    }

    private class ActionModeCallbackWrapper implements ActionMode.Callback {
        private final ActionMode.Callback mWrapped;

        private ActionModeCallbackWrapper(ActionMode.Callback mWrapped) {
            this.mWrapped = mWrapped;
        }
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mActionMode = null;

            if (mActionModeFade != null) mActionModeFade.cancel();
            mActionModeFade = ViewCompat.animate(mBinding.actionModeAppBarLayout).alpha(0f);
            mActionModeFade.setListener(new ViewPropertyAnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(View view) {
                    mBinding.actionModeAppBarLayout.setVisibility(View.GONE);
                    mActionModeFade.setListener(null);
                    mActionModeFade = null;
                }
            });
            mActionModeFade.start();
        }
    }

    private class BackPressedCallback extends OnBackPressedCallback {
        private boolean mDoubleBackToExit = false;

        public BackPressedCallback() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {
            NavBackStackEntry currentEntry = mNavController.getCurrentBackStackEntry();
            if (currentEntry != null) {
                if (mTopLevelDestinations.contains(currentEntry.getDestination().getId())) {
                    if (mDoubleBackToExit) {
                        finish();
                    } else {
                        pressAgain();
                    }
                    return;
                }
            }

            NavBackStackEntry previousEntry = mNavController.getPreviousBackStackEntry();
            if (mDoubleBackToExit || (previousEntry != null)) {
                this.setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                this.setEnabled(true);
            } else {
                pressAgain();
            }
        }

        private void pressAgain() {
            mDoubleBackToExit = true;
            Toast.makeText(MainActivity.this, R.string.double_back_to_exit_toast, Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> mDoubleBackToExit = false, 2000);
        }
    }
}
