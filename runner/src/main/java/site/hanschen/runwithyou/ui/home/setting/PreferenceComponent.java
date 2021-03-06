package site.hanschen.runwithyou.ui.home.setting;

import dagger.Component;
import site.hanschen.runwithyou.application.ApplicationComponent;
import site.hanschen.runwithyou.dagger.FragmentScoped;

/**
 * @author HansChen
 */
@FragmentScoped
@Component(dependencies = ApplicationComponent.class, modules = PreferenceModule.class)
interface PreferenceComponent {

    void inject(SettingPreferenceFragment fragment);
}
