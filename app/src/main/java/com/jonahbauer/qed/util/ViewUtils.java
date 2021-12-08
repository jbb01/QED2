package com.jonahbauer.qed.util;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;

import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.databinding.AlertDialogEditTextBinding;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("unused")
public class ViewUtils {

    /**
     * Expands the given view.
     */
    public static void expand(final View view) {
        // measure view
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(matchParentMeasureSpec, wrapContentMeasureSpec);

        final int duration = view.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        final int targetHeight = view.getMeasuredHeight();
        final int initialPadding = view.getPaddingTop();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        view.getLayoutParams().height = 1;
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0);

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    view.setAlpha(1);
                    setPaddingTop(view, initialPadding);
                } else {
                    view.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                    view.setAlpha(interpolatedTime);
                    setPaddingTop(view, initialPadding - (int) (targetHeight * (1 - interpolatedTime)));
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    /**
     * Collapses the given view.
     */
    public static void collapse(final View view) {
        final int duration = view.getContext().getResources().getInteger(android.R.integer.config_mediumAnimTime);
        final int initialHeight = view.getMeasuredHeight();
        final int initialPadding = view.getPaddingTop();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1);
                    setPaddingTop(view, initialPadding);
                } else {
                    view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    view.setAlpha(1 - interpolatedTime);
                    setPaddingTop(view, initialPadding - (int) (initialHeight * interpolatedTime));
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    public static void setPaddingTop(View view, int paddingTop) {
        view.setPadding(
                view.getPaddingLeft(),
                paddingTop,
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingLeft(View view, int paddingLeft) {
        view.setPadding(
                paddingLeft,
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        );
    }

    public static void setPaddingRight(View view, int paddingRight) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                paddingRight,
                view.getPaddingBottom()
        );
    }

    public static void setPaddingBottom(View view, int paddingBottom) {
        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                paddingBottom
        );
    }

    public static void setupDateEditText(EditText editText, MutableLiveData<LocalDate> date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        editText.setText(formatter.format(date.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        date.setValue(LocalDate.of(year, month + 1, dayOfMonth));
                        editText.setText(formatter.format(date.getValue()));
                    },
                    date.getValue().getYear(),
                    date.getValue().getMonthValue() - 1,
                    date.getValue().getDayOfMonth()
            );
            dialog.show();
        });
    }

    public static void setupTimeEditText(EditText editText, MutableLiveData<LocalTime> time) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault());

        editText.setText(formatter.format(time.getValue()));
        editText.setOnClickListener(v -> {
            Context context = editText.getContext();

            TimePickerDialog dialog = new TimePickerDialog(
                    context,
                    (view, hourOfDay, minute) -> {
                        time.setValue(LocalTime.of(hourOfDay, minute));
                        editText.setText(formatter.format(time.getValue()));
                    },
                    time.getValue().getHour(),
                    time.getValue().getMinute(),
                    android.text.format.DateFormat.is24HourFormat(context)
            );

            dialog.show();
        });
    }

    public static void showPreferenceDialog(Context context, @StringRes int title, Supplier<String> getter, Consumer<String> setter) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(title);

        var binding = AlertDialogEditTextBinding.inflate(LayoutInflater.from(dialog.getContext()));
        binding.input.setText(getter.get());

        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(R.string.ok, (d, which) -> {
            setter.accept(binding.input.getText().toString());
            d.dismiss();
        });
        dialog.setNegativeButton(R.string.cancel, (d, which) -> d.cancel());
        dialog.show();
    }

    public static void setError(EditText editText, boolean error) {
        editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, error ? R.drawable.ic_error : 0, 0);
    }
}