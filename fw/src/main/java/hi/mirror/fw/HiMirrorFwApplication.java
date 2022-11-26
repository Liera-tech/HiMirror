package hi.mirror.fw;

import android.app.Application;
import android.content.Context;

public abstract class HiMirrorFwApplication extends Application {

    private static HiMirrorFwApplication mContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        mContext = this;
        onCreateBefore();
        super.onCreate();
        onCreateAfter();
    }

    public static HiMirrorFwApplication getFwApplication(){
        return mContext;
    }

    protected abstract void onCreateBefore();

    protected abstract void onCreateAfter();

}
