package com.farmerbb.taskbar.helper

import com.farmerbb.taskbar.util.ToastInterface
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToastHelperTest {
    private lateinit var toastHelper: ToastHelper
    private var lastToast: ToastInterface? = null

    @Before
    fun setUp() {
        toastHelper = ToastHelper.getInstance()
        lastToast = toastHelper.lastToast
    }

    @After
    fun tearDown() {
        toastHelper.lastToast = lastToast
    }

    @Test
    fun getInstance_AlwaysBeTheSame() {
        Assert.assertNotNull(toastHelper)
        for (i in 1..20) {
            Assert.assertEquals(toastHelper, ToastHelper.getInstance())
        }
    }

    @Test
    fun getLastToast_DefaultValueIsNull() {
        assertThat(toastHelper.lastToast).isNull()
    }

    @Test
    fun setLastToast_ShouldChangedToSpecificValue() {
        class ToastInterfaceImpl : ToastInterface {
            override fun show() {
                // Do nothing
            }

            override fun cancel() {
                // Do nothing
            }
        }

        val toastInterface = ToastInterfaceImpl()
        toastHelper.lastToast = toastInterface
        assertThat(toastHelper.lastToast).isNotNull()
        assertThat(toastHelper.lastToast).isEqualTo(toastInterface)
    }
}
