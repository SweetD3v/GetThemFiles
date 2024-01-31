package com.filerecover.photorecovery.allrecover.restore.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale


class LocaleHelper {
    companion object {
        fun onAttach(context: Context): Context {
            val lang = getPersistedData(context, Locale.getDefault().language)
            return setLocale(context, lang)
        }

        fun onAttach(context: Context, defaultLanguage: String): Context {
            val lang = getPersistedData(context, defaultLanguage)
            return setLocale(context, lang)
        }

        fun getLanguage(context: Context): String? {
            return getPersistedData(context, Locale.getDefault().language)
        }

        fun setLocale(context: Context, language: String?): Context {
            persist(context, language)
            if (isNougatPlus())
                return updateResources(context, language)
            return updateResourcesLegacy(context, language)
        }

        private fun getPersistedData(context: Context, defaultLanguage: String): String? {
            val preferences = PrefsManager.newInstance(context)
            return preferences.getString(KEY_SELECTED_LANGUAGE_CODE, defaultLanguage)
        }

        private fun persist(context: Context, language: String?) {
            val preferences = PrefsManager.newInstance(context)
            preferences.putString(KEY_SELECTED_LANGUAGE_CODE, language ?: "")
        }

        private fun updateResources(context: Context, language: String?): Context {
            val locale = Locale(language ?: "en")
            Locale.setDefault(locale)
            val resources = context.resources
            val configuration: Configuration = resources.configuration
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context.createConfigurationContext(configuration)
        }

        @Suppress("deprecation")
        private fun updateResourcesLegacy(context: Context, language: String?): Context {
            val locale = Locale(language ?: "en")
            Locale.setDefault(locale)
            val resources = context.resources
            val configuration: Configuration = resources.configuration
            configuration.locale = locale
            configuration.setLayoutDirection(locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
}