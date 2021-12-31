package com.exzell.uppics

import org.junit.Test

import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun checkDate(){
        val format = SimpleDateFormat("dd-MM-yyyy,HH:mm:ss", Locale.JAPAN).format(Date())

        println(format)
    }
}