package com.yinling.guard.ui.home

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
import com.yinling.guard.core.model.AppConfig
import com.yinling.guard.testing.TestFixtures
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class HomeFragmentTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        TestFixtures.setup(context)
        TestFixtures.saveConfig(
            context,
            AppConfig(
                onboardingCompleted = true,
                guardEnabled = true,
                firstGuardDate = "2026-08-10"
            )
        )
    }

    @Test
    fun showsGuardStatusAndStats() {
        TestFixtures.seedBlockLog(context)

        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withId(R.id.greetingText)).check(matches(withText(containsString("守护"))))
        onView(withId(R.id.todayCountText)).check(matches(withText(containsString("今日拦截"))))
        onView(withId(R.id.guardDaysText)).check(matches(withText(containsString("累计守护"))))
        onView(withId(R.id.statusDot)).check(matches(isDisplayed()))
    }
}
