package io.github.minthem.noob.http.multipart

import io.github.minthem.noob.http.exception.MalformedMultipartException
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.MutableHttpHeaders
import io.github.minthem.noob.http.message.contentDisposition
import io.github.minthem.noob.http.message.contentType
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream

/**
 * Parses multipart body content from an input stream, handling headers and boundaries.
 *
 * This class is responsible for processing multipart data streams, identifying
 * individual parts, and separating them into form fields or file uploads. It is
 * designed to work with HTTP multipart requests, such as those used in file uploads
 * or form submissions.
 *
 * @constructor
 * @param stream The input stream to read the multipart data from.
 * @param boundary The boundary string used to separate parts in the multipart content.
 */
internal class MultipartBodyParser(
    private val stream: InputStream,
    private val boundary: String,
) {
    private var firstPart = true
    private val boundaryEnd = "\r\n--$boundary".toByteArray()
    private val channel = Channels.newChannel(stream)
    private val buffer = ByteBuffer.allocate(4096).flip()
    private var exhausted = false

    /**
     * Reads the next multipart section from the input stream, processes its headers, and extracts its body content.
     * This method is responsible for determining whether the part represents a form field or a file upload,
     * constructing the respective `Multipart` object, and returning it. If the stream has been completely
     * processed, it consumes any remaining content and returns `null`.
     *
     * @return A `Multipart` object representing the next part of the multipart request, or `null` if no parts remain.
     * @throws MalformedMultipartException If the multipart data or headers are invalid.
     */
    fun nextPart(): Multipart? {
        consumeBoundary()
        if (exhausted) {
            // ボディ終了したので、stream全部読み込んで消費する
            // streamはBodySourceによって、ボディ終端まで読んでくれる
            stream.transferTo(OutputStream.nullOutputStream())
            return null
        }

        val headers = parseHeaders()

        val disposition =
            headers.contentDisposition
                ?: throw MalformedMultipartException("Invalid Content-Disposition header")

        val filename = disposition.filename
        val name = disposition.name ?: throw MalformedMultipartException("Invalid Content-Disposition header")
        val charset = headers.contentType?.charset ?: Charsets.UTF_8

        return if (filename == null) {
            val output = ByteArrayOutputStream()
            outputPartBody(output)
            Multipart.FormField(name, headers, output.toString(charset))
        } else {
            val path = Files.createTempFile("noob.httpserver", "part")
            path.outputStream(StandardOpenOption.WRITE, StandardOpenOption.CREATE).use { output ->
                outputPartBody(output)
            }
            Multipart.FileUpload(name, headers, filename, path)
        }
    }

    private fun parseHeaders(): HttpHeaders {
        val headers = MutableHttpHeaders()
        val lineEnd = "\r\n".toByteArray()
        while (true) {
            val index = findByteSequence(lineEnd)
            if (index == -1) {
                refillBuffer()
                continue
            }

            // 改行が読み取れているため、+2してもバッファから取得できるはず
            val lb = ByteArray(index - buffer.position() + 2)
            buffer.get(lb)
            val line = String(lb, Charsets.US_ASCII).removeSuffix("\r\n")
            if (line.isEmpty()) break

            val field = line.substringBefore(":")
            val value = line.substringAfter(":").trimStart()
            headers.add(field, value)
        }

        return headers.toImmutable()
    }

    private fun outputPartBody(output: OutputStream) {
        val writeBuffer = ByteArray(1024 * 10) // TODO Parameterized
        var writeBufferUsed = 0

        while (true) {
            val boundaryIndex = findByteSequence(boundaryEnd)
            val bytesToRead =
                if (boundaryIndex == -1) {
                    // boundaryがバッファ内にまだ無い
                    // 読み込み済みデータ - boundaryサイズ分だけ後でreadする
                    val readLen = maxOf(buffer.remaining() - boundaryEnd.size, 0)
                    // 補充
                    refillBuffer()
                    readLen
                } else {
                    boundaryIndex - buffer.position()
                }

            if (writeBuffer.size < writeBufferUsed + bytesToRead) {
                output.write(writeBuffer, 0, writeBufferUsed)
                writeBufferUsed = 0
            }

            buffer.get(writeBuffer, writeBufferUsed, bytesToRead)
            writeBufferUsed += bytesToRead

            if (boundaryIndex != -1) break
        }

        output.write(writeBuffer, 0, writeBufferUsed)
    }

    private fun consumeBoundary() {
        val boundary =
            if (firstPart) {
                firstPart = false
                "--$boundary".toByteArray()
            } else {
                "\r\n--$boundary".toByteArray()
            }

        // boundary開始まで読み進める
        while (true) {
            val index = findByteSequence(boundary)
            if (index == -1) {
                buffer.position(maxOf(buffer.position() - boundary.size, 0))
                refillBuffer()
                continue
            }

            // positionをboundary終端に設定
            buffer.position(index + boundary.size)
            break
        }

        // boundaryの次の2文字を確認
        while (true) {
            if (buffer.remaining() < 2) {
                refillBuffer()
                continue
            }

            val bnc = ByteArray(2)
            buffer.get(bnc)

            // body 終了している --${boundary}--
            // 残りのボディの消費は呼び出し元で行う
            if (bnc[0] == '-'.code.toByte() && bnc[1] == '-'.code.toByte()) {
                exhausted = true
                return
            }

            // ここで改行以外が来るのはおかしい
            if (bnc[0] != '\r'.code.toByte() || bnc[1] != '\n'.code.toByte()) {
                throw MalformedMultipartException("Invalid multipart boundary")
            }

            // パートのヘッダ先頭にpositionが来ているはず
            break
        }
    }

    private fun refillBuffer() {
        buffer.compact()
        val n = channel.read(buffer)
        if (n == -1) throw MalformedMultipartException("Unexpected end of stream")
        buffer.flip()
    }

    private fun findByteSequence(sequence: ByteArray): Int {
        val end = buffer.limit() - sequence.size
        for (i in buffer.position()..end) {
            var match = true
            for (j in sequence.indices) {
                if (buffer.get(i + j) != sequence[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }

        return -1
    }
}
