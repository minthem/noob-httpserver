package io.github.minthem.noob.http.config

import io.github.minthem.noob.http.codec.StreamCodec

data class BodyConfig(
    val codecs: List<StreamCodec> = listOf(),
)
