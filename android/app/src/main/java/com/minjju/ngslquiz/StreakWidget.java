package com.minjju.ngslquiz;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class StreakWidget extends AppWidgetProvider {
    static final String PREFS = "streak";
    static final String KEY_DAYS = "days";

    private static final int[] DAY_IDS = {R.id.day0, R.id.day1, R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6};

    static final int FULL = 0, SMALL = 1, MEDIUM = 2;

    protected int size() { return FULL; }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        RemoteViews v = build(context, size());
        for (int id : ids) manager.updateAppWidget(id, v);
    }

    static void updateAll(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        Class<?>[] classes = {StreakWidget.class, StreakWidget1x1.class, StreakWidget2x1.class};
        int[] sizes = {FULL, SMALL, MEDIUM};
        for (int k = 0; k < classes.length; k++) {
            int[] ids = m.getAppWidgetIds(new ComponentName(context, classes[k]));
            if (ids == null || ids.length == 0) continue;
            RemoteViews v = build(context, sizes[k]);
            for (int id : ids) m.updateAppWidget(id, v);
        }
    }

    private static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static boolean has(JSONObject days, Calendar c) {
        return days.optInt(key(c), 0) > 0;
    }

    static RemoteViews build(Context context, int size) {
        JSONObject days;
        try {
            String s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DAYS, "{}");
            days = new JSONObject(s);
        } catch (Exception e) {
            days = new JSONObject();
        }

        Calendar today = Calendar.getInstance();
        boolean doneToday = has(days, today);
        int todayCount = days.optInt(key(today), 0);

        // 연속 일수: 오늘 했으면 오늘부터, 아니면 어제부터 거꾸로 센다
        Calendar c = (Calendar) today.clone();
        if (!doneToday) c.add(Calendar.DAY_OF_MONTH, -1);
        int streak = 0;
        while (has(days, c) && streak < 10000) {
            streak++;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }

        int layout = size == SMALL ? R.layout.widget_streak_1x1 : size == MEDIUM ? R.layout.widget_streak_2x1 : R.layout.widget_streak;
        RemoteViews v = new RemoteViews(context.getPackageName(), layout);
        v.setTextViewText(R.id.count, String.valueOf(streak));
        v.setFloat(R.id.fire, "setAlpha", doneToday ? 1f : 0.35f);
        v.setOnClickPendingIntent(R.id.root, openApp(context));
        if (size == SMALL) return v;
        String msg;
        if (doneToday) msg = "오늘 " + todayCount + "문제 완료! 🍓";
        else if (streak > 0) msg = "오늘 한 문제 풀면 " + (streak + 1) + "일 연속!";
        else msg = "오늘 한 문제 풀고 시작해요!";
        if (size == MEDIUM) {
            msg = doneToday ? "오늘 완료! 🍓" : (streak > 0 ? "오늘 풀면 " + (streak + 1) + "일!" : "오늘 시작해요!");
            v.setTextViewText(R.id.msg, msg);
            return v;
        }
        v.setTextViewText(R.id.msg, msg);

        // 이번 주 (월~일)
        Calendar mon = (Calendar) today.clone();
        int dow = today.get(Calendar.DAY_OF_WEEK); // 일=1 … 토=7
        int back = (dow + 5) % 7;                  // 월=0
        mon.add(Calendar.DAY_OF_MONTH, -back);
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) mon.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            boolean done = has(days, d);
            boolean isToday = i == back;
            int bg = isToday ? (done ? R.drawable.day_today_done : R.drawable.day_today)
                    : (done ? R.drawable.day_done : R.drawable.day_empty);
            v.setInt(DAY_IDS[i], "setBackgroundResource", bg);
            v.setTextViewText(DAY_IDS[i], done ? "🔥" : "");
        }

        return v;
    }

    private static PendingIntent openApp(Context context) {
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
