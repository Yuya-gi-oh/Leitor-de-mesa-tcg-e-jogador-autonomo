package com.yuyagioh.tcgreader.app.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yuyagioh.tcgreader.app.R
import com.yuyagioh.tcgreader.core.model.GameId

@Composable
fun CameraScreen(
    gameId: String,
    onBack: () -> Unit,
    cameraViewModel: CameraViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    LaunchedEffect(gameId) {
        cameraViewModel.bind(runCatching { GameId.valueOf(gameId) }.getOrDefault(GameId.MAGIC))
    }

    val scans by cameraViewModel.scans.collectAsStateWithLifecycle()
    val selectedCard by cameraViewModel.selectedCard.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }

        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(R.string.camera_permission_needed),
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Column
        }

        val previewView = remember { PreviewView(context) }
        DisposableEffect(lifecycleOwner) {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            var providerRef: ProcessCameraProvider? = null
            val mainExecutor = ContextCompat.getMainExecutor(context)
            providerFuture.addListener(
                {
                    val provider = providerFuture.get()
                    providerRef = provider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(mainExecutor) { frame -> cameraViewModel.onAnalyzerFrame(frame) }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                },
                mainExecutor,
            )
            onDispose {
                runCatching { providerRef?.unbindAll() }
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.scanning),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (scans.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_matches),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    items(scans) { scan ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "\u201E${scan.rawText}\u201C",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                scan.candidates.forEach { candidate ->
                                    AssistChip(
                                        onClick = { cameraViewModel.selectCandidate(candidate) },
                                        label = {
                                            Text(
                                                candidate.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (candidate == selectedCard) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (selectedCard != null) {
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
