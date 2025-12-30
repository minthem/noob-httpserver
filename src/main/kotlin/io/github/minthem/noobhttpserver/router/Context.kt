package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpRequest

class Context(
    val req: HttpRequest
) {

    // TODO add queryParams, pathParams (decode済み)

    // TODO ここにdecodeしたpathとかを出力するメソッド追加してもいいかも
}
