package com.lzt.summaryofslides.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzt.summaryofslides.ui.entrydetail.EntryDetailViewModel
import com.lzt.summaryofslides.ui.entrydetail.EntryDetailViewModelFactory
import com.lzt.summaryofslides.util.createTempCameraFile
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    entryId: String,
    onBack: () -> Unit,
) {
    val vm: EntryDetailViewModel = viewModel(factory = EntryDetailViewModelFactory(entryId))
    val errorMessage by vm.errorMessage.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturedCount by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf("轻触取景区域可重新对焦，拍照后会停留在本页面") }

    val requestCameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            statusMessage =
                if (granted) {
                    "相机已就绪，拍照后会继续停留在当前界面"
                } else {
                    "未授予相机权限，无法进入连续拍照"
                }
        }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(previewView, hasCameraPermission, lifecycleOwner) {
        val view = previewView
        if (!hasCameraPermission || view == null) {
            onDispose { }
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            val listener =
                Runnable {
                    val provider = runCatching { future.get() }.getOrNull() ?: return@Runnable
                    val preview =
                        Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }
                    val capture =
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetRotation(view.display?.rotation ?: Surface.ROTATION_0)
                            .build()
                    runCatching {
                        provider.unbindAll()
                        camera =
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                            )
                        imageCapture = capture
                    }.onFailure {
                        statusMessage = "相机初始化失败：${it.message ?: "未知错误"}"
                    }
                }
            future.addListener(listener, mainExecutor)
            onDispose {
                imageCapture = null
                camera = null
                if (future.isDone) {
                    runCatching { future.get().unbindAll() }
                }
            }
        }
    }

    fun focusAt(x: Float, y: Float) {
        val activeCamera = camera ?: return
        val activePreview = previewView ?: return
        val point = activePreview.meteringPointFactory.createPoint(x, y)
        val action =
            FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(5, TimeUnit.MINUTES)
                .build()
        activeCamera.cameraControl.startFocusAndMetering(action)
        statusMessage = "已锁定当前对焦区域，后续拍照不会主动退出相机界面"
    }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        if (isCapturing) return
        isCapturing = true
        statusMessage = "正在拍照并保存..."
        val tempFile = createTempCameraFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
        capture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    vm.importCapturedFile(tempFile) {
                        capturedCount += 1
                        isCapturing = false
                        statusMessage = "已保存第 $capturedCount 张，可继续拍摄或手动返回"
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    tempFile.delete()
                    isCapturing = false
                    statusMessage = "拍照失败：${exception.message ?: "未知错误"}"
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连续拍照") },
                navigationIcon = { Button(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("需要相机权限才能进入常驻连续拍照模式")
                Button(onClick = { requestCameraPermission.launch(Manifest.permission.CAMERA) }) {
                    Text("授予相机权限")
                }
                Button(onClick = onBack) {
                    Text("返回")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            keepScreenOn = true
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        previewView = view
                    },
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(camera, previewView) {
                            detectTapGestures { offset ->
                                focusAt(offset.x, offset.y)
                            }
                        },
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = statusMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "已拍摄 $capturedCount 张",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = ::capturePhoto,
                            enabled = imageCapture != null && !isCapturing,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("拍照")
                            }
                        }
                        Button(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("完成返回")
                        }
                    }
                }
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = vm::clearError,
            confirmButton = {
                Button(onClick = vm::clearError) {
                    Text("知道了")
                }
            },
            title = { Text("提示") },
            text = { Text(errorMessage.orEmpty()) },
        )
    }
}
