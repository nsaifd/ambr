package app.ambr.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ambr.R
import app.ambr.data.ProofKind
import app.ambr.ui.components.AmbrButton
import app.ambr.ui.components.ButtonStyle
import app.ambr.ui.components.CircleIconButton
import app.ambr.ui.theme.AmbrColors
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val CameraBg = Color(0xFF0E1320)
private val Glass = Color(0x33FFFFFF)

@Composable
fun CaptureFlow(vm: CaptureViewModel, onClose: () -> Unit, onTypeIt: () -> Unit, onResult: (CaptureResult) -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val close = { vm.discard(); onClose() }
    BackHandler {
        when (s.stage) {
            Stage.Camera -> if (s.pages.isEmpty()) close() else vm.toPages()
            Stage.Pages -> close()
            Stage.Reading -> Unit
        }
    }
    when (s.stage) {
        Stage.Camera -> CameraStage(vm, s, close, onTypeIt)
        Stage.Pages -> PagesStage(vm, s, close) { vm.finish(onResult) }
        Stage.Reading -> ReadingStage(s, vm.forExistingItem)
    }
}

@Composable
private fun CameraStage(vm: CaptureViewModel, s: CaptureState, onClose: () -> Unit, onTypeIt: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; asked = true }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { if (it.isNotEmpty()) vm.importImages(it) }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importPdf) }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    val pickImages = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    if (!granted && asked) {
        CameraDenied(onClose, pickImages, onTypeIt)
        return
    }

    val lifecycle = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) } }
    var flash by remember { mutableStateOf(false) }
    var shooting by remember { mutableStateOf(false) }
    DisposableEffect(granted) {
        if (granted) controller.bindToLifecycle(lifecycle)
        onDispose { controller.unbind() }
    }
    LaunchedEffect(flash) { controller.imageCaptureFlashMode = if (flash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF }

    Box(Modifier.fillMaxSize().background(CameraBg)) {
        if (granted) AndroidView({ PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } }, Modifier.fillMaxSize())
        // Frame guide
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.78f).aspectRatio(0.62f).border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(24.dp)))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(R.drawable.ic_cross_small, "Close camera", onClose, background = Glass, tint = Color.White)
                Spacer(Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(22.dp)).background(Glass).padding(4.dp)) {
                    listOf(ProofKind.Receipt, ProofKind.WarrantyCard).forEach { k ->
                        val on = s.kind == k
                        Text(
                            k.label, style = MaterialTheme.typography.titleSmall, color = if (on) AmbrColors.TextPrimary else Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (on) Color.White else Color.Transparent)
                                .clickable(role = Role.Tab) { vm.setKind(k) }.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                CircleIconButton(R.drawable.ic_bolt, if (flash) "Flash on" else "Flash off", { flash = !flash }, background = if (flash) AmbrColors.Honey else Glass, tint = if (flash) AmbrColors.TextPrimary else Color.White)
            }
            Text(
                if (s.kind == ProofKind.Receipt) "Fit the receipt inside the frame" else "Fit the warranty card inside the frame",
                style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (s.pages.isNotEmpty()) Text("${s.pages.size} ${if (s.pages.size == 1) "page" else "pages"} so far · take the next one", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                SideButton(R.drawable.ic_picture, "Gallery", pickImages)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(78.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).padding(7.dp).clip(CircleShape)
                        .background(if (shooting || s.busy) AmbrColors.HoneyPressed else AmbrColors.Honey)
                        .clickable(enabled = granted && !shooting && !s.busy, role = Role.Button) {
                            shooting = true
                            val file = vm.newFile()
                            controller.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(),
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) { shooting = false; vm.captured(file) }
                                    override fun onError(e: ImageCaptureException) { shooting = false; file.delete() }
                                },
                            )
                        }
                        .semantics { contentDescription = "Take photo" },
                    contentAlignment = Alignment.Center,
                ) { if (shooting || s.busy) CircularProgressIndicator(Modifier.size(28.dp), color = AmbrColors.TextPrimary, strokeWidth = 3.dp) }
                Spacer(Modifier.weight(1f))
                SideButton(R.drawable.ic_file_pdf, "PDF") { pdf.launch(arrayOf("application/pdf")) }
            }
            if (!vm.forExistingItem && s.pages.isEmpty()) Text(
                "No receipt? Type it in", style = MaterialTheme.typography.titleSmall, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onTypeIt).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SideButton(icon: Int, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun CameraDenied(onClose: () -> Unit, onGallery: () -> Unit, onTypeIt: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(CameraBg).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CircleIconButton(R.drawable.ic_cross_small, "Close", onClose, background = Glass, tint = Color.White)
        Spacer(Modifier.weight(1f))
        Box(Modifier.align(Alignment.CenterHorizontally).size(96.dp).clip(RoundedCornerShape(30.dp)).background(Glass), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_camera), null, tint = Color.White, modifier = Modifier.size(42.dp))
            Box(Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(AmbrColors.Honey), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_lock), null, tint = AmbrColors.TextPrimary, modifier = Modifier.size(16.dp))
            }
        }
        Text("Camera is off for Ambr", style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Ambr only uses the camera to photograph receipts and warranty cards. The photos stay on your phone.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.OnDarkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.weight(1f))
        AmbrButton("Open settings", {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }, icon = R.drawable.ic_settings)
        AmbrButton("Pick from gallery", onGallery, style = ButtonStyle.Strong, icon = R.drawable.ic_picture)
        Text("Add without a photo", style = MaterialTheme.typography.titleSmall, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onTypeIt).padding(12.dp))
    }
}

