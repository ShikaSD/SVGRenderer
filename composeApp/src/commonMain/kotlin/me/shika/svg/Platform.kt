package me.shika.svg

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform