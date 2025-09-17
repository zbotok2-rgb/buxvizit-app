package com.yourapp.buxvizit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Названия и ссылки
    private final String[][] LINKS = new String[][]{
            {"Сёрфинг", "https://bux-vizit.ru/earn/serf"},
            {"Задания", "https://bux-vizit.ru/earn/task"},
            {"Копилка", "https://bux-vizit.ru/earn/Kopilka2"},
            {"Direct Links", "https://bux-vizit.ru/earn/direct_links"},
            {"Luckywatch (YouTube)", "https://luckywatch.pro/u/0ohys"}
    };

    private ImageView imgState;
    private TextView tvStatus;
    private SharedPreferences prefs;
    private static final String PREFS = "bux_prefs";
    private static final String KEY_LAST_CLICK = "last_click_ts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgState = findViewById(R.id.imgState);
        tvStatus = findViewById(R.id.tvStatus);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout container = findViewById(R.id.linksContainer);

        // --- Список разделов ---
        for (final String[] item : LINKS) {
            String title = item[0];
            String url = item[1];

            TextView tv = new TextView(this);
            tv.setText("▶ " + title);
            tv.setTextSize(18f);
            tv.setPadding(16, 24, 16, 24);

            tv.setOnClickListener(v -> onLinkClick(url));
            container.addView(tv);
        }

        // --- ПОЯСНЕНИЕ ДЛЯ ПОЛЬЗОВАТЕЛЕЙ ---
        TextView info = new TextView(this);
        info.setText(
                "ℹ️ Что значит статус:\n\n" +
                "🟢 Зелёный — Значит в разделах есть новые задания или просмотры, которые появляются через каждые 30 минут.\n" +
                "🟠 Оранжевый — значит вы уже кликнули на один из пунктов этого меню подождите 30 минут, Однако это не означает что заработок на сайте закончился, это просто индикатор клика в этом приложении.\n\n" +
                "▶ Сёрфинг — ссылки обновляются каждые 6 часов.\n" +
                "▶ Direct Links — доступны снова через 30–60 минут.\n" +
                "▶ Просмотры YouTube — новые 50 просмотров через каждый 1 час."
        );
        info.setTextSize(14f);
        info.setPadding(16, 32, 16, 32);

        container.addView(info);

        updateStateUI();
    }

    private void onLinkClick(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

        long ts = System.currentTimeMillis();
        prefs.edit().putLong(KEY_LAST_CLICK, ts).apply();
        setStateOrange();

        scheduleRestoreGreen(30 * 60 * 1000L);
    }

    private void setStateOrange() {
        imgState.setImageResource(R.drawable.ic_orange);
        tvStatus.setText("Статус: Оранжевый");
        toggleAlias(".LAUNCHER_ORANGE", true);
        toggleAlias(".LAUNCHER_GREEN", false);
    }

    private void setStateGreen() {
        imgState.setImageResource(R.drawable.ic_green);
        tvStatus.setText("Статус: Зелёный");
        toggleAlias(".LAUNCHER_ORANGE", false);
        toggleAlias(".LAUNCHER_GREEN", true);
    }

    private void updateStateUI() {
        long last = prefs.getLong(KEY_LAST_CLICK, 0);
        if (System.currentTimeMillis() - last < 30 * 60 * 1000L) {
            setStateOrange();
        } else {
            setStateGreen();
        }
    }

    private void scheduleRestoreGreen(long delayMs) {
        Intent intent = new Intent(this, IconToggleReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am != null)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, pi);
    }

    private void toggleAlias(String alias, boolean enabled) {
        PackageManager pm = getPackageManager();
        ComponentName comp = new ComponentName(this, getPackageName() + alias);
        pm.setComponentEnabledSetting(comp,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
