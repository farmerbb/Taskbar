LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

appcompat_dir := frameworks/support/v7/appcompat
res_dir := app/src/main/res
nonlib_res_dir := app/src/nonlib/res

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/$(res_dir) $(LOCAL_PATH)/$(nonlib_res_dir) $(appcompat_dir)/res

LOCAL_SRC_FILES := \
        $(call all-java-files-under, app/src/main/java) \
        $(call all-java-files-under, app/src/androidx86/java) \
        $(call all-java-files-under, app/src/nonlib/java) \
        $(call all-java-files-under, app/src/nonplaystore/java) \
        $(call all-java-files-under, app/src/compat-$(PLATFORM_SDK_VERSION)/java)

LOCAL_MANIFEST_FILE := app/src/androidx86/AndroidManifest.xml

LOCAL_STATIC_JAVA_LIBRARIES := \
        androidx.legacy_legacy-support-v4 \
        androidx.appcompat_appcompat \
        androidx.browser_browser \
        com.google.android.material_material \

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := Taskbar

LOCAL_JAVA_LANGUAGE_VERSION := 1.8

LOCAL_SDK_VERSION := current

LOCAL_PRIVILEGED_MODULE := true

ifeq ($(ENABLE_TASKBAR_REPLACE),true)
LOCAL_OVERRIDES_PACKAGES := Home Launcher2 Launcher3 Launcher3QuickStep
endif

LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --rename-manifest-package com.farmerbb.taskbar.androidx86 \
        --extra-packages androidx.appcompat \
        --extra-packages com.google.android.material

include $(BUILD_PACKAGE)
