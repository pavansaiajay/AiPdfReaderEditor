package pavansaiajayx.aipdfreadereditor.app.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(@ApplicationContext private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    fun loadRewardedAd(onAdLoaded: (Boolean) -> Unit) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            "ca-app-pub-3940256099942544/5224354917",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    onAdLoaded(false)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    onAdLoaded(true)
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit): Boolean {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { _ -> 
                onRewardEarned()
            }
            rewardedAd = null 
            return true
        }
        return false
    }
}
