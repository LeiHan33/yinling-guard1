package com.yinling.guard.ui.records

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
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class RecordsFragmentTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        TestFixtures.setup(context)
    }

    @Test
    fun showsEmptyStateWhenNoLogs() {
        launchFragmentInContainer<RecordsFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withId(R.id.emptyText)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerView)).check(matches(not(isDisplayed())))
    }

    @Test
    fun showsBlockLogItemWhenLogsExist() {
        TestFixtures.seedBlockLog(context, title = "震惊！测试视频", keyword = "震惊")

        launchFragmentInContainer<RecordsFragment>(themeResId = R.style.Theme_YinlingGuard)

        onView(withText("震惊！测试视频")).check(matches(isDisplayed()))
        onView(withText("震惊")).check(matches(isDisplayed()))
        onView(withText("14:32")).check(matches(isDisplayed()))
        onView(withId(R.id.emptyText)).check(matches(not(isDisplayed())))
    }
}
