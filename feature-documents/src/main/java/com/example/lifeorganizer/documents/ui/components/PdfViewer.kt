package com.example.lifeorganizer.documents.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException

@Composable
fun PdfViewer(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(uri) {
        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let {
                pdfRenderer = PdfRenderer(it)
                pageCount = pdfRenderer?.pageCount ?: 0
            }
            if (fileDescriptor == null) failed = true
        } catch (e: Exception) {
            // Missing file, revoked permission or password-protected PDF
            e.printStackTrace()
            failed = true
        }

        onDispose {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    if (failed) {
        Text(
            com.example.lifeorganizer.core.i18n.Str.fileUnavailable.of(com.example.lifeorganizer.core.i18n.LocalAppLanguage.current),
            modifier = modifier.padding(24.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.error
        )
    } else if (pageCount > 0 && pdfRenderer != null) {
        LazyColumn(modifier = modifier) {
            items(pageCount) { index ->
                PdfPage(pdfRenderer = pdfRenderer!!, pageIndex = index)
            }
        }
    }
}

@Composable
fun PdfPage(pdfRenderer: PdfRenderer, pageIndex: Int) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(pageIndex) {
        val page = pdfRenderer.openPage(pageIndex)
        // Render at a higher resolution for better quality
        val width = context.resources.displayMetrics.widthPixels
        val scale = width.toFloat() / page.width
        val height = (page.height * scale).toInt()
        
        val renderBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(renderBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap = renderBitmap
        page.close()

        onDispose {
            // Bitmap is automatically handled by GC, but if we have memory issues, 
            // we could recycle it. For now, let Compose handle it.
        }
    }

    bitmap?.let { b ->
        Box(modifier = Modifier.padding(bottom = 8.dp).background(Color.White)) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(b.width.toFloat() / b.height.toFloat()),
                contentScale = ContentScale.Fit
            )
        }
    }
}
