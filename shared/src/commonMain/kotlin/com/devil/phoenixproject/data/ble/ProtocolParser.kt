package com.devil.phoenixproject.data.ble

/**
 * Pure byte parsing utility functions for the Vitruvian BLE protocol.
 *
 * These functions extract integers and floats from ByteArray data received
 * from the trainer hardware. All functions are stateless and handle endianness
 * and sign extension correctly.
 *
 * CRITICAL: All byte-to-int conversions MUST mask with `and 0xFF` to prevent
 * sign extension bugs (Kotlin bytes are signed, so 0xFF becomes -1 without masking).
 */

/**
 * Read unsigned 16-bit integer in LITTLE-ENDIAN format (LSB first).
 * This is the primary format used by the Vitruvian BLE protocol.
 *
 * @param data The byte array to read from
 * @param offset The starting position in the array
 * @return Unsigned value in range [0, 65535]
 */
fun getUInt16LE(data: ByteArray, offset: Int): Int {
    return (data[offset].toInt() and 0xFF) or
           ((data[offset + 1].toInt() and 0xFF) shl 8)
}

/**
 * Read signed 16-bit integer in LITTLE-ENDIAN format (LSB first).
 * Used for position values which can be negative (Issue #197).
 *
 * @param data The byte array to read from
 * @param offset The starting position in the array
 * @return Signed value in range [-32768, 32767]
 */
fun getInt16LE(data: ByteArray, offset: Int): Int {
    val unsigned = (data[offset].toInt() and 0xFF) or
                   ((data[offset + 1].toInt() and 0xFF) shl 8)
    // Sign-extend from 16-bit to 32-bit
    return if (unsigned >= 0x8000) unsigned - 0x10000 else unsigned
}

/**
 * Read unsigned 16-bit integer in BIG-ENDIAN format (MSB first).
 * Used for some packet types in the protocol.
 *
 * @param data The byte array to read from
 * @param offset The starting position in the array
 * @return Unsigned value in range [0, 65535]
 */
fun getUInt16BE(data: ByteArray, offset: Int): Int {
    return ((data[offset].toInt() and 0xFF) shl 8) or
           (data[offset + 1].toInt() and 0xFF)
}

/**
 * Read signed 32-bit integer in LITTLE-ENDIAN format.
 * Used for rep counters in 24-byte packets.
 *
 * @param data The byte array to read from
 * @param offset The starting position in the array
 * @return Signed 32-bit value
 */
fun getInt32LE(data: ByteArray, offset: Int): Int {
    return (data[offset].toInt() and 0xFF) or
           ((data[offset + 1].toInt() and 0xFF) shl 8) or
           ((data[offset + 2].toInt() and 0xFF) shl 16) or
           ((data[offset + 3].toInt() and 0xFF) shl 24)
}

/**
 * Read 32-bit float in LITTLE-ENDIAN format (IEEE 754).
 * Used for ROM boundaries in 24-byte rep packets.
 *
 * @param data The byte array to read from
 * @param offset The starting position in the array
 * @return Float value
 */
fun getFloatLE(data: ByteArray, offset: Int): Float {
    val bits = getInt32LE(data, offset)
    return Float.fromBits(bits)
}

/**
 * Convert a Byte to a two-character uppercase hex string.
 * KMP-compatible implementation (no Java dependencies).
 *
 * @return Two-character uppercase hex string (e.g., "FF", "0A", "00")
 */
fun Byte.toVitruvianHex(): String {
    val hex = "0123456789ABCDEF"
    val value = this.toInt() and 0xFF
    return "${hex[value shr 4]}${hex[value and 0x0F]}"
}
