package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.core.emoji.ObservableLinearLayout;
import ru.korniltsev.telegram.chat.adapter.Adapter;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.recycler.CheckRecyclerViewSpan;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.DaySplitter;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.Utils.uiName;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatView extends ObservableLinearLayout implements HandlesBack {
    public static final int SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT = 10;
    @Inject Presenter presenter;
    @Inject RxGlide picasso;
    @Inject ActivityOwner activity;

    private RecyclerView list;
    private MessagePanel messagePanel;
    private LinearLayoutManager layout;
    private ToolbarUtils toolbar;
    private AvatarView toolbarAvatar;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;

    private Adapter adapter;
    private final int toolbarAvatarSize;

    private Runnable viewSpanNotFilledAction = new Runnable() {
        @Override
        public void run() {
            presenter.listScrolledToEnd();
        }
    };
    private View btnScrollDown;
    private View emptyView;

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        toolbarAvatarSize = context.getResources().getDimensionPixelSize(R.dimen.toolbar_avatar_size);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = initToolbar(this)
                .pop(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                        Flow.get(v)
                                .goBack();
                    }
                })
                .customView(R.layout.chat_toolbar_title)
                .inflate(R.menu.chat)
                .setMenuClickListener(presenter);
        View customView = toolbar.getCustomView();
        assertNotNull(customView);
        toolbarAvatar = ((AvatarView) customView.findViewById(R.id.chat_avatar));
        toolbarTitle = ((TextView) customView.findViewById(R.id.title));
        toolbarSubtitle = ((TextView) customView.findViewById(R.id.subtitle));
        list = (RecyclerView) findViewById(R.id.list);
        messagePanel = (MessagePanel) findViewById(R.id.message_panel);
        btnScrollDown = findViewById(R.id.scroll_down);
        messagePanel.setListener(presenter);

        layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        adapter = new Adapter(getContext(), picasso, presenter.getPath().chat.lastReadOutboxMessageId, presenter.getPath().me.id);
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        btnScrollDown.setAlpha(0f);
        list.setOnScrollListener(
                new EndlessOnScrollListener(layout, adapter, /*waitForLastItem*/  new Runnable() {
                    @Override
                    public void run() {
                        presenter.listScrolledToEnd();
                    }
                }) {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        updateBtnScrollDown();
                    }
                });

        btnScrollDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnScrollDown.getAlpha() == 0f) {
                    return;
                }
                layout.scrollToPosition(0);
                list.stopScroll();
                btnScrollDown.clearAnimation();
                btnScrollDown.setAlpha(0);
            }
        });
        emptyView = findViewById(R.id.empty_view);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (adapter.getItemCount() == 0) {
                    if (emptyView.getVisibility() == INVISIBLE) {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setAlpha(0f);
                        emptyView.animate()
                                .alpha(1);
                    }
                } else {
                    emptyView.setVisibility(View.INVISIBLE);
                }
            }
        });
        activity.setStatusBarColor(getResources().getColor(R.color.primary_dark));

    }

    boolean scrollDownButtonIsVisible = false;

    private void updateBtnScrollDown() {
        boolean newVisible = layout.findFirstVisibleItemPosition() >= SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT;
        if (newVisible != scrollDownButtonIsVisible) {
            btnScrollDown.clearAnimation();
            btnScrollDown.animate()
                    .alpha(newVisible ? 1f : 0f);
            scrollDownButtonIsVisible = newVisible;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void initMenu(boolean groupChat, boolean muted) {
        if (!groupChat) {
            toolbar.hideMenu(R.id.menu_leave_group);
        }
        if (muted) {
            toolbar.hideMenu(R.id.menu_mute);
            toolbar.showMenu(R.id.menu_unmute);
        } else {
            toolbar.showMenu(R.id.menu_mute);
            toolbar.hideMenu(R.id.menu_unmute);
        }
    }

    public void loadToolBarImage(TdApi.Chat chat) {
        toolbarAvatar.loadAvatarFor(chat);
    }

    public void setGroupChatTitle(TdApi.GroupChat groupChat) {
        toolbarTitle.setText(
                groupChat.title);
    }

    public void setPrivateChatTitle(TdApi.User user) {
        toolbarTitle.setText(
                uiName(user));
    }

    public void setwGroupChatSubtitle(int total, int online) {
        //todo updates
        Resources res = getResources();
        String totalStr = res.getQuantityString(R.plurals.group_chat_members, total, total);
        String onlineStr = res.getQuantityString(R.plurals.group_chat_members_online, online, online);
        toolbarSubtitle.setText(
                totalStr + ", " + onlineStr);
    }

    private static DateTimeFormatter SUBTITLE_FORMATTER = DateTimeFormat.forPattern("dd/MM/yy");

    public void setPirvateChatSubtitle(TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline) {
            toolbarSubtitle.setText(R.string.user_status_online);
        } else if (status instanceof TdApi.UserStatusOffline) {
            long wasOnline = ((TdApi.UserStatusOffline) status).wasOnline;
            long timeInMillis = wasOnline * 1000;
            //            Date date = new Date(timeInMillis);
            DateTime dateTime = new DateTime(timeInMillis, DateTimeZone.UTC).withZone(DateTimeZone.getDefault());
            String date = SUBTITLE_FORMATTER.print(dateTime);
            toolbarSubtitle.setText(getResources().getString(R.string.user_status_last_seen) + " " + date);
        } else if (status instanceof TdApi.UserStatusLastWeek) {
            toolbarSubtitle.setText(R.string.user_status_last_week);
        } else if (status instanceof TdApi.UserStatusLastMonth) {
            toolbarSubtitle.setText(R.string.user_status_last_month);
        } else if (status instanceof TdApi.UserStatusRecently) {
            toolbarSubtitle.setText(R.string.user_status_recently);
        } else {
            //empty
        }
    }

    public void updateData(RxChat rxChat) {
        Adapter a = adapter;
        a.setChat(rxChat);
        List<TdApi.Message> messages = rxChat.getMessages();
        setMessages( messages);

        CheckRecyclerViewSpan.check(list, viewSpanNotFilledAction);
    }

    private final DaySplitter splitter = new DaySplitter();

    public void setMessages( List<TdApi.Message> messages) {
        List<RxChat.ChatListItem> split = splitter.split(messages);

//        boolean scrollDown;
//        int firstFullVisible = layout.findFirstCompletelyVisibleItemPosition();
//        if (firstFullVisible == 0) {
//            scrollDown = true;
//        } else {
//            if (layout.findFirstVisibleItemPosition() == 0) {
//                scrollDown = true;
//            } else {
//                scrollDown = false;
//            }
//        }
        adapter.setData(split);
//        if (scrollDown) {
//            layout.scrollToPosition(0);
//        }
    }



    @Override
    public boolean onBackPressed() {
        return messagePanel.onBackPressed();
    }

    public void showMessagePanel(boolean left) {
        if (left) {
            messagePanel.setVisibility(View.GONE);
        } else {
            messagePanel.setVisibility(View.VISIBLE);
        }
    }

    public void scrollToBottom() {
        layout.scrollToPosition(0);
    }

    public void hideAttachPannel() {
        messagePanel.hideAttachPannel();
    }



    public void addNewMessage(TdApi.Message message) {
        boolean scrollDown;
        int firstFullVisible = layout.findFirstCompletelyVisibleItemPosition();
        if (firstFullVisible == 0) {
            scrollDown = true;
        } else {
            if (layout.findFirstVisibleItemPosition() == 0) {
                scrollDown = true;
            } else {
                scrollDown = false;
            }
        }
        List<RxChat.ChatListItem> data = adapter.getData();
        List<RxChat.ChatListItem> prepend= splitter.prepend(data, message);

        Collections.reverse(prepend);
        adapter.addFirst(prepend);
        if (scrollDown) {
            layout.scrollToPosition(0);
        }
    }
}