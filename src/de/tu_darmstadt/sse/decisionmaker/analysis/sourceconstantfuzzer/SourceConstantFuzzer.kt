package de.tu_darmstadt.sse.decisionmaker.analysis.sourceconstantfuzzer

import com.sun.org.apache.xpath.internal.operations.Bool
import de.tu_darmstadt.sse.apkspecific.CodeModel.CodePositionManager
import de.tu_darmstadt.sse.appinstrumentation.UtilInstrumenter
import de.tu_darmstadt.sse.appinstrumentation.transformer.InstrumentedCodeTag
import de.tu_darmstadt.sse.decisionmaker.DeterministicRandom
import de.tu_darmstadt.sse.decisionmaker.analysis.AnalysisDecision
import de.tu_darmstadt.sse.decisionmaker.analysis.FuzzyAnalysis
import de.tu_darmstadt.sse.decisionmaker.analysis.randomFuzzer.RandomPrimitives
import de.tu_darmstadt.sse.decisionmaker.server.ThreadTraceManager
import de.tu_darmstadt.sse.decisionmaker.server.TraceManager
import de.tu_darmstadt.sse.sharedclasses.networkconnection.DecisionRequest
import de.tu_darmstadt.sse.sharedclasses.networkconnection.ServerResponse
import soot.Scene
import soot.SootClass
import soot.Unit
import soot.jimple.*
import soot.jimple.infoflow.solver.cfg.InfoflowCFG
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableSet
import kotlin.collections.contains
import kotlin.collections.dropLastWhile
import kotlin.collections.forEach
import kotlin.collections.isEmpty
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.toTypedArray


class SourceConstantFuzzer : FuzzyAnalysis() {

    private val RETURN_DUPLICATES = false

    private var ifCFG: InfoflowCFG? = null

    private val stringContainer = ConstantContainer()
    private val intContainer = ConstantContainer()
    private val longContainer = ConstantContainer()
    private val doubleContainer = ConstantContainer()
    private val floatContainer = ConstantContainer()

    private val booleanContainer = ConstantContainer()
    private val charContainer = ConstantContainer()
    private val shortContainer = ConstantContainer()
    private val byteContainer = ConstantContainer()

    // once we improved the metric we stick to our "good" response
    private val pinnedResponses = HashMap<DecisionRequest, ServerResponse>()

    private val oldValues = HashMap<Int, HashSet<Any>>()

    private val randPrimitives = RandomPrimitives()

    override fun doPreAnalysis(targetUnits: MutableSet<Unit>, traceManager: TraceManager) {

        ifCFG = InfoflowCFG()

        Scene.v().classes
                .filter { UtilInstrumenter.isAppDeveloperCode(it) }
                .forEach { sc ->
                    sc.methods
                            .filter { it.hasActiveBody() }
                            .flatMap { it.activeBody.units }
                            .filterNot { it.hasTag(InstrumentedCodeTag.name) }
                            .forEach { getConstants(it, sc) }
                }

        fillWithDummyValues()

        convertSets2Arrays()

        // debug output to show found strings
        //		for (String className : doubleContainer.getArrayMap().keySet()) {
        //			System.out.println("SourceConstantFuzzer: Classname: " + className);
        //			Object[] valuesString = stringContainer.getArrayMap().get(className);
        //
        //			Object[] valuesInt = intContainer.getArrayMap().get(className);
        //			Object[] valuesLong = longContainer.getArrayMap().get(className);
        //			Object[] valuesDouble = doubleContainer.getArrayMap().get(className);
        //			Object[] valuesFloat = floatContainer.getArrayMap().get(className);
        //			Object[] valuesBoolean = stringContainer.getArrayMap().get(className); //TODO!
        //
        //			if(null != valuesString)
        //			{
        //				for (Object o : valuesString) {
        //					System.out.println("\tFound String: " + o);
        //				}
        //			}
        //			else
        //			{
        //				System.out.println("###No Strings in this class###");
        //			}
        //		}

        println("#Constants in App including dummy values")
        println("#Strings in App: " + stringContainer.allValues.size)
        println("#Integers in App: " + intContainer.allValues.size)
        println("#Longs in App: " + longContainer.allValues.size)
        println("#Doubles in App: " + doubleContainer.allValues.size)
        println("#Floats in App: " + floatContainer.allValues.size)
        println("#Booleans in App: " + booleanContainer.allValues.size)
        println("#Chars in App: " + charContainer.allValues.size)
        println("#Shorts in App: " + shortContainer.allValues.size)
        println("#bytes in App: " + byteContainer.allValues.size)

    }

