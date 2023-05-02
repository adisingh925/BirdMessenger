package com.adreal.birdmessenger.Constants

object Constants {
    /** Universal constants */

    const val BLANK = ""
    const val Users = "Users"
    const val FCM_API_KEY = "AAAAoQDQF3Q:APA91bGNNGPAVsuOqbpBe3iF8hI19IAU7Auvrba9J5UoFLslkxdwJDRVWa3_rSVB8JnSLNJihtSOx3WIFrY0mX55KkRPkTFPIkcYVN-11ERtDxkb8VbtOY6YUD9MN_Uivjs5ejr3wqUj"
    const val FCM_BASE_URL = "https://fcm.googleapis.com/"
    const val COMMON_FCM_TOPIC = "Everyone"
    const val CONNECTION_TIMEOUT = 60
    const val READ_TIMEOUT = 60
    const val WRITE_TIMEOUT = 60
    const val WORKER_TAG = "Upload"

    /** Agora constants*/

    const val agoraAppId = "489ab3de83944e87b7ffd278c4ccf35b"
    const val agoraChannelName = "birdMessenger"
    const val agoraToken = "007eJxTYAiWinnSd+ecyz7Vu4HFmzcl+jXlPEpx1tJkl56lk35sv7YCg4mFZWKScUqqhbGliUmqhXmSeVpaipG5RbJJcnKasWlS7nHZlIZARgZlvqWsjAwQCOLzMiRlFqX4phYXp+alpxYxMAAAJrohlQ==/PSU4sYGABUuyIy"


    /** StartActivity Constants */

    const val ON_FOREGROUND = "onForeground"
    const val YES = "y"
    const val NO = "n"

    /** StartActivityViewModel Constants */

    const val INSTALLATION_ID = "installationId"
    const val STATUS = "status"
    const val TOKEN = "token"
    const val COMMON_TOPIC_SUBSCRIBE = "commonTopicSubscribe"
    const val INDIVIDUAL_TOPIC_SUBSCRIBE = "individualTopicSubscribe"
    const val IS_TOKEN_UPLOADED = "isTokenUploaded"

    /** 1. DH */

    const val DH_KEY_PAIR = "DHKeyPair"
    const val DH_PRIVATE = "DHPrivate"
    const val DH_PUBLIC = "DHPublic"
    const val IS_DH_KEY_UPLOADED = "isDHKeyUploaded"

    /** 2. ECDH */

    const val ECDH_KEY_PAIR = "ECDHKeyPair"
    const val ECDH_PUBLIC = "ECDHPublic"
    const val ECDH_PRIVATE = "ECDHPrivate"
    const val IS_ECDH_KEY_UPLOADED = "isECDHKeyUploaded"
}