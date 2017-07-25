package me.zhenhao.forced.appinstrumentation.transformer

import me.zhenhao.forced.shared.SharedClassesSettings
import soot.Body
import soot.RefType
import soot.Scene
import soot.Value
import soot.jimple.*
import java.util.*

class DummyMethodHookTransformer : AbstractInstrumentationTransformer() {

	override fun internalTransform(body: Body, phaseName: String, options: Map<String, String>) {
		if (!canInstrumentMethod(body.method))
			return

		val smLogI = Scene.v().getMethod("<android.util.Log: int i(java.lang.String,java.lang.String)>")
		val constTag = StringConstant.v(SharedClassesSettings.TAG)
		val constSigRead = StringConstant.v("Signature read")
		val constLoadingDex = StringConstant.v("Loading dex file...")
		val constOpeningAsset = StringConstant.v("Opening asset file...")

		val unitIt = body.units.snapshotIterator()
		while (unitIt.hasNext()) {
			val curUnit = unitIt.next()

			if (curUnit is AssignStmt) {
				val curAssignStmt = curUnit

				if (curAssignStmt.rightOp is InstanceFieldRef) {
					val fref = curAssignStmt.rightOp as InstanceFieldRef
					if ((fref.base.type as RefType).sootClass.name == "android.content.pm.PackageInfo" && fref.field.name == "signatures") {
						val args = ArrayList<Value>()
						args.add(constTag)
						args.add(constSigRead)
						body.units.insertAfter(Jimple.v().newInvokeStmt(
								Jimple.v().newStaticInvokeExpr(smLogI.makeRef(), args)), curUnit)
					}
				} else if (curAssignStmt.containsInvokeExpr()) {
					val invokeExpr = curAssignStmt.invokeExpr
					val sm = invokeExpr.method
					val mSig = sm.signature

					if (mSig == "<android.content.pm.PackageManager: android.content.pm.PackageInfo getPackageInfo(java.lang.String,int)>") {
						if (invokeExpr is VirtualInvokeExpr) {
							val vie = invokeExpr
							val dummyMethod = Scene.v().getMethod("<me.zhenhao.forced.android.wrapper.DummyWrapper: android.content.pm.PackageInfo dummyWrapper_getPackageInfo(android.content.pm.PackageManager,java.lang.String,int)>")
							val newInv = Jimple.v().newStaticInvokeExpr(dummyMethod.makeRef(), vie.base, vie.getArg(0), vie.getArg(1))
							curAssignStmt.rightOp = newInv
						}
					} else if (mSig == "<java.util.Properties: java.lang.String getProperty(java.lang.String,java.lang.String)>") {
						if (invokeExpr is VirtualInvokeExpr) {
							val vie = invokeExpr
							val dummyMethod = Scene.v().getMethod("<me.zhenhao.forced.android.wrapper.DummyWrapper: java.lang.String dummyWrapper_getProperty(java.util.Properties,java.lang.String,java.lang.String)>")
							val newInv = Jimple.v().newStaticInvokeExpr(dummyMethod.makeRef(), vie.base, vie.getArg(0), vie.getArg(1))
							curAssignStmt.rightOp = newInv
						}
					} else if (mSig == "<java.util.Properties: java.lang.String getProperty(java.lang.String)>") {
						if (invokeExpr is VirtualInvokeExpr) {
							val vie = invokeExpr
							val dummyMethod = Scene.v().getMethod("<me.zhenhao.forced.android.wrapper.DummyWrapper: java.lang.String dummyWrapper_getProperty(java.util.Properties,java.lang.String)>")
							val newInv = Jimple.v().newStaticInvokeExpr(dummyMethod.makeRef(), vie.base, vie.getArg(0))
							curAssignStmt.rightOp = newInv
						}
					} else if (mSig == "<dalvik.system.DexClassLoader: java.lang.Class loadClass(java.lang.String)>" || mSig == "<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>") {
						if (invokeExpr is VirtualInvokeExpr) {
							val vie = invokeExpr
							val dummyMethod = Scene.v().getMethod("<me.zhenhao.forced.android.wrapper.DummyWrapper: java.lang.Class dummyWrapper_loadClass(java.lang.String,java.lang.ClassLoader)>")
							val newInv = Jimple.v().newStaticInvokeExpr(dummyMethod.makeRef(), vie.getArg(0), vie.base)
							curAssignStmt.rightOp = newInv
						}
					} else if (mSig == "<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>") {
						if (invokeExpr is VirtualInvokeExpr) {
							val vie = invokeExpr
							val dummyMethod = Scene.v().getMethod("<me.zhenhao.forced.android.wrapper.DummyWrapper: java.lang.reflect.Method dummyWrapper_getMethod(java.lang.Class,java.lang.String,java.lang.Class[])>")
							val newInv = Jimple.v().newStaticInvokeExpr(dummyMethod.makeRef(), vie.base, vie.getArg(0), vie.getArg(1))
							curAssignStmt.rightOp = newInv
						}
					} else if (mSig == "<dalvik.system.DexFile: dalvik.system.DexFile loadDex(java.lang.String,java.lang.String,int)>") {
						// Add logging
						val args = ArrayList<Value>()
						args.add(constTag)
						args.add(constLoadingDex)
						body.units.insertBefore(Jimple.v().newInvokeStmt(
								Jimple.v().newStaticInvokeExpr(smLogI.makeRef(), args)), curUnit)
					} else if (mSig == "<android.content.res.AssetManager: java.io.InputStream open(java.lang.String)>") {
						// Add logging
						val args = ArrayList<Value>()
						args.add(constTag)
						args.add(constOpeningAsset)
						body.units.insertBefore(Jimple.v().newInvokeStmt(
								Jimple.v().newStaticInvokeExpr(smLogI.makeRef(), args)), curUnit)
					} else if (mSig == "<com.kakaka.googleplay.Application\$DialogActivity$1$1: int decrypt(int[],int[],byte[],int,int)>") {
						// Add logging
						var args: MutableList<Value> = ArrayList()
						args.add(constTag)
						args.add(StringConstant.v("Before decrypt"))
						body.units.insertBefore(Jimple.v().newInvokeStmt(
								Jimple.v().newStaticInvokeExpr(smLogI.makeRef(), args)), curUnit)

						args = ArrayList<Value>()
						args.add(constTag)
						args.add(StringConstant.v("After decrypt"))
						body.units.insertAfter(Jimple.v().newInvokeStmt(
								Jimple.v().newStaticInvokeExpr(smLogI.makeRef(), args)), curUnit)
					}
				}
			}
		}

	}

}