    private fun fillWithDummyValues() {
        if (stringContainer.isEmpty) {
            println("*** No Strings in App ***")

            generateDummy("String", 5)
        }

        if (intContainer.isEmpty) {
            println("*** No Integers in App ***")

            generateDummy("int", 5)
        }

        if (longContainer.isEmpty) {
            println("*** No Longs in App ***")
            generateDummy("long", 5)
        }

        if (doubleContainer.isEmpty) {
            println("*** No Doubles in App ***")
            generateDummy("double", 5)
        }

        if (floatContainer.isEmpty) {
            println("*** No Floats in App ***")
            generateDummy("float", 5)
        }

        //always do dummys for boolean
        generateDummy("boolean", 2)

        if (byteContainer.isEmpty) {
            generateDummy("byte", 5)
        }

        if (shortContainer.isEmpty) {
            generateDummy("short", 5)
        }

        if (charContainer.isEmpty) {
            generateDummy("char", 5)
        }

    }

    private fun generateDummy(type: String, count: Int) {

        if ("String" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("String")
                stringContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("int" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("int")
                intContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("long" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("long")
                longContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("double" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("double")
                doubleContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("float" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("float")
                floatContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("boolean" == type) {
            booleanContainer.insertTmpValue("DummyClass", true as Any)
            booleanContainer.insertTmpValue("DummyClass", false as Any)
        } else if ("char" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("char")
                charContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("short" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("short")
                shortContainer.insertTmpValue("DummyClass", value)
            }
        } else if ("byte" == type) {
            for (i in 0..count - 1) {
                val value = randPrimitives.next("byte")
                byteContainer.insertTmpValue("DummyClass", value)
            }
        }

    }

    override fun resolveRequest(clientRequest: DecisionRequest, completeHistory: ThreadTraceManager): List<AnalysisDecision> {

        // We only model "after"-style hooks
        if (!clientRequest.isHookAfter) {
            return ArrayList()
        }

        // We only model method return values
        val hookSignature = clientRequest.loggingPointSignature

        val returnType = extractReturnType(hookSignature)
        if (returnType == "void") {
            return ArrayList()
        }

        //store codeposition
        val codePosition = clientRequest.codePosition

        // by default we return the original value for types we do not
        // handle
        var newReturnValue: Any? = clientRequest.runtimeValueOfReturn
        val response = ServerResponse()
        response.analysisName = getAnalysisName()

        // from the 3rd attempt compare metrics of last and lastlast value
        if (completeHistory.getHistories().size > 1) {
            val bdLast = completeHistory.getNewestClientHistory()?.getProgressValue("ApproachLevel")
            val bdLastLast = completeHistory.getLastClientHistory()?.getProgressValue("ApproachLevel")
            var improvedOverLastRun: Boolean = false
            if (bdLast != null && bdLastLast != null && bdLast < bdLastLast)
                improvedOverLastRun = true

            // look at history to not return same value again, only
            // do this if metric improves

            // If we already have a good value for this place, let's keep it
            val pinnedResponse = pinnedResponses[clientRequest]
            if (pinnedResponse != null) {
                pinnedResponse.analysisName = getAnalysisName()
                val finalDecision = AnalysisDecision()
                finalDecision.decisionWeight = 3
                finalDecision.analysisName = getAnalysisName()
                finalDecision.serverResponse = pinnedResponse
                return listOf(finalDecision)
            } else if (improvedOverLastRun) {
                val lastHistory = completeHistory.getLastClientHistory()
                if (lastHistory != null) {
                    // return the last value and stick to it for the future
                    val decision = lastHistory.getResponseForRequest(
                            clientRequest)
                    if (decision != null) {
                        val lastReturnValue = decision.serverResponse
                        if (lastReturnValue != null) {
                            lastReturnValue.analysisName = getAnalysisName()
                            pinnedResponses.put(clientRequest, lastReturnValue)
                            val finalDecision = AnalysisDecision()
                            finalDecision.decisionWeight = 3
                            finalDecision.analysisName = getAnalysisName()
                            finalDecision.serverResponse = lastReturnValue
                            return listOf(finalDecision)
                        }
                    }
                }
            }
            // if not improvedOverLastRun -> pick new value, see below
        }

        // pick a new return value:

        val codePosMgr = CodePositionManager.codePositionManagerInstance
        val codeUnit = codePosMgr.getUnitForCodePosition(codePosition)

        val originMethod = ifCFG!!.getMethodOf(codeUnit) ?: return ArrayList()
        val originClass = originMethod.declaringClass
        val className = originClass.name

        val lastReturnValuesForCodePos = oldValues[codePosition]

        // System.out.println("SourceConstantFuzzer: Lookup value in class: " +
        // className);

        when (returnType) {
            "java.lang.String" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, stringContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL string constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, stringContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "int" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, intContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL int constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, intContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "long" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, longContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL long constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, longContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "float" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, floatContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL float constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, floatContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "double" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, doubleContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL double constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, doubleContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "boolean" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, booleanContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL boolean constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, booleanContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "char" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, charContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL boolean constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, charContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "short" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, shortContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL boolean constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, shortContainer.allValues)
                }
                response.setResponseExist(true)
            }
            "byte" -> {
                newReturnValue = chooseNewValue(lastReturnValuesForCodePos, byteContainer.arrayMap[className])
                if (null == newReturnValue)
                // we did not find anything in current
                // class, try ALL boolean constants
                {
                    newReturnValue = chooseNewValue(lastReturnValuesForCodePos, byteContainer.allValues)
                }
                response.setResponseExist(true)
            }
            else -> {
            }
        }

        if (null == newReturnValue) {
            System.err.println(
                    "SourceConstantFuzzer: ALL Values have been tried (out of options)! -> returning NULL now")

            // if we ran out of options, we can return null here
            return ArrayList()
        }

        // remember old return values
        val oldSet = oldValues[codePosition]
        if (null != oldSet) {
            oldSet.add(newReturnValue)
        } else {
            val tmpSet = HashSet<Any>()
            tmpSet.add(newReturnValue)
            oldValues.put(codePosition, tmpSet)
        }

        // prepare new return value response
        response.returnValue = newReturnValue
        val finalDecision = AnalysisDecision()
        finalDecision.decisionWeight = 3
        finalDecision.analysisName = getAnalysisName()
        finalDecision.serverResponse = response
        return listOf(finalDecision)
    }

    override fun reset() {}

    private fun getConstants(u: Unit, sootClass: SootClass) {

        u.useBoxes
                .map { it.value }
                .forEach {
                    if (it is StringConstant) {
                        stringContainer.insertTmpValue(sootClass.name, it.value)
                    } else if (it is IntConstant) {
                        val value = it.value
                        intContainer.insertTmpValue(sootClass.name, value)

                        //test size of int, whether it might also be a char, byte or short

                        //char (or 65,535 inclusive)
                        if (value in 0..65535) {
                            charContainer.insertTmpValue(sootClass.name, value.toChar())
                        }
                        //short -32,768 and a maximum value of 32,767 (inclusive)
                        if (value in -32768..32767) {
                            shortContainer.insertTmpValue(sootClass.name, value.toShort())
                        }
                        //byte: -128 and a maximum value of 127 (inclusive)
                        if (value in -128..127) {
                            byteContainer.insertTmpValue(sootClass.name, value.toByte())
                        }

                    } else if (it is LongConstant) {
                        longContainer.insertTmpValue(sootClass.name, it.value)
                    } else if (it is DoubleConstant) {
                        doubleContainer.insertTmpValue(sootClass.name, it.value)
                    } else if (it is FloatConstant) {
                        floatContainer.insertTmpValue(sootClass.name, it.value)
                    }
                }
    }

    private fun convertSets2Arrays() {
        stringContainer.convertSetsToArrays()
        intContainer.convertSetsToArrays()
        floatContainer.convertSetsToArrays()
        longContainer.convertSetsToArrays()
        doubleContainer.convertSetsToArrays()
        booleanContainer.convertSetsToArrays()
        charContainer.convertSetsToArrays()
        shortContainer.convertSetsToArrays()
        byteContainer.convertSetsToArrays()
    }

    private fun chooseNewValue(usedValues: HashSet<Any>?, allValues: Array<Any>?): Any? {

        var newValue: Any?

        // if we do not have any constant values for the requested type, we
        // return null
        if (null == allValues || allValues.isEmpty()) {
            return null
        }

        // if we havent used any values, just pick one
        if (null == usedValues || usedValues.size == 0) {
            newValue = allValues[DeterministicRandom.theRandom.nextInt(allValues.size)]
        } else {
            if (!RETURN_DUPLICATES) {
                // if all values have been tried -> return null == out of values
                if (usedValues.size == allValues.size) {
                    return null
                }

                do {
                    newValue = allValues[DeterministicRandom.theRandom.nextInt(allValues.size)]
                } while (usedValues.contains(newValue))
            } else {
                // just choose one and dont care about duplicates
                newValue = allValues[DeterministicRandom.theRandom.nextInt(allValues.size)]
            }

        }

        return newValue
    }

    private fun extractReturnType(methodSignature: String): String {
        return methodSignature.split(": ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    }

    override fun getAnalysisName(): String {
        return "SourceConstantFuzzer"
    }

}
