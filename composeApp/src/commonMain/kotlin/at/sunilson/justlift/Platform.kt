package at.sunilson.justlift

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform