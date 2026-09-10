package pavansaiajayx.aipdfreadereditor.app.core.reviews

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun showReviewDialog(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(context)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    manager.launchReviewFlow(activity, reviewInfo)
                } else {
                    // Silently fail, do not disturb the user if the API is unavailable
                    Log.e("AppReviewManager", "Review flow failed to load")
                }
            }
        } catch (e: Exception) {
            Log.e("AppReviewManager", "Error attempting to show review dialog", e)
        }
    }
}
