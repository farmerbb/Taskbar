package com.farmerbb.taskbar.helper

import android.os.Build.VERSION_CODES.O_MR1
import android.os.Build.VERSION_CODES.P
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class GlobalHelperTest {
    private lateinit var globalHelper: GlobalHelper
    private var defaultIsOnMainActivity: Boolean = false

    @Before
    fun setUp() {
        globalHelper = GlobalHelper.getInstance()
        defaultIsOnMainActivity = globalHelper.isOnMainActivity
    }

    @After
    fun tearDown() {
        globalHelper.isOnMainActivity = defaultIsOnMainActivity
    }

    @Test
    fun getInstance_AlwaysBeTheSame() {
        Assert.assertNotNull(globalHelper)
        for (i in 1..20) {
            Assert.assertEquals(globalHelper, GlobalHelper.getInstance())
        }
    }

    @Test
    fun isOnMainActivity_DefaultValueIsFalse() {
        assertThat(globalHelper.isOnMainActivity).isFalse()
    }

    @Test
    fun setOnMainActivity_ShouldChangedToSpecificValue() {
        val defaultIsOnMainActivity = globalHelper.isOnMainActivity
        globalHelper.isOnMainActivity = !defaultIsOnMainActivity
        assertThat(globalHelper.isOnMainActivity).isEqualTo(!defaultIsOnMainActivity)
    }

    @Test
    @Config(maxSdk = O_MR1)
    fun isReflectionAllowed_ShouldTrueDefaultBeforeP() {
        assertThat(globalHelper.isReflectionAllowed).isTrue()
    }

    @Test
    @Config(minSdk = P)
    fun isReflectionAllowed_ShouldFalseDefaultFromP() {
        assertThat(globalHelper.isReflectionAllowed).isFalse()
    }

    @Test
    fun setReflectionAllowed_ShouldChangedToSpecificValue() {
        val defaultReflectionAllowed = globalHelper.isReflectionAllowed
        globalHelper.isReflectionAllowed = !defaultReflectionAllowed
        assertThat(globalHelper.isReflectionAllowed).isEqualTo(!defaultReflectionAllowed)
    }
}
