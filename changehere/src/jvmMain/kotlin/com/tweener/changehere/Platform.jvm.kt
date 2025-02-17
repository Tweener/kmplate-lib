package com.tweener.changehere

class JvmPlatform : Platform {
    override val name: String = "JVM"
}

actual fun getPlatform(): Platform = JvmPlatform()
