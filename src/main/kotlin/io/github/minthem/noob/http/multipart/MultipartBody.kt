package io.github.minthem.noob.http.multipart

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Files

/**
 * Represents a multipart HTTP request body, allowing parsing and access to individual parts.
 *
 * This class facilitates streaming multipart data and provides an interface to access specific
 * parts or iterate through all parts of the request body. It maintains an internal cache of
 * already-read parts to avoid redundant parsing.
 *
 * @constructor
 * Creates an instance of `MultipartBody`. This is intended for internal use and is constructed
 * with an input stream and a boundary string.
 *
 * @param stream The input stream from which the multipart data will be read.
 * @param boundary The boundary string used to separate parts within the multipart content.
 */
class MultipartBody internal constructor(
    stream: InputStream,
    boundary: String,
) : Closeable {
    private val parser = MultipartBodyParser(stream, boundary)
    private val readParts = LinkedHashMap<String, Multipart>()

    /**
     * Retrieves a specific part from the multipart content by its name.
     *
     * If the part with the specified name has already been parsed, it is returned from the internal cache.
     * Otherwise, the method continues parsing the multipart content until a part with the matching name
     * is found or until all parts are exhausted.
     *
     * @param name The name of the part to retrieve.
     * @return The multipart component matching the given name, or null if no such part exists.
     */
    fun part(name: String): Multipart? {
        if (readParts.containsKey(name)) {
            return readParts[name]
        }

        while (true) {
            val part = parser.nextPart() ?: return null

            readParts[part.name] = part

            if (part.name == name) {
                return part
            }
        }
    }

    /**
     * Iterates over each part of the multipart content and applies the given block of logic.
     *
     * This method iterates through already-read parts followed by parsing and processing
     * any additional parts that have not yet been read, in the order they are encountered in the stream.
     *
     * @param block A lambda function to be applied to each [Multipart] part.
     */
    fun forEachPart(block: (Multipart) -> Unit) {
        readParts.values.forEach(block)

        while (true) {
            val part = parser.nextPart() ?: return
            readParts[part.name] = part
            block(part)
        }
    }

    /**
     * Closes the resources associated with this instance and performs cleanup operations.
     *
     * Specifically, this method iterates through the multipart components stored in `readParts`.
     * If a component is of type [Multipart.FileUpload], it ensures that the temporary file
     * associated with the `savePath` of the file upload is deleted from the file system.
     *
     * After handling all file uploads, the `readParts` collection is cleared to release references.
     */
    override fun close() {
        readParts.values.forEach { multipart ->
            if (multipart is Multipart.FileUpload) {
                multipart.savePath.let { Files.deleteIfExists(it) }
            }
        }

        readParts.clear()
    }
}
