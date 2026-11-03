package com.hivid.app.media

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin MP4 fast-start relocator.
 * Moves the 'moov' atom to the front of the file (before 'mdat')
 * and adjusts 'stco' / 'co64' chunk offsets accordingly for instant web and social playback.
 */
object FastStartMp4 {

    fun process(inputFile: File, outputFile: File): Boolean {
        try {
            RandomAccessFile(inputFile, "r").use { reader ->
                val length = reader.length()
                var moovOffset = -1L
                var moovSize = -1L
                var mdatOffset = -1L
                var mdatSize = -1L
                var ftypOffset = -1L
                var ftypSize = -1L

                var offset = 0L
                val headerBytes = ByteArray(8)

                while (offset < length - 8) {
                    reader.seek(offset)
                    reader.readFully(headerBytes)
                    val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
                    var size = buffer.int.toLong() and 0xFFFFFFFFL
                    val type = String(headerBytes, 4, 4, Charsets.US_ASCII)

                    if (size == 1L) {
                        // 64-bit large box size
                        val largeBytes = ByteArray(8)
                        reader.readFully(largeBytes)
                        size = ByteBuffer.wrap(largeBytes).order(ByteOrder.BIG_ENDIAN).long
                    } else if (size == 0L) {
                        // Extends to end of file
                        size = length - offset
                    }

                    when (type) {
                        "ftyp" -> {
                            ftypOffset = offset
                            ftypSize = size
                        }
                        "moov" -> {
                            moovOffset = offset
                            moovSize = size
                        }
                        "mdat" -> {
                            mdatOffset = offset
                            mdatSize = size
                        }
                    }

                    if (size <= 0) break
                    offset += size
                }

                // If moov is already before mdat or moov/mdat not found, simple copy
                if (moovOffset == -1L || mdatOffset == -1L || moovOffset < mdatOffset) {
                    inputFile.copyTo(outputFile, overwrite = true)
                    return true
                }

                // Read moov bytes into memory
                val moovBytes = ByteArray(moovSize.toInt())
                reader.seek(moovOffset)
                reader.readFully(moovBytes)

                // Patch chunk offsets (stco and co64) inside moovBytes
                patchChunkOffsets(moovBytes, moovSize)

                // Write output file: ftyp -> moov (patched) -> mdat and other boxes
                RandomAccessFile(outputFile, "rw").use { writer ->
                    writer.setLength(0)

                    // 1. Write ftyp if present
                    if (ftypOffset >= 0 && ftypSize > 0) {
                        val ftypBytes = ByteArray(ftypSize.toInt())
                        reader.seek(ftypOffset)
                        reader.readFully(ftypBytes)
                        writer.write(ftypBytes)
                    }

                    // 2. Write relocated moov box
                    writer.write(moovBytes)

                    // 3. Write all other boxes except ftyp and moov
                    val bufferChunk = ByteArray(64 * 1024)
                    reader.seek(0)
                    var curPos = 0L

                    while (curPos < length) {
                        val boxHeader = ByteArray(8)
                        reader.seek(curPos)
                        val read = reader.read(boxHeader)
                        if (read < 8) break

                        val bBuf = ByteBuffer.wrap(boxHeader).order(ByteOrder.BIG_ENDIAN)
                        var boxSize = bBuf.int.toLong() and 0xFFFFFFFFL
                        val bType = String(boxHeader, 4, 4, Charsets.US_ASCII)

                        if (boxSize == 1L) {
                            val largeB = ByteArray(8)
                            reader.readFully(largeB)
                            boxSize = ByteBuffer.wrap(largeB).order(ByteOrder.BIG_ENDIAN).long
                        } else if (boxSize == 0L) {
                            boxSize = length - curPos
                        }

                        if (bType != "ftyp" && bType != "moov") {
                            reader.seek(curPos)
                            var remaining = boxSize
                            while (remaining > 0) {
                                val toRead = minOf(remaining, bufferChunk.size.toLong()).toInt()
                                val bytesRead = reader.read(bufferChunk, 0, toRead)
                                if (bytesRead <= 0) break
                                writer.write(bufferChunk, 0, bytesRead)
                                remaining -= bytesRead
                            }
                        }

                        if (boxSize <= 0) break
                        curPos += boxSize
                    }
                }
            }
            return true
        } catch (e: Exception) {
            inputFile.copyTo(outputFile, overwrite = true)
            return false
        }
    }

    private fun patchChunkOffsets(moovBytes: ByteArray, moovOffsetShift: Long) {
        val stcoTag = "stco".toByteArray(Charsets.US_ASCII)
        val co64Tag = "co64".toByteArray(Charsets.US_ASCII)

        // Find 'stco' (32-bit chunk offsets)
        var i = 0
        while (i < moovBytes.size - 8) {
            if (moovBytes[i] == stcoTag[0] &&
                moovBytes[i + 1] == stcoTag[1] &&
                moovBytes[i + 2] == stcoTag[2] &&
                moovBytes[i + 3] == stcoTag[3]
            ) {
                // stco structure: 4 bytes type, 4 bytes version/flags, 4 bytes entry count, entries (4 bytes each)
                val entryCountPos = i + 8
                if (entryCountPos + 4 <= moovBytes.size) {
                    val entryCount = ByteBuffer.wrap(moovBytes, entryCountPos, 4).order(ByteOrder.BIG_ENDIAN).int
                    var entryPos = entryCountPos + 4
                    for (e in 0 until entryCount) {
                        if (entryPos + 4 > moovBytes.size) break
                        val oldOffset = ByteBuffer.wrap(moovBytes, entryPos, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
                        val newOffset = (oldOffset + moovOffsetShift).toInt()
                        ByteBuffer.wrap(moovBytes, entryPos, 4).order(ByteOrder.BIG_ENDIAN).putInt(newOffset)
                        entryPos += 4
                    }
                }
            } else if (moovBytes[i] == co64Tag[0] &&
                moovBytes[i + 1] == co64Tag[1] &&
                moovBytes[i + 2] == co64Tag[2] &&
                moovBytes[i + 3] == co64Tag[3]
            ) {
                // co64 structure: 4 bytes type, 4 bytes version/flags, 4 bytes entry count, entries (8 bytes each)
                val entryCountPos = i + 8
                if (entryCountPos + 4 <= moovBytes.size) {
                    val entryCount = ByteBuffer.wrap(moovBytes, entryCountPos, 4).order(ByteOrder.BIG_ENDIAN).int
                    var entryPos = entryCountPos + 4
                    for (e in 0 until entryCount) {
                        if (entryPos + 8 > moovBytes.size) break
                        val oldOffset = ByteBuffer.wrap(moovBytes, entryPos, 8).order(ByteOrder.BIG_ENDIAN).long
                        val newOffset = oldOffset + moovOffsetShift
                        ByteBuffer.wrap(moovBytes, entryPos, 8).order(ByteOrder.BIG_ENDIAN).putLong(newOffset)
                        entryPos += 8
                    }
                }
            }
            i++
        }
    }
}
