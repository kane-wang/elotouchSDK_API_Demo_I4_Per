LOCAL_PATH := $(call my-dir)
################################################################################
# Cloudy: set TARGET_BUILD_APPS to true to build so into apk and no odex file
#TARGET_BUILD_APPS := true
include $(CLEAR_VARS)

#LOCAL_MODULE_TAGS := tests
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_PACKAGE_NAME := elotouchSDK_API_Demo

#LOCAL_PROPRIETARY_MODULE := optional

#Kelly, for POE settings demo UI
LOCAL_STATIC_ANDROID_LIBRARIES := \
      androidx.preference_preference \
      androidx.recyclerview_recyclerview

# Cloudy: use LOCAL_STATIC_JAVA_LIBRARIES to build jar into apk
LOCAL_STATIC_JAVA_LIBRARIES := eloperipherallib

#LOCAL_CERTIFICATE := platform
LOCAL_PRIVATE_PLATFORM_APIS := true

# and when built explicitly put it in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/preinstall/app

include $(BUILD_PACKAGE)

################################################################################
# Cloudy: clear TARGET_BUILD_APPS to be sure no other modules will be effected
#TARGET_BUILD_APPS := 
