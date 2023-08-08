#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#if !defined(__java_net_gorry_mxdrvg_jniwrap_h__)
#define __java_net_gorry_mxdrvg_jniwrap_h__

#include <jni.h>
#include <android/log.h>

#if !defined(LOG_SUBTAG)
#define LOG_SUBTAG __FILE__
#endif

// リリースビルドではLOGI以下のメッセージはログに表示しない
#ifdef NDEBUG
#undef LOGV_ENABLE
#undef LOGD_ENABLE
#undef LOGI_ENABLE
#define LOGV_ENABLE 0
#define LOGD_ENABLE 0
#define LOGI_ENABLE 0
#endif  // NDEBUG

// ログに表示する関数名
#define __MYFUNC__ __FUNCTION__
// #define __MYFUNC__ __func__

// ログ表示
#define  LOGV(...)  if (LOGV_ENABLE) {char tmp[1024]; sprintf(tmp, __VA_ARGS__); __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, LOG_SUBTAG ": %s(): %s", __MYFUNC__, tmp);}
#define  LOGD(...)  if (LOGD_ENABLE) {char tmp[1024]; sprintf(tmp, __VA_ARGS__); __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG, LOG_SUBTAG ": %s(): %s", __MYFUNC__, tmp);}
#define  LOGI(...)  if (LOGI_ENABLE) {char tmp[1024]; sprintf(tmp, __VA_ARGS__); __android_log_print(ANDROID_LOG_INFO   , LOG_TAG, LOG_SUBTAG ": %s(): %s", __MYFUNC__, tmp);}
#define  LOGW(...)  if (1          ) {char tmp[1024]; sprintf(tmp, __VA_ARGS__); __android_log_print(ANDROID_LOG_WARN   , LOG_TAG, LOG_SUBTAG ": %s(): %s", __MYFUNC__, tmp);}
#define  LOGE(...)  if (1          ) {char tmp[1024]; sprintf(tmp, __VA_ARGS__); __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, LOG_SUBTAG ": %s(): %s", __MYFUNC__, tmp);}

// JNI関数名の作成
#define JNI_MAKEFUNCNAME2(a,b) a##_##b
#define JNI_MAKEFUNCNAME1(a,b) JNI_MAKEFUNCNAME2(a,b)
#define JNI_EXPORT_FUNCNAME(c) JNI_MAKEFUNCNAME1(JNI_MAKEFUNCNAME1(JNI_MAKEFUNCNAME1(Java,MY_JNI_EXPORT_PATH),MY_JNI_EXPORT_NAME),c)
#define JNI_IMPORT_FUNCNAME(c) JNI_MAKEFUNCNAME1(JNI_MAKEFUNCNAME1(JNI,MY_JNI_EXPORT_NAME),c)

#define JNIAPI(a,b) JNIEXPORT a JNICALL Java_net_gorry_ndk_Natives_##b

#ifdef __cplusplus
extern "C" {
#endif

JNIAPI(jint, ndkEntry)(
	JNIEnv *env,
	jclass cls,
	jobjectArray jargv
);

JNIAPI(jint, mxdrvgStart)(
	JNIEnv *env,
	jclass cls, 
	jint samprate,
	jint fastmode,
	jint mdxbufsize,
	jint pdxbufsize,
	jint useirq
);

JNIAPI(void, mxdrvgEnd)(
	JNIEnv *env,
	jclass cls,
	jint dummy
);

JNIAPI(jint, mxdrvgGetPCM)(
	JNIEnv *env,
	jclass cls, 
	jshortArray buf,
	jint ofs,
	jint len
);

JNIAPI(void, mxdrvgSetData)(
	JNIEnv *env,
	jclass cls, 
	jbyteArray mdx,
	jint mdxsize,
	jbyteArray pdx,
	jint pdxsize
);

JNIAPI(void, mxdrvg)(
	JNIEnv *env,
	jclass cls, 
	jint arg1,
	jint arg2
);

JNIAPI(jint, mxdrvgMeasurePlayTime)(
	JNIEnv *env,
	jclass cls, 
	jint loop,
	jint fadeout
);

JNIAPI(void, mxdrvgPlay)(
	JNIEnv *env,
	jclass cls, 
	jint dummy
);

JNIAPI(void, mxdrvgPlayAt)(
	JNIEnv *env,
	jclass cls, 
	jint playat,
	jint loop,
	jint fadeout
);

JNIAPI(jint, mxdrvgGetPlayAt)(
	JNIEnv *env,
	jclass cls, 
	jint dummy
);

JNIAPI(jint, mxdrvgGetTerminated)(
	JNIEnv *env,
	jclass cls, 
	jint dummy
);

JNIAPI(void, mxdrvgTotalVolume)(
	JNIEnv *env,
	jclass cls, 
	jint vol
);

JNIAPI(jint, mxdrvgGetTotalVolume)(
	JNIEnv *env,
	jclass cls, 
	jint vol
);

JNIAPI(jint, mxdrvgGetChannelMask)(
	JNIEnv *env,
	jclass cls, 
	jint dummy
);

JNIAPI(void, mxdrvgChannelMask)(
	JNIEnv *env,
	jclass cls, 
	jint mask
);

JNIAPI(jint, mxdrvgGetPCM8Enable)(
	JNIEnv *env,
	jclass cls, 
	jint dummy
);

JNIAPI(void, mxdrvgPCM8Enable)(
	JNIEnv *env,
	jclass cls, 
	jint sw
);




JNIAPI(jint, slesConstruct)(
	JNIEnv *env,
	jclass cls,
	jint dummy
);

JNIAPI(int, slesDestruct)(
	JNIEnv *env,
	jclass cls,
	jint dummy
);

JNIAPI(jint, slesTotalVolume)(
	JNIEnv *env,
	jclass cls,
	jint vol
);

JNIAPI(jint, slesPacketOpenStream)(
	JNIEnv *env,
	jclass cls,
	jint ch,
	jint handle,
	jint freq,
	jint bufs
);

JNIAPI(jint, slesPacketCloseStream)(
	JNIEnv *env,
	jclass cls,
	jint ch
);

JNIAPI(jint, slesPacketSetChannel)(
	JNIEnv *env,
	jclass cls,
	jint ch
);

JNIAPI(void, slesPacketDestroy)(
	JNIEnv *env,
	jclass cls,
	jint ch
);

JNIAPI(jint, slesPacketPlay)(
	JNIEnv *env,
	jclass cls,
	jint ch,
	jint mask,
	jint vol,
	jint pan,
	jint rate
);

JNIAPI(jint, slesPacketStop)(
	JNIEnv *env,
	jclass cls,
	jint ch
);

JNIAPI(jint, slesPacketPause)(
	JNIEnv *env,
	jclass cls,
	jint ch,
	jint sw
);

JNIAPI(jint, slesPacketVolume)(
	JNIEnv *env,
	jclass cls,
	jint ch,
	jint vol
);

JNIAPI(jint, slesPacketPanpot)(
	JNIEnv *env,
	jclass cls,
	jint ch,
	jint pan
);

JNIAPI(jint, slesPacketRate)(
	JNIEnv *env,
	jclass cls,
	jint ch,
	jint rate
);

JNIAPI(jint, slesPacketGetState)(
	JNIEnv *env,
	jclass cls,
	jint ch
);


#ifdef __cplusplus
}
#endif

#endif  // __java_net_gorry_mxdrvg_jniwrap_h__

// [EOF]

