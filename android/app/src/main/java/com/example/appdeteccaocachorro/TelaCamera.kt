
package com.example.appdeteccaocachorro

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File
import kotlin.math.roundToInt

@Composable
fun TelaCamera(
    onFotoCapturada: (Bitmap, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Confiança mínima escolhida para o YOLO.
    var confiancaMinima by remember {
        mutableFloatStateOf(0.5f)
    }

    var permissaoConcedida by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var imageCapture by remember {
        mutableStateOf<ImageCapture?>(null)
    }

    var capturando by remember {
        mutableStateOf(false)
    }

    var mensagem by remember {
        mutableStateOf("")
    }

    // Solicita permissão para acessar a câmera.
    val solicitarPermissao =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedida ->
            permissaoConcedida = concedida
        }

    LaunchedEffect(Unit) {
        if (!permissaoConcedida) {
            solicitarPermissao.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (permissaoConcedida) {

            val previewView = remember {
                PreviewView(context)
            }

            // Inicia a câmera e sua pré-visualização.
            DisposableEffect(
                lifecycleOwner,
                previewView
            ) {

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(context)

                var ativo = true
                var cameraProvider: ProcessCameraProvider? = null

                cameraProviderFuture.addListener({

                    if (ativo) {
                        try {

                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider

                            val preview = Preview.Builder()
                                .build()
                                .also { cameraPreview ->
                                    cameraPreview.setSurfaceProvider(
                                        previewView.surfaceProvider
                                    )
                                }

                            val captura = ImageCapture.Builder()
                                .setCaptureMode(
                                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                                )
                                .build()

                            provider.unbindAll()

                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                captura
                            )

                            imageCapture = captura
                            mensagem = ""

                        } catch (e: Exception) {
                            mensagem =
                                "Erro ao iniciar câmera: ${e.message}"
                        }
                    }

                }, ContextCompat.getMainExecutor(context))

                onDispose {
                    ativo = false
                    imageCapture = null
                    cameraProvider?.unbindAll()
                }
            }

            // Mostra a imagem ao vivo.
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Controle de confiança da câmera.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Confiança mínima",
                            style =
                                MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = "${(confiancaMinima * 100).roundToInt()}%",
                            style =
                                MaterialTheme.typography.titleSmall
                        )
                    }

                    Slider(
                        value = confiancaMinima,
                        onValueChange = { valor ->
                            confiancaMinima = valor
                        },
                        valueRange = 0.1f..1f,
                        enabled = !capturando
                    )

                    Text(
                        text =
                            "Ajuste a confiança antes de tirar a foto.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botão para capturar uma foto.
            Button(
                onClick = {

                    val captura = imageCapture

                    if (captura == null) {
                        mensagem = "A câmera ainda está iniciando."
                        return@Button
                    }

                    // Congela a confiança escolhida nesta captura.
                    val confiancaEscolhida = confiancaMinima

                    capturando = true
                    mensagem = "Capturando foto..."

                    val arquivo = try {
                        File.createTempFile(
                            "foto_cachorro_",
                            ".jpg",
                            context.cacheDir
                        )
                    } catch (e: Exception) {
                        capturando = false
                        mensagem =
                            "Erro ao preparar a foto: ${e.message}"
                        return@Button
                    }

                    val opcoes =
                        ImageCapture.OutputFileOptions.Builder(
                            arquivo
                        ).build()

                    try {
                        captura.takePicture(
                            opcoes,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {

                                override fun onImageSaved(
                                    output: ImageCapture.OutputFileResults
                                ) {

                                    scope.launch {
                                        try {

                                            val bitmap = withContext(
                                                Dispatchers.IO
                                            ) {

                                                try {

                                                    val original =
                                                        BitmapFactory.decodeFile(
                                                            arquivo.absolutePath
                                                        ) ?: error(
                                                            "Não foi possível ler a foto."
                                                        )

                                                    // Corrige a orientação EXIF.
                                                    val exif = ExifInterface(
                                                        arquivo.absolutePath
                                                    )

                                                    val orientacao =
                                                        exif.getAttributeInt(
                                                            ExifInterface.TAG_ORIENTATION,
                                                            ExifInterface.ORIENTATION_NORMAL
                                                        )

                                                    val matriz = Matrix()

                                                    when (orientacao) {

                                                        ExifInterface.ORIENTATION_ROTATE_90 ->
                                                            matriz.postRotate(90f)

                                                        ExifInterface.ORIENTATION_ROTATE_180 ->
                                                            matriz.postRotate(180f)

                                                        ExifInterface.ORIENTATION_ROTATE_270 ->
                                                            matriz.postRotate(270f)
                                                    }

                                                    if (matriz.isIdentity) {
                                                        original
                                                    } else {
                                                        Bitmap.createBitmap(
                                                            original,
                                                            0,
                                                            0,
                                                            original.width,
                                                            original.height,
                                                            matriz,
                                                            true
                                                        )
                                                    }

                                                } finally {
                                                    arquivo.delete()
                                                }
                                            }

                                            mensagem = "Foto capturada!"

                                            // Envia a imagem e a confiança.
                                            onFotoCapturada(
                                                bitmap,
                                                confiancaEscolhida
                                            )

                                        } catch (e: Exception) {

                                            mensagem =
                                                "Erro ao abrir foto: ${e.message}"

                                        } finally {
                                            capturando = false
                                        }
                                    }
                                }

                                override fun onError(
                                    exception: ImageCaptureException
                                ) {
                                    arquivo.delete()
                                    capturando = false

                                    mensagem =
                                        "Erro na captura: ${exception.message}"
                                }
                            }
                        )
                    } catch (e: Exception) {
                        arquivo.delete()
                        capturando = false
                        mensagem =
                            "Erro ao capturar foto: ${e.message}"
                    }
                },

                modifier = Modifier.fillMaxWidth(),

                enabled = !capturando && imageCapture != null,

                shape = RoundedCornerShape(14.dp)

            ) {

                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    if (capturando) {
                        "Capturando..."
                    } else {
                        "Tirar foto"
                    }
                )
            }

        } else {

            Text(
                "Precisamos da permissão da câmera para tirar fotos."
            )

            Button(
                onClick = {
                    solicitarPermissao.launch(
                        Manifest.permission.CAMERA
                    )
                }
            ) {
                Text("Permitir câmera")
            }
        }

        if (mensagem.isNotEmpty()) {
            Text(mensagem)
        }
    }
}
