package app.ambr.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.ambr.R
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.theme.AmbrColors
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning

/** Reads the serial barcode from a box or back sticker. Returns the text, or null when cancelled. */
@Composable
fun BarcodeScanScreen(onDone: (String?) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; if (!it) onDone(null) }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    var found by remember { mutableStateOf<String?>(null) }
    var torch by remember { mutableStateOf(false) }
    val scanner: BarcodeScanner = remember { BarcodeScanning.getClient() }
    val controller = remember { LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_ANALYSIS) } }

    DisposableEffect(granted) {
        if (granted) {
            val executor = ContextCompat.getMainExecutor(context)
            controller.setImageAnalysisAnalyzer(executor, MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
                if (found == null) {
                    // Serials are usually the longest code on the sticker; skip short and URL codes.
                    result.getValue(scanner)?.mapNotNull { it.rawValue?.trim() }?.filter { it.length >= 6 && !it.startsWith("http") }?.maxByOrNull { it.length }?.let { found = it }
                }
            })
            controller.bindToLifecycle(lifecycle)
        }
        onDispose { controller.clearImageAnalysisAnalyzer(); controller.unbind(); scanner.close() }
    }
    LaunchedEffect(torch) { controller.enableTorch(torch) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0E1320))) {
        if (granted) AndroidView({ PreviewView(it).apply { this.controller = controller } }, Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.82f).height(150.dp).border(2.dp, if (found != null) Color(0xFF5FD39A) else Color.White, RoundedCornerShape(20.dp)))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(R.drawable.ic_cross_small, "Close scanner", { onDone(null) }, background = Color(0x33FFFFFF), tint = Color.White)
                Text("Scan serial number", style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                CircleIconButton(R.drawable.ic_bolt, "Flash", { torch = !torch }, background = if (torch) AmbrColors.Honey else Color(0x33FFFFFF), tint = if (torch) AmbrColors.TextPrimary else Color.White)
            }
            Text("Point at the barcode on the sticker or box", style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            found?.let { code ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AmbrColors.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(AmbrColors.CoveredTint), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_barcode_read), null, tint = AmbrColors.CoveredInk, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Found it", style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted)
                            Text(code, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmbrColors.TextPrimary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AmbrButton("Scan again", { found = null }, Modifier.weight(1f), style = ButtonStyle.Quiet, height = 48.dp)
                        AmbrButton("Use this", { onDone(code) }, Modifier.weight(1f), height = 48.dp)
                    }
                }
            } ?: Text("Type it instead", style = MaterialTheme.typography.titleSmall, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onDone(null) }.padding(12.dp))
            Spacer(Modifier.height(4.dp))
        }
    }
}
