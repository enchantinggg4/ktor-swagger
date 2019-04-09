package ru.yoldi.ktor.swagger

interface OperationBase {
    val responses: Map<HttpStatus, ResponseBase>
    val parameters: List<ParameterBase>
    val tags: List<Tag>?
    val summary: String
    val description: String?
}

interface ResponseBase {
    val description: String
    val schema: Schema
//    val schema: Field
}

data class Tag(
    val name: String
)

interface ParameterBase {
    val name: String
    val `in`: ParameterInputType
    val description: String?
    val required: Boolean
}

interface BodyParameter : ParameterBase {
    val schema: Schema
//    val schema: Field
}

enum class ParameterInputType {
    query,
    path,
    /**
     * Not supported in OpenAPI v3.
     */
    body,
    header
}

typealias Definitions = HashMap<ModelName, Any>


open class Field(
    val type: String,
    val description: String? = null
)

data class ArrayFieldDescriptor(val type: String)

class ObjectField(type: String, description: String, val properties: Map<String, Field>) : Field(type, description)
class ArrayField(type: String, description: String, val items: Field) : Field(type, description)

open class Schema(
    val type: String,
    val properties: Map<String, Field>
)

class ListSchema(
    type: String,
    child: Field
) : Schema(type, mapOf(
    "items" to child
))

class Information(
    val description: String? = null,
    val version: String? = null,
    val title: String? = null,
    val contact: Contact? = null
)


class Contact(
    val name: String? = null,
    val url: String? = null,
    val email: String? = null
)


typealias ModelName = String
typealias PropertyName = String
typealias Path = String
typealias Paths = HashMap<Path, Methods>
typealias MethodName = String
typealias HttpStatus = String
typealias Methods = HashMap<MethodName, OperationBase>

interface CommonBase {
    val info: Information?
    val paths: Paths
}

class Swagger : CommonBase {
    val swagger = "2.0"
    override var info: Information? = null
    override val paths: Paths = hashMapOf()
    val definitions: Definitions = hashMapOf()
}