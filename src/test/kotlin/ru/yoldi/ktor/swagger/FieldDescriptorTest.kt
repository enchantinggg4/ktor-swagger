package ru.yoldi.ktor.swagger

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


data class DataClassWithPrimitive(val id: Int)
class ClassWithPrimitive(val id: Int)

data class DataClassWithObject(val inner: DataClassWithPrimitive)
data class ClassWithObject(val inner: DataClassWithPrimitive)


data class DataClassWithList(val list: List<String>)

internal class FieldDescriptorTest {

    private fun dataClassWithPrimitiveCase(){
        val field = FieldDescriptor.obj(DataClassWithPrimitive::class)

        assertNotNull(
            field.properties["id"]
        )
        assertEquals(
            1,
            field.properties.size
        )

        assertEquals(
            "integer",
            field.properties.getValue("id").type
        )
    }

    private fun classWithPrimitiveCase(){
        val field = FieldDescriptor.obj(ClassWithPrimitive::class)

        assertNotNull(
            field.properties["id"]
        )
        assertEquals(
            1,
            field.properties.size
        )

        assertEquals(
            "integer",
            field.properties.getValue("id").type
        )
    }

    private fun dataClassWithObjectCase(){
        val field = FieldDescriptor.obj(DataClassWithObject::class)

        assertNotNull(
            field.properties["inner"]
        )
        assertEquals(
            1,
            field.properties.size
        )

        assertEquals(
            "object",
            field.properties.getValue("inner").type
        )


        assertEquals(
            "integer",
            (field.properties.getValue("inner") as ObjectField).properties.getValue("id").type
        )
    }

    private fun classWithObjectCase(){
        val field = FieldDescriptor.obj(ClassWithObject::class)

        assertNotNull(
            field.properties["inner"]
        )
        assertEquals(
            1,
            field.properties.size
        )

        assertEquals(
            "object",
            field.properties.getValue("inner").type
        )


        assertEquals(
            "integer",
            (field.properties.getValue("inner") as ObjectField).properties.getValue("id").type
        )
    }

    private fun dataClassWithListCase(){
        val field = FieldDescriptor.obj(DataClassWithList::class)

        assertNotNull(
            field.properties["list"]
        )
        assertEquals(
            1,
            field.properties.size
        )

        assertEquals(
            "array",
            field.properties.getValue("list").type
        )


        assertEquals(
            "string",
            (field.properties.getValue("list") as ArrayField).items.type
        )
    }

    @Test
    fun primitive() {
        assertEquals(
            "integer",
            FieldDescriptor.primitive(Int::class).type
        )

        assertEquals(
            "integer",
            FieldDescriptor.primitive(Long::class).type
        )

        assertEquals(
            "number",
            FieldDescriptor.primitive(Float::class).type
        )

        assertEquals(
            "number",
            FieldDescriptor.primitive(Double::class).type
        )

        assertEquals(
            "string",
            FieldDescriptor.primitive(String::class).type
        )
    }

    @Test
    fun obj() {
        dataClassWithPrimitiveCase()
        classWithPrimitiveCase()

        //

        dataClassWithObjectCase()
        classWithObjectCase()

        //
        dataClassWithListCase()

    }

    @Test
    fun list() {
        // primitive
        assertEquals(
           "string",
            FieldDescriptor.list(String::class).items.type
        )

        // array
        assertEquals(
            "array",
            FieldDescriptor.list(List::class).items.type
        )

        // object

        assertEquals(
            "object",
            FieldDescriptor.list(DataClassWithPrimitive::class).items.type
        )
    }

}