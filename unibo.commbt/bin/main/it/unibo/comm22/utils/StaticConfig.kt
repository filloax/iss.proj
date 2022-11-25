package it.unibo.comm22.utils

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import unibo.comm22.utils.ColorsOut
import java.io.*
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

object StaticConfig {
    private val emptyHook: (KMutableProperty<*>, Any) -> Any? = { _, _ -> null }

    fun <T : Any> setConfiguration(clazz: KClass<out T>, obj: T, resourceName: String, noWrite: Boolean = false) {
        setConfiguration(clazz, obj, resourceName, emptyHook, emptyHook, noWrite)
    }

    fun <T : Any> setConfiguration(
        clazz: KClass<out T>, obj: T, resourceName: String,
        beforeSaveHook: (KMutableProperty<*>, Any) -> Any?,
        afterLoadHook: (KMutableProperty<*>, Any) -> Any?,
        noWrite: Boolean = false
    ) {
        //Nella distribuzione resourceName è in una dir che include la bin
        try {
            ColorsOut.out("Set configuration for class ${clazz.java.name} from file: '$resourceName'\n" +
                    "\t\tSet config on instance ${obj.hashCode()}, thread ${Thread.currentThread()}"
            )
            val reader = FileReader(resourceName)
            var writer: FileWriter? = null
            try {
                val tokener = JSONTokener(reader)
                val jsonObj = JSONObject(tokener)
                val changed = setFields(clazz, obj, jsonObj, beforeSaveHook, afterLoadHook)
                if (changed && !noWrite) {
                    saveConfigFile(jsonObj, resourceName)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            } finally {
                if (writer != null) {
                    try {
                        writer.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            if (noWrite) {
                ColorsOut.outappl(
                    "Config file not found, but noWrite enabled",
                    ColorsOut.YELLOW
                )
            } else {
                ColorsOut.outappl(
                    "Config file not found, saving default config file to $resourceName",
                    ColorsOut.YELLOW
                )
                saveConfigFile(createJSONObject(clazz, obj, beforeSaveHook)!!, resourceName)
            }
        }
    }

    // Per testing, se usato con FileWriter sovrascriverà il file di configurazione
    // prima della lettura
    fun <T: Any> setConfiguration(clazz: KClass<out T>, obj: T, reader: Reader?, writer: Writer, noWrite: Boolean = false) {
        //Nella distribuzione resourceName è in una dir che include la bin
        try {
            val tokener = JSONTokener(reader)
            val jsonObj = JSONObject(tokener)
            val changed = setFields(clazz, obj, jsonObj, emptyHook, emptyHook)
            if (changed && !noWrite) {
                ColorsOut.outappl("\tTesting: saving config file for class $clazz", ColorsOut.BLUE)
                saveConfigFile(jsonObj, writer)
            }
        } catch (e: JSONException) {
            ColorsOut.outerr("setConfiguration ERROR " + e.message)
        }
    }

    fun saveConfigFile(obj: JSONObject, resourceName: String) {
        ColorsOut.outappl("\tSaving changed configuration file to $resourceName", ColorsOut.BLUE)
        saveConfigFile(obj, FileWriter(resourceName))
    }

    fun saveConfigFile(obj: JSONObject, writer: Writer) {
        try {
            writer.write(obj.toString(4))
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        } finally {
            try {
                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun <T: Any> createJSONObject(clazz: KClass<out T>, obj: T, beforeSaveHook: (KMutableProperty<*>, Any) -> Any?): JSONObject? {
        return try {
            val jsonObj = JSONObject()
            for (field in getPublicStaticFields(clazz)) {
                val value = field.getter.call(obj) ?: throw Exception("StaticConfig: field ${field.name} has no value when saving")
                jsonObj.put(field.name, beforeSaveHook(field, value)?: value)
            }
            jsonObj
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            System.err.println("Illegal access exception when saving JSON object, shouldn't happen")
            null
        }
    }

    /**
     * @return true if the object was changed due to missing fields
     */
    private fun <T: Any> setFields(
        clazz: KClass<out T>, obj: T, loadedObject: JSONObject,
        beforeSaveHook: (KMutableProperty<*>, Any) -> Any?,
        afterLoadHook: (KMutableProperty<*>, Any) -> Any?
    ): Boolean {
        var changed = false
        for (field in getPublicStaticFields(clazz)) {
            val name = field.name
            var value: Any? = loadedObject.opt(name)
            if (value != null) {
                val hookReturn = afterLoadHook(field, value)
                hookReturn?.let { value = hookReturn }
                try {
//                    println("Debug: ${name}, ${clazz.java}, ${field.returnType}, ${value!!.javaClass}, $value")
                    // Sub map
                    if (value is JSONObject) {
                        if (! Map::class.java.isAssignableFrom(field.returnType.jvmErasure.java))
                            throw Exception("Config item $name has wrong type ${value!!::class}")

                        val loadedMap = (value as JSONObject).toMap()
                        var changedThis = false
                        @Suppress("UNCHECKED_CAST")
                        for (entry in (field.getter.call(obj) as Map<String,*>)) {
                            if (!loadedMap.containsKey(entry.key)) {
                                loadedMap[entry.key] = entry.value
                                changed = true
                                changedThis = true
                            }
                        }

                        if (changedThis)
                            loadedObject.put(name, loadedMap)

                        field.setter.call(obj, loadedMap)
                    } else {
                        if (! value!!::class.isSubclassOf(field.returnType.jvmErasure))
                            throw Exception("Config item $name has wrong type ${value!!::class}")

                        field.setter.call(obj, value)
                    }
                } catch (e: IllegalAccessException) {
                    e.printStackTrace() // shouldn't happen, but jic
                }
            } else {
                try {
                    val defaultValue = field.getter.call(obj) ?: throw Exception("StaticConfig: field $name needs default value")
                    loadedObject.put(name, beforeSaveHook(field, defaultValue) ?: defaultValue)
                    changed = true
                    ColorsOut.outappl("Field $name not present in config, using default", ColorsOut.ANSI_YELLOW)
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
        return changed
    }

    private fun getPublicStaticFields(clazz: KClass<*>): List<KMutableProperty<*>> {
        return clazz.members.filter{ it.visibility == KVisibility.PUBLIC }
            .filterIsInstance<KMutableProperty<*>>()
            .map { it }
    }
}