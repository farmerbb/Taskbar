LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

res_dir := app/src/main/res
nonlib_res_dir := app/src/nonlib/res

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/$(res_dir) $(LOCAL_PATH)/$(nonlib_res_dir)

LOCAL_SRC_FILES := \
        $(call all-java-files-under, app/src/main/java) \
        $(call all-java-files-under, app/src/androidx86/java) \
        $(call all-java-files-under, app/src/nonlib/java) \
        $(call all-java-files-under, app/src/nonplaystore/java) \
        $(call all-java-files-under, app/src/compat-$(PLATFORM_SDK_VERSION)/java)

LOCAL_MANIFEST_FILE := app/src/androidx86/AndroidManifest.xml

LOCAL_STATIC_ANDROID_LIBRARIES := \
        android-support-v4 \
        android-support-v7-appcompat \
        android-support-design

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := Taskbar

LOCAL_JAVA_LANGUAGE_VERSION := 1.8

LOCAL_SDK_VERSION := current

LOCAL_PRIVILEGED_MODULE := true

LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --rename-manifest-package com.farmerbb.taskbar.androidx86

include $(BUILD_PACKAGE)
