package com.example.seedcatalog.data.local

enum class PhotoType(val dbValue: String, val displayName: String) {
    FRONT("front", "Front"),
    BACK("back", "Back"),
    CLOSEUP("closeup", "Closeup"),
    PLANT("plant", "Plant")
}
