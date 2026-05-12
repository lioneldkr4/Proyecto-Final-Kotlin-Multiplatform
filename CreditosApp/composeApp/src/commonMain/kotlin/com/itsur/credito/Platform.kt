package com.itsur.credito

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform