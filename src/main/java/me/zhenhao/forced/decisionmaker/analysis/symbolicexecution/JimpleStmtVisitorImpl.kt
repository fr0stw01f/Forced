package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution

import java.util.HashMap
import java.util.HashSet

import com.google.common.collect.Table

import soot.ArrayType
import soot.BooleanType
import soot.DoubleType
import soot.IntType
import soot.Local
import soot.LongType
import soot.RefType
import soot.SootField
import soot.Type
import soot.Unit
import soot.Value
import soot.jimple.ArrayRef
import soot.jimple.AssignStmt
import soot.jimple.BreakpointStmt
import soot.jimple.CaughtExceptionRef
import soot.jimple.Constant
import soot.jimple.EnterMonitorStmt
import soot.jimple.ExitMonitorStmt
import soot.jimple.Expr
import soot.jimple.FieldRef
import soot.jimple.GotoStmt
import soot.jimple.IdentityStmt
import soot.jimple.IfStmt
import soot.jimple.InstanceInvokeExpr
import soot.jimple.IntConstant
import soot.jimple.InvokeStmt
import soot.jimple.LookupSwitchStmt
import soot.jimple.NopStmt
import soot.jimple.ParameterRef
import soot.jimple.RetStmt
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.Stmt
import soot.jimple.StmtSwitch
import soot.jimple.StringConstant
import soot.jimple.TableSwitchStmt
import soot.jimple.ThisRef
import soot.jimple.ThrowStmt
import soot.jimple.infoflow.data.AccessPath
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import soot.jimple.infoflow.source.data.SourceSinkDefinition
import me.zhenhao.forced.appinstrumentation.InstrumentUtil
import me.zhenhao.forced.commandlinelogger.LogHelper
import me.zhenhao.forced.decisionmaker.analysis.dynamicValues.DynamicValueInformation
import me.zhenhao.forced.decisionmaker.analysis.smartconstantdataextractor.NotYetSupportedException
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTAssertStatement
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTAssignment
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBinding
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBinding.TYPE
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBindingValue
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTConstantValue
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTProgram
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTSimpleAssignment

