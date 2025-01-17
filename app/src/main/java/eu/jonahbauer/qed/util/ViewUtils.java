package eu.jonahbauer.qed.util;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import androidx.annotation.*;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import com.google.android.material.chip.Chip;
import eu.jonahbauer.qed.R;
import eu.jonahbauer.qed.databinding.AlertDialogEditTextBinding;
import eu.jonahbauer.qed.ui.transition.ExpandableView;
import eu.jonahbauer.qed.ui.views.InterceptingView;
import it.unimi.dsi.fastutil.Pair;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
@SuppressWarnings("unused")
public class ViewUtils {

    //<editor-fold desc="Padding" defaultstate="collapsed">
    public static void setPaddingTop(@NonNull View view, int paddingTop) {
        view.setPadding(
                view.getPaddingLeft(),
                paddingTop,
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingLeft(@NonNull View view, int paddingLeft) {
        view.setPadding(
                paddingLeft,
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingRight(@NonNull View view, int paddingRight) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                paddingRight,
                view.getPaddingBottom()
        );
    }

    public static void setPaddingBottom(@NonNull View view, int paddingBottom) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                paddingBottom
        );
    }
    //</editor-fold>

    public static void setWidth(@NonNull View view, int width) {
        setWidth(view, width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static void setWidth(@NonNull View view, int width, int fallbackHeight) {
        var params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(width, fallbackHeight);
        } else {
            params.width = width;
        }
        view.setLayoutParams(params);
    }

    public static void setHeight(@NonNull View view, int height) {
        setHeight(view, height, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public static void setHeight(@NonNull View view, int height, int fallbackWidth) {
        var params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(fallbackWidth, height);
        } else {
            params.height = height;
        }
        view.setLayoutParams(params);
    }

    public static void setSize(@NonNull View view, int width, int height) {
        var params = view.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(width, height);
        } else {
            params.width = width;
            params.height = height;
        }
        view.setLayoutParams(params);
    }

    /**
     * Prepares the given {@link EditText} for input of a {@link LocalDate} using a {@link DatePickerDialog}, i.e.
     * when the text field is clicked a date picker will be shown and the selected date will be reflected in the
     * text field and the provided live data.
     * Only when a {@link LifecycleOwner} is provided will changes to the live data also be reflected in the text field.
     * Since the provided live data stores a {@link LocalDateTime} it can be shared with a
     * {@linkplain #setupTimeEditText(LifecycleOwner, EditText, MutableLiveData) time text field}.
     *
     * @param owner a lifecycle owner used for observing the live data
     * @param editText a text field
     * @param date a live data that contains the currently selected date. {@link LiveData#getValue()} must not return {@code null}.
     * @see #setupTimeEditText(LifecycleOwner, EditText, MutableLiveData)
     */
    public static void setupDateEditText(@Nullable LifecycleOwner owner, @NonNull EditText editText, @NonNull MutableLiveData<LocalDateTime> date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        if (owner != null) {
            date.observe(owner, dateTime -> {
                editText.setText(formatter.format(dateTime));
            });
        }

        editText.setText(formatter.format(date.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            var value = Objects.requireNonNull(date.getValue());
            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        date.setValue(date.getValue().with(LocalDate.of(year, month + 1, dayOfMonth)));
                        editText.setText(formatter.format(date.getValue()));
                    },
                    value.getYear(),
                    value.getMonthValue() - 1,
                    value.getDayOfMonth()
            );
            dialog.show();
        });
    }

    /**
     * Prepares the given {@link EditText} for input of a {@link LocalTime} using a {@link TimePickerDialog}, i.e.
     * when the text field is clicked a ticker picker will be shown and the selected time will be reflected in the
     * text field and the provided live data.
     * Only when a {@link LifecycleOwner} is provided will changes to the live data also be reflected in the text field.
     * Since the provided live data stores a {@link LocalDateTime} it can be shared with a
     * {@linkplain #setupDateEditText(LifecycleOwner, EditText, MutableLiveData) date text field}.
     *
     * @param owner a lifecycle owner used for observing the live data
     * @param editText a text field
     * @param time a live data that contains the currently selected time. {@link LiveData#getValue()} must not return {@code null}.
     * @see #setupDateEditText(LifecycleOwner, EditText, MutableLiveData)
     */
    public static void setupTimeEditText(@Nullable LifecycleOwner owner, @NonNull EditText editText, @NonNull MutableLiveData<LocalDateTime> time) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        if (owner != null) {
            time.observe(owner, dateTime -> {
                editText.setText(formatter.format(dateTime));
            });
        }

        editText.setText(formatter.format(time.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            var value = Objects.requireNonNull(time.getValue());
            TimePickerDialog dialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        time.setValue(time.getValue().with(LocalTime.of(hourOfDay, minute)));
                        editText.setText(formatter.format(time.getValue()));
                    },
                    value.getHour(),
                    value.getMinute(),
                    android.text.format.DateFormat.is24HourFormat(context)
            );

            dialog.show();
        });
    }

    @Value
    @EqualsAndHashCode(of = "value")
    public class ChipItem {
        CharSequence label;
        String value;
    }

    @NonNull
    public static AlertDialog createPreferenceDialog(
            @NonNull Context context,
            @StringRes int title,
            @NonNull Supplier<String> getter,
            @NonNull Consumer<String> setter
    ) {
        return createPreferenceDialog(context, title, getter, setter, Collections.emptyList());
    }

    @NonNull
    public static AlertDialog createPreferenceDialog(
            @NonNull Context context,
            @StringRes int title,
            @NonNull Supplier<String> getter,
            @NonNull Consumer<String> setter,
            @NonNull Collection<? extends ChipItem> suggestions
    ) {
        var builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        var binding = AlertDialogEditTextBinding.inflate(LayoutInflater.from(context));
        builder.setView(binding.getRoot());

        var imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        builder.setPositiveButton(R.string.ok, (d, which) -> {
            var value = binding.input.getText().toString();
            setter.accept(value);
            // hiding the ime in onDismiss would be nicer but doesn't work, so we have to do it
            // in both button listeners
            imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        });
        builder.setNegativeButton(R.string.cancel, (d, which) -> {
            imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        });

        // add suggestion chips
        if (!suggestions.isEmpty()) {
            binding.suggestionLayout.setVisibility(View.VISIBLE);
        }
        var chips = suggestions.stream().map(suggestion -> {
            var view = new Chip(context);
            view.setText(suggestion.getLabel());
            binding.suggestions.addView(view);
            return Pair.of(view, suggestion.getValue());
        }).collect(Collectors.toList());

        // handle showing the ime analogous to EditTextPreferenceDialogFragmentCompat
        var showSoftInputRunnable = new Runnable() {
            private static final long SHOW_REQUEST_TIMEOUT = 1000;
            private long mShowRequestTime = -1;

            private boolean hasPendingShowSoftInputRequest() {
                return (mShowRequestTime != -1 && ((mShowRequestTime + SHOW_REQUEST_TIMEOUT)
                        > SystemClock.currentThreadTimeMillis()));
            }

            private void setPendingShowSoftInputRequest(boolean pendingShowSoftInputRequest) {
                mShowRequestTime = pendingShowSoftInputRequest ? SystemClock.currentThreadTimeMillis() : -1;
            }

            private void scheduleShowSoftInput() {
                setPendingShowSoftInputRequest(true);
                scheduleShowSoftInputInner();
            }

            private void scheduleShowSoftInputInner() {
                var editText = binding.input;
                if (hasPendingShowSoftInputRequest()) {
                    if (!editText.isFocused()) {
                        setPendingShowSoftInputRequest(false);
                        return;
                    }
                    // Schedule showSoftInput once the input connection of the editor established.
                    if (imm.showSoftInput(editText, 0)) {
                        setPendingShowSoftInputRequest(false);
                    } else {
                        editText.removeCallbacks(this);
                        editText.postDelayed(this, 50);
                    }
                }
            }

            @Override
            public void run() {
                scheduleShowSoftInputInner();
            }
        };

        var dialog = builder.create();

        chips.forEach(chip -> chip.left().setOnClickListener(v -> {
            setter.accept(chip.right());
            imm.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

            dialog.dismiss();
        }));

        dialog.setOnShowListener(d -> {
            binding.input.setText(getter.get());
            binding.input.requestFocus();
            binding.input.setSelection(binding.input.getText().length());
            showSoftInputRunnable.scheduleShowSoftInput();
        });
        return dialog;
    }

    public static void showPreferenceDialog(
            @NonNull Context context,
            @StringRes int title,
            @NonNull Supplier<String> getter,
            @NonNull Consumer<String> setter
    )
    {
        showPreferenceDialog(context, title, getter, setter, Collections.emptyList());
    }

    public static void showPreferenceDialog(
            @NonNull Context context,
            @StringRes int title,
            @NonNull Supplier<String> getter,
            @NonNull Consumer<String> setter,
            @NonNull Collection<? extends ChipItem> suggestions
    ) {
        createPreferenceDialog(context, title, getter, setter, suggestions).show();
    }

    public static void setError(@NonNull EditText editText, boolean error) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, error ? R.drawable.ic_error : 0, 0);
    }

    //<editor-fold desc="Conversions" defaultstate="collapsed">
    public static float spToPx(@NonNull DisplayMetrics displayMetrics, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, displayMetrics);
    }

    public static float spToPx(@NonNull Resources resources, float sp) {
        return spToPx(resources.getDisplayMetrics(), sp);
    }

    public static float spToPx(@NonNull Context context, float sp) {
        return spToPx(context.getResources(), sp);
    }

    public static float spToPx(@NonNull View view, float sp) {
        return spToPx(view.getResources(), sp);
    }

    public static float dpToPx(@NonNull DisplayMetrics displayMetrics, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }

    public static float dpToPx(@NonNull Resources resources, float dp) {
        return dpToPx(resources.getDisplayMetrics(), dp);
    }

    public static float dpToPx(@NonNull Context context, float dp) {
        return dpToPx(context.getResources(), dp);
    }

    public static float dpToPx(@NonNull View view, float dp) {
        return dpToPx(view.getResources(), dp);
    }

    public static float dpToPx(@NonNull Fragment fragment, float dp) {
        return dpToPx(fragment.getResources(), dp);
    }
    //</editor-fold>

    /**
     * Sets the title of the {@linkplain AppCompatActivity#getSupportActionBar() action bar} when present.
     * @throws IllegalStateException if the fragment is not attached to an activity.
     */
    public static void setActionBarText(@NonNull Fragment fragment, CharSequence title) {
        AppCompatActivity activity = (AppCompatActivity) fragment.requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    /**
     * Makes the status and navigation bar transparent.
     * @throws IllegalStateException if the fragment is not attached to an activity
     * @throws NullPointerException if the activity is not attached to a window
     * @see #resetTransparentSystemBars(Fragment)
     */
    public static void setTransparentSystemBars(@NonNull Fragment fragment) {
        var activity = fragment.requireActivity();
        var window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    /**
     * Resets the status and navigation bar to their default colors using the fragment's context
     * to resolve the respective attributes
     * ({@link android.R.attr#statusBarColor} and {@link android.R.attr#navigationBarColor}).
     * @throws IllegalStateException if the fragment is not attached to an activity
     * @throws NullPointerException if the activity is not attached to a window
     */
    public static void resetTransparentSystemBars(@NonNull Fragment fragment) {
        var theme = fragment.requireContext().getTheme();
        TypedValue statusBarColor = new TypedValue();
        theme.resolveAttribute(android.R.attr.statusBarColor, statusBarColor, true);
        TypedValue navigationBarColor = new TypedValue();
        theme.resolveAttribute(android.R.attr.navigationBarColor, navigationBarColor, true);

        var activity = fragment.requireActivity();
        var window = activity.getWindow();
        window.setStatusBarColor(statusBarColor.data);
        window.setNavigationBarColor(navigationBarColor.data);
    }

    public static @Px float getActionBarSize(@NonNull Context context) {
        var out = new TypedValue();
        if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, out, true)) {
            return out.getDimension(context.getResources().getDisplayMetrics());
        } else {
            return dpToPx(context, 56);
        }
    }

    @NonNull
    @Contract("_,_ -> new")
    public static ViewModelProvider getViewModelProvider(Fragment fragment, @IdRes int destinationId) {
        NavBackStackEntry entry = NavHostFragment.findNavController(fragment).getBackStackEntry(destinationId);
        return new ViewModelProvider(entry.getViewModelStore(), entry.getDefaultViewModelProviderFactory());
    }

    /**
     * Links the button's checked to the enabled state of the provided views. When the button gets checked
     * the first view will request focus. When an {@link InterceptingView} is provided and the button is not checked,
     * all touch events on the view will be intercepted and clicks will check the button.
     * @param parent an intercepting view. must be a parent of the button and all the specified target views
     */
    public static void link(@NonNull CompoundButton button, @Nullable InterceptingView parent, View...views) {
        var interceptListener = new InterceptClickListener(button, views, v -> {
            if (v != button) {
                button.setChecked(true);
                if (v != null) v.performClick();
            }
        });

        if (views.length == 0) return;
        var listener = (CompoundButton.OnCheckedChangeListener) (buttonView, isChecked) -> {
            if (parent != null) parent.setOnInterceptTouchListener(isChecked ? null : interceptListener);
            for (View view : views) {
                view.setEnabled(isChecked);
            }
            if (isChecked) views[0].requestFocus();
        };
        button.setOnCheckedChangeListener(listener);
        listener.onCheckedChanged(button, button.isChecked());
    }

    /**
     * Links the button's checked state to the visibility of the expanding view.
     * @see ExpandableView
     */
    public static void setupExpandable(
            @NonNull LifecycleOwner owner, @NonNull MutableLiveData<Boolean> state,
            @NonNull CompoundButton button, @NonNull View expandingView
    ) {
        var drawable = SeekableAnimatedVectorDrawable.create(button.getContext(), R.drawable.ic_arrow_down_accent_animation);
        button.setButtonDrawable(drawable);

        var expandable = ExpandableView.from(expandingView);
        expandable.setDrawable(drawable);

        var observer = (Observer<Boolean>) isChecked -> {
            if (isChecked == null) isChecked = false;
            expandable.setExpanded(isChecked, true);
            button.setChecked(isChecked);
        };

        var isInitiallyChecked = (boolean) Objects.requireNonNullElse(state.getValue(), false);
        expandable.setExpanded(isInitiallyChecked, false);
        button.setChecked(isInitiallyChecked);

        state.observe(owner, observer);

        button.setOnCheckedChangeListener((view, isChecked) -> {
            state.setValue(isChecked);
        });
    }

    @RequiredArgsConstructor
    private static class InterceptClickListener implements InterceptingView.OnInterceptTouchListener {
        private static final Object NO_TARGET = new Object();

        private final View[] mViews;
        private final Rect mRect = new Rect();

        private Object mDownTarget = NO_TARGET;
        private final @NonNull Consumer<View> mOnClick;

        private InterceptClickListener(@NonNull View button, @NonNull View[] views, @NonNull Consumer<View> onClick) {
            mViews = new View[views.length + 1];
            mViews[0] = button;
            System.arraycopy(views, 0, mViews, 1, views.length);
            mOnClick = onClick;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            var x = (int) event.getX();
            var y = (int) event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDownTarget = getTarget(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    var target = getTarget(x, y);
                    if (mDownTarget == target) {
                        mOnClick.accept(target);
                    }
                    mDownTarget = NO_TARGET;
                    break;
            }
            return false;
        }

        private @Nullable View getTarget(int x, int y) {
            for (var view : mViews) {
                view.getHitRect(mRect);
                if (mRect.contains(x, y)) {
                    return view;
                }
            }
            return null;
        }
    }
}
