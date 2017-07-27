package me.zhenhao.forced.decisionmaker.analysis.symbolicexecution

import java.util.HashSet

import me.zhenhao.forced.decisionmaker.analysis.smartconstantdataextractor.NotYetSupportedException
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTAssertStatement
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBinding
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBindingValue
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTBooleanEqualsAssignment
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTComplexBinaryOperation
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTComplexBinaryOperation.SMTComplexBinaryOperator
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTConcatMethodCall
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTConstantValue
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTContainsMethodCall
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTIndexOfMethodCall
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTLengthMethodCall
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTMethodAssignment
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTRegexDigitOperation
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTSimpleAssignment
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTSimpleBinaryOperation
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTStartsWithMethodCall
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTStatement
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTSubstringMethodCall
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTUnaryOperation
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTUnaryOperation.SMTUnaryOperator
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTUnaryOperationAssignment
import me.zhenhao.forced.decisionmaker.analysis.symbolicexecution.datastructure.SMTValue
import soot.Value
import soot.jimple.AddExpr
import soot.jimple.AndExpr
import soot.jimple.ArrayRef
import soot.jimple.AssignStmt
import soot.jimple.CastExpr
import soot.jimple.CmpExpr
import soot.jimple.CmpgExpr
import soot.jimple.CmplExpr
import soot.jimple.DivExpr
import soot.jimple.DynamicInvokeExpr
import soot.jimple.EqExpr
import soot.jimple.ExprSwitch
import soot.jimple.GeExpr
import soot.jimple.GtExpr
import soot.jimple.InstanceOfExpr
import soot.jimple.IntConstant
import soot.jimple.InterfaceInvokeExpr
import soot.jimple.InvokeExpr
import soot.jimple.LeExpr
import soot.jimple.LengthExpr
import soot.jimple.LtExpr
import soot.jimple.MulExpr
import soot.jimple.NeExpr
import soot.jimple.NegExpr
import soot.jimple.NewArrayExpr
import soot.jimple.NewExpr
import soot.jimple.NewMultiArrayExpr
import soot.jimple.OrExpr
import soot.jimple.RemExpr
import soot.jimple.ShlExpr
import soot.jimple.ShrExpr
import soot.jimple.SpecialInvokeExpr
import soot.jimple.StaticInvokeExpr
import soot.jimple.Stmt
import soot.jimple.StringConstant
import soot.jimple.SubExpr
import soot.jimple.UshrExpr
import soot.jimple.VirtualInvokeExpr
import soot.jimple.XorExpr
import soot.jimple.infoflow.source.data.SourceSinkDefinition

