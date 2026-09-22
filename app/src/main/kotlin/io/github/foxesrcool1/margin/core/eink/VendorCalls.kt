package io.github.foxesrcool1.margin.core.eink

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Calls methods by name on an object whose class this app cannot see at
 * compile time.
 *
 * Nothing here throws. A missing class, a missing method, a wrong argument
 * and an exception inside the vendor code all come back as a failed
 * [Result], with the reason kept for the log.
 */
internal class VendorObject(val target: Any?, val type: Class<*>) {

    fun call(name: String, vararg args: Any?): Result<Any?> = runCatching {
        val method = findMethod(name, args)
            ?: throw NoSuchMethodException("${type.name}.$name with ${args.size} arguments")
        method.isAccessible = true
        val receiver = if (Modifier.isStatic(method.modifiers)) null else target
        try {
            method.invoke(receiver, *args)
        } catch (wrapped: java.lang.reflect.InvocationTargetException) {
            // The real reason is inside. The wrapper says nothing useful.
            throw wrapped.targetException ?: wrapped
        }
    }

    fun has(name: String): Boolean = allMethods().any { it.name == name }

    /** Every method, as one readable line each, for the log. */
    fun describeMethods(): List<String> =
        allMethods()
            .map { method ->
                val parameters = method.parameterTypes.joinToString(", ") { it.simpleName }
                val static = if (Modifier.isStatic(method.modifiers)) "static " else ""
                "$static${method.returnType.simpleName} ${method.name}($parameters)"
            }
            .distinct()
            .sorted()

    private fun allMethods(): List<Method> =
        runCatching { (type.methods.toList() + type.declaredMethods.toList()).distinct() }
            .getOrDefault(emptyList())

    private fun findMethod(name: String, args: Array<out Any?>): Method? =
        allMethods().firstOrNull { method ->
            method.name == name &&
                method.parameterTypes.size == args.size &&
                method.parameterTypes.withIndex().all { (index, parameter) ->
                    accepts(parameter, args[index])
                }
        }

    private fun accepts(parameter: Class<*>, value: Any?): Boolean {
        if (value == null) return !parameter.isPrimitive
        val boxed = when (parameter) {
            java.lang.Integer.TYPE -> java.lang.Integer::class.java
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            java.lang.Float.TYPE -> java.lang.Float::class.java
            java.lang.Double.TYPE -> java.lang.Double::class.java
            else -> parameter
        }
        return boxed.isInstance(value)
    }

    companion object {

        /**
         * Finds a class and its singleton. [instanceGetter] is the static
         * method that hands out the instance, `getInstance` for the ViWoods
         * class.
         */
        fun load(className: String, instanceGetter: String = "getInstance"): Result<VendorObject> =
            runCatching {
                val type = Class.forName(className)
                val getter = type.getMethod(instanceGetter)
                val instance = getter.invoke(null)
                    ?: throw IllegalStateException("$className.$instanceGetter() returned null")
                VendorObject(instance, type)
            }
    }
}

/** A short, log friendly reason for a failure. */
internal fun Throwable.shortReason(): String {
    val text = message?.lineSequence()?.firstOrNull()?.take(160).orEmpty()
    return if (text.isEmpty()) javaClass.simpleName else "${javaClass.simpleName}: $text"
}
