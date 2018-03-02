package com.hxdcml.kraps.sql.dataset

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase
import com.hxdcml.kraps.compatibility.tuple.KPair
import com.hxdcml.kraps.compatibility.tuple.KPair.Hook.component1
import com.hxdcml.kraps.compatibility.tuple.KPair.Hook.component2
import com.hxdcml.kraps.objects.*
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldEqual
import org.apache.spark.sql.Encoders
import org.junit.Test

/**
 * Author: Soul
 * Date: 3/1/2018
 */
class DatasetTest : JavaDatasetSuiteBase() {
    @Test
    fun mapDifferentType() {
        val spark = sqlContext().sparkSession()

        val text = listOf("This", "Is", "A", "String")
        val expected = text.map { it.length }

        val df = spark.createDataset(text, Encoders.STRING())

        val result = df.map(Encoders.INT()) { it.length }
                .collectAsList()

        result shouldEqual expected
    }

    @Test
    fun mapSameType() {
        val spark = sqlContext().sparkSession()

        val text = listOf("This", "Is", "A", "String")
        val transform: (String) -> String = { "${it.length}" }
        val expected = text.map(transform)

        val df = spark.createDataset(text, Encoders.STRING())

        val result = df.map(Encoders.STRING(), transform)
                .collectAsList()

        result shouldEqual expected
    }

    @Test
    fun map() {
        val spark = sqlContext().sparkSession()
        val text = listOf("This", "Is", "A", "String")
        val transform: (String) -> Field = { Field(it.length, it) }
        val expected = text.map(transform)

        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.map(body = transform)
                .collectAsList()

        result shouldEqual expected
    }

    @Test
    fun mapWithFields() {
        val spark = sqlContext().sparkSession()
        val text = listOf("This", "Is", "A", "String")
        val transform: (String) -> Fields = { Fields(it.map { it.toString() }) }

        val expected = text.map(transform)
        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.map(body = transform)
                .collectAsList()

        result shouldEqual expected
    }

    @Test
    fun mapWithNestedField() {
        val spark = sqlContext().sparkSession()
        val text = listOf("This", "Is", "A", "String")

        val transform: (String) -> NestField = {
            NestField(Field(it.length, it), it.map { Field(it.toInt(), it.toString()) })
        }

        val expected = text.map(transform)
        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.map(body = transform)
                .collectAsList()

        result shouldEqual expected
    }

    @Test
    fun asPrimitive() {
        val spark = sqlContext().sparkSession()
        val text = listOf(4, 2, 1, 6)
        val expected = text.map { "$it" }

        val result = spark.createDataset(text, Encoders.INT())
                .smartAs(Encoders.STRING())
                .collectAsList()
        result shouldEqual expected
    }

    @Test
    fun asObject() {
        val spark = sqlContext().sparkSession()
        val text = listOf("This", "Is", "A", "String")

        val df = spark.createDataset(text, Encoders.STRING())
                .map { Child(it) }
        val result = df.smartAs<Child, Parent>()
                .select("child", "parent")
        val expected = spark.createDataset(text.map { KPair(it, "P") }, Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
                .toDF("child", "parent")

        assertDatasetEquals(expected, result)
    }

    @Test
    fun flatMap() {
        val spark = sqlContext().sparkSession()
        val text = listOf("This", "Is", "A", "String")
        val body: (String) -> List<Field> = { it.map { Field(it.toByte().toInt(), it.toString()) } }

        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.flatMap(body = body)
                .collectAsList()

        val expected = text.flatMap(body)

        result shouldEqual expected
    }

    @Test
    fun mapPartitions() {
        val spark = sqlContext().sparkSession()
        val text = listOf("This", "Is", "A", "String")

        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.mapPartitions(Encoders.STRING()) { v ->
            v.asSequence().map { it }.asIterable()
        }.collectAsList()

        result shouldEqual text
    }

    @Test
    fun groupByKey() {
        val spark = sqlContext().sparkSession()
        val text = listOf("A", "AA", "AAA", "BA", "BB", "CA", "AC", "CC")

        val expected = text.groupingBy { Field(0, it.first().toString()) }
                .eachCount()
                .entries
                .map { (field, count) -> KPair(field, count.toLong()) }

        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.groupByKey { Field(0, it.first().toString()) }
                .count()
                .collectAsList()
                .map { (field, count) -> KPair(field, count as Long)}

        result shouldContainAll expected
    }
}