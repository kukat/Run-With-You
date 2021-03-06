package site.hanschen.runwithyou.ui.home.devicelist;

import dagger.Module;
import dagger.Provides;

/**
 * @author HansChen
 */
@Module
class DeviceListModule {

    private final DeviceListContract.View mView;

    DeviceListModule(DeviceListContract.View view) {
        mView = view;
    }

    @Provides
    DeviceListContract.View provideTodayContractView() {
        return mView;
    }
}
