package com.luc4n3x.levyra.desktop.player

data class AudioOutputDevice(
    val id: String,
    val label: String,
    val outputName: String = "",
    val deviceId: String = id
) {
    companion object {
        const val SYSTEM_DEFAULT_ID = ""
        private const val ID_SEPARATOR = "\u001F"

        fun create(outputName: String, deviceId: String, label: String): AudioOutputDevice {
            val cleanOutput = outputName.trim()
            val cleanDevice = deviceId.trim()
            val persistedId = if (cleanOutput.isEmpty()) cleanDevice else "$cleanOutput$ID_SEPARATOR$cleanDevice"
            return AudioOutputDevice(
                id = persistedId,
                label = label.trim().ifEmpty { cleanDevice.ifEmpty { persistedId } },
                outputName = cleanOutput,
                deviceId = cleanDevice
            )
        }

        fun fromPersistedId(id: String): AudioOutputDevice {
            val clean = id.trim()
            if (clean.isEmpty()) return AudioOutputDevice(SYSTEM_DEFAULT_ID, "", "", "")
            val separator = clean.indexOf(ID_SEPARATOR)
            return if (separator <= 0 || separator >= clean.lastIndex) {
                AudioOutputDevice(id = clean, label = clean, outputName = "", deviceId = clean)
            } else {
                AudioOutputDevice(
                    id = clean,
                    label = clean,
                    outputName = clean.substring(0, separator),
                    deviceId = clean.substring(separator + ID_SEPARATOR.length)
                )
            }
        }

        fun sanitize(devices: List<AudioOutputDevice>): List<AudioOutputDevice> {
            val seen = HashSet<String>(devices.size)
            return devices.mapNotNull { device ->
                val id = device.id.trim()
                val deviceId = device.deviceId.trim()
                if (id.isEmpty() || deviceId.isEmpty() || !seen.add(id)) return@mapNotNull null
                device.copy(
                    id = id,
                    label = device.label.trim().ifEmpty { deviceId },
                    outputName = device.outputName.trim(),
                    deviceId = deviceId
                )
            }
        }
    }
}
