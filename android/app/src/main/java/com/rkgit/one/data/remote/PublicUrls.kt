package com.rkgit.one.data.remote

import com.rkgit.one.BuildConfig

object PublicUrls {
    /** Public URL of a file in a public Supabase Storage bucket. */
    fun file(bucket: String, path: String): String =
        "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/$bucket/$path"
}
