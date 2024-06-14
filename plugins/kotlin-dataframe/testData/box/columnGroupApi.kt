import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*


@DataSchema
data class Record(val a: String, val b: Int)

fun box(): String {
    val df = List(10) { Record(it.toString(), it) }.let { dataFrameOf(*it.toTypedArray()) }
    val group = df.group { a and b }.into("c")
    val df1 = group.c.add("d") { 1 }
    df1.a
    df1.b
    df1.d
    return "OK"
}