@Composable
private fun PagesStage(vm: CaptureViewModel, s: CaptureState, onClose: () -> Unit, onRead: () -> Unit) {
    val page = s.pages.getOrNull(s.current) ?: return
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleIconButton(R.drawable.ic_cross_small, "Discard", onClose)
            Text("Page ${s.current + 1} of ${s.pages.size}", style = MaterialTheme.typography.headlineSmall, color = AmbrColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("Retake", style = MaterialTheme.typography.titleSmall, color = AmbrColors.HoneyInk, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { vm.retake() }.padding(10.dp))
        }
        Row(Modifier.clip(RoundedCornerShape(16.dp)).background(AmbrColors.Ring).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ProofKind.entries.forEach { k ->
                val on = page.kind == k
                Text(
                    k.label, style = MaterialTheme.typography.titleSmall, color = if (on) AmbrColors.TextPrimary else AmbrColors.TextMuted,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) AmbrColors.Card else AmbrColors.Ring).clickable(role = Role.Tab) { vm.setPageKind(k) }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(24.dp))) {
            AsyncImage(
                ImageRequest.Builder(context).data(page.file).memoryCacheKey("${page.file.path}#${page.version}").diskCacheKey("${page.file.path}#${page.version}").build(),
                page.kind.label, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolButton(R.drawable.ic_rotate_right, "Rotate", Modifier.weight(1f)) { vm.rotate() }
            ToolButton(R.drawable.ic_trash, "Remove", Modifier.weight(1f), danger = true) { vm.remove() }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            itemsIndexed(s.pages) { i, p ->
                Box(
                    Modifier.size(48.dp, 60.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, if (i == s.current) AmbrColors.Navy else AmbrColors.Ring, RoundedCornerShape(12.dp)).clickable { vm.select(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(ImageRequest.Builder(context).data(p.file).memoryCacheKey("${p.file.path}#${p.version}").build(), "Page ${i + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            item {
                Box(Modifier.size(48.dp, 60.dp).clip(RoundedCornerShape(12.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.FieldBorder, RoundedCornerShape(12.dp)).clickable { vm.backToCamera() }, contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_plus), "Add page", tint = AmbrColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
        AmbrButton(if (vm.forExistingItem) "Save pages" else "Read details", onRead, icon = if (vm.forExistingItem) R.drawable.ic_check else R.drawable.ic_sparkles)
    }
}

@Composable
private fun ToolButton(icon: Int, label: String, modifier: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), null, tint = if (danger) AmbrColors.Danger else AmbrColors.TextPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (danger) AmbrColors.Danger else AmbrColors.TextPrimary)
    }
}

@Composable
private fun ReadingStage(s: CaptureState, existing: Boolean) {
    val shimmer = rememberInfiniteTransition(label = "reading")
    val alpha by shimmer.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
    Column(Modifier.fillMaxSize().background(AmbrColors.Ground).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(40.dp))
        s.pages.firstOrNull()?.let {
            AsyncImage(it.file, null, Modifier.align(Alignment.CenterHorizontally).size(120.dp, 150.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, AmbrColors.Ring, RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
        }
        Text(if (existing) "Saving pages…" else "Reading your receipt…", style = MaterialTheme.typography.headlineLarge, color = AmbrColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Takes a second or two. It all happens on your phone.", style = MaterialTheme.typography.bodyLarge, color = AmbrColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        if (!existing) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AmbrColors.Card).border(1.dp, AmbrColors.Ring, RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(R.drawable.ic_sparkles), null, tint = AmbrColors.HoneyInk, modifier = Modifier.size(14.dp))
                Text("Finding store, price and date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AmbrColors.HoneyInk)
            }
            listOf("Item name", "Store", "Price", "Bought on", "Warranty").forEach { label ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = AmbrColors.TextMuted)
                    Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(AmbrColors.Divider.copy(alpha = alpha)))
                }
            }
        }
    }
}
