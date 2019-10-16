package com.jonahbauer.qed.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jonahbauer.qed.R;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

    }

    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(pref.getKey())
                .commit();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref, rootKey);
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);
        }
    }

    public static class ChatPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_chat, rootKey);
            setHasOptionsMenu(true);

            EditTextPreference name = findPreference(getString(R.string.preferences_chat_name_key));
            EditTextPreference channel = findPreference(getString(R.string.preferences_chat_channel_key));

            if (name != null)
                name.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            if (channel != null)
                channel.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    public static class GalleryPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
        private Preference deleteThumbnails;
        private Preference deleteImages;
        private Preference showDir;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_gallery, rootKey);

            deleteThumbnails = findPreference(getString(R.string.preferences_gallery_delete_thumbnails_key));
            deleteImages = findPreference(getString(R.string.preferences_gallery_delete_images_key));
            showDir = findPreference(getString(R.string.preferences_gallery_show_dir_key));
            deleteThumbnails.setOnPreferenceClickListener(this);
            deleteImages.setOnPreferenceClickListener(this);
            showDir.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Context context = getActivity();
            if (context == null) return false;

            if (preference.equals(deleteImages)) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setMessage(R.string.confirm_delete_images);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    File dir = context.getExternalFilesDir(context.getString(R.string.gallery_folder_images));
                    if (dir != null && dir.exists()) {
                        try {
                            FileUtils.cleanDirectory(dir);
                        } catch (IOException ignored) {
                        }
                    }
                });
                alertDialog.show();
                return true;
            } else if (preference.equals(deleteThumbnails)) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setMessage(R.string.confirm_delete_thumbnails);
                alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                alertDialog.setPositiveButton(R.string.delete, (dialog, which) -> {
                    File dir = context.getExternalFilesDir(context.getString(R.string.gallery_folder_thumbnails));
                    if (dir != null && dir.exists()) {
                        try {
                            FileUtils.cleanDirectory(dir);
                        } catch (IOException ignored) {
                        }
                    }
                });
                alertDialog.show();
                return true;
            } else if (preference.equals(showDir)) {
                File dir = context.getExternalFilesDir("gallery");
                if (dir == null) return false;


                Intent intent2 = new Intent();
                intent2.setAction(Intent.ACTION_VIEW);
                intent2.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Uri uri = Uri.parse(dir.toString());

                intent2.setDataAndType(uri, "resource/folder");
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent2, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                if (resInfoList.size() == 0) {
                    Toast.makeText(context, context.getString(R.string.no_file_explorer), Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(intent2);
                }

                return true;
            }
            return false;
        }
    }

}
