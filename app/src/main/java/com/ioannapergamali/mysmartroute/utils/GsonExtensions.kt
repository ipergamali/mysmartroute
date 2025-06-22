package com.ioannapergamali.mysmartroute.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Απλή συνάρτηση επέκτασης για χρήση της βιβλιοθήκης Gson με generics. */
inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson(json, object : TypeToken<T>() {}.type)

