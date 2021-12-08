package com.jonahbauer.qed.model.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.jonahbauer.qed.R;
import com.jonahbauer.qed.layoutStuff.views.MathView;
import com.jonahbauer.qed.layoutStuff.views.MessageView;
import com.jonahbauer.qed.model.Message;
import com.jonahbauer.qed.util.Preferences;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MessageAdapter extends ArrayAdapter<Message> {
    private final Context mContext;
    private final List<Message> mMessageList;

    // Date Banners
    private final DateTimeFormatter mDateBannerFormat = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault());

    private final int mDp3;

    // Settings
    private boolean mLinkify;
    private boolean mColorful;
    private boolean mKatex;
    private final boolean mExtended;

    private boolean mKatexSet;
    private final boolean mLinkifySet;

    @Px
    private float mDefaultTextSize;
    @ColorInt
    private int mDefaultTextColor;
    private final LinearLayout mathPreload;

    public MessageAdapter(Context context, @NonNull List<Message> messageList, @Nullable LinearLayout mathPreload) {
        this(context, messageList, mathPreload, null, null, false);
    }

    public MessageAdapter(Context context, @NonNull List<Message> messageList, @Nullable LinearLayout mathPreload, @Nullable Boolean katex, @Nullable Boolean linkify, boolean extended) {
        super(context, 0, messageList);
        this.mContext = context;
        this.mMessageList = messageList;
        this.mathPreload = mathPreload;

        this.mExtended = extended;

        this.mLinkifySet = linkify != null;
        if (this.mLinkifySet) this.mLinkify = linkify;

        this.mKatexSet = katex != null;
        if (this.mKatexSet) this.mKatex = katex;

        if (mExtended && mKatexSet && mKatex) throw new IllegalArgumentException("Extended message views do not support Katex!");

        mDp3 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, context.getResources().getDisplayMetrics());
        obtainDefaultTextAppearance();
        reload();
    }

    private void obtainDefaultTextAppearance() {
        mDefaultTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, mContext.getResources().getDisplayMetrics());
        mDefaultTextColor = Color.BLACK;

        TypedArray typedArray = mContext.obtainStyledAttributes(R.style.Widget_App_Message, R.styleable.MessageView);
        int textAppearanceResId = typedArray.getResourceId(R.styleable.MessageView_messageTextAppearance, -1);
        if (textAppearanceResId != -1) {
            TypedArray textAppearance = mContext.obtainStyledAttributes(textAppearanceResId, new int[] {android.R.attr.textSize, android.R.attr.textColor});
            mDefaultTextSize = textAppearance.getDimension(0, mDefaultTextSize);
            mDefaultTextColor = textAppearance.getColor(1, mDefaultTextColor);
            textAppearance.recycle();
        }
        mDefaultTextSize = typedArray.getDimension(R.styleable.MessageView_messageTextSize, mDefaultTextSize);
        mDefaultTextColor = typedArray.getColor(R.styleable.MessageView_messageTextColor, mDefaultTextColor);

        typedArray.recycle();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Message message = mMessageList.get(position);

        MessageView view = null;
        if (convertView instanceof MessageView) {
            view = (MessageView) convertView;
            if (view.isExtended() != mExtended) {
                view = null;
            }
        }

        if (view == null) {
            view = new MessageView(mContext, mExtended);
        }

        boolean hasBanner = (position == 0)
                || (message.getLocalDate().isAfter(mMessageList.get(position - 1).getLocalDate()));

        view.setPadding(mDp3, mDp3, mDp3, mDp3);
        view.setKatex(mKatex);
        view.setMessage(message);
        view.setFocusable(false);
        view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        view.setDateBanner(hasBanner ? mDateBannerFormat.format(message.getLocalDate()) : null);
        view.setLinkify(mLinkify);
        view.setColorful(mColorful);

        return view;
    }

    public List<Message> getData() {
        return new LinkedList<>(mMessageList);
    }

    @Override
    public void add(Message message) {
        super.add(message);

        if (mKatex) {
            MathView.extractAndPreload(mContext, null, message.getMessage(), (int) mDefaultTextSize, mDefaultTextColor, message.getId(), mathPreload);
        }
    }

    @Override
    public void addAll(@NonNull Collection<? extends Message> collection) {
        super.addAll(collection);

        if (mKatex) {
            for (Message message : collection) {
                MathView.extractAndPreload(mContext, null, message.getMessage(), (int) mDefaultTextSize, mDefaultTextColor, message.getId(), mathPreload);
            }
        }
    }

    @Override
    public int getCount() {
        return mMessageList.size();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        if (position < mMessageList.size())
            return mMessageList.get(position).getId();
        else
            return -1;
    }

    public void reload() {
        mColorful = Preferences.chat().isColorful();
        if (!mLinkifySet)
            mLinkify = Preferences.chat().isLinkify();
        if (!mKatexSet)
            mKatex = Preferences.chat().isKatex();

        MathView.clearCache();
        if (mathPreload != null) mathPreload.removeAllViews();
        if (mKatex && !mExtended) {
            for (Message message : mMessageList) {
                MathView.extractAndPreload(mContext, null, message.getMessage(), (int) mDefaultTextSize, mDefaultTextColor, message.getId(), mathPreload);
            }
        }
    }

    public void setKatex(Boolean katex) {
        this.mKatexSet = katex != null;
        if (this.mKatexSet) this.mKatex = katex;

        reload();
    }

    @Override
    public void clear() {
        super.clear();
        reload();
    }
}