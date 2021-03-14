import java.io.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.io.FileWriter

import java.io.BufferedWriter
import java.time.Instant


private const val IPV4_PATTERN = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$"

private val pattern: Pattern = Pattern.compile(IPV4_PATTERN)

var originalIPSize = 0
var fileIndex = 0
var nameBaseFile: String? = null

object Prop {
    private var prop: Properties? = null

    @JvmName("getProp")
    fun getProp(): Properties {
        if (prop == null) {
            prop = createProp()
        }
        return prop as Properties
    }

    private fun createProp(): Properties {
        val fis: FileInputStream
        val property = Properties()

        try {
            fis = FileInputStream("src/main/resources/prop.properties")
            property.load(fis)
        } catch (e: IOException) {
            System.err.println("Property file is not exist")
            throw e
        }

        return property
    }
}


fun main(@Suppress("UNUSED_PARAMETER") args: Array<String>) {
    try {
        println("${Instant.now()} - Init")
        if (checkCorrectPath(Prop.getProp().getProperty("testDirectory"))) {
            println("temp directory is not correct")
            return
        }
        //cleanTempDirectory()

//        if (checkCorrectPath(Prop.getProp().getProperty("fileName"))) {
//            println("file name is not correct")
//            return
//        }

        println("${Instant.now()} - Start")
        val result = parseFile()

        println("Original IP size is $originalIPSize")
        println("Unique IP address is $result")
        println("Delta IP is ${originalIPSize - result}")

        println("Finish")
    } catch (e: Exception) {
        System.err.println("Program is crashed")
        e.printStackTrace()
    }
}

fun cleanTempDirectory() {
    val dir = File(Prop.getProp().getProperty("tempDirectory"))
    if (dir.isDirectory) {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") val children: Array<String> = dir.list()
        for (i in children.indices) {
            File(dir, children[i]).delete()
        }
    }
}

fun isValid(email: String?): Boolean {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") val matcher: Matcher = pattern.matcher(email)
    return matcher.matches()
}

private fun parseFile(): Long {
    //firstLine()
    println("${Instant.now()} - firstLine finish")
    return secondLine()
}

fun secondLine(): Long {
    val map = createFileMap()
    println("${Instant.now()} - file map created")
    val setIP = sortedSetOf<Long>()

    val maxSize = Prop.getProp().getProperty("maxSizeSet").toInt()
    val limit = maxSize / map.size

    var result: Long = 0
    var value: Long
    var line: String

    firstLoad(map, limit, setIP)
    println("${Instant.now()} - first load end")

    while (map.isNotEmpty()) {
        println("${Instant.now()} - second line main loop. Result is $result")
        var deletedBuilder: BufferedReader? = null
        result += clearTopSet(setIP, limit)
        val readiedBufferElemMap = getReadiedBufferMapElem(map) ?: throw Exception("readiedBuffer is null")

        for (i in 0..(maxSize - setIP.size)) {
            setIP.add(readiedBufferElemMap.value)
            if (readiedBufferElemMap.key.ready()) {
                line = readiedBufferElemMap.key.readLine()
                if (line.isNotEmpty()) {
                    value = java.lang.Long.parseLong(line/*, 16*/)
                    //setIP.add(value)
                    readiedBufferElemMap.setValue(value)
                } else {
                    println("${Instant.now()} - file is end")
                }

            } else {
                deletedBuilder = readiedBufferElemMap.key
                break
            }
        }
        if (deletedBuilder != null) {
            deletedBuilder.close()
            map.remove(deletedBuilder)
        }

    }
    result += setIP.size
    println("Size uniqie elem is $result")
    return result
}

fun getReadiedBufferMapElem(map: MutableMap<BufferedReader, Long>): MutableMap.MutableEntry<BufferedReader, Long>? {
    var minValue: Long? = null
    for (elem in map.entries) {
        if (minValue == null) {
            minValue = elem.value
        }
        if (minValue > elem.value) {
            minValue = elem.value
        }
    }
    for (elem in map.entries) {
        if (elem.value == minValue) {
            return elem
        }
    }
    return null
}

