package ru.yoldi.ktor.swagger

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.OptionalParameterRouteSelector
import io.ktor.routing.ParameterRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.filter
import kotlin.collections.getOrPut
import kotlin.collections.hashMapOf
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapOf
import kotlin.collections.set
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation


@Target(AnnotationTarget.PROPERTY)
annotation class QueryParam


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class PathParam

annotation class Description(val description: String)

data class Pet(val id: Int, val name: String)


class SwaggerInstance {
    private val swagger = Swagger()

    fun info(info: Information) {
        swagger.info = info
    }

    private fun registerGenericMethod(path: String, method: String, params: List<ParameterBase>, schema: Schema) {
        swagger.paths.getOrPut(path) { hashMapOf() }[method] = object : OperationBase {
            override val responses: Map<HttpStatus, ResponseBase> = mapOf(
                "200" to object : ResponseBase {
                    override val schema = schema
                    override val description: String = ""
                }
            )
            override val parameters: List<ParameterBase> = params
            override val tags: List<Tag>? = listOf()
            override val summary: String = "Summary"
            override val description: String? = null

        }
    }


    fun registerGET(path: String, params: List<ParameterBase>, schema: Schema) =
        registerGenericMethod(path, "get", params, schema)

    fun registerPOST(path: String, params: List<ParameterBase>, schema: Schema) =
        registerGenericMethod(path, "post", params, schema)

    @KtorExperimentalLocationsAPI
    companion object : ApplicationFeature<Application, SwaggerConfiguration, SwaggerInstance> {
        override val key: AttributeKey<SwaggerInstance> = AttributeKey("SwaggerGeneration")


        override fun install(pipeline: Application, configure: SwaggerConfiguration.() -> Unit): SwaggerInstance {
            val configuration = SwaggerConfiguration().apply(configure)
            val instance = SwaggerInstance()

            configuration.info?.also {
                instance.info(it)
            }

            pipeline.attributes.put(key, instance)

            pipeline.intercept(ApplicationCallPipeline.Call) {

                if (call.request.path() == configuration.path) {
                    val some = ObjectMapper().also { it.setSerializationInclusion(JsonInclude.Include.NON_NULL) }
                        .writeValueAsString(instance.swagger)
                    call.respond(some)
                }
            }

            return instance
        }


        inline fun <reified T : Any> retrieveParameters(): List<ParameterBase> {
            return T::class.declaredMemberProperties.filter { it.findAnnotation<QueryParam>() != null || it.findAnnotation<PathParam>() != null }
                .map {
                    object : ParameterBase {
                        override val name: String = it.name
                        override val `in`: ParameterInputType
                            get() {
                                return when {
                                    it.findAnnotation<QueryParam>() != null -> ParameterInputType.query
                                    it.findAnnotation<PathParam>() != null -> ParameterInputType.path
                                    else -> throw Exception("Well that can not be happening - filtering annotations gone wrong?")
                                }
                            }
                        override val description: String? = it.findAnnotation<Description>()?.description
                        override val required: Boolean = try {
                            !it.returnType.isMarkedNullable
                        } catch (e: Exception) {
                            true
                        }

                    }
                }
        }

        inline fun <reified T : Any> constructBody(): List<ParameterBase> {
            return listOf(
                object : BodyParameter {
                    override val name: String = "body"
                    override val `in`: ParameterInputType =
                        ParameterInputType.body
                    override val description: String? = T::class.findAnnotation<Description>()?.description
                    override val schema = produceObjectSchema<T>()
                    override val required: Boolean = true
                }
            )
        }

        private fun getPath(r: Route): String {
            when (r.selector) {
                is ParameterRouteSelector, is OptionalParameterRouteSelector -> return if (r.parent != null) getPath(
                    r.parent!!
                ) else "/"
            }
            return if (r.parent != null) getPath(r.parent!!) + "/" + r.selector.toString() else r.selector.toString()
        }

        fun location(r: Route): String = getPath(r)

        val Route.swaggerInstance
            get() = application.attributes[key]


        inline fun <reified T : Any, reified B : Any> Route.get(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> B): Route {
            val route = this@get.get<T> {
                call.respond(body(it))
            }


            swaggerInstance.registerGET(
                location(route),
                retrieveParameters<T>(),
                produceObjectSchema<B>()
            )
            return route
        }

        inline fun <reified T : Any, reified B : Any> Route.post(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> B): Route {
            val route = this@post.post<T> {
                call.respond(body(it))
            }
            swaggerInstance.registerPOST(
                location(route),
                constructBody<T>(),
                produceObjectSchema<B>()
            )
            return route
        }


        inline fun <reified T : Any, reified B : List<C>, reified C : Any> Route.lget(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> B): Route {
            val route = this@lget.get<T> {
                call.respond(body(it))
            }
            swaggerInstance.registerGET(
                location(route),
                retrieveParameters<T>(),
                produceListSchema<C>()
            )
            return route
        }


        inline fun<reified T: Any> produceObjectSchema(): Schema {
            val f = FieldDescriptor.obj(T::class)
            return Schema(
                f.type,
                f.properties
            )
        }

        inline fun<reified C: Any> produceListSchema(): Schema {
            val f = FieldDescriptor.list(C::class)
            return ListSchema(
                f.type,
                f.items
            )
        }
    }


    class SwaggerConfiguration {
        var info: Information? = null
        var path: String? = "/swagger.json"
    }
}