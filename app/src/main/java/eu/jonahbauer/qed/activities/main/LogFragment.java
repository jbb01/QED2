package eu.jonahbauer.qed.activities.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import eu.jonahbauer.qed.Application;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.FragmentLogBinding;
import eu.jonahbauer.qed.model.LogRequest;
import eu.jonahbauer.qed.ui.adapter.MessageAdapter;
import eu.jonahbauer.qed.model.room.Database;
import eu.jonahbauer.qed.model.viewmodel.LogViewModel;
import eu.jonahbauer.qed.networking.Reason;
import eu.jonahbauer.qed.util.*;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static eu.jonahbauer.qed.model.LogRequest.DateRecentLogRequest;

public class LogFragment extends Fragment implements MenuProvider {
    public static final String LOG_REQUEST_KEY = "logRequest";
    private final LogRequest DEFAULT_REQUEST = new DateRecentLogRequest(Preferences.getChat().getChannel(), 24, TimeUnit.HOURS);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_DONE = 2;

    private MessageAdapter mMessageAdapter;
    private FragmentLogBinding mBinding;

    private LogViewModel mLogViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // handle deep link
        Bundle arguments = getArguments();
        if (arguments != null) {
            var request = LogFragmentArgs.fromBundle(arguments).getLogRequest();

            if (request == null && arguments.containsKey(NavController.KEY_DEEP_LINK_INTENT)) {
                var intent = (Intent) arguments.get(NavController.KEY_DEEP_LINK_INTENT);
                if (intent != null) {
                    request = LogRequest.parse(intent.getData());
                    if (request == null) {
                        Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            if (request != null) {
                var entry = NavHostFragment.findNavController(this).getCurrentBackStackEntry();
                if (entry != null) {
                    entry.getSavedStateHandle().set(LOG_REQUEST_KEY, request);
                } else {
                    Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show();
                }
            }
        }

        TransitionUtils.setupDefaultTransitions(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentLogBinding.inflate(inflater, container, false);
        mLogViewModel = ViewUtils.getViewModelProvider(this, R.id.nav_chat_log).get(LogViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        NavController navController = NavHostFragment.findNavController(this);
        NavBackStackEntry entry = navController.getBackStackEntry(R.id.nav_chat_log);
        Objects.requireNonNull(entry);
        entry.getSavedStateHandle()
             .getLiveData(LOG_REQUEST_KEY, DEFAULT_REQUEST)
             .observe(getViewLifecycleOwner(), mLogViewModel::load);

        mMessageAdapter = new MessageAdapter(mBinding.list, null, false, null, false);
        mBinding.list.setAdapter(mMessageAdapter);
        mBinding.list.setOnItemClickListener((parent, view1, position, id) -> {
            setCheckedItem(MessageAdapter.INVALID_POSITION);
        });
        mBinding.list.setOnItemLongClickListener((parent, view1, position, id) -> {
            setCheckedItem(position);
            return true;
        });

        mBinding.subtitle.setOnClickListener(v -> {
            var dialog = LogDialog.newInstance(Objects.requireNonNullElse(mLogViewModel.getLogRequest().getValue(), DEFAULT_REQUEST));
            dialog.show(getChildFragmentManager(), null);
        });

        mLogViewModel.getDownloadStatus().observe(getViewLifecycleOwner(), pair -> {
            if (pair == null) {
                mBinding.setDownloadStatus(STATUS_PENDING);
                mBinding.setDownloadText(getString(R.string.log_status_download_pending));
            } else {
                double done = pair.leftLong() / 1_048_576d;

                if (done > Application.MEMORY_CLASS) {
                    mBinding.setStatusText(getString(R.string.log_status_likely_oom));
                }

                if (pair.leftLong() == pair.rightLong()) {
                    mBinding.setDownloadStatus(STATUS_DONE);
                    mBinding.setDownloadText(getString(R.string.log_status_download_successful, done));
                } else {
                    mBinding.setDownloadStatus(STATUS_RUNNING);
                    mBinding.setDownloadText(getString(R.string.log_status_downloading, done));
                }
            }
        });

        mLogViewModel.getParseStatus().observe(getViewLifecycleOwner(), pair -> {
            if (pair == null) {
                mBinding.setParseStatus(STATUS_PENDING);
                mBinding.setParseText(getString(R.string.log_status_parse_pending));
            } else {
                long done = pair.leftLong();
                long total = pair.rightLong();

                if (done == total) {
                    mBinding.setParseStatus(STATUS_DONE);
                } else {
                    mBinding.setParseStatus(STATUS_RUNNING);
                }
                mBinding.setParseText(getString(R.string.log_status_parsing, done, total));
            }
        });

        mLogViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            mBinding.setStatus(messages.getCode());

            mMessageAdapter.clear();
            if (messages.getCode() == StatusWrapper.STATUS_LOADED) {
                mMessageAdapter.addAll(messages.getValue());
            } else if (messages.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = messages.getReason();
                mBinding.setError(getString(reason.getStringRes()));
            }
            mMessageAdapter.notifyDataSetChanged();

            if (messages.getCode() == StatusWrapper.STATUS_LOADED) {
                setCheckedItem(mLogViewModel.getCheckedItemPosition());
            } else {
                setCheckedItem(MessageAdapter.INVALID_POSITION);
            }
        });

        mLogViewModel.getLogRequest().observe(getViewLifecycleOwner(), logRequest -> {
            mBinding.setSubtitle(logRequest.getSubtitle(getResources()));
            mBinding.setStatusText(null);
        });
    }

    /**
     * Sets the checked item in the list and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link #mMessageAdapter}
     */
    private void setCheckedItem(int position) {
        mLogViewModel.setCheckedItemPosition(position);
        MessageUtils.setCheckedItem(
                this,
                mBinding.list,
                mMessageAdapter,
                (mode, msg) -> NavHostFragment.findNavController(this)
                                              .navigate(LogFragmentDirections.showMessage(msg)),
                null,
                position
        );
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_log, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.log_save) {
            mBinding.setSaving(true);
            item.setEnabled(false);
            //noinspection ResultOfMethodCallIgnored
            Database.getInstance(requireContext()).messageDao()
                    .insert(mMessageAdapter.getData())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                        mBinding.setSaving(false);
                        item.setEnabled(true);
                    })
                    .subscribe(
                            () -> Snackbar.make(requireView(), R.string.saved, Snackbar.LENGTH_SHORT).show(),
                            (e) -> Snackbar.make(requireView(), R.string.save_error, Snackbar.LENGTH_SHORT).show()
                    );
            return true;
        }
        return false;
    }
}
