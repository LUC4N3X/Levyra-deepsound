package com.luc4n3x.levyra.desktop.player

data class AudioOutputDevice(
    val id: String,
    val label: String
) {
    companion object {
        const val SYSTEM_DEFAULT_ID = ""

        fun sanitize(devices: List<AudioOutputDevice>): List<AudioOutputDevice> {
            val seen = HashSet<String>(devices.size)
            return devices.mapNotNull { device ->
                val id = device.id.trim()
                if (id.isEmpty() || !seen.add(id)) return@mapNotNull null
                AudioOutputDevice(id = id, label = device.label.trim().ifEmpty { id })
            }
        }
    }
}
