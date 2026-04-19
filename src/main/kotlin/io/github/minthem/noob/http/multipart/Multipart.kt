package io.github.minthem.noob.http.multipart

import io.github.minthem.noob.http.message.HttpHeaders
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Represents a multipart body part, which can be either a form field or a file upload. This sealed class
 * serves as the base for different types of multipart components, encapsulating shared functionality and attributes.
 *
 * @param name The name associated with the multipart component.
 * @param headers The HTTP headers associated with the multipart component.
 */
sealed class Multipart (
    val name: String,
    val headers: HttpHeaders,
) {

    /**
     * Represents a form field in a multipart request.
     *
     * This class is a specific type of multipart component that encapsulates a key-value pair where the `value`
     * represents the content associated with the form field. It inherits the shared attributes and functionality
     * of the `Multipart` sealed class.
     *
     * @constructor Initializes a new instance of the `FormField` class.
     * @param name The name of the form field.
     * @param headers The HTTP headers associated with the form field.
     * @param value The content value of the form field.
     */
    class FormField(
        name: String,
        headers: HttpHeaders,
        val value: String
    ) : Multipart(name, headers)

    /**
     * Represents an entity responsible for handling uploaded file data in a multipart request.
     * Extends the [Multipart] class to provide specific behaviors for files.
     *
     * @constructor Creates a new instance of [FileUpload].
     * @param name The name associated with the file field in the multipart request.
     * @param headers The HTTP headers associated with the uploaded file.
     * @param filename The name of the uploaded file as specified by the client.
     * @param savePath The filesystem path where the uploaded file has been temporarily stored.
     */
    class FileUpload(
        name: String,
        headers: HttpHeaders,
        val filename: String,
        internal val savePath: Path,
    ) : Multipart(name, headers) {

        /**
         * Provides an input stream to read the contents of the underlying file associated with this instance.
         *
         * @return An [InputStream] to read the file's content. The caller is responsible for closing the stream.
         */
        fun asStream(): InputStream = savePath.inputStream(StandardOpenOption.READ)

        /**
         * Copies the content of the current file to the specified target path.
         *
         * @param path The target file path to copy the content to. If the file does not exist, it will be created.
         * If the file exists, its content will be overwritten.
         */
        fun copyTo(path: Path) {
            path.outputStream(
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use {
                asStream().copyTo(it)
            }
        }
    }
}
