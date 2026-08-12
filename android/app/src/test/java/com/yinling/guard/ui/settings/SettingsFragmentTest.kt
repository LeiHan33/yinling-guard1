package com.yinling.guard.ui.settings

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yinling.guard.R
import com.yinling.guard.testing.TestFixtures
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class SettingsFragmentTest {
    @Before
    fun setup() {
        TestFixtures.setup(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun showsSettingsControls() {
        launchFragmentInContainer<SettingsFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withId(R.id.guardSwitch)).check(matches(isDisplayed()))
        onView(withId(R.id.toastSwitch)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.familyButton), withText("子女管理"))).check(matches(isDisplayed()))
        onView(withId(R.id.helpButton)).check(matches(isDisplayed()))
        val versionLabel = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.settings_version, "1.0.2")
        onView(withText(versionLabel)).check(matches(isDisplayed()))
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class HelpFragmentTest {
    @Test
    fun showsHelpContent() {
        launchFragmentInContainer<HelpFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withText("银龄守护使用说明")).check(matches(isDisplayed()))
        onView(withText("【首次使用】")).check(matches(isDisplayed()))
    }
}
