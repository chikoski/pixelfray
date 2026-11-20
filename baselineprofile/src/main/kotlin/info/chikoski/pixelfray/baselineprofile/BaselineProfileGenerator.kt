package info.chikoski.pixelfray.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val targetPackage = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "info.chikoski.pixelfray"

        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = true,
        ) {
            device.wakeUp()
            device.executeShellCommand("wm dismiss-keyguard")
            pressHome()
            startActivityAndWait { intent ->
                intent.putExtra("info.chikoski.pixelfray.BENCHMARK_WARMUP", true)
            }
            device.wait(Until.hasObject(By.descContains("ms/frame")), 10_000)
            device.waitForIdle()
        }
    }
}
