package me.zhenhao.forced.decisionmaker.analysis.randomFuzzer

import java.util.Random

import me.zhenhao.forced.decisionmaker.DeterministicRandom

class RandomPrimitives {

	private val intPredefined = intArrayOf(-100, -3, -2, -1, 0, 1, 2, 3, 100)
	private val charPredefined = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '~', '`', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=', '{', '}', '[', ']', ':', ';', '\'', '<', '>', ',', '.', '?', '/', '|', ' ') // all chars found on a US keyboard, except for '\' and '"' which causes trouble in Strings
	private val floatPredefined = floatArrayOf(-100f, -3.0f, -2.0f, -1.0f, -0.1f, 0.0f, 0.1f, 1.0f, 2.0f, 3.0f, 100f)
	private val doublePredefined = doubleArrayOf(-100.0, -3.0, -2.0, -1.0, -0.1, 0.0, 0.1, 1.0, 2.0, 3.0, 100.0)

	fun next(type: String): Any {
		if (type == "int" || type == "java.lang.Integer")
			return nextInt()
		else if (type == "java.lang.String")
			return nextString() // we treat Strings as primitive types
		else if (type == "byte" || type == "java.lang.Byte")
			return nextByte()
		else if (type == "short" || type == "java.lang.Short")
			return nextShort()
		else if (type == "long" || type == "java.lang.Long")
			return nextLong()
		else if (type == "char" || type == "java.lang.Character")
			return nextChar()
		else if (type == "boolean" || type == "java.lang.Boolean")
			return nextBoolean()
		else if (type == "float" || type == "java.lang.Float")
			return nextFloat()
		else if (type == "double" || type == "java.lang.Double")
			return nextDouble()
		else
			throw RuntimeException("unsupported type: " + type)
	}

	fun isSupportedType(type: String): Boolean {
		if (type == "int" || type == "java.lang.Integer")
			return true
		else if (type == "java.lang.String")
			return true // we treat Strings as primitive types
		else if (type == "byte" || type == "java.lang.Byte")
			return true
		else if (type == "short" || type == "java.lang.Short")
			return true
		else if (type == "long" || type == "java.lang.Long")
			return true
		else if (type == "char" || type == "java.lang.Character")
			return true
		else if (type == "boolean" || type == "java.lang.Boolean")
			return true
		else if (type == "float" || type == "java.lang.Float")
			return true
		else return type == "double" || type == "java.lang.Double"
	}

	private fun nextBoolean(): Boolean {
		return r().nextBoolean()
	}

	private fun nextByte(): Byte {
		return intPredefined[r().nextInt(intPredefined.size)].toByte()
	}

	private fun nextChar(): Char {
		return charPredefined[r().nextInt(charPredefined.size)] // always use predefined chars, r() chars typeically make no sense
	}

	private fun nextDouble(): Double {
		return doublePredefined[r().nextInt(doublePredefined.size)]
	}

	private fun nextFloat(): Float {
		return floatPredefined[r().nextInt(floatPredefined.size)]
	}

	private fun nextInt(): Int {
		return intPredefined[r().nextInt(intPredefined.size)]
	}

	private fun nextLong(): Long {
		return intPredefined[r().nextInt(intPredefined.size)].toLong()
	}

	private fun nextShort(): Short {
		return intPredefined[r().nextInt(intPredefined.size)].toShort()
	}

	private fun nextString(): String {
		val sb = StringBuilder()
		while (r().nextBoolean())
			sb.append(nextChar())
		return sb.toString()
	}

	private fun r(): Random {
		return DeterministicRandom.theRandom
	}

}
