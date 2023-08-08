LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libmxdrvg

LOCAL_SRC_FILES := \
 jniwrap.cpp \
 \
 $(LOCAL_PATH)/../portable_mdx/src/mxdrv/mxdrv.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/mxdrv/mxdrv_context.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/mxdrv/sound_iocs.cpp \
 \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound_adpcm.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound_context.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound_lfo.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound_op.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound_opm.cpp \
 $(LOCAL_PATH)/../portable_mdx/src/x68sound/x68sound_pcm8.cpp \

LOCAL_SHARED_LIBRARIES := 

LOCAL_STATIC_LIBRARIES :=

LOCAL_C_INCLUDES += \
 $(LOCAL_PATH)/../portable_mdx/include \

LOCAL_CFLAGS    := -Wall -DLOG_TAG=\"LibMxdrvg\" -O3

LOCAL_CPPFLAGS  := -std=c++11

LOCAL_PRELINK_MODULE := false

LOCAL_LDLIBS    := -llog \

include $(BUILD_SHARED_LIBRARY)