fun clearTopSet(ip: TreeSet<Long>, limit: Int): Int {
    var result = 0
    var value: Long?
    for (i in 0..limit) {
        value = ip.pollFirst()
        if (value != null) {
            result++
        } else {
            break
        }
    }

    return result
}

private fun firstLoad(
    map: MutableMap<BufferedReader, Long>,
    limit: Int,
    setIP: TreeSet<Long>
) {
    var value: Long
    for (elem in map.entries) {
        for (i in 0..limit) {
            setIP.add(elem.value)
            if (elem.key.ready()) {
                value = java.lang.Long.parseLong(elem.key.readLine()/*, 16*/)
                elem.setValue(value)
                //setIP.add(value)
                //elem.setValue(value)
            } else {
                break
            }
        }
    }
}

private fun createFileMap(): MutableMap<BufferedReader, Long> {
    val map = mutableMapOf<BufferedReader, Long>()

    val dir = File(Prop.getProp().getProperty("testDirectory"))
    if (dir.isDirectory) {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") val children: Array<String> = dir.list()
        for (i in children.indices) {
            val bis = BufferedInputStream(FileInputStream(File(dir, children[i])))
            val reader = BufferedReader(
                InputStreamReader(bis),
                Prop.getProp().getProperty("cashSize").toInt() / children.size
            )

            if (reader.ready()) {
                map[reader] = java.lang.Long.parseLong(reader.readLine()/*, 16*/)
            } else {
                map[reader] = 0
            }
        }
    }
    return map
}

fun firstLine() {
    val maxSize = Prop.getProp().getProperty("maxSizeSet").toInt()
    val setIP = sortedSetOf<Long>()

    var line: String
    var i = 0

    try {
        val bis = BufferedInputStream(FileInputStream(Prop.getProp().getProperty("fileName")))
        val mainReader = BufferedReader(InputStreamReader(bis), Prop.getProp().getProperty("cashSize").toInt())

        while (mainReader.ready()) {
            line = mainReader.readLine()
            i++
            originalIPSize++
            if (!isValid(line)) {
                println("|$line| is not valid IP")
                continue
            }
            setIP.add(line.replace(".", "0").toLong())
            if (setIP.size > maxSize) {
                saveCurrentDataIntoFile(setIP)
                println("file created")
                setIP.clear()
            }
            if (i > 5000000) {
                println("${Instant.now()} - 5000000 past")
                i = 0
            }
        }
        saveCurrentDataIntoFile(setIP)
        println("last file created")
        mainReader.close()
    } catch (ex: IOException) {
        ex.printStackTrace()
    }

}

private fun saveCurrentDataIntoFile(setIP: MutableSet<Long>) {
    val filePref = Prop.getProp().getProperty("firstLineFilePref")
    val tempDirectory = Prop.getProp().getProperty("tempDirectory")
    val nameCurrentFile = getCurrentFile("$tempDirectory\\$filePref")

    val out = BufferedWriter(FileWriter(nameCurrentFile))
    val it: Iterator<*> = setIP.iterator()

    while (it.hasNext()) {
        out.write(java.lang.Long.toHexString(it.next() as Long))
        out.newLine()
    }
    out.close()
}

private fun getCurrentFile(filePref: String): String {
    nameBaseFile = filePref + getNextFileIndex()
    return nameBaseFile as String
}

private fun getNextFileIndex(): String {
    val result = fileIndex.toString()
    fileIndex++
    return result
}


private fun checkCorrectPath(path: String?): Boolean {
    if (path.isNullOrEmpty()) {
        print("path is null or empty")
        return true
    }

    val file = File(path)
    val fileExists = file.exists()

    if (!fileExists) {
        print("$path does not exist.")
        return true
    }
    return false
}