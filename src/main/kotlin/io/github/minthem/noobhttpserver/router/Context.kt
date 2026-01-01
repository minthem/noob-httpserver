package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpRequest

class Context internal constructor(
    private val req: HttpRequest,
    val pathParams: Map<String, String>
) {

    // TODO add queryParams, pathParams (decode済み)

    // TODO ここにdecodeしたpathとかを出力するメソッド追加してもいいかも
}
