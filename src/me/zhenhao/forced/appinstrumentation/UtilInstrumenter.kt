package me.zhenhao.forced.appinstrumentation

import me.zhenhao.forced.FrameworkOptions
import me.zhenhao.forced.appinstrumentation.transformer.InstrumentedCodeTag
import me.zhenhao.forced.shared.util.Pair
import soot.*
import soot.Unit
import soot.javaToJimple.LocalGenerator
import soot.jimple.IntConstant
import soot.jimple.InvokeExpr
import soot.jimple.Jimple
import java.io.File
import java.util.*


object UtilInstrumenter {

    val ROOT_PACKAGE_OF_FORCED_CODE = "me.zhenhao.forced."
    val ROOT_PACKAGE_OF_ADDITIONAL_CODE = ROOT_PACKAGE_OF_FORCED_CODE + "android."

    val JAVA_CLASS_FOR_INSTRUMENTATION = ROOT_PACKAGE_OF_ADDITIONAL_CODE + "tracing.BytecodeLogger"
    val JAVA_CLASS_FOR_CRASH_REPORTING = ROOT_PACKAGE_OF_ADDITIONAL_CODE + "crashreporter.CrashReporter"
    val JAVA_CLASS_FOR_PATH_EXECUTION  = ROOT_PACKAGE_OF_ADDITIONAL_CODE + "pathexecution.PathExecutor"

    val HELPER_SERVICE_FOR_PATH_TRACKING = ROOT_PACKAGE_OF_ADDITIONAL_CODE + "tracing.TracingService"
    val HELPER_SERVICE_FOR_COMPONENT_CALLER = ROOT_PACKAGE_OF_ADDITIONAL_CODE + "ComponentCallerService"
    val HELPER_APPLICATION_FOR_FORCED_CODE_INIT = ROOT_PACKAGE_OF_ADDITIONAL_CODE + "ForcedCodeInitHelperApp"

    val ADDITIONAL_APP_CLASSES_BIN = FrameworkOptions.frameworkDir + "android-bin/"
    val SHARED_CLASSES_BIN = FrameworkOptions.frameworkDir + "shared-bin/"

    val SOOT_OUTPUT = FrameworkOptions.frameworkDir + "sootOutput/"
    val SOOT_OUTPUT_APK = SOOT_OUTPUT + File(FrameworkOptions.getApkName()).name + ".apk"
    val SOOT_OUTPUT_DEPLOYED_APK = SOOT_OUTPUT + File(FrameworkOptions.getApkName()).name + "_deployed.apk"


    fun isApiCall(invokeExpr: InvokeExpr): Boolean {
        return invokeExpr.method.declaringClass.isLibraryClass || invokeExpr.method.declaringClass.isJavaLibraryClass
    }

    fun isAppDeveloperCode(className: SootClass): Boolean {
        return !(className.packageName.startsWith("android.") ||
                className.packageName.startsWith("java.") ||
                className.toString().startsWith(ROOT_PACKAGE_OF_FORCED_CODE) ||
                className.toString().contains("dummyMainClass"))
    }


    fun makeJimpleStaticCallForPathExecution(methodName: String, vararg args: Any): Unit {
        val sootClass = Scene.v().getSootClass(JAVA_CLASS_FOR_PATH_EXECUTION)

        val generated: Unit?

        val argTypes = ArrayList<Type>()
        val argList = ArrayList<Value>()


        if (args.size % 2 != 0) {
            throw RuntimeException(
                    "Mismatched argument types:values in static call to " + methodName)
        } else {
            for (i in args.indices)
                if (i % 2 == 0)
                // First type, then argument
                    argTypes.add(args[i] as Type)
                else
                    argList.add(args[i] as Value)
        }


        val createAndAdd = sootClass.getMethod(methodName, argTypes)
        val sie = Jimple.v().newStaticInvokeExpr(
                createAndAdd.makeRef(), argList)


        generated = Jimple.v().newInvokeStmt(sie)

        return generated
    }


    fun generateParameterArray(parameterList: List<Value>, body: Body): Pair<Value, List<Unit>> {
        val generated = ArrayList<Unit>()

        val arrayExpr = Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(parameterList.size))

