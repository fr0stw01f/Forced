package me.zhenhao.forced.decisionmaker.analysis.stringtotypeextractor

import me.zhenhao.forced.apkspecific.CodeModel.CodePositionManager
import soot.jimple.infoflow.handlers.ResultsAvailableHandler
import soot.jimple.infoflow.results.InfoflowResults
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG
import java.util.*

class StringToPrimitiveTypeExtractorDataflowHandler(private val valuesToFuzz: MutableMap<Int, Set<Any>>) : ResultsAvailableHandler {
    internal var codePositionManager = CodePositionManager.codePositionManagerInstance

    override fun onResultsAvailable(cfg: IInfoflowCFG, results: InfoflowResults) {
        for (sinkInfo in results.results.keySet()) {
            val sink = sinkInfo.sink
            val sinkExpr = sink.invokeExpr
            val sinkMethod = sinkExpr.method

            val values = HashSet<Any>()

            when (sinkMethod.signature) {
                "<java.lang.Boolean: boolean parseBoolean(java.lang.String)>" -> {
                    values.add("true")
                    values.add("false")
                }

            //we add two random values
                "<java.lang.Byte: byte parseByte(java.lang.String)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Byte: byte parseByte(java.lang.String, int)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Short: short parseShort(java.lang.String)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Short: short parseShort(java.lang.String, int)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Integer: int parseInteger(java.lang.String)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Integer: int parseInteger(java.lang.String, int)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Long: long parseLong(java.lang.String)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Long: long parseLong(java.lang.String, int)>" -> {
                    values.add("0")
                    values.add("42")
                }

            //we add two random values
                "<java.lang.Double: double parseDouble(java.lang.String)>" -> {
                    values.add("0")
                    values.add("42.0")
                }

            //we add two random values
                "<java.lang.Float: float parseFloat(java.lang.String)>" -> {
                    values.add("0")
                    values.add("20.75f")
                }
            }

            //all sources
            val sourceInfo = results.results.get(sinkInfo)
            sourceInfo
                    .map { it.source }
                    .map { codePositionManager.getCodePositionForUnit(it).id }
                    .forEach { valuesToFuzz.put(it, values) }
        }

    }

}
