package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.appinstrumentation.UtilInstrumenter
import soot.*
import soot.Unit
import soot.javaToJimple.LocalGenerator
import soot.jimple.Jimple
import soot.jimple.SpecialInvokeExpr
import soot.jimple.Stmt
import java.util.*


class GlobalInstanceTransformer : SceneTransformer() {

	override fun internalTransform(phaseName: String, options: Map<String, String>) {
		// Get some system components
		val scActivity = Scene.v().getSootClassUnsafe("android.app.Activity")
		val scService = Scene.v().getSootClassUnsafe("android.app.Service")
		val scBroadcastReceiver = Scene.v().getSootClassUnsafe("android.app.BroadcastReceiver")
		val scContentProvider = Scene.v().getSootClassUnsafe("android.app.ContentProvider")

		// Get the registration class
		val scRegistrar = Scene.v().getSootClassUnsafe("me.zhenhao.forced.additionalappclasses.ComponentCallerService")
		val smRegistrarRef = scRegistrar.getMethodByName("registerGlobalInstance").makeRef()

		// Get the getClass() method
		val classType = Scene.v().getType("java.lang.Class")
		val smGetClass = Scene.v().objectType.sootClass.getMethod("java.lang.Class getClass()").makeRef()

		// Is this an Android component?
		for (sc in Scene.v().applicationClasses) {
			// We only instrument user code
			if (!UtilInstrumenter.isAppDeveloperCode(sc))
				continue

			// Is this class a component?
			if (Scene.v().orMakeFastHierarchy.canStoreType(sc.type, scActivity.type)
					|| Scene.v().orMakeFastHierarchy.canStoreType(sc.type, scService.type)
					|| Scene.v().orMakeFastHierarchy.canStoreType(sc.type, scBroadcastReceiver.type)
					|| Scene.v().orMakeFastHierarchy.canStoreType(sc.type, scContentProvider.type)) {
				var b: Body?
				var locThis: Local?
				var lastUnit: Unit? = null

				// Do we already have a constructor?
				var cons: SootMethod? = sc.getMethodUnsafe("void <init>()")
				if (cons == null) {
					val smSuperClassCons = sc.superclass.getMethodUnsafe("void <init>()") ?: continue

					// Create the new constructor
					cons = SootMethod("<init>", emptyList<Type>(), VoidType.v())
					sc.addMethod(cons)
					b = Jimple.v().newBody(cons)
					cons.activeBody = b

					// Add a reference to the "this" object
					locThis = Jimple.v().newLocal("this", sc.type)
					b!!.locals.add(locThis)
					b.units.add(Jimple.v().newIdentityStmt(locThis, Jimple.v().newThisRef(sc.type)))

					// Add a call to the superclass constructor
					b.units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(locThis,
							smSuperClassCons.makeRef())))

					// Add a return statement
					lastUnit = Jimple.v().newReturnVoidStmt()
					b.units.add(lastUnit)
				} else {
					b = cons.activeBody
					locThis = b!!.thisLocal

					// Find where we can inject out code. We must have called
					// the super constructor first, or the Dalvik verifier will
					// complain that the "this" local is not yet initialized.
					for (u in b.units) {
						val s = u as Stmt
						if (s.containsInvokeExpr()) {
							val iexpr = s.invokeExpr
							if (iexpr is SpecialInvokeExpr) {
								if (iexpr.getMethod().name == "<init>" && iexpr.base === locThis) {
									lastUnit = b.units.getSuccOf(u)
									break
								}
							}
						}
					}
				}

				// Get the class
				val localGen = LocalGenerator(b)
				val locClass = localGen.generateLocal(classType)
				val stmtAssignClass = Jimple.v().newAssignStmt(locClass, Jimple.v().newVirtualInvokeExpr(
						locThis, smGetClass))
				stmtAssignClass.addTag(InstrumentedCodeTag)
				b.units.insertBefore(stmtAssignClass, lastUnit!!)

				// Register the instance
				val argList = ArrayList<Value>()
				argList.add(locClass)
				argList.add(locThis)
				val stmtRegister = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(
						smRegistrarRef, argList))
				stmtRegister.addTag(InstrumentedCodeTag)
				b.units.insertBefore(stmtRegister, lastUnit)
			}
		}
	}

}
