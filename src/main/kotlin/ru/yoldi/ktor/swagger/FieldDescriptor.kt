package ru.yoldi.ktor.swagger

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

object FieldDescriptor {
    private fun typeToString(klass: KClassifier) = when (klass) {
        Int::class, Long::class -> "integer"
        Float::class, Double::class -> "number"
        String::class -> "string"
        Boolean::class -> "boolean"
        List::class -> "array"
        else -> "object"
    }


    fun primitive(T: KClass<*>): Field = Field(
        typeToString(T)
    )

    fun obj(T: KClass<*>): ObjectField {

        val properties= T.memberProperties.map {
            it.name to when (typeToString(it.returnType.classifier!!)) {
                "object" -> {
                    obj(it.returnType.classifier as KClass<*>)
                }
                "array" -> {
                    if (it.javaField != null)
                        list(
                            getFieldGenericType(it.javaField!!).kotlin
                        )
                    else list(
                        getMethodReturnGenericType(it.javaGetter!!).kotlin
                    )
                }
                else -> {
                    primitive(it.returnType.classifier as KClass<*>)
                }
            }
        }.toMap()

        return ObjectField(
            "object",
            "",
            properties
        )
    }

    fun list(genericClass: KClass<*>): ArrayField = ArrayField(
        "array",
        "",
        when (typeToString(genericClass)) {
            "object" -> {
                obj(genericClass)
            }
            "array" -> {
                // list of lists. what to do?
                // well i guess nothing
                ArrayField(
                    "array",
                    "",
                    obj(Any::class)
                )
            }
            else -> {
                primitive(genericClass)
            }
        }
    )

    private fun getFieldGenericType(field: java.lang.reflect.Field): Class<*> {
        val type = field.genericType
        return if (type is ParameterizedType) {
            val some = type.actualTypeArguments[0]
            some as Class<*>
        } else {
            field.type
        }
    }

    private fun getMethodReturnGenericType(method: Method): Class<*> {
        val returnClass = method.returnType


        if (Collection::class.java.isAssignableFrom(returnClass)) {
            val returnType = method.genericReturnType

            return if (returnType is ParameterizedType) {
                val argTypes = returnType.actualTypeArguments
                if (argTypes.isNotEmpty()) {
                    println("Generic type is " + argTypes[0])
                    argTypes[0] as Class<*>
                } else {
                    Any::class.java
                }
            } else {
                Any::class.java
            }
        } else {
            return Any::class.java
        }
    }
}