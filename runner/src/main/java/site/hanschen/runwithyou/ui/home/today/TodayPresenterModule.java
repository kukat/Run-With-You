package site.hanschen.runwithyou.ui.home.today;

import dagger.Module;
import dagger.Provides;

/**
 * @author HansChen
 */
@Module
class TodayPresenterModule {

    private final TodayContract.View mView;

    TodayPresenterModule(TodayContract.View view) {
        mView = view;
    }

    @Provides
    TodayContract.View provideTodayContractView() {
        return mView;
    }
}
