package android.app;

import android.app.servertransaction.PendingTransactionActions;
import android.appwidget.AppWidgetHostView;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.IBinder;

import com.android.internal.content.ReferrerIntent;

import java.util.ArrayList;
import java.util.List;

public class ActivityThread {

    public static final class ActivityClientRecord {
        public IBinder token;
        public IBinder assistToken;
        public LoadedApk packageInfo;
    }

    public static ActivityThread currentActivityThread() {
        throw new RuntimeException("Stub!");
    }

    public final ActivityInfo resolveActivityInfo(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public ActivityClientRecord getActivityClient(IBinder token) {
        throw new RuntimeException("Stub!");
    }

    public void handleStartActivity(IBinder token, PendingTransactionActions pendingActions) {
        throw new RuntimeException("Stub!");
    }

    public ActivityClientRecord performResumeActivity(IBinder token, boolean finalStateRequest,
                                                      String reason) {
        throw new RuntimeException("Stub!");
    }

    public void performRestartActivity(IBinder token, boolean start) {
        throw new RuntimeException("Stub!");
    }

    final void performStopActivity(IBinder token, boolean saveState, String reason) {
        throw new RuntimeException("Stub!");
    }

    public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai,
                                                 CompatibilityInfo compatInfo) {
        throw new RuntimeException("Stub!");
    }

    public void handleNewIntent(IBinder token, List<ReferrerIntent> intents) {
        throw new RuntimeException("Stub!");
    }

}
