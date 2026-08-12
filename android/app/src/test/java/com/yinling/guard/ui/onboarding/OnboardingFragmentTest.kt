package com.yinling.guard.ui.onboarding

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
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class OnboardingWelcomeFragmentTest {
    @Test
    fun showsWelcomeContentAndStartButton() {
        launchFragmentInContainer<OnboardingWelcomeFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withId(R.id.stepIndicator)).check(matches(withText("步骤 1 / 4")))
        onView(withId(R.id.titleText)).check(matches(withText("银龄守护")))
        onView(allOf(withId(R.id.startButton), withText("开始使用"))).check(matches(isDisplayed()))
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class OnboardingIntroFragmentTest {
    @Test
    fun showsFeatureCards() {
        launchFragmentInContainer<OnboardingIntroFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withText("自动识别")).check(matches(isDisplayed()))
        onView(withText("自动跳过")).check(matches(isDisplayed()))
        onView(withText("家人可管理")).check(matches(isDisplayed()))
        onView(withId(R.id.nextButton)).check(matches(isDisplayed()))
    }
}
