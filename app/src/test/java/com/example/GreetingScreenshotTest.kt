package com.example

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { 
        MyApplicationTheme { 
            Row(modifier = Modifier.padding(16.dp)) {
                Text("C", color = ChromeBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("h", color = ChromeRed, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("r", color = ChromeYellow, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("o", color = ChromeBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("m", color = ChromeGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("e", color = ChromeRed, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
