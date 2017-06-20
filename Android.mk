LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

appcompat_dir := ../../../frameworks/support/v7/appcompat
design_dir := ../../../frameworks/support/design
res_dir := app/src/main/res $(appcompat_dir)/res $(design_dir)/res

LOCAL_MODULE_TAGS := optional

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))

LOCAL_SRC_FILES := \
        $(call all-java-files-under, app/src/main/java) \
        $(call all-java-files-under, app/src/androidx86/java)

LOCAL_MANIFEST_FILE := app/src/androidx86/AndroidManifest.xml

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v4 \
        android-support-v7-appcompat \
        android-support-v7-recyclerview \
        android-support-design

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := Taskbar

LOCAL_JAVA_LANGUAGE_VERSION := 1.8

LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --rename-manifest-package com.farmerbb.taskbar.androidx86 \
        --extra-packages android.support.v7.appcompat \
        --extra-packages android.support.v7.recyclerview \
        --extra-packages android.support.design

include $(BUILD_PACKAGE)
