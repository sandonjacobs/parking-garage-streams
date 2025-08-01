package io.sandonjacobs.parking.kstreams.serde

import com.google.protobuf.Message
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
import kotlin.reflect.KClass

object SerdeProvider {

    fun <T : Message> createProtobufSerde(clazz: KClass<T>, props: Map<String, String>, isKey: Boolean = false): KafkaProtobufSerde<T> {
        val serde = KafkaProtobufSerde<T>()
        serde.configure(
            props + mapOf("specific.protobuf.value.type" to clazz.java.name),
            isKey
        )
        return serde
    }
}