        val newArrayLocal = generateFreshLocal(body, parameterArrayType)
        val newAssignStmt = Jimple.v().newAssignStmt(newArrayLocal, arrayExpr)
        generated.add(newAssignStmt)

        for (i in parameterList.indices) {
            val index = IntConstant.v(i)
            val leftSide = Jimple.v().newArrayRef(newArrayLocal, index)
            val rightSide = generateCorrectObject(body, parameterList[i], generated)

            val parameterInArray = Jimple.v().newAssignStmt(leftSide, rightSide)
            generated.add(parameterInArray)
        }

        return Pair(newArrayLocal, generated)
    }


    fun generateCorrectObject(body: Body, value: Value, generated: MutableList<Unit>): Value {
        if (value.type is PrimType) {
            //in case of a primitive type, we use boxing (I know it is not nice, but it works...) in order to use the Object type
            if (value.type is BooleanType) {
                val booleanLocal = generateFreshLocal(body, RefType.v("java.lang.Boolean"))

                val sootClass = Scene.v().getSootClass("java.lang.Boolean")
                val valueOfMethod = sootClass.getMethod("java.lang.Boolean valueOf(boolean)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(booleanLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return booleanLocal
            } else if (value.type is ByteType) {
                val byteLocal = generateFreshLocal(body, RefType.v("java.lang.Byte"))

                val sootClass = Scene.v().getSootClass("java.lang.Byte")
                val valueOfMethod = sootClass.getMethod("java.lang.Byte valueOf(byte)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(byteLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return byteLocal
            } else if (value.type is CharType) {
                val characterLocal = generateFreshLocal(body, RefType.v("java.lang.Character"))

                val sootClass = Scene.v().getSootClass("java.lang.Character")
                val valueOfMethod = sootClass.getMethod("java.lang.Character valueOf(char)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(characterLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return characterLocal
            } else if (value.type is DoubleType) {
                val doubleLocal = generateFreshLocal(body, RefType.v("java.lang.Double"))

                val sootClass = Scene.v().getSootClass("java.lang.Double")
                val valueOfMethod = sootClass.getMethod("java.lang.Double valueOf(double)")

                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(doubleLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return doubleLocal
            } else if (value.type is FloatType) {
                val floatLocal = generateFreshLocal(body, RefType.v("java.lang.Float"))

                val sootClass = Scene.v().getSootClass("java.lang.Float")
                val valueOfMethod = sootClass.getMethod("java.lang.Float valueOf(float)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(floatLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return floatLocal
            } else if (value.type is IntType) {
                val integerLocal = generateFreshLocal(body, RefType.v("java.lang.Integer"))

                val sootClass = Scene.v().getSootClass("java.lang.Integer")
                val valueOfMethod = sootClass.getMethod("java.lang.Integer valueOf(int)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(integerLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return integerLocal
            } else if (value.type is LongType) {
                val longLocal = generateFreshLocal(body, RefType.v("java.lang.Long"))

                val sootClass = Scene.v().getSootClass("java.lang.Long")
                val valueOfMethod = sootClass.getMethod("java.lang.Long valueOf(long)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(longLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return longLocal
            } else if (value.type is ShortType) {
                val shortLocal = generateFreshLocal(body, RefType.v("java.lang.Short"))

                val sootClass = Scene.v().getSootClass("java.lang.Short")
                val valueOfMethod = sootClass.getMethod("java.lang.Short valueOf(short)")
                val staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOfMethod.makeRef(), value)

                val newAssignStmt = Jimple.v().newAssignStmt(shortLocal, staticInvokeExpr)
                newAssignStmt.addTag(InstrumentedCodeTag)
                generated.add(newAssignStmt)

                return shortLocal
            } else
                throw RuntimeException("Ooops, something went all wonky!")
        } else
            //just return the value, there is nothing to box
            return value
    }


    fun generateFreshLocal(body: Body, type: Type): Local {
        val lg = LocalGenerator(body)
        return lg.generateLocal(type)
    }


    val parameterArrayType: Type
        get() {
            val parameterArrayType = RefType.v("java.lang.Object")
            val parameterArray = ArrayType.v(parameterArrayType, 1)

            return parameterArray
        }
}
