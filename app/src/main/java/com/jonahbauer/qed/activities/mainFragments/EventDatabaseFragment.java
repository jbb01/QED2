package com.jonahbauer.qed.activities.mainFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.FragmentEventsDatabaseBinding;
import com.jonahbauer.qed.model.Event;
import com.jonahbauer.qed.model.adapter.EventAdapter;
import com.jonahbauer.qed.model.viewmodel.EventListViewModel;
import com.jonahbauer.qed.networking.Reason;
import com.jonahbauer.qed.util.StatusWrapper;
import com.jonahbauer.qed.util.ViewUtils;

import java.util.ArrayList;

public class EventDatabaseFragment extends Fragment implements AdapterView.OnItemClickListener {
    private EventAdapter mEventAdapter;
    private FragmentEventsDatabaseBinding mBinding;

    private EventListViewModel mEventListViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentEventsDatabaseBinding.inflate(inflater, container, false);
        mEventListViewModel = ViewUtils.getViewModelProvider(this).get(EventListViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewUtils.setFitsSystemWindowsBottom(view);

        mEventAdapter = new EventAdapter(getContext(), new ArrayList<>());
        mBinding.list.setOnItemClickListener(this);
        mBinding.list.setAdapter(mEventAdapter);

        mEventListViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            mBinding.setStatus(events.getCode());

            mEventAdapter.clear();
            if (events.getCode() == StatusWrapper.STATUS_LOADED) {
                mEventAdapter.addAll(events.getValue());
            } else if (events.getCode() == StatusWrapper.STATUS_ERROR) {
                Reason reason = events.getReason();
                mBinding.setError(getString(reason == Reason.EMPTY ? R.string.database_empty : reason.getStringRes()));
            }
            mEventAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Event event = mEventAdapter.getItem(position);
        if (event != null) {
            var action = EventDatabaseFragmentDirections.showEvent(event);
            Navigation.findNavController(view).navigate(action);
        }
    }
}
