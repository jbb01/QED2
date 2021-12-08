package com.jonahbauer.qed.util;

import android.app.Activity;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.activities.MainActivity;
import com.jonahbauer.qed.activities.mainFragments.QEDFragment;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.model.adapter.MessageAdapter;
import com.jonahbauer.qed.networking.NetworkConstants;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageUtils {
    private static final String COPY_FORMAT = "%3$tm-%3$td %3$tH:%3$tM:%3$tS\t%4$s\t%1$s:\t%2$s";

    /**
     * Sets the checked item in the list and shows an appropriate toolbar.
     *
     * @param position the position of the checked item in the {@link MessageAdapter}
     * @param value if the item is checked or not
     */
    public static void setChecked(@NonNull QEDFragment fragment,
                                  @NonNull ListView listView,
                                  @NonNull MessageAdapter adapter,
                                  int position, boolean value) {
        listView.setItemChecked(position, value);

        Activity activity = fragment.getActivity();
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;

            if (value) {
                Message msg = adapter.getItem(position);
                if (msg == null) return;

                Toolbar toolbar = mainActivity.borrowAltToolbar();
                toolbar.setNavigationOnClickListener(v -> setChecked(fragment, listView, adapter, position, false));

                toolbar.inflateMenu(R.menu.menu_message);
                toolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.message_info) {
                        Actions.showInfoSheet(fragment, msg);
                    } else if (item.getItemId() == R.id.message_copy) {
                        Actions.copy(fragment.requireContext(), fragment.requireView(), msg.getName(), MessageUtils.copyFormat(msg));
                        Actions.copy(fragment.requireContext(), fragment.requireView(), msg.getName(), msg.getMessage());
                    } else if (item.getItemId() == R.id.message_reply) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.GERMANY);
                        Actions.copy(fragment.requireContext(), fragment.requireView(), msg.getName(),
                                sdf.format(msg.getDate()) + "     " + msg.getName() + ": " + msg.getMessage() + "\n");
                    }

                    return false;
                });

                toolbar.setTitle(msg.getName().trim().isEmpty() ? activity.getText(R.string.message_name_anonymous) : msg.getName());
            } else {
                mainActivity.returnAltToolbar();
            }
        }
    }

    /**
     * The messages sent from the server contain only {@link java.time.LocalDateTime} information.
     * When trying to convert {@code LocalDateTime} to {@link java.time.Instant} on the day of
     * setting the clocks back both, the period from 02:00 CEST to 03:00 CEST and 02:00 to 03:00 CET
     * get mapped to 00:00 to 01:00 UTC.
     * Without further context this problem cannot be solved, but since the messages are ordered by
     * their id it is possible in most cases to find out which messages was sent from 02:00 to
     * 03:00 CEST and which from 02:00 to 03:00 CET.
     * <br>
     * The {@link Function} returned by this method keeps track of the most recent received timestamp
     * and shifts messages that are determined to be mistakenly assigned CEST one hour into the future.
     * <br>
     * For this {@code Function} to work, it must be applied to all the messages in the order of
     * their {@code id}.
     */
    public static Function<Message, Message> dateFixer() {
        // usually the timestamp should be increasing with increasing id
        // only when there is a local time overlap or a race condition or someone messed with the
        // database, can there be a higher timestamp before any message
        return new Function<>() {
            long max = Long.MIN_VALUE;

            @Override
            public Message apply(Message message) {
                long timestamp = message.getDate().getEpochSecond();

                max = Math.max(max, timestamp);
                if (max > timestamp) {
                    // to prevent detection of race conditions only mark messages within the correct
                    // time frame as needing a fix
                    // the timeframe is on the first sunday after (or on) october 25th of each year
                    // from 00:00:00 to 00:59:59 UTC
                    OffsetDateTime dateTime = OffsetDateTime.ofInstant(message.getDate(), ZoneId.of("UTC"));
                    if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY
                            && dateTime.getMonth() == Month.OCTOBER
                            && dateTime.getDayOfMonth() >= 25
                            && dateTime.getHour() == 0) {
                        return new Message(
                                message.getId(),
                                message.getName(),
                                message.getMessage(),
                                message.getDate().plusSeconds(3600),
                                message.getUserId(),
                                message.getUserName(),
                                message.getColor(),
                                message.getBottag(),
                                message.getChannel()
                        );
                    }
                }

                return message;
            }
        };
    }

    /**
     * Returns a string that mimics what one would get when trying to copy a message on the
     * browser chat client.
     */
    public static String copyFormat(Message message) {
        return String.format(
                Locale.GERMANY,
                COPY_FORMAT,
                message.getName(),
                message.getMessage(),
                ZonedDateTime.ofInstant(message.getDate(), NetworkConstants.SERVER_TIME_ZONE),
                message.getUserName() != null ? "✓" : ""
        );
    }
}