package hi.mirror;

import com.liera.hi.linkwifi.LinkWifiManager;

import hi.mirror.fw.HiMirrorFwApplication;

public class HiMirrorApplication extends HiMirrorFwApplication {

    @Override
    protected void onCreateBefore() {

    }

    @Override
    protected void onCreateAfter() {
        LinkWifiManager linkWifiManager = new LinkWifiManager(this);
        linkWifiManager.register();
    }
}