class JimpleStmtVisitorImpl(sources: Set<SourceSinkDefinition>,
                            val jimpleDataFlowStatements: List<Stmt>, private val accessPathPath: List<AccessPath>,
                            private val targetUnits: Set<Unit>, private val cfg: IInfoflowCFG, val splitInfos: Table<List<Stmt>, Stmt, List<List<String>>>) : StmtSwitch {
    internal val smtPrograms: MutableSet<SMTProgram>
    internal var tmpSmtProgram = SMTProgram()

    internal var currentSMTProgram: SMTProgram

    internal var globalScopeSSAFormHelper: MutableMap<Value, SMTBinding> = HashMap()

    internal var fieldSSAFormHelper: MutableMap<SootField, SMTBinding> = HashMap()

    internal var arrayHelper: MutableMap<String, SMTBinding> = HashMap()

    internal var thisRefSSAFormHelper: Map<ThisRef, SMTBinding> = HashMap()
    private val exprVisitor: JimpleExprVisitorImpl = JimpleExprVisitorImpl(sources, this)

    private val dynamicValueInfo = HashMap<SMTProgram, MutableSet<DynamicValueInformation>>()


    var notSupported = false

    init {
        this.smtPrograms = HashSet<SMTProgram>()
        //initial adding of a single SMTProgram
        currentSMTProgram = SMTProgram()
        smtPrograms.add(currentSMTProgram)
    }

    fun getSMTPrograms(): Set<SMTProgram> {
        return this.smtPrograms
    }


    fun addValueBindingToVariableDeclaration(value: Value, binding: SMTBinding?) {
        //update current program
        tmpSmtProgram.addVariableDeclaration(binding)

        //update all smt-programs
        for (smtProgram in smtPrograms)
            smtProgram.addVariableDeclaration(binding)
        //global scope is the same for all smt-programs
        if (binding != null)
            this.globalScopeSSAFormHelper.put(value, binding)
    }


    fun addFieldBindingToVariableDeclaration(field: SootField, binding: SMTBinding) {
        //update current program
        tmpSmtProgram.addVariableDeclaration(binding)

        //update all smt-programs
        for (smtProgram in smtPrograms)
            smtProgram.addVariableDeclaration(binding)
        //field scope is the same for all smt-programs
        this.fieldSSAFormHelper.put(field, binding)
    }


    fun getLatestBindingForValue(value: Value): SMTBinding? {
        if (hasBindingForValue(value))
            return this.globalScopeSSAFormHelper[value]
        else
            return null
    }


    fun getLatestBindingForField(field: SootField): SMTBinding? {
        if (hasBindingForField(field))
            return this.fieldSSAFormHelper[field]
        else
            return null
    }

    fun hasBindingForValue(value: Value): Boolean {
        return this.globalScopeSSAFormHelper.containsKey(value)
    }

    fun hasBindingForField(field: SootField): Boolean {
        return this.fieldSSAFormHelper.containsKey(field)
    }

    fun getLatestBindingForThisRef(thisRef: ThisRef): SMTBinding? {
        if (hasBindingForThisRef(thisRef))
            return thisRefSSAFormHelper[thisRef]
        else
            return null
    }

    fun hasBindingForThisRef(thisRef: ThisRef): Boolean {
        return this.thisRefSSAFormHelper.containsKey(thisRef)
    }


    fun createNewBindingForValue(value: Value): SMTBinding? {
        var binding: SMTBinding? = null
        if (hasBindingForValue(value)) {
            val oldBinding = getLatestBindingForValue(value)
            var ssaVersionOldBinding = oldBinding!!.version
            //increment version
            ssaVersionOldBinding += 1
            binding = SMTBinding(oldBinding.variableName, oldBinding.type, ssaVersionOldBinding)
        } else {
            if (value is Local) {
                val local = value
                val bindingType = createBindingType(local.type)
                var localName = local.name
                //check if such a local name is already taken
                var countOccurance = 0
                for ((key) in globalScopeSSAFormHelper) {
                    if (key.toString() == localName)
                        countOccurance += 1
                }
                if (countOccurance > 0) {
                    val tmp = localName
                    for (i in 0..countOccurance - 1)
                        localName += tmp
                }
                binding = SMTBinding(localName, bindingType, 0)
            } else if (value is StringConstant) {
                val constantStringValue = value.value
                binding = SMTBinding(constantStringValue, TYPE.String, 0)
            }
        }
        return binding
    }

    fun createNewBindingForThisRef(thisRef: ThisRef): SMTBinding {
        var binding: SMTBinding? = null
        if (hasBindingForThisRef(thisRef)) {
            val oldBinding = getLatestBindingForThisRef(thisRef)
            var ssaVersionOldBinding = oldBinding!!.version
            //increment version
            ssaVersionOldBinding += 1
            binding = SMTBinding(oldBinding.variableName, oldBinding.type, ssaVersionOldBinding)
            return binding
        } else {
            return SMTBinding(thisRef.type.toString(), TYPE.String)
        }
    }


    fun addAssertStmtToAllPrograms(assertStmt: SMTAssertStatement) {
        //update current program
        tmpSmtProgram.addAssertStatement(assertStmt)

        for (smtProgram in smtPrograms)
            smtProgram.addAssertStatement(assertStmt)
    }


    fun addAssertStmtToSingleProgram(smtProgram: SMTProgram, assertStmt: SMTAssertStatement) {
        smtProgram.addAssertStatement(assertStmt)
    }


    fun doubleSMTPrograms() {
        val newSMTPrograms = HashSet<SMTProgram>()
        for (smtProgram in smtPrograms)
            newSMTPrograms.add(smtProgram.clone())
        smtPrograms.addAll(newSMTPrograms)
    }


    fun addingTrueAndFalseConditionalAssertions(lhs: SMTBinding) {
        var i = 0
        for (smtProgram in smtPrograms) {
            if (i % 2 == 0) {
                val assignment = SMTSimpleAssignment(lhs, SMTConstantValue(false))
                val assignAssert = SMTAssertStatement(assignment)
                addAssertStmtToSingleProgram(smtProgram, assignAssert)

                //CAUTION: update current program; in this case, we add a wrong statement
                tmpSmtProgram.addAssertStatement(assignAssert)
            } else {
                val assignment = SMTSimpleAssignment(lhs, SMTConstantValue(true))
                val assignAssert = SMTAssertStatement(assignment)
                addAssertStmtToSingleProgram(smtProgram, assignAssert)

                //CAUTION: update current program; in this case, we add a wrong statement
                tmpSmtProgram.addAssertStatement(assignAssert)
            }
            i += 1
        }
    }


    fun createNewBindingForField(field: SootField): SMTBinding {
        var binding: SMTBinding? = null
        if (hasBindingForField(field)) {
            val oldBinding = getLatestBindingForField(field)
            var ssaVersionOldBinding = oldBinding!!.version
            //increment version
            ssaVersionOldBinding += 1
            binding = SMTBinding(oldBinding.variableName, oldBinding.type, ssaVersionOldBinding)
            return binding
        } else {
            val bindingType = createBindingType(field.type)
            var fieldName = field.toString().replace(" ", "_")
            fieldName = fieldName.replace(".", "_")
            fieldName = fieldName.replace(":", "")
            binding = SMTBinding("FIELD_" + fieldName, bindingType)
        }
        return binding
    }

    fun createTemporalBinding(type: SMTBinding.TYPE): SMTBinding {
        var tmpName: String? = null
        when (type) {
            SMTBinding.TYPE.String -> tmpName = "StringTMP"
            SMTBinding.TYPE.Int -> tmpName = "IntTMP"
            SMTBinding.TYPE.Bool -> tmpName = "BoolTMP"
            SMTBinding.TYPE.Real -> {
            }
            else -> {
            }
        }
        val tmpValue = StringConstant.v(tmpName)

        var binding: SMTBinding? = null
        if (hasBindingForValue(tmpValue)) {
            val oldBinding = getLatestBindingForValue(tmpValue)
            var ssaVersionOldBinding = oldBinding!!.version
            //increment version
            ssaVersionOldBinding += 1
            binding = SMTBinding(oldBinding.variableName, oldBinding.type, ssaVersionOldBinding)
        } else {
            binding = SMTBinding(tmpName, type, 0)
        }
        addValueBindingToVariableDeclaration(tmpValue, binding)
        return binding
    }

    override fun caseBreakpointStmt(stmt: BreakpointStmt) {
        throw RuntimeException("todo")

    }

    override fun caseInvokeStmt(stmt: InvokeStmt) {
        val invokeExpr = stmt.invokeExpr
        val declaringClass = invokeExpr.method.declaringClass
        if (exprVisitor.isExpressionThatNeedsToBeConvertedToSMT(invokeExpr))
            exprVisitor.convertSpecialExpressionsToSMT(invokeExpr, stmt)
        else if (InstrumentUtil.isAppDeveloperCode(declaringClass)) {
            val method = invokeExpr.method
            val body = method.retrieveActiveBody()

            val newRhs = getBindingForTaintedValue(stmt) ?: return
            //if there is no taint-tracking involved (newRhs == null), we do not have to do anything here

            val indexOfInterest = (0..invokeExpr.argCount - 1)
                    .firstOrNull { newRhs.variableName == invokeExpr.getArg(it).toString() }
                    ?: -1

            if (indexOfInterest == -1)
                return

            for (unit in body.units) {
                if (unit is IdentityStmt) {
                    val identity = unit
                    val rhs = identity.rightOp
                    if (rhs is ParameterRef) {
                        if (rhs.index == indexOfInterest) {
                            val lhs = identity.leftOp
                            val newLhs = createNewBindingForValue(lhs) ?: continue
                            addValueBindingToVariableDeclaration(lhs, newLhs)
                            val simpleAssignment = SMTSimpleAssignment(newLhs, SMTBindingValue(newRhs))
                            val assignmentAssert = SMTAssertStatement(simpleAssignment)
                            addAssertStmtToAllPrograms(assignmentAssert)
                        }
                    }
                }
            }
        } else {
            System.err.println(String.format("Double-Check if the following method contains useful information which can be extracted: \n%s", stmt))
        }

    }


    override fun caseAssignStmt(stmt: AssignStmt) {
        val leftOp = stmt.leftOp
        val rightOp = stmt.rightOp
        var rhs: SMTBinding? = null

        //        handling of boolean-assignments $a = 1 (aka $a = true)
        //        Since Jimple's representation of boolean assignments is actually an integer assignment of
        //        either 0 or 1, we have to treat this assignment in a specific way.
        //        Assumption: If the previous statement of the current statement is a boolean assignment and
        //        the current statement is an integer-assignment, the integer-assignment is a boolean assignment
        //        since we manually insert the boolean (integer) assignment, this is a valuable assumption.
        //        boolean assignmentProcessed = false;
        //        int currentIndex = jimpleDataFlowStatements.indexOf(stmt);
        //        if(currentIndex > 0) {
        //            Stmt predecessor = jimpleDataFlowStatements.get(currentIndex-1);
        //
        //            if(predecessor instanceof AssignStmt) {
        //                AssignStmt predAssign = (AssignStmt)predecessor;
        //                if(predAssign.getLeftOp().getType() instanceof BooleanType) {
        //                    assignmentProcessed = true;
        //
        //                    SMTConstantValue boolValue;
        //                    if(rightOp instanceof IntConstant) {
        //                        IntConstant intValue = (IntConstant)rightOp;
        //                        if(intValue.value == 0)
        //                            boolValue = new SMTConstantValue<Boolean>(false);
        //                        else if(intValue.value == 1)
        //                            boolValue = new SMTConstantValue<Boolean>(true);
        //                        else
        //                            throw new RuntimeException("This should not happen");
        //                    }
        //                    else
        //                        throw new RuntimeException("This should not happen");
        //
        //                    //since we manually added (lhs = 0 or lhs = 1), there has to be a lhs!
        //                    SMTBinding lhs = getLatestBindingForValue(leftOp);
        //
        //                    SMTSimpleAssignment simpleAss = new SMTSimpleAssignment(lhs, boolValue);
        //                    SMTAssertStatement assertStmt = new SMTAssertStatement(simpleAss);
        //                    addAssertStmtToAllPrograms(assertStmt);
        //                }
        //            }
        //        }
        //
        //        if(assignmentProcessed == false) {
        //treatment of right side
        if (rightOp is Expr) {
            exprVisitor.setCurrentStatement(stmt)
            rightOp.apply(exprVisitor)
            rhs = exprVisitor.result
        } else if (rightOp is Local) {
            if (hasBindingForValue(rightOp))
                rhs = getLatestBindingForValue(rightOp)
            else {
                System.err.println("###### Doulbe-Check this here... #######")
                rhs = createNewBindingForValue(rightOp)
            }
        } else if (rightOp is ArrayRef) {
            val arrayRHS = rightOp
            val arrayBinding = getCorrectBindingForArrayRef(arrayRHS)
            if (arrayBinding != null)
                rhs = arrayBinding
            else {
                val bindingType = createBindingType(arrayRHS.type)
                rhs = createTemporalBinding(bindingType)
            }
        } else if (rightOp is FieldRef) {
            val field = rightOp
            if (hasBindingForField(field.field)) {
                rhs = getLatestBindingForField(field.field)
            } else {
                //base is tainted
                //=> just propagate the taint value of previous statement
                val prevStmt = getPreviousDataFlowPathElement(stmt)
                if (prevStmt == null)
                    throw RuntimeException("there is no previous statement")
                else {
                    rhs = getBindingForTaintedValue(prevStmt)
                    if (rhs == null)
                        throw RuntimeException("double check this here")
                }
            }
        } else if (rightOp is Constant) {
            if (rightOp is IntConstant) {
                //special treatment if int constant is a placeholder for a boolean true or false:
                if (leftOp.type is BooleanType) {
                    val boolValue: SMTConstantValue<*>

                    val intValue = rightOp
                    if (intValue.value == 0)
                        boolValue = SMTConstantValue(false)
                    else if (intValue.value == 1)
                        boolValue = SMTConstantValue(true)
                    else
                        throw RuntimeException("This should not happen")

                    val lhs = getLatestBindingForValue(leftOp)

                    val simpleAss = SMTSimpleAssignment(lhs, boolValue)
                    val assertStmt = SMTAssertStatement(simpleAss)
                    addAssertStmtToAllPrograms(assertStmt)

                    //we already treated the lhs => we can return here
                    return
                }
            }
        } else
            throw RuntimeException("todo")


        //treatement of left side
        //condition treatment
        if (rhs != null && rhs.type == SMTBinding.TYPE.Bool) {
            val lhs = createNewBindingForValue(leftOp) ?: return
            addValueBindingToVariableDeclaration(leftOp, lhs)

            val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(rhs))
            val assertStmt = SMTAssertStatement(simpleAss)
            addAssertStmtToAllPrograms(assertStmt)


            //            List<Stmt> dataFlow = this.jimpleDataFlowStatements;
            //            int currentPosInDataFlow = dataFlow.indexOf(stmt);
            //            Set<Boolean> possibleConditions = null;
            //            //in case we do have a conditional statement on the dataflow path, but is is not the last one,
            //            //we have to check whether this conditional statement has to be set to true or false in order
            //            //to reach the next statement after the conditonal statement.
            //            if(currentPosInDataFlow < dataFlow.size()-1) {
            //                Set<Unit> nextDataFlowStmt = new HashSet<Unit>();
            //                nextDataFlowStmt.add(dataFlow.get(currentPosInDataFlow+1));
            //                possibleConditions = UtilSMT.extractConditionForReachingAUnit(cfg, stmt, nextDataFlowStmt);
            //            }
            //            //if the conditional statement is the last statement on the dataflow path, we have to get the
            //            //condition how one reaches the target statement
            //            else
            //                possibleConditions = UtilSMT.extractConditionForReachingAUnit(cfg, stmt, targetUnits);
            //
            //            if(possibleConditions.size() == 0) {
            //                System.err.println("### Double-check this here... ###");
            //            }
            //            //there is only one possible condition for reaching the logging point
            //            else if(possibleConditions.size() == 1) {
            //                boolean concreteCondition = possibleConditions.iterator().next();
            //                SMTSimpleAssignment assignment = null;
            //                if(concreteCondition == true)
            //                    //rhs is now lhs
            //                    assignment = new SMTSimpleAssignment(rhs, new SMTConstantValue<Boolean>(true));
            //                else
            //                    //rhs is now lhs
            //                    assignment = new SMTSimpleAssignment(rhs, new SMTConstantValue<Boolean>(false));
            //                SMTAssertStatement assignAssert = new SMTAssertStatement(assignment);
            //                addAssertStmtToAllPrograms(assignAssert);
            //            }
            //            //logging point can be reached either on the this-branch or the else-branch
            //            else {
            //                doubleSMTPrograms();
            //                addingTrueAndFalseConditionalAssertions(rhs);
            //            }
        } else if (leftOp.type is ArrayType) {
            if (rhs == null) {
                //there is nothing to track
                return
            } else {
                LogHelper.logWarning("Arrays are not supported yet")
            }
        } else if (leftOp is ArrayRef) {
            throw RuntimeException("SMTConverter: ArrayRef not implemented yet")
        } else if (leftOp is FieldRef) {
            val field = leftOp
            val fieldBinding = createNewBindingForField(field.field)
            addFieldBindingToVariableDeclaration(field.field, fieldBinding)

            val simpleAss = SMTSimpleAssignment(fieldBinding, SMTBindingValue(rhs))
            val assertStmt = SMTAssertStatement(simpleAss)
            addAssertStmtToAllPrograms(assertStmt)
        } else {
            val lhs = createNewBindingForValue(leftOp) ?: return
            addValueBindingToVariableDeclaration(leftOp, lhs)

            val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(rhs))
            val assertStmt = SMTAssertStatement(simpleAss)
            addAssertStmtToAllPrograms(assertStmt)
        }//e.g. a[0] = "aa"
        //e.g. String[] a = getArray()
        //        }


    }


    fun getCorrectBindingForArrayRef(arrayRef: ArrayRef): SMTBinding? {
        for ((key, value) in arrayHelper) {
            if (key == arrayRef.toString())
                return value
        }

        return null
    }


    fun createBindingType(type: Type): SMTBinding.TYPE {
        if (type is RefType) {
            val refType = type
            if (refType.className == "java.lang.String" || refType.className == "java.lang.StringBuilder")
                return SMTBinding.TYPE.String
            else
                return findProperTypeFor(type)
        } else if (type is ArrayType) {
            val arrayType = type
            if (arrayType.baseType is RefType) {
                val baseTypeRef = arrayType.baseType as RefType
                if (baseTypeRef.className == "java.lang.String")
                    return SMTBinding.TYPE.String
                else
                    throw RuntimeException("todo")
            } else
                throw RuntimeException("todo")
        } else if (type is IntType) {
            return SMTBinding.TYPE.Int
        } else if (type is BooleanType)
            return SMTBinding.TYPE.Bool
        else if (type is DoubleType)
            return SMTBinding.TYPE.Real
        else if (type is LongType)
            return SMTBinding.TYPE.Int
        else
            throw RuntimeException("todo")
    }


    private fun findProperTypeFor(type: Type): SMTBinding.TYPE {
        if (this.tmpSmtProgram.assertStatements != null && this.tmpSmtProgram.assertStatements.size > 0) {
            val lastIndex = this.tmpSmtProgram.assertStatements.size - 1
            val smtStmt = this.tmpSmtProgram.assertStatements[lastIndex].statement
            if (smtStmt is SMTAssignment) {
                return smtStmt.lhs.type
            } else
                throw NotYetSupportedException("type " + type.toString() + " not supported yet")
        } else
            throw NotYetSupportedException("type " + type.toString() + " not supported yet")
    }


    val lhsOfLastAssertStmt: SMTBinding
        get() {
            if (this.tmpSmtProgram.assertStatements != null && this.tmpSmtProgram.assertStatements.size > 0) {
                val lastIndex = this.tmpSmtProgram.assertStatements.size - 1
                val smtStmt = this.tmpSmtProgram.assertStatements[lastIndex].statement
                if (smtStmt is SMTAssignment) {
                    return smtStmt.lhs
                }
            }
            throw RuntimeException("wrong assumption")
        }


    val taintedValueTypeAtSink: SMTBinding.TYPE
        get() {
            val sinkStmt = jimpleDataFlowStatements[jimpleDataFlowStatements.size - 1]
            val taintBinding = getBindingForTaintedValue(sinkStmt) ?: throw RuntimeException("double check this")

            return taintBinding.type
        }


    fun getBindingForTaintedValue(stmt: Stmt): SMTBinding? {
        if (stmt.containsInvokeExpr()) {
            val invokeExpr = stmt.invokeExpr
            val sootMethod = invokeExpr.method
            //special handling of non-api calls:
            //the tainted value provided by flowdroid is actually the identity statement in the body and not the
            //argument, but we need the argument in this case. Therefore, we have to identify the correct tainted argument
            if (InstrumentUtil.isAppDeveloperCode(sootMethod.declaringClass)) {
                val accessPath = getCorrectAccessPathForStmt(stmt)
                val identityStmtTaintValue = accessPath.plainValue

                if (identityStmtTaintValue != null) {
                    val body = sootMethod.retrieveActiveBody()
                    for (unit in body.units) {
                        if (unit is IdentityStmt) {
                            val identity = unit
                            val rhs = identity.rightOp
                            if (rhs is ParameterRef) {
                                if (identity.leftOp === identityStmtTaintValue) {
                                    val index = rhs.index
                                    val argument = invokeExpr.getArg(index)
                                    if (hasBindingForValue(argument))
                                        return getLatestBindingForValue(argument)
                                    else
                                        return createNewBindingForValue(argument)
                                }
                            } else if (rhs is ThisRef) {
                                if (identity.leftOp === identityStmtTaintValue) {
                                    if (invokeExpr is InstanceInvokeExpr) {
                                        val base = invokeExpr.base
                                        if (hasBindingForValue(base))
                                            return getLatestBindingForValue(base)
                                        else
                                            return createNewBindingForValue(base)
                                    } else
                                        throw RuntimeException("this should not happen...")
                                }
                            }
                        }
                    }
                    throw RuntimeException("There should be an identity statement!")
                } else {
                    return null
                }
            } else {
                val accessPath = getCorrectAccessPathForStmt(stmt)
                val taintValue = accessPath.plainValue
                if (hasBindingForValue(taintValue))
                    return getLatestBindingForValue(taintValue)
                else {
                    return createNewBindingForValue(taintValue)
                }
            }
        } else {
            val accessPath = getCorrectAccessPathForStmt(stmt)
            val taintValue = accessPath.plainValue
            if (hasBindingForValue(taintValue))
                return getLatestBindingForValue(taintValue)
            else {
                return createNewBindingForValue(taintValue)
            }
        }
    }


    fun getCorrectAccessPathForStmt(stmt: Stmt): AccessPath {
        this.jimpleDataFlowStatements.indices
                .filter { this.jimpleDataFlowStatements[it] === stmt }
                .forEach { return this.accessPathPath[it] }
        throw RuntimeException("There should be a statement in the data flow path")
    }


    fun addArrayRef(arrayWithIndex: String, value: SMTBinding) {
        this.arrayHelper.put(arrayWithIndex, value)
    }


    fun getPreviousDataFlowPathElement(currentStmt: Stmt): Stmt? {
        if (jimpleDataFlowStatements.contains(currentStmt)) {
            val index = jimpleDataFlowStatements.indexOf(currentStmt)
            if (index == 0)
                return null
            else
                return jimpleDataFlowStatements[index - 1]
        } else {
            return null
        }
    }

    override fun caseIdentityStmt(stmt: IdentityStmt) {
        val leftOp = stmt.leftOp
        val rightOp = stmt.rightOp
        var rhs: SMTBinding? = null


        if (rightOp is Local) {
            if (hasBindingForValue(rightOp))
                rhs = getLatestBindingForValue(rightOp)
            else {
                rhs = createNewBindingForValue(rightOp) ?: return
                addValueBindingToVariableDeclaration(rightOp, rhs)
            }
        } else if (rightOp is ArrayRef) {
            val arrayRHS = rightOp
            val arrayBinding = getCorrectBindingForArrayRef(arrayRHS)
            if (arrayBinding != null)
                rhs = arrayBinding
            else {
                val bindingType = createBindingType(arrayRHS.type)
                rhs = createTemporalBinding(bindingType)
            }
        } else if (rightOp is FieldRef) {
            val field = rightOp
            if (hasBindingForField(field.field)) {
                rhs = getLatestBindingForField(field.field)
            } else {
                //base is tainted
                //=> just propagate the taint value of previous statement
                val prevStmt = getPreviousDataFlowPathElement(stmt)
                if (prevStmt == null)
                    throw RuntimeException("there is no previous statement")
                else {
                    rhs = getBindingForTaintedValue(prevStmt)
                    if (rhs == null)
                        throw RuntimeException("double check this here")
                }
            }
        } else if (rightOp is Constant) {
            if (rightOp is IntConstant) {
                //special treatment if int constant is a placeholder for a boolean true or false:
                if (leftOp.type is BooleanType) {
                    val boolValue: SMTConstantValue<*>

                    val intValue = rightOp
                    if (intValue.value == 0)
                        boolValue = SMTConstantValue(false)
                    else if (intValue.value == 1)
                        boolValue = SMTConstantValue(true)
                    else
                        throw RuntimeException("This should not happen")

                    val lhs = getLatestBindingForValue(leftOp)

                    val simpleAss = SMTSimpleAssignment(lhs, boolValue)
                    val assertStmt = SMTAssertStatement(simpleAss)
                    addAssertStmtToAllPrograms(assertStmt)

                    //we already treated the lhs => we can return here
                    return
                }
            }
        } else if (rightOp is ThisRef) {
            rhs = createNewBindingForThisRef(rightOp)
        } else if (rightOp is CaughtExceptionRef) {
            rhs = createTemporalBinding(TYPE.String)
        } else if (rightOp is ParameterRef) {
            //base is tainted
            //=> just propagate the taint value of previous statement
            val prevStmt = getPreviousDataFlowPathElement(stmt)
            if (prevStmt == null)
                throw RuntimeException("there is no previous statement")
            else {
                rhs = getBindingForTaintedValue(prevStmt)
                if (rhs == null)
                    throw RuntimeException("double check this here")
            }
        } else
            throw RuntimeException("todo")//e.g. @parameter0: com.a.a.AR

        val lhs = createNewBindingForValue(leftOp) ?: return
        addValueBindingToVariableDeclaration(leftOp, lhs)

        val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(rhs))
        val assertStmt = SMTAssertStatement(simpleAss)
        addAssertStmtToAllPrograms(assertStmt)
    }

    override fun caseEnterMonitorStmt(stmt: EnterMonitorStmt) {
        throw RuntimeException("todo")

    }

    override fun caseExitMonitorStmt(stmt: ExitMonitorStmt) {
        throw RuntimeException("todo")

    }

    override fun caseGotoStmt(stmt: GotoStmt) {
        throw RuntimeException("todo")

    }

    override fun caseIfStmt(stmt: IfStmt) {
        throw RuntimeException("todo")

    }

    override fun caseLookupSwitchStmt(stmt: LookupSwitchStmt) {
        throw RuntimeException("todo")

    }

    override fun caseNopStmt(stmt: NopStmt) {
        throw RuntimeException("todo")

    }

    override fun caseRetStmt(stmt: RetStmt) {
        throw RuntimeException("todo")

    }

    override fun caseReturnStmt(stmt: ReturnStmt) {
        //in case of return CONSTANT, we do nothing; unfortunately, this is part of FlowDroid's path
        if (stmt.op is Constant)
            return
        val index = jimpleDataFlowStatements.indexOf(stmt)
        val ap = accessPathPath[index]
        val local = ap.plainValue

        val lhs = createNewBindingForValue(local) ?: return
        addValueBindingToVariableDeclaration(local, lhs)

        if (!hasBindingForValue(stmt.op))
            throw RuntimeException("There has to be a tainted value")
        val rhs = getLatestBindingForValue(stmt.op)

        val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(rhs))
        val assertStmt = SMTAssertStatement(simpleAss)
        addAssertStmtToAllPrograms(assertStmt)
    }

    override fun caseReturnVoidStmt(stmt: ReturnVoidStmt) {
        //do nothing
        return
    }

    override fun caseTableSwitchStmt(stmt: TableSwitchStmt) {
        throw RuntimeException("todo")

    }

    override fun caseThrowStmt(stmt: ThrowStmt) {
        throw RuntimeException("todo")

    }

    override fun defaultCase(obj: Any) {
        throw RuntimeException("todo")
    }

    fun getDynamicValueInfos(): Map<SMTProgram, Set<DynamicValueInformation>> {
        return dynamicValueInfo
    }


    fun addNewDynamicValueForBaseObjectToMap(stmt: Stmt, binding: SMTBinding?) {
        if (binding == null)
            return

        if (!dynamicValueInfo.keys.contains(currentSMTProgram))
            dynamicValueInfo.put(currentSMTProgram, HashSet<DynamicValueInformation>())

        val dynValueInfo = DynamicValueInformation(stmt, binding)
        dynValueInfo.isBaseObject = true

        dynamicValueInfo[currentSMTProgram]!!.add(dynValueInfo)
    }


    fun addNewDynamicValueForArgumentToMap(stmt: Stmt, binding: SMTBinding?, argPos: Int) {
        if (binding == null)
            return

        if (!dynamicValueInfo.keys.contains(currentSMTProgram))
            dynamicValueInfo.put(currentSMTProgram, HashSet<DynamicValueInformation>())

        val dynValueInfo = DynamicValueInformation(stmt, binding)
        dynValueInfo.isBaseObject = false
        dynValueInfo.argPos = argPos

        dynamicValueInfo[currentSMTProgram]!!.add(dynValueInfo)
    }

}
