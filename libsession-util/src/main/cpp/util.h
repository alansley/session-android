#ifndef SESSION_ANDROID_UTIL_H
#define SESSION_ANDROID_UTIL_H

#include <jni.h>
#include <array>
#include <optional>
#include "session/types.hpp"
#include "session/config/groups/info.hpp"
#include "session/config/groups/keys.hpp"
#include "session/config/groups/members.hpp"
#include "session/config/profile_pic.hpp"
#include "session/config/user_groups.hpp"
#include "session/config/expiring.hpp"
#include <android/log.h>

#define LOG_TAG "libsession-jni"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

namespace util {
    extern std::mutex util_mutex_;
    jbyteArray bytes_from_ustring(JNIEnv* env, session::ustring_view from_str);
    session::ustring ustring_from_bytes(JNIEnv* env, jbyteArray byteArray);
    session::ustring ustring_from_jstring(JNIEnv* env, jstring string);
    jobject serialize_user_pic(JNIEnv* env, session::config::profile_pic pic);
    std::pair<jstring, jbyteArray> deserialize_user_pic(JNIEnv* env, jobject user_pic);
    jobject serialize_base_community(JNIEnv* env, const session::config::community& base_community);
    session::config::community deserialize_base_community(JNIEnv* env, jobject base_community);
    jobject serialize_expiry(JNIEnv* env, const session::config::expiration_mode& mode, const std::chrono::seconds& time_seconds);
    std::pair<session::config::expiration_mode, long> deserialize_expiry(JNIEnv* env, jobject expiry_mode);
    jobject serialize_group_member(JNIEnv* env, const session::config::groups::member& member);
    session::config::groups::member deserialize_group_member(JNIEnv* env, jobject member);
    jobject jlongFromOptional(JNIEnv* env, std::optional<long long> optional);
    jstring jstringFromOptional(JNIEnv* env, std::optional<std::string_view> optional);
    jobject serialize_session_id(JNIEnv* env, std::string_view session_id);
    std::string deserialize_session_id(JNIEnv* env, jobject session_id);
}

#endif