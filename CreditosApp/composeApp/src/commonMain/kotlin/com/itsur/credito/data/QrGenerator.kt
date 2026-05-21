package com.itsur.credito.data

import androidx.compose.ui.graphics.ImageBitmap

expect fun generarQrImageBitmap(texto: String, size: Int = 300): ImageBitmap
