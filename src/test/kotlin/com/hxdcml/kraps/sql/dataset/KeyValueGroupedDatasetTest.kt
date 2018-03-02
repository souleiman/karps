package com.hxdcml.kraps.sql.dataset

import com.holdenkarau.spark.testing.JavaDatasetSuiteBase
import com.hxdcml.kraps.objects.Field
import org.amshove.kluent.shouldContainAll
import org.apache.spark.sql.Encoders
import org.junit.Test

import com.hxdcml.kraps.sql.dataset.KeyValueGroupedDataset.flatMapGroups

/**
 * Author: Soul
 * Date: 3/2/2018
 */
class KeyValueGroupedDatasetTest : JavaDatasetSuiteBase() {
    @Test
    fun flatMapGroups() {
        val spark = sqlContext().sparkSession()
        val text = listOf("A", "AA", "AAA", "BA", "BB", "CA", "AC", "CC", "CX")

        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.groupByKey { Field(0, it.first().toString()) }
                .flatMapGroups(Encoders.STRING()) { _, values ->
                    val combine = values.asSequence()
                            .flatMap { it.map { it.toString() }.asSequence() }
                            .distinct()
                            .joinToString("")

                    listOf(combine)
                }.collectAsList()

        result shouldContainAll setOf("CAX", "BA", "AC")
    }

    @Test
    fun flatMapGroupsObject() {
        val spark = sqlContext().sparkSession()
        val text = listOf("A", "AA", "AAA", "BA", "BB", "CA", "AC", "CC", "CX")

        val df = spark.createDataset(text, Encoders.STRING())
        val result = df.groupByKey { Field(0, it.first().toString()) }
                .flatMapGroups { _, values ->
                    val combine = values.asSequence()
                            .flatMap { it.map { it.toString() }.asSequence() }
                            .distinct()
                            .joinToString("")

                    listOf(Field(value = combine))
                }.collectAsList()

        result shouldContainAll setOf(Field(value = "CAX"), Field(value = "BA"), Field(value = "AC"))
    }
}