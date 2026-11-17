package com.moviesnight.tv.data

import com.moviesnight.tv.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Anonymous client. The publishable key only allows what RLS grants the
 * public role, which for this app is SELECT on records and genres.
 */
fun createAppSupabaseClient(
    url: String = BuildConfig.SUPABASE_URL,
    key: String = BuildConfig.SUPABASE_KEY,
): SupabaseClient = createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
    install(Postgrest)
}
