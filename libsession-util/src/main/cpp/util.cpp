#include "util.h"
#include <string>
#include <sodium/crypto_sign.h>

namespace util {

    std::mutex util_mutex_ = std::mutex();

    jbyteArray bytes_from_ustring(JNIEnv* env, session::ustring_view from_str) {
        size_t length = from_str.length();
        auto jlength = (jsize)length;
        jbyteArray new_array = env->NewByteArray(jlength);
        env->SetByteArrayRegion(new_array, 0, jlength, (jbyte*)from_str.data());
        return new_array;
    }

    session::ustring ustring_from_bytes(JNIEnv* env, jbyteArray byteArray) {
        size_t len = env->GetArrayLength(byteArray);
        auto bytes = env->GetByteArrayElements(byteArray, nullptr);

        session::ustring st{reinterpret_cast<const unsigned char *>(bytes), len};
        env->ReleaseByteArrayElements(byteArray, bytes, 0);
        return st;
    }

    session::ustring ustring_from_jstring(JNIEnv* env, jstring string) {
        size_t len = env->GetStringUTFLength(string);
        auto chars = env->GetStringUTFChars(string, nullptr);

        session::ustring st{reinterpret_cast<const unsigned char *>(chars), len};
        env->ReleaseStringUTFChars(string, chars);
        return st;
    }

    jobject serialize_user_pic(JNIEnv *env, session::config::profile_pic pic) {
        jclass returnObjectClass = env->FindClass("network/loki/messenger/libsession_util/util/UserPic");
        jmethodID constructor = env->GetMethodID(returnObjectClass, "<init>", "(Ljava/lang/String;[B)V");
        jstring url = env->NewStringUTF(pic.url.data());
        jbyteArray byteArray = util::bytes_from_ustring(env, pic.key);
        return env->NewObject(returnObjectClass, constructor, url, byteArray);
    }

    std::pair<jstring, jbyteArray> deserialize_user_pic(JNIEnv *env, jobject user_pic) {
        jclass userPicClass = env->FindClass("network/loki/messenger/libsession_util/util/UserPic");
        jfieldID picField = env->GetFieldID(userPicClass, "url", "Ljava/lang/String;");
        jfieldID keyField = env->GetFieldID(userPicClass, "key", "[B");
        auto pic = (jstring)env->GetObjectField(user_pic, picField);
        auto key = (jbyteArray)env->GetObjectField(user_pic, keyField);
        return {pic, key};
    }