internal class JimpleExprVisitorImpl(//all source definitions
        private val sources: Set<SourceSinkDefinition>, private val stmtVisitor: JimpleStmtVisitorImpl) : ExprSwitch {
    var result: SMTBinding? = null
    lateinit private var currentStatement: Stmt

    override fun caseAddExpr(v: AddExpr) {
        val op1 = v.op1
        val op2 = v.op2

        if (stmtVisitor.hasBindingForValue(op1)) {
            this.result = stmtVisitor.getLatestBindingForValue(op1)
        } else if (stmtVisitor.hasBindingForValue(op2)) {
            this.result = stmtVisitor.getLatestBindingForValue(op2)
        } else
            throw RuntimeException("This should not happen...")

    }

    override fun caseAndExpr(v: AndExpr) {
        throw RuntimeException("todo")

    }

    override fun caseCmpExpr(v: CmpExpr) {
        throw RuntimeException("todo")

    }

    override fun caseCmpgExpr(v: CmpgExpr) {
        throw RuntimeException("todo")

    }

    override fun caseCmplExpr(v: CmplExpr) {
        throw RuntimeException("todo")

    }

    override fun caseDivExpr(v: DivExpr) {
        throw RuntimeException("todo")

    }

    override fun caseEqExpr(v: EqExpr) {
        throw RuntimeException("todo")

    }

    override fun caseNeExpr(v: NeExpr) {
        throw RuntimeException("todo")

    }

    override fun caseGeExpr(v: GeExpr) {
        throw RuntimeException("todo")

    }

    override fun caseGtExpr(v: GtExpr) {
        throw RuntimeException("todo")

    }

    override fun caseLeExpr(v: LeExpr) {
        throw RuntimeException("todo")

    }

    override fun caseLtExpr(v: LtExpr) {
        throw RuntimeException("todo")

    }

    override fun caseMulExpr(v: MulExpr) {
        throw RuntimeException("todo")

    }

    override fun caseOrExpr(v: OrExpr) {
        throw RuntimeException("todo")

    }

    override fun caseRemExpr(v: RemExpr) {
        throw RuntimeException("todo")

    }

    override fun caseShlExpr(v: ShlExpr) {
        throw RuntimeException("todo")

    }

    override fun caseShrExpr(v: ShrExpr) {
        throw RuntimeException("todo")

    }

    override fun caseUshrExpr(v: UshrExpr) {
        throw RuntimeException("todo")

    }

    override fun caseSubExpr(v: SubExpr) {
        throw RuntimeException("todo")

    }

    override fun caseXorExpr(v: XorExpr) {
        throw RuntimeException("todo")

    }

    override fun caseInterfaceInvokeExpr(v: InterfaceInvokeExpr) {
        if (isSourceMethod(v)) {
            val newSourceValue = StringConstant.v("loggingPoint")
            val binding = stmtVisitor.createNewBindingForValue(newSourceValue)
            stmtVisitor.addValueBindingToVariableDeclaration(newSourceValue, binding)
            //no smt-statement required, just return the binding
            this.result = binding

            // Additionally check whether the source method need special treatment
            if (isExpressionThatNeedsToBeConvertedToSMT(v)) {
                convertSpecialExpressionsToSMT(v, currentStatement)
            }
        } else if (isExpressionThatNeedsToBeConvertedToSMT(v)) {
            convertSpecialExpressionsToSMT(v, currentStatement)
        } else {
            //just propagate the taint value of previous statement
            val prevStmt = stmtVisitor.getPreviousDataFlowPathElement(currentStatement)
            if (prevStmt == null)
                throw RuntimeException("there is no previous statement")
            else {
                this.result = stmtVisitor.getBindingForTaintedValue(prevStmt)
                if (this.result == null)
                    throw RuntimeException("double check this here")
            }
        }
    }

    override fun caseSpecialInvokeExpr(v: SpecialInvokeExpr) {
        //is the invokeExpr a source method?
        if (isSourceMethod(v)) {
            val newSourceValue = StringConstant.v("loggingPoint")
            val binding = stmtVisitor.createNewBindingForValue(newSourceValue)
            stmtVisitor.addValueBindingToVariableDeclaration(newSourceValue, binding)
            //no smt-statement required, just return the binding
            this.result = binding

            // Additionally check whether the source method need special treatment
            if (isExpressionThatNeedsToBeConvertedToSMT(v)) {
                convertSpecialExpressionsToSMT(v, currentStatement)
            }

        } else {
            if (isStringOperationSupportedBySMT(v))
                convertStringOperationToSMT(v, v.base)
            else if (isExpressionThatNeedsToBeConvertedToSMT(v))
                convertSpecialExpressionsToSMT(v, currentStatement)
            else
                convertAPIMethodToSMT(v)
        }
    }

    override fun caseStaticInvokeExpr(v: StaticInvokeExpr) {
        //just propagate the taint value of previous statement
        val prevStmt = stmtVisitor.getPreviousDataFlowPathElement(currentStatement)
        if (prevStmt == null)
            throw RuntimeException("there is no previous statement")
        else {
            //create an assignment between the incoming taint-value and the
            //assigned taint-value inside the method

            val bindingPrevStmt = stmtVisitor.getBindingForTaintedValue(prevStmt) ?: return
            //if there is no taint-tracking involved, we do not have to create an SMT formula

            val accessPath = stmtVisitor.getCorrectAccessPathForStmt(currentStatement)
            val identityStmtTaintValue = accessPath.plainValue

            var bindingCurentStmt: SMTBinding? = stmtVisitor.getLatestBindingForValue(identityStmtTaintValue)
            if (bindingCurentStmt == null) {
                bindingCurentStmt = stmtVisitor.createNewBindingForValue(identityStmtTaintValue)
                stmtVisitor.addValueBindingToVariableDeclaration(identityStmtTaintValue, bindingCurentStmt)
            }

            if (bindingCurentStmt !== bindingPrevStmt) {
                val simpleAssignForTaintProp = SMTSimpleAssignment(bindingCurentStmt, SMTBindingValue(bindingPrevStmt))
                val simpleAssignAssert = SMTAssertStatement(simpleAssignForTaintProp)
                stmtVisitor.addAssertStmtToAllPrograms(simpleAssignAssert)
            }

            this.result = bindingCurentStmt
        }
    }

    override fun caseDynamicInvokeExpr(v: DynamicInvokeExpr) {
        //        caseInvokeExpr(v, null);
        throw RuntimeException("todo")
    }

    override fun caseCastExpr(v: CastExpr) {
        //just propagate the taint value of previous statement
        val prevStmt = stmtVisitor.getPreviousDataFlowPathElement(currentStatement)
        if (prevStmt == null)
            throw RuntimeException("there is no previous statement")
        else {
            this.result = stmtVisitor.getBindingForTaintedValue(prevStmt)
            if (this.result == null)
                throw RuntimeException("double check this here")
        }
    }

    override fun caseInstanceOfExpr(v: InstanceOfExpr) {
        throw RuntimeException("todo")

    }

    override fun caseNewArrayExpr(v: NewArrayExpr) {
        throw RuntimeException("todo")

    }

    override fun caseNewMultiArrayExpr(v: NewMultiArrayExpr) {
        throw RuntimeException("todo")

    }

    override fun caseNewExpr(v: NewExpr) {
        throw RuntimeException("todo")

    }

    override fun caseLengthExpr(v: LengthExpr) {
        throw RuntimeException("todo")

    }

    override fun caseNegExpr(v: NegExpr) {
        throw RuntimeException("todo")

    }

    override fun defaultCase(obj: Any) {
        throw RuntimeException("todo")

    }

    override fun caseVirtualInvokeExpr(virtualInvokeExpr: VirtualInvokeExpr) {
        //is the invokeExpr a source method?
        if (isSourceMethod(virtualInvokeExpr)) {
            val newSourceValue = StringConstant.v("loggingPoint")
            val binding = stmtVisitor.createNewBindingForValue(newSourceValue)
            stmtVisitor.addValueBindingToVariableDeclaration(newSourceValue, binding)
            //no smt-statement required, just return the binding
            this.result = binding

            // Additionally check whether the source method need special treatment
            if (isExpressionThatNeedsToBeConvertedToSMT(virtualInvokeExpr)) {
                convertSpecialExpressionsToSMT(virtualInvokeExpr, currentStatement)
            }

        } else {
            if (isStringOperationSupportedBySMT(virtualInvokeExpr))
                convertStringOperationToSMT(virtualInvokeExpr, virtualInvokeExpr.base)
            else if (isExpressionThatNeedsToBeConvertedToSMT(virtualInvokeExpr))
                convertSpecialExpressionsToSMT(virtualInvokeExpr, currentStatement)
            else
                convertAPIMethodToSMT(virtualInvokeExpr)
        }
    }


    private fun convertStringOperationToSMT(invokeExpr: InvokeExpr, base: Value) {
        val methodSignature = invokeExpr.method.signature

        if (methodSignature == "<java.lang.String: java.lang.String substring(int,int)>" || methodSignature == "<java.lang.String: java.lang.String substring(int)>")
            generateSMTSubstringStmt(invokeExpr, base)
        else if (methodSignature == "<java.lang.String: boolean equals(java.lang.Object)>"
                || methodSignature == "<java.lang.String: boolean equalsIgnoreCase(java.lang.String)>"
                || methodSignature == "<java.lang.String: boolean matches(java.lang.String)>")
            generateSMTEqualStmt(invokeExpr, base)
        else if (methodSignature == "<java.lang.String: int indexOf(java.lang.String)>")
            generateSMTIndexOfStmt(invokeExpr, base)
        else if (methodSignature == "<java.lang.String: int indexOf(int,int)>")
            throw NotYetSupportedException("indexOf(int,int) not supported yet")
        else if (methodSignature == "<java.lang.String: boolean startsWith(java.lang.String)>")
            generateSMTStartsWithStmt(invokeExpr, base)
        else if (methodSignature == "<java.lang.String: boolean contains(java.lang.CharSequence)>" || methodSignature == "<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>")
            generateSMTContainsStmt(invokeExpr, base)
        else if (methodSignature == "<java.lang.String: java.lang.String[] split(java.lang.String)>")
            generateSMTSplitStmt(invokeExpr, base)
        else if (methodSignature == "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>")
            generateSMTAppendStmt(invokeExpr, base)
        else {
            throw RuntimeException("todo")
        }//            stmtVisitor.notSupported = true;
    }


    private fun generateSMTSubstringStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.substring(b,c) and a.substring(b) treatment ##############
        //(= t (substring a b (Length a)) )

        //treatment of lhs
        val lhs = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String)

        //base treatment
        val baseBinding: SMTBinding?
        if (stmtVisitor.hasBindingForValue(base))
            baseBinding = stmtVisitor.getLatestBindingForValue(base)
        else {
            baseBinding = stmtVisitor.createNewBindingForValue(base)
            stmtVisitor.addValueBindingToVariableDeclaration(base, baseBinding)
        }
        val stringValue = SMTBindingValue(baseBinding)

        //treatement of rhs
        //treatment of first argument
        val fromIndex = invokeExpr.getArg(0)
        val fromValue: SMTValue?
        if (fromIndex is IntConstant) {
            fromValue = SMTConstantValue(fromIndex.value)
        } else if (stmtVisitor.hasBindingForValue(fromIndex)) {
            val from = stmtVisitor.getLatestBindingForValue(fromIndex)
            fromValue = SMTBindingValue(from)

        } else {
            System.err.println("###### Doulbe-Check this here... #######")
            val tmpBinding = stmtVisitor.createNewBindingForValue(fromIndex)
            stmtVisitor.addValueBindingToVariableDeclaration(fromIndex, tmpBinding)
            fromValue = SMTBindingValue(tmpBinding)
        }

        var toValue: SMTValue? = null
        //in case we do only have a startsAt argument and the endAt is the length of the string:
        if (invokeExpr.argCount == 1) {
            //to treatment
            //first: (assert (= length (Length a) ))
            val length = SMTLengthMethodCall(SMTBindingValue(baseBinding))
            val tmpBinding = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
            val lengthMethodAssignment = SMTMethodAssignment(tmpBinding, length)
            //second: (assert (= sublength (- length b )  ) )
            val unaryOperation = SMTUnaryOperation(SMTUnaryOperator.Minus, SMTBindingValue(tmpBinding), fromValue)
            val lengthBindingInt = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
            val unaryAssignment = SMTUnaryOperationAssignment(lengthBindingInt, unaryOperation)
            val unaryOperationAssert = SMTAssertStatement(unaryAssignment)

            val lengthAssert = SMTAssertStatement(lengthMethodAssignment)
            stmtVisitor.addAssertStmtToAllPrograms(lengthAssert)
            stmtVisitor.addAssertStmtToAllPrograms(unaryOperationAssert)

            toValue = SMTBindingValue(lengthBindingInt)
        } else if (invokeExpr.argCount == 2) {
            val toIndex = invokeExpr.getArg(1)
            if (toIndex is IntConstant) {
//SMT solver has length as second argument not to index
                if (fromIndex is IntConstant) {
                    val fromConstant = fromIndex.value
                    val substrLength = toIndex.value - fromConstant
                    toValue = SMTConstantValue(substrLength)
                } else {
                    val to = stmtVisitor.getLatestBindingForValue(toIndex)
                    toValue = SMTBindingValue(to)
                }
            } else if (stmtVisitor.hasBindingForValue(toIndex)) {
                val to = stmtVisitor.getLatestBindingForValue(toIndex)
                toValue = SMTBindingValue(to)
            } else {
                //to treatment
                //first: (assert (= length (Length a) ))
                val length = SMTLengthMethodCall(SMTBindingValue(baseBinding))
                val tmpBinding = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
                val lengthMethodAssignment = SMTMethodAssignment(tmpBinding, length)
                //second: (assert (= sublength (- length b )  ) )
                val unaryOperation = SMTUnaryOperation(SMTUnaryOperator.Minus, SMTBindingValue(tmpBinding), fromValue)
                val lengthBindingInt = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
                val unaryAssignment = SMTUnaryOperationAssignment(lengthBindingInt, unaryOperation)
                val unaryOperationAssert = SMTAssertStatement(unaryAssignment)

                val lengthAssert = SMTAssertStatement(lengthMethodAssignment)
                stmtVisitor.addAssertStmtToAllPrograms(lengthAssert)
                stmtVisitor.addAssertStmtToAllPrograms(unaryOperationAssert)

                toValue = SMTBindingValue(lengthBindingInt)

            }
        }//in case we do have a startsAt argument AND the endAt argument:


        val substring = SMTSubstringMethodCall(stringValue, fromValue, toValue)
        val methodAss = SMTMethodAssignment(lhs, substring)
        val substringAssert = SMTAssertStatement(methodAss)

        stmtVisitor.addAssertStmtToAllPrograms(substringAssert)

        this.result = lhs
    }


    private fun generateSMTEqualStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.equals(b), a.equalsIgnoreCase(b) and a.matches(b) treatment ##############
        //(= a b)

        //treatment of lhs
        val lhs: SMTBinding?
        if (stmtVisitor.hasBindingForValue(base))
            lhs = stmtVisitor.getLatestBindingForValue(base)
        else {
            lhs = stmtVisitor.createNewBindingForValue(base)
            //created a new binding => dynamic values are necessary here for improving the result
            if (lhs!!.version == 0) {
                stmtVisitor.addNewDynamicValueForBaseObjectToMap(currentStatement, lhs)
            }
            stmtVisitor.addValueBindingToVariableDeclaration(base, lhs)
        }

        //treatment of rhs
        val equalsCheck = invokeExpr.getArg(0)
        val smtArgumentValue: SMTValue?
        if (equalsCheck is StringConstant)
            smtArgumentValue = SMTConstantValue(equalsCheck.value)
        else {
            //no constant string available; there is maybe a need for dynamic information to improve the result
            val tmpBinding: SMTBinding?
            if (stmtVisitor.hasBindingForValue(equalsCheck))
                tmpBinding = stmtVisitor.getLatestBindingForValue(equalsCheck)
            else {
                tmpBinding = stmtVisitor.createNewBindingForValue(equalsCheck)
                stmtVisitor.addValueBindingToVariableDeclaration(equalsCheck, tmpBinding)
                //created a new binding => dynamic values are necessary here for improving the result
                stmtVisitor.addNewDynamicValueForArgumentToMap(currentStatement, tmpBinding, 0)
            }

            smtArgumentValue = SMTBindingValue(tmpBinding)
        }

        val outerLHS = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Bool)
        val booleanEqualsAssignment = SMTBooleanEqualsAssignment(outerLHS, SMTBindingValue(lhs), smtArgumentValue)
        val booleanEqualsnAssert = SMTAssertStatement(booleanEqualsAssignment)
        stmtVisitor.addAssertStmtToAllPrograms(booleanEqualsnAssert)

        // result is treated in JimpleStmtVisitor
        this.result = outerLHS
    }


    private fun generateSMTIndexOfStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.indexOf(b) treatment ##############
        //(= t (Indexof a b)

        //lhs treatment
        val lhs = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)

        //rhs treatment
        val indexOf = invokeExpr.getArg(0)
        val argumentValue: SMTValue?
        if (indexOf is StringConstant) {
            argumentValue = SMTConstantValue(indexOf.value)
        } else {
            val tmpBinding: SMTBinding?
            if (stmtVisitor.hasBindingForValue(indexOf))
                tmpBinding = stmtVisitor.getLatestBindingForValue(indexOf)
            else {
                tmpBinding = stmtVisitor.createNewBindingForValue(indexOf)
                stmtVisitor.addValueBindingToVariableDeclaration(indexOf, tmpBinding)
                stmtVisitor.addNewDynamicValueForArgumentToMap(currentStatement, tmpBinding, 0)
            }

            argumentValue = SMTBindingValue(tmpBinding)
        }

        //base treatment
        val baseBinding: SMTBinding?
        if (stmtVisitor.hasBindingForValue(base))
            baseBinding = stmtVisitor.getLatestBindingForValue(base)
        else {
            baseBinding = stmtVisitor.createNewBindingForValue(base)
            stmtVisitor.addValueBindingToVariableDeclaration(base, baseBinding)
            stmtVisitor.addNewDynamicValueForBaseObjectToMap(currentStatement, baseBinding)
        }

        val indexOfMethod = SMTIndexOfMethodCall(SMTBindingValue(baseBinding), argumentValue)
        val methodAssignment = SMTMethodAssignment(lhs, indexOfMethod)
        val assertStmt = SMTAssertStatement(methodAssignment)

        stmtVisitor.addAssertStmtToAllPrograms(assertStmt)

        this.result = lhs
    }


    private fun generateSMTStartsWithStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.startsWith(b) treatment ##############
        //(= t (StartsWith a b)

        //lhs treatment
        val lhs = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Bool)

        //rhs treatment
        val argumentValue = invokeExpr.getArg(0)
        val argumentSMTForm: SMTValue?
        if (argumentValue is StringConstant) {
            argumentSMTForm = SMTConstantValue(argumentValue.value)
        } else {
            val tmpBinding: SMTBinding?
            if (stmtVisitor.hasBindingForValue(argumentValue))
                tmpBinding = stmtVisitor.getLatestBindingForValue(argumentValue)
            else {
                tmpBinding = stmtVisitor.createNewBindingForValue(argumentValue)
                stmtVisitor.addValueBindingToVariableDeclaration(argumentValue, tmpBinding)
                stmtVisitor.addNewDynamicValueForArgumentToMap(currentStatement, tmpBinding, 0)
            }

            argumentSMTForm = SMTBindingValue(tmpBinding)
        }

        //base treatment
        val baseBinding: SMTBinding?
        if (stmtVisitor.hasBindingForValue(base))
            baseBinding = stmtVisitor.getLatestBindingForValue(base)
        else {
            baseBinding = stmtVisitor.createNewBindingForValue(base)
            stmtVisitor.addValueBindingToVariableDeclaration(base, baseBinding)
            stmtVisitor.addNewDynamicValueForBaseObjectToMap(currentStatement, baseBinding)
        }

        val startsWithMethod = SMTStartsWithMethodCall(SMTBindingValue(baseBinding), argumentSMTForm)
        val methodAss = SMTMethodAssignment(lhs, startsWithMethod)
        val assertStmt = SMTAssertStatement(methodAss)

        stmtVisitor.addAssertStmtToAllPrograms(assertStmt)
        this.result = lhs
    }


    private fun generateSMTContainsStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.contains(b), a.replaceAll(b, c) treatment ##############
        //(= t (Contains a b) )
        val lhs = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Bool)

        //rhs treatment
        val argumentValue = invokeExpr.getArg(0)
        val argumentSMTForm: SMTValue?
        if (argumentValue is StringConstant) {
            argumentSMTForm = SMTConstantValue(argumentValue.value)
        } else {
            val tmpBinding: SMTBinding?
            if (stmtVisitor.hasBindingForValue(argumentValue))
                tmpBinding = stmtVisitor.getLatestBindingForValue(argumentValue)
            else {
                tmpBinding = stmtVisitor.createNewBindingForValue(argumentValue)
                stmtVisitor.addValueBindingToVariableDeclaration(argumentValue, tmpBinding)
                stmtVisitor.addNewDynamicValueForArgumentToMap(currentStatement, tmpBinding, 0)
            }
            argumentSMTForm = SMTBindingValue(tmpBinding)
        }

        //base treatment
        val baseBinding: SMTBinding?
        if (stmtVisitor.hasBindingForValue(base))
            baseBinding = stmtVisitor.getLatestBindingForValue(base)
        else {
            baseBinding = stmtVisitor.createNewBindingForValue(base)
            stmtVisitor.addValueBindingToVariableDeclaration(base, baseBinding)
            stmtVisitor.addNewDynamicValueForBaseObjectToMap(currentStatement, baseBinding)
        }

        val containsMethod = SMTContainsMethodCall(SMTBindingValue(baseBinding), argumentSMTForm)
        val methodAss = SMTMethodAssignment(lhs, containsMethod)
        val assertStmt = SMTAssertStatement(methodAss)

        stmtVisitor.addAssertStmtToAllPrograms(assertStmt)
        this.result = lhs
    }


    private fun generateSMTAppendStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.append(b) treatment ##############
        //(= t (Concat a b) )

        //treatment of lhs
        val lhs = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String)

        //base treatment
        val baseBinding: SMTBinding?
        if (stmtVisitor.hasBindingForValue(base))
            baseBinding = stmtVisitor.getLatestBindingForValue(base)
        else {
            baseBinding = stmtVisitor.createNewBindingForValue(base)
            stmtVisitor.addValueBindingToVariableDeclaration(base, baseBinding)
            stmtVisitor.addNewDynamicValueForBaseObjectToMap(currentStatement, baseBinding)
        }

        //rhs treatment
        val argumentValue = invokeExpr.getArg(0)
        val argumentSMTForm: SMTValue?
        if (argumentValue is StringConstant) {
            argumentSMTForm = SMTConstantValue(argumentValue.value)
        } else {
            val tmpBinding: SMTBinding?
            if (stmtVisitor.hasBindingForValue(argumentValue))
                tmpBinding = stmtVisitor.getLatestBindingForValue(argumentValue)
            else {
                tmpBinding = stmtVisitor.createNewBindingForValue(argumentValue)
                stmtVisitor.addValueBindingToVariableDeclaration(argumentValue, tmpBinding)
                stmtVisitor.addNewDynamicValueForBaseObjectToMap(currentStatement, tmpBinding)
            }
            argumentSMTForm = SMTBindingValue(tmpBinding)
        }

        val concat = SMTConcatMethodCall(SMTBindingValue(baseBinding), argumentSMTForm)
        val conacatAss = SMTMethodAssignment(lhs, concat)
        val assertStmt = SMTAssertStatement(conacatAss)
        stmtVisitor.addAssertStmtToAllPrograms(assertStmt)

        this.result = lhs
    }


    private fun generateSMTSplitStmt(invokeExpr: InvokeExpr, base: Value) {
        //############## a.split(b) treatment ##############

        //split-element
        var splitter: String? = null
        if (invokeExpr.getArg(0) is StringConstant) {
            splitter = (invokeExpr.getArg(0) as StringConstant).value
        } else {
            System.err.println("###### Doulbe-Check this here... #######")
        }

        val currentDataFlow = this.stmtVisitor.jimpleDataFlowStatements
        val currentSplitAPIStatement = currentStatement

        //check if we have concrete information for the split API call
        if (this.stmtVisitor.splitInfos.contains(currentDataFlow, currentSplitAPIStatement)) {
            val allSplitInfos = this.stmtVisitor.splitInfos.get(currentDataFlow, currentSplitAPIStatement)
            //get first element
            val iterator = (allSplitInfos as MutableList).iterator()
            if (iterator.hasNext()) {
                val splitElements = iterator.next()

                var splitValue = ""
                for (i in splitElements.indices) {
                    val splitElem = splitElements[i]
                    splitValue += splitElem

                    if (i < splitElements.size - 1)
                        splitValue += splitter
                }

                iterator.remove()

                val taintedValue = stmtVisitor.getBindingForTaintedValue(currentStatement)

                val splitValueSMT = SMTConstantValue(splitValue)
                val simpleAss = SMTSimpleAssignment(taintedValue, splitValueSMT)
                val assertion = SMTAssertStatement(simpleAss)
                stmtVisitor.addAssertStmtToAllPrograms(assertion)
            }
        } else {

        }


        //        //base treatment
        //        SMTBinding baseBinding = null;
        //        if(stmtVisitor.hasBindingForValue(base))
        //            baseBinding = stmtVisitor.getLatestBindingForValue(base);
        //        else {
        //            System.err.println("###### Doulbe-Check this here... #######");
        //            baseBinding = stmtVisitor.createNewBindingForValue(base);
        //        }
        //
        //        Value argumentValue = invokeExpr.getArg(0);
        //        SMTValue argumentSMTForm = null;
        //        if(argumentValue instanceof StringConstant) {
        //            argumentSMTForm = new SMTConstantValue<String>(((StringConstant) argumentValue).value);
        //        }
        //        else {
        //            System.err.println("###### Doulbe-Check this here... #######");
        //            SMTBinding tmpBinding = stmtVisitor.createNewBindingForValue(argumentValue);
        //            stmtVisitor.addValueBindingToVariableDeclaration(argumentValue, tmpBinding);
        //            argumentSMTForm = new SMTBindingValue(tmpBinding);
        //        }
        //
        //        Value LHS = null;
        //        if(currentStatement instanceof AssignStmt){
        //            AssignStmt assignment = (AssignStmt) currentStatement;
        //            LHS = assignment.getLeftOp();
        //        }
        //        else
        //            throw new RuntimeException("LHS should not be null");
        //
        //        SMTBindingValue baseBindingValue = new SMTBindingValue(baseBinding);
        //        //FIRST:     (assert (= tmpString0 (Substring a 0 i0 )) )
        //        SMTBinding tmpLength0 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int);
        //        SMTBindingValue tmpLengthValue0 = new SMTBindingValue(tmpLength0);
        //        SMTSubstringMethodCall sub0 = new SMTSubstringMethodCall(baseBindingValue, new SMTConstantValue<Integer>(0), tmpLengthValue0);
        //        SMTBinding subString0 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String);
        //        //add subString0 to map
        //        String arrayElement0 = String.format("%s[0]", LHS.toString());
        //        stmtVisitor.addArrayRef(arrayElement0, subString0);
        //
        //        SMTMethodAssignment subAssign0 = new SMTMethodAssignment(subString0, sub0);
        //        SMTAssertStatement sub0Assert = new SMTAssertStatement(subAssign0);
        //        stmtVisitor.addAssertStmtToAllPrograms(sub0Assert);
        //        //SECOND:     (= tmpString2 (CharAt s i1) )
        //        //            (assert (= tmpString2 ":" ) )
        //        SMTCharAtMethodCall charAt0 = new SMTCharAtMethodCall(baseBindingValue, tmpLengthValue0);
        //        SMTBinding tmpCharAt = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String);
        //        SMTMethodAssignment charAtMethod0 = new SMTMethodAssignment(tmpCharAt, charAt0);
        //        SMTAssertStatement charAtAssert0 = new SMTAssertStatement(charAtMethod0);
        //        stmtVisitor.addAssertStmtToAllPrograms(charAtAssert0);
        //        SMTSimpleAssignment simpleAssignForCharAt = new SMTSimpleAssignment(tmpCharAt, argumentSMTForm);
        //        SMTAssertStatement simpleAssignAssert = new SMTAssertStatement(simpleAssignForCharAt);
        //        stmtVisitor.addAssertStmtToAllPrograms(simpleAssignAssert);
        //
        //        //THIRD:     (assert (= tmpString1 (Substring a i0 i1 )) )
        //        SMTBinding tmpLength1 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int);
        //        SMTBindingValue tmpLengthValue1 = new SMTBindingValue(tmpLength1);
        //        SMTSubstringMethodCall sub1 = new SMTSubstringMethodCall(baseBindingValue, tmpLengthValue0, tmpLengthValue1);
        //        SMTBinding subString1 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String);
        //        //add subString1 to map
        //        String arrayElement1 = String.format("%s[1]", LHS.toString());
        //        stmtVisitor.addArrayRef(arrayElement1, subString1);
        //
        //        SMTMethodAssignment subAssign1 = new SMTMethodAssignment(subString1, sub1);
        //        SMTAssertStatement sub1Assert = new SMTAssertStatement(subAssign1);
        //        stmtVisitor.addAssertStmtToAllPrograms(sub1Assert);
        //
        //        //FOURTH: (assert (> i1 i0 ) )
        //        SMTSimpleBinaryOperation gt = new SMTSimpleBinaryOperation(SMTSimpleBinaryOperator.GT, tmpLengthValue1, tmpLengthValue0);
        //        SMTAssertStatement gtAssert = new SMTAssertStatement(gt);
        //        stmtVisitor.addAssertStmtToAllPrograms(gtAssert);
        //
        //        int maxIndex = findMaxIndexOfArray(invokeExpr);
        //        SMTBindingValue tmpLengthValueAtN = tmpLengthValue1;
        //        for(int i = 2; i <= maxIndex; i++) {
        //
        //            //FIFTH:     (assert (= tmpStringn (Substring a iN iN+1 )) )
        //            SMTBinding tmpLengthNPLUS1 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int);
        //            SMTBindingValue tmpLengthValueNPLUS1 = new SMTBindingValue(tmpLengthNPLUS1);
        //            SMTSubstringMethodCall subN = new SMTSubstringMethodCall(baseBindingValue, tmpLengthValueAtN, tmpLengthValueNPLUS1);
        //            SMTBinding subStringN = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String);
        //
        //            //add subStringN to map
        //            String arrayElementN = String.format("%s[%d]", LHS.toString(), i);
        //            stmtVisitor.addArrayRef(arrayElementN, subStringN);
        //
        //            SMTMethodAssignment subAssignN = new SMTMethodAssignment(subStringN, subN);
        //            SMTAssertStatement subNAssert = new SMTAssertStatement(subAssignN);
        //            stmtVisitor.addAssertStmtToAllPrograms(subNAssert);
        //            tmpLengthValueAtN = tmpLengthValueNPLUS1;
        //
        //            //SIXTH:     (= tmpStringn (CharAt s i) )
        //            //            (assert (= tmpStringn ":" ) )
        //            SMTBinding tmpLengthI = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int);
        //            SMTBindingValue tmpLengthValueI = new SMTBindingValue(tmpLengthI);
        //            SMTCharAtMethodCall charAtI = new SMTCharAtMethodCall(baseBindingValue, tmpLengthValueI);
        //            SMTBinding tmpStringn = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String);
        //            SMTMethodAssignment charAtMethodI = new SMTMethodAssignment(tmpStringn, charAtI);
        //            SMTAssertStatement charAtAssertI = new SMTAssertStatement(charAtMethodI);
        //            stmtVisitor.addAssertStmtToAllPrograms(charAtAssertI);
        //
        //            SMTSimpleAssignment simpleAssignForCharAtI = new SMTSimpleAssignment(tmpStringn, argumentSMTForm);
        //            SMTAssertStatement simpleAssignAssertI = new SMTAssertStatement(simpleAssig    nForCharAtI);
        //            stmtVisitor.addAssertStmtToAllPrograms(simpleAssignAssertI);
        //        }

        this.result = null
    }


    private fun findMaxIndexOfArray(invokeExpr: InvokeExpr): Int {
        var array: Value? = null
        var maxIndex = -1
        stmtVisitor.jimpleDataFlowStatements
                .asSequence()
                .filterIsInstance<AssignStmt>()
                .map { it }
                .forEach {
                    if (array == null) {
                        if (it.rightOp == invokeExpr) {
                            array = it.leftOp
                        }
                    } else {
                        val rhs = it.rightOp
                        if (rhs is ArrayRef) {
                            val arrayRef = rhs
                            if (arrayRef.base == array) {
                                val index = arrayRef.index
                                if (index is IntConstant) {
                                    maxIndex = index.value
                                }
                            }
                        }
                    }
                }
        return maxIndex
    }

    private fun convertAPIMethodToSMT(invokeExpr: InvokeExpr) {
        //lhs treatment
        val returnType = invokeExpr.method.returnType
        val bindingType = stmtVisitor.createBindingType(returnType)
        val lhs = stmtVisitor.createTemporalBinding(bindingType)

        //rhs treatment
        //just propagate the taint value of previous statement
        val prevStmt = stmtVisitor.getPreviousDataFlowPathElement(currentStatement) ?: throw RuntimeException("there is no previous statement")

        //        SMTBinding bindingPreviousStmt = stmtVisitor.getBindingForTaintedValue(prevStmt);

        //it can be the case that the binding for the

        val bindingPreviousStmt = stmtVisitor.lhsOfLastAssertStmt



        if (bindingPreviousStmt.type != lhs.type) {
            val tmpBinding = stmtVisitor.createTemporalBinding(lhs.type)
            val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(tmpBinding))
            val assertStmt = SMTAssertStatement(simpleAss)
            stmtVisitor.addAssertStmtToAllPrograms(assertStmt)

            this.result = lhs
        } else if (bindingPreviousStmt != null) {
            val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(bindingPreviousStmt))
            val assertStmt = SMTAssertStatement(simpleAss)
            stmtVisitor.addAssertStmtToAllPrograms(assertStmt)

            this.result = lhs
        } else
            throw RuntimeException("previous stmt should be an AssignStmt!")
    }

    private fun isSourceMethod(invoke: InvokeExpr): Boolean {
        return sources
                .map { it.method.signature }
                .any { invoke.method.signature == it }
    }

    private fun isStringOperationSupportedBySMT(invokeExpr: InvokeExpr): Boolean {
        val methodSignature = invokeExpr.method.signature
        if (methodSignature == "<java.lang.String: java.lang.String substring(int,int)>"
                || methodSignature == "<java.lang.String: java.lang.String substring(int)>"
                || methodSignature == "<java.lang.String: boolean equals(java.lang.Object)>"
                || methodSignature == "<java.lang.String: boolean equalsIgnoreCase(java.lang.String)>"
                || methodSignature == "<java.lang.String: int indexOf(java.lang.String)>"
                || methodSignature == "<java.lang.String: int indexOf(int,int)>"
                || methodSignature == "<java.lang.String: boolean startsWith(java.lang.String)>"
                || methodSignature == "<java.lang.String: boolean matches(java.lang.String)>"
                || methodSignature == "<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>"
                || methodSignature == "<java.lang.String: boolean contains(java.lang.CharSequence)>"
                || methodSignature == "<java.lang.String: java.lang.String[] split(java.lang.String)>"
                || methodSignature == "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>")
            return true
        return false
    }


    fun setCurrentStatement(currentStatement: Stmt) {
        this.currentStatement = currentStatement
    }


    fun isExpressionThatNeedsToBeConvertedToSMT(invokeExpr: InvokeExpr): Boolean {
        val methodSignature = invokeExpr.method.signature
        if (methodSignature == "<java.lang.Integer: int parseInt(java.lang.String)>"
                || methodSignature == "<org.apache.http.client.methods.HttpGet: void <init>(java.lang.String)>"
                || methodSignature == "<java.net.URL: void <init>(java.lang.String)>"
                || methodSignature == "<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>"
                || methodSignature == "<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>"
                || methodSignature == "<android.telephony.SmsMessage: java.lang.String getDisplayOriginatingAddress()>"
                || methodSignature == "<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>"
                || methodSignature == "<java.util.Map: java.lang.Object get(java.lang.Object)>"
                || methodSignature == "<android.telephony.TelephonyManager: java.lang.String getNetworkOperator()>"
                || methodSignature == "<android.telephony.TelephonyManager: java.lang.String getSimOperator()>")
            return true
        return false
    }

    fun convertSpecialExpressionsToSMT(invokeExpr: InvokeExpr, currentStmt: Stmt) {
        val methodSignature = invokeExpr.method.signature

        //###### sendTextMessage ####
        if (methodSignature == "<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>" || methodSignature == "<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>") {
            val taintBinding = stmtVisitor.getBindingForTaintedValue(currentStmt)
            convertSendTextMessage(taintBinding, invokeExpr)
        } else if (methodSignature.startsWith("<java.util.Map:")) {
            convertUtilMaps(methodSignature, currentStmt)
        } else if (methodSignature == "<android.telephony.SmsMessage: java.lang.String getDisplayOriginatingAddress()>") {
            convertSMSSenderNumber(currentStmt)
        } else if (methodSignature == "<android.telephony.TelephonyManager: java.lang.String getNetworkOperator()>") {
            val taintBinding = stmtVisitor.getBindingForTaintedValue(currentStmt)
            convertNetworkOperator(taintBinding, currentStmt)
        } else if (methodSignature == "<android.telephony.TelephonyManager: java.lang.String getSimOperator()>") {
            val taintBinding = stmtVisitor.getBindingForTaintedValue(currentStmt)
            convertSimOperator(taintBinding, currentStmt)
        } else
            throw RuntimeException("todo")//###### getDisplayOriginatingAddress() #######
        //###### Util.maps #######
    }


    private fun convertUtilMaps(methodSignature: String, currentStmt: Stmt) {
        if (methodSignature == "<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>") {
            val prevStmt = stmtVisitor.getPreviousDataFlowPathElement(currentStmt) ?: return
            val rhs = stmtVisitor.getBindingForTaintedValue(prevStmt) ?: return
            val apCurrent = stmtVisitor.getCorrectAccessPathForStmt(currentStmt)

            //we do not support Maps right now. Therefore, we take the type of the incoming taint (previous statement)
            val bindingType = rhs.type
            val localName = apCurrent.plainValue.name
            val lhs = SMTBinding(localName, bindingType, 0)
            stmtVisitor.addValueBindingToVariableDeclaration(apCurrent.plainValue, lhs)


            val simpleAss = SMTSimpleAssignment(lhs, SMTBindingValue(rhs))
            val assertStmt = SMTAssertStatement(simpleAss)
            stmtVisitor.addAssertStmtToAllPrograms(assertStmt)

            this.result = lhs
        } else if (methodSignature == "<java.util.Map: java.lang.Object get(java.lang.Object)>") {
            val prevStmt = stmtVisitor.getPreviousDataFlowPathElement(currentStmt) ?: return
            val rhs = stmtVisitor.getBindingForTaintedValue(prevStmt)
            this.result = rhs
        }//we take the previous taint-value
    }


    private fun convertSendTextMessage(taintBinding: SMTBinding?, invokeExpr: InvokeExpr) {
        if (taintBinding != null) {
            //sms number: we know that the length should be at least 4 and the characters are digits
            val smsNr = invokeExpr.getArg(0)
            if (smsNr.toString() == taintBinding.variableName) {
                val length = SMTLengthMethodCall(SMTBindingValue(taintBinding))
                val tmpBinding = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
                val lengthMethodAssignment = SMTMethodAssignment(tmpBinding, length)
                val lengthMethodAssert = SMTAssertStatement(lengthMethodAssignment)
                stmtVisitor.addAssertStmtToAllPrograms(lengthMethodAssert)
                // (assert (> int 4 ) )
                val valueThreeBinding = SMTConstantValue(4)
                val gtBinaryOperation = SMTSimpleBinaryOperation(SMTSimpleBinaryOperation.SMTSimpleBinaryOperator.GT, SMTBindingValue(tmpBinding), valueThreeBinding)
                val gtBinaryAssertion = SMTAssertStatement(gtBinaryOperation)
                stmtVisitor.addAssertStmtToAllPrograms(gtBinaryAssertion)

                //second: (assert (RegexIn a (RegexStar (RegexDigit "") ) ) )
                val isDigitOperation = SMTRegexDigitOperation(taintBinding)
                val isDigitAssert = SMTAssertStatement(isDigitOperation)
                //Todo: temporarily disabled this one due to performance reasons; please enable it!!
                stmtVisitor.addAssertStmtToAllPrograms(isDigitAssert)
            }
            //there is no return value
            this.result = null
        } else
            throw RuntimeException("it should be an assignment!")
    }

    private fun convertSMSSenderNumber(currentStmt: Stmt) {
        //Returns the originating address, or email from address if this message was from an email gateway. Returns null if originating address unavailable.
        val taintedValue = stmtVisitor.getBindingForTaintedValue(currentStmt)
        //FIRST: (assert (= s0 "+" ) )
        val s0 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String)
        val plusAssign = SMTSimpleAssignment(s0, SMTConstantValue("+"))
        val plusAssertion = SMTAssertStatement(plusAssign)
        stmtVisitor.addAssertStmtToAllPrograms(plusAssertion)

        //SECOND: (assert ( RegexIn s1 ( RegexStar ( RegexDigit "" ) ) ))
        val s1 = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.String)
        val digitOp = SMTRegexDigitOperation(s1)
        val digitAssert = SMTAssertStatement(digitOp)
        stmtVisitor.addAssertStmtToAllPrograms(digitAssert)

        //THIRD: (assert (or (= s2 (Concat s0 s1) ) (= s2 s1)) )
        val concat = SMTConcatMethodCall(SMTBindingValue(s0), SMTBindingValue(s1))
        val conacatAss = SMTMethodAssignment(taintedValue, concat)
        val simpleAss = SMTSimpleAssignment(taintedValue, SMTBindingValue(s1))
        val allOrStmts = HashSet<SMTStatement>()
        allOrStmts.add(conacatAss)
        allOrStmts.add(simpleAss)
        val orBinaryOp = SMTComplexBinaryOperation(SMTComplexBinaryOperator.OR, allOrStmts)
        val orAssertion = SMTAssertStatement(orBinaryOp)
        stmtVisitor.addAssertStmtToAllPrograms(orAssertion)

        //ToDo: email-address
    }


    private fun convertNetworkOperator(taintBinding: SMTBinding?, currentStmt: Stmt) {
        if (taintBinding != null) {
            val length = SMTLengthMethodCall(SMTBindingValue(taintBinding))
            val tmpBinding = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
            val lengthMethodAssignment = SMTMethodAssignment(tmpBinding, length)
            val lengthMethodAssert = SMTAssertStatement(lengthMethodAssignment)
            stmtVisitor.addAssertStmtToAllPrograms(lengthMethodAssert)

            //            // (assert (or (= int 5 ) (= int 6))
            //            SMTValue valueBinding5 = new SMTConstantValue<Integer>(5);
            //            SMTSimpleAssignment simpleAss5 = new SMTSimpleAssignment(tmpBinding, valueBinding5);
            //            SMTValue valueBinding6 = new SMTConstantValue<Integer>(6);
            //            SMTSimpleAssignment simpleAss6 = new SMTSimpleAssignment(tmpBinding, valueBinding6);
            //            Set<SMTStatement> allOrStmts = new HashSet<SMTStatement>();
            //            allOrStmts.add(simpleAss5);
            //            allOrStmts.add(simpleAss6);
            //            SMTComplexBinaryOperation orBinaryOp = new SMTComplexBinaryOperation(SMTComplexBinaryOperator.OR, allOrStmts);
            //            SMTAssertStatement orAssertion = new SMTAssertStatement(orBinaryOp);
            //            stmtVisitor.addAssertStmtToAllPrograms(orAssertion);

            val valueBinding5 = SMTConstantValue(5)
            val simpleAss5 = SMTSimpleAssignment(tmpBinding, valueBinding5)
            val assertStmt = SMTAssertStatement(simpleAss5)
            stmtVisitor.addAssertStmtToAllPrograms(assertStmt)
        } else
            throw RuntimeException("it should be an assignment!")
    }

    private fun convertSimOperator(taintBinding: SMTBinding?, currentStmt: Stmt) {
        if (taintBinding != null) {
            val length = SMTLengthMethodCall(SMTBindingValue(taintBinding))
            val tmpBinding = stmtVisitor.createTemporalBinding(SMTBinding.TYPE.Int)
            val lengthMethodAssignment = SMTMethodAssignment(tmpBinding, length)
            val lengthMethodAssert = SMTAssertStatement(lengthMethodAssignment)
            stmtVisitor.addAssertStmtToAllPrograms(lengthMethodAssert)

            //            // (assert (or (= int 5 ) (= int 6))
            //            SMTValue valueBinding5 = new SMTConstantValue<Integer>(5);
            //            SMTSimpleAssignment simpleAss5 = new SMTSimpleAssignment(tmpBinding, valueBinding5);
            //            SMTValue valueBinding6 = new SMTConstantValue<Integer>(6);
            //            SMTSimpleAssignment simpleAss6 = new SMTSimpleAssignment(tmpBinding, valueBinding6);
            //            Set<SMTStatement> allOrStmts = new HashSet<SMTStatement>();
            //            allOrStmts.add(simpleAss5);
            //            allOrStmts.add(simpleAss6);
            //            SMTComplexBinaryOperation orBinaryOp = new SMTComplexBinaryOperation(SMTComplexBinaryOperator.OR, allOrStmts);
            //            SMTAssertStatement orAssertion = new SMTAssertStatement(orBinaryOp);
            //            stmtVisitor.addAssertStmtToAllPrograms(orAssertion);

            val valueBinding5 = SMTConstantValue(5)
            val simpleAss5 = SMTSimpleAssignment(tmpBinding, valueBinding5)
            val assertStmt = SMTAssertStatement(simpleAss5)
            stmtVisitor.addAssertStmtToAllPrograms(assertStmt)
        } else
            throw RuntimeException("it should be an assignment!")
    }
}
