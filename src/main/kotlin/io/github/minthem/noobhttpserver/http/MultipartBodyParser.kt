package io.github.minthem.noobhttpserver.http

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream


internal class MultipartBodyParser(
    private val stream: InputStream,
    private val boundary: String
) {

    private var firstPart = true
    private val boundaryEnd = "\r\n--$boundary".toByteArray()
    private val channel = Channels.newChannel(stream)
    private val buffer = ByteBuffer.allocate(4096).flip()
    private var exhausted = false

    fun nextPart(): Multipart? {
        consumeBoundary()
        if (exhausted) {
            // ボディ終了したので、stream全部読み込んで消費する
            // streamはBodySourceによって、ボディ終端まで読んでくれる
            stream.transferTo(OutputStream.nullOutputStream())
            return null
        }

        val headers = parseHeaders()
        if (!headers.contains("Content-Disposition")) {
            throw IllegalArgumentException("Multipart body must contain a Content-Disposition header")
        }

        val disposition = headers.contentDisposition
            ?: throw IllegalArgumentException("Invalid Content-Disposition header")

        // TODO Pair戻しがブサイク, いずれ直す
        val (bodyStream, path) = outputPartBody()

        val filename = disposition.filename
        val name = disposition.name ?: throw IllegalArgumentException("Invalid Content-Disposition header")
        val charset = headers.contentType?.charset ?: Charsets.UTF_8

        return if (filename == null) {
            Multipart.FormField(name, headers, String(bodyStream.readBytes(), charset))
        } else {
            Multipart.FileUpload(name, headers, filename, path, { bodyStream })
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

    private fun outputPartBody(): Pair<InputStream, Path?> {
        var dst: OutputStream = ByteArrayOutputStream(1 * 1024 * 1024)
        var findBoundary = false
        var bodySize = 0L
        var outputFile: Path? = null

        try {
            while (!findBoundary) {
                val index = findByteSequence(boundaryEnd)
                val len = if (index == -1) {
                    // boundaryがバッファ内にまだ無い
                    // 補充
                    refillBuffer()
                    maxOf(buffer.remaining() - boundaryEnd.size, 0)
                } else {
                    findBoundary = true
                    index - buffer.position()
                }
                bodySize += len
                val b = ByteArray(len)
                buffer.get(b, 0, len)

                if (outputFile == null && bodySize > 1 * 1024 * 1024) {
                    outputFile = Files.createTempFile("noobhttpserver", ".part")
                    dst = outputFile.outputStream(
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                    )
                }
                dst.write(b)
            }
        } finally {
            dst.close()
        }

        val stream = outputFile?.let {
            Files.newInputStream(it)
        } ?: ByteArrayInputStream(
            (dst as ByteArrayOutputStream).toByteArray()
        )

        return stream to outputFile
    }

    private fun consumeBoundary() {
        val boundary = if (firstPart) {
            firstPart = false
            "--$boundary".toByteArray()
        } else {
            "\r\n--$boundary".toByteArray()
        }

        // boundary開始まで読み進める
        while (true) {
            val index = findByteSequence(boundary)
            if (index == -1) {
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
                throw IllegalArgumentException("Invalid multipart boundary")
            }

            // パートのヘッダ先頭にpositionが来ているはず
            break
        }
    }

    private fun refillBuffer() {
        buffer.compact()
        val n = channel.read(buffer)
        if (n == -1) throw IllegalStateException("Unexpected end of stream")
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