    jobject serialize_base_community(JNIEnv *env, const session::config::community& community) {
        jclass base_community_clazz = env->FindClass("network/loki/messenger/libsession_util/util/BaseCommunityInfo");
        jmethodID base_community_constructor = env->GetMethodID(base_community_clazz, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        auto base_url = env->NewStringUTF(community.base_url().data());
        auto room = env->NewStringUTF(community.room().data());
        auto pubkey_jstring = env->NewStringUTF(community.pubkey_hex().data());
        jobject ret = env->NewObject(base_community_clazz, base_community_constructor, base_url, room, pubkey_jstring);
        return ret;
    }

    session::config::community deserialize_base_community(JNIEnv *env, jobject base_community) {
        jclass base_community_clazz = env->FindClass("network/loki/messenger/libsession_util/util/BaseCommunityInfo");
        jfieldID base_url_field = env->GetFieldID(base_community_clazz, "baseUrl", "Ljava/lang/String;");
        jfieldID room_field = env->GetFieldID(base_community_clazz, "room", "Ljava/lang/String;");
        jfieldID pubkey_hex_field = env->GetFieldID(base_community_clazz, "pubKeyHex", "Ljava/lang/String;");
        auto base_url = (jstring)env->GetObjectField(base_community,base_url_field);
        auto room = (jstring)env->GetObjectField(base_community, room_field);
        auto pub_key_hex = (jstring)env->GetObjectField(base_community, pubkey_hex_field);
        auto base_url_chars = env->GetStringUTFChars(base_url, nullptr);
        auto room_chars = env->GetStringUTFChars(room, nullptr);
        auto pub_key_hex_chars = env->GetStringUTFChars(pub_key_hex, nullptr);

        auto community = session::config::community(base_url_chars, room_chars, pub_key_hex_chars);

        env->ReleaseStringUTFChars(base_url, base_url_chars);
        env->ReleaseStringUTFChars(room, room_chars);
        env->ReleaseStringUTFChars(pub_key_hex, pub_key_hex_chars);
        return community;
    }

    jobject serialize_expiry(JNIEnv *env, const session::config::expiration_mode& mode, const std::chrono::seconds& time_seconds) {
        jclass none = env->FindClass("network/loki/messenger/libsession_util/util/ExpiryMode$NONE");
        jfieldID none_instance = env->GetStaticFieldID(none, "INSTANCE", "Lnetwork/loki/messenger/libsession_util/util/ExpiryMode$NONE;");
        jclass after_send = env->FindClass("network/loki/messenger/libsession_util/util/ExpiryMode$AfterSend");
        jmethodID send_init = env->GetMethodID(after_send, "<init>", "(J)V");
        jclass after_read = env->FindClass("network/loki/messenger/libsession_util/util/ExpiryMode$AfterRead");
        jmethodID read_init = env->GetMethodID(after_read, "<init>", "(J)V");

        if (mode == session::config::expiration_mode::none) {
            return env->GetStaticObjectField(none, none_instance);
        } else if (mode == session::config::expiration_mode::after_send) {
            return env->NewObject(after_send, send_init, time_seconds.count());
        } else if (mode == session::config::expiration_mode::after_read) {
            return env->NewObject(after_read, read_init, time_seconds.count());
        }
        return nullptr;
    }

    std::pair<session::config::expiration_mode, long> deserialize_expiry(JNIEnv *env, jobject expiry_mode) {
        jclass parent = env->FindClass("network/loki/messenger/libsession_util/util/ExpiryMode");
        jclass after_read = env->FindClass("network/loki/messenger/libsession_util/util/ExpiryMode$AfterRead");
        jclass after_send = env->FindClass("network/loki/messenger/libsession_util/util/ExpiryMode$AfterSend");
        jfieldID duration_seconds = env->GetFieldID(parent, "expirySeconds", "J");

        jclass object_class = env->GetObjectClass(expiry_mode);

        if (object_class == after_read) {
            return std::pair(session::config::expiration_mode::after_read, env->GetLongField(expiry_mode, duration_seconds));
        } else if (object_class == after_send) {
            return std::pair(session::config::expiration_mode::after_send, env->GetLongField(expiry_mode, duration_seconds));
        }
        return std::pair(session::config::expiration_mode::none, 0);
    }

    jobject serialize_group_member(JNIEnv* env, const session::config::groups::member& member) {
        jclass group_member_class = env->FindClass("network/loki/messenger/libsession_util/util/GroupMember");
        jmethodID constructor = env->GetMethodID(group_member_class, "<init>", "(Ljava/lang/String;Ljava/lang/String;Lnetwork/loki/messenger/libsession_util/util/UserPic;ZZZZZ)V");
        jobject user_pic = serialize_user_pic(env, member.profile_picture);
        jstring session_id = env->NewStringUTF(member.session_id.data());
        jstring name = env->NewStringUTF(member.name.data());
        jboolean invite_failed = member.invite_failed();
        jboolean invite_pending = member.invite_pending();
        jboolean admin = member.admin;
        jboolean promotion_failed = member.promotion_failed();
        jboolean promotion_pending = member.promotion_pending();
        return env->NewObject(group_member_class,
                              constructor,
                              session_id,
                              name,
                              user_pic,
                              invite_failed,
                              invite_pending,
                              admin,
                              promotion_failed,
                              promotion_pending);
    }

    session::config::groups::member deserialize_group_member(JNIEnv* env, jobject member) {
        jclass group_member_class = env->FindClass("network/loki/messenger/libsession_util/util/GroupMember");
        jfieldID session_id_field = env->GetFieldID(group_member_class, "sessionId", "Ljava/lang/String;");
        jfieldID name_field = env->GetFieldID(group_member_class, "name", "Ljava/lang/String;");
        jfieldID user_pic_field = env->GetFieldID(group_member_class,"profilePicture", "Lnetwork/loki/messenger/libsession_util/util/UserPic;");
        jfieldID invite_failed_field = env->GetFieldID(group_member_class, "inviteFailed", "Z");
        jfieldID invite_pending_field = env->GetFieldID(group_member_class, "invitePending", "Z");
        jfieldID admin_field = env->GetFieldID(group_member_class, "admin", "Z");
        jfieldID promotion_failed_field = env->GetFieldID(group_member_class, "promotionFailed", "Z");
        jfieldID promotion_pending_field = env->GetFieldID(group_member_class, "promotionPending", "Z");
        auto session_id = (jstring)env->GetObjectField(member, session_id_field);
        auto session_id_bytes = env->GetStringUTFChars(session_id, nullptr);
        auto name = (jstring)env->GetObjectField(member, name_field);
        auto name_bytes = env->GetStringUTFChars(name, nullptr);
        auto user_pic_jobject = env->GetObjectField(member, user_pic_field);
        auto user_pic = deserialize_user_pic(env, user_pic_jobject);
        auto url_bytes = env->GetStringUTFChars(user_pic.first, nullptr);
        auto pic_key = ustring_from_bytes(env, user_pic.second);
        auto invite_failed = env->GetBooleanField(member, invite_failed_field);
        auto invite_pending = env->GetBooleanField(member, invite_pending_field);
        auto admin = env->GetBooleanField(member, admin_field);
        auto promotion_failed = env->GetBooleanField(member, promotion_failed_field);
        auto promotion_pending = env->GetBooleanField(member, promotion_pending_field);

        // set up the object
        session::config::groups::member group_member(session_id_bytes);
        group_member.name = name_bytes;
        group_member.profile_picture.url = url_bytes;
        group_member.profile_picture.set_key(pic_key);

        if (invite_pending) {
            group_member.set_invited(false);
        } else if (invite_failed) {
            group_member.set_invited(true);
        }

        if (promotion_pending) {
            group_member.set_promoted(false);
        } else if (promotion_failed) {
            group_member.set_promoted(true);
        }

        group_member.admin = admin;

        env->ReleaseStringUTFChars(user_pic.first, url_bytes);
        env->ReleaseStringUTFChars(session_id, session_id_bytes);
        env->ReleaseStringUTFChars(name, name_bytes);
        return group_member;
    }

    jobject jlongFromOptional(JNIEnv* env, std::optional<long long> optional) {
        if (!optional) {
            return nullptr;
        }
        jclass longClass = env->FindClass("java/lang/Long");
        jmethodID constructor = env->GetMethodID(longClass, "<init>", "(J)V");
        jobject returned = env->NewObject(longClass, constructor, (jlong)*optional);
        return returned;
    }

    jstring jstringFromOptional(JNIEnv* env, std::optional<std::string_view> optional) {
        if (!optional) {
            return nullptr;
        }
        return env->NewStringUTF(optional->data());
    }

    jobject serialize_session_id(JNIEnv* env, std::string_view session_id) {
        if (session_id.size() != 66) return nullptr;

        jclass id_class = env->FindClass("org/session/libsignal/utilities/SessionId");
        jmethodID session_id_constructor = env->GetStaticMethodID(id_class, "from", "(Ljava/lang/String;)Lorg/session/libsignal/utilities/SessionId;");

        jstring session_id_string = env->NewStringUTF(session_id.data());

        return env->CallStaticObjectMethod(id_class, session_id_constructor, session_id_string);
    }

    std::string deserialize_session_id(JNIEnv* env, jobject session_id) {
        jclass session_id_class = env->FindClass("org/session/libsignal/utilities/SessionId");
        jmethodID get_string = env->GetMethodID(session_id_class, "hexString", "()Ljava/lang/String;");
        auto hex_jstring = (jstring)env->CallObjectMethod(session_id, get_string);
        auto hex_bytes = env->GetStringUTFChars(hex_jstring, nullptr);
        std::string hex_string{hex_bytes};
        env->ReleaseStringUTFChars(hex_jstring, hex_bytes);
        return hex_string;
    }

}

extern "C"
JNIEXPORT jobject JNICALL
Java_network_loki_messenger_libsession_1util_util_Sodium_ed25519KeyPair(JNIEnv *env, jobject thiz, jbyteArray seed) {
    std::array<unsigned char, 32> ed_pk; // NOLINT(cppcoreguidelines-pro-type-member-init)
    std::array<unsigned char, 64> ed_sk; // NOLINT(cppcoreguidelines-pro-type-member-init)
    auto seed_bytes = util::ustring_from_bytes(env, seed);
    crypto_sign_ed25519_seed_keypair(ed_pk.data(), ed_sk.data(), seed_bytes.data());

    jclass kp_class = env->FindClass("network/loki/messenger/libsession_util/util/KeyPair");
    jmethodID kp_constructor = env->GetMethodID(kp_class, "<init>", "([B[B)V");

    jbyteArray pk_jarray = util::bytes_from_ustring(env, session::ustring_view {ed_pk.data(), ed_pk.size()});
    jbyteArray sk_jarray = util::bytes_from_ustring(env, session::ustring_view {ed_sk.data(), ed_sk.size()});

    jobject return_obj = env->NewObject(kp_class, kp_constructor, pk_jarray, sk_jarray);
    return return_obj;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_network_loki_messenger_libsession_1util_util_Sodium_ed25519PkToCurve25519(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jbyteArray pk) {
    auto ed_pk = util::ustring_from_bytes(env, pk);
    std::array<unsigned char, 32> curve_pk; // NOLINT(cppcoreguidelines-pro-type-member-init)
    int success = crypto_sign_ed25519_pk_to_curve25519(curve_pk.data(), ed_pk.data());
    if (success != 0) {
        jclass exception = env->FindClass("java/lang/Exception");
        env->ThrowNew(exception, "Invalid crypto_sign_ed25519_pk_to_curve25519 operation");
        return nullptr;
    }
    jbyteArray curve_pk_jarray = util::bytes_from_ustring(env, session::ustring_view {curve_pk.data(), curve_pk.size()});
    return curve_pk_jarray;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_network_loki_messenger_libsession_1util_util_BaseCommunityInfo_00024Companion_parseFullUrl(
        JNIEnv *env, jobject thiz, jstring full_url) {
    auto bytes = env->GetStringUTFChars(full_url, nullptr);
    auto [base, room, pk] = session::config::community::parse_full_url(bytes);
    env->ReleaseStringUTFChars(full_url, bytes);

    jclass clazz = env->FindClass("kotlin/Triple");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");

    auto base_j = env->NewStringUTF(base.data());
    auto room_j = env->NewStringUTF(room.data());
    auto pk_jbytes = util::bytes_from_ustring(env, pk);

    jobject triple = env->NewObject(clazz, constructor, base_j, room_j, pk_jbytes);
    return triple;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_network_loki_messenger_libsession_1util_util_BaseCommunityInfo_fullUrl(JNIEnv *env,
                                                                            jobject thiz) {
    auto deserialized = util::deserialize_base_community(env, thiz);
    auto full_url = deserialized.full_url();
    return env->NewStringUTF(full_url.data());
}