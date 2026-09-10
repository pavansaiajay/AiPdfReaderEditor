package pavansaiajayx.aipdfreadereditor.app.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsManager(private val firebaseAnalytics: FirebaseAnalytics) {

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                putString(key, value)
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    fun logScreenView(screenName: String) {
        logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(FirebaseAnalytics.Param.SCREEN_NAME to screenName)
        )
    }
}
