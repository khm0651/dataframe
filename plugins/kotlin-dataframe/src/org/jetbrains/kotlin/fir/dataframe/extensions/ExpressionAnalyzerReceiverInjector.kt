/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.extensions

import org.jetbrains.kotlin.cli.common.repl.replEscapeLineBreaks
import org.jetbrains.kotlin.fir.dataframe.*
import org.jetbrains.kotlin.fir.dataframe.Names.COLUM_GROUP_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.Names.DF_CLASS_ID
import org.jetbrains.kotlin.fir.dataframe.loadInterpreter
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.SimpleFrameColumn
import kotlin.math.abs

class SchemaContext(val properties: List<SchemaProperty>)

fun KotlinTypeFacade.generateAccessorsScopesForRefinedCall(
    functionCall: FirFunctionCall,
    scopeState: MutableMap<ClassId, SchemaContext>,
    tokenState: MutableMap<ClassId, SchemaContext>,
    associatedScopes: MutableMap<ClassId, List<ConeKotlinType>>,
    reporter: InterpretationErrorReporter = InterpretationErrorReporter.DEFAULT,
    nextName: (String) -> ClassId,
    nextScope: (String) -> ClassId,
): List<ConeKotlinType> {
    // root marker is generated as return type of intercepted function
    val (rootMarker, dataFrameSchema) =
        analyzeRefinedCallShape(functionCall, reporter) ?: return emptyList()

    val types: MutableList<ConeClassLikeType> = mutableListOf()

    fun PluginDataFrameSchema.materialize(rootMarker: ConeTypeProjection? = null, suggestedName: String? = null): ConeTypeProjection {
        val scopeId: ClassId
        var tokenId = rootMarker?.type?.classId
        if (tokenId == null) {
            requireNotNull(suggestedName)
            tokenId = nextName(suggestedName)
            scopeId = nextScope(suggestedName)
        } else {
            scopeId = nextScope(tokenId.shortClassName.asString())
        }
        val marker = rootMarker ?: ConeClassLikeLookupTagImpl(tokenId)
            .constructClassType(emptyArray(), isNullable = false)
        val properties = columns().map {
            fun PluginDataFrameSchema.materialize(column: SimpleCol): ConeTypeProjection {
                // TODO
                val name = "${column.name.titleCase().replEscapeLineBreaks()}_${abs(functionCall.calleeReference.name.hashCode() + tokenId.hashCode())}"
                return materialize(suggestedName = name)
            }
            @Suppress("USELESS_IS_CHECK")
            when (it) {
                is SimpleColumnGroup -> {
                    val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize(it)
                    val columnsContainerReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(COLUM_GROUP_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = false
                        )

                    val dataRowReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(Names.DATA_ROW_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = false
                        )

                    SchemaProperty(marker, it.name, dataRowReturnType, columnsContainerReturnType)
                }

                is SimpleFrameColumn -> {
                    val nestedClassMarker = PluginDataFrameSchema(it.columns()).materialize(it)
                    val frameColumnReturnType =
                        ConeClassLikeTypeImpl(
                            ConeClassLikeLookupTagImpl(DF_CLASS_ID),
                            typeArguments = arrayOf(nestedClassMarker),
                            isNullable = it.nullable
                        )

                    SchemaProperty(
                        marker = marker,
                        name = it.name,
                        dataRowReturnType = frameColumnReturnType,
                        columnContainerReturnType = frameColumnReturnType.toFirResolvedTypeRef().projectOverDataColumnType()
                    )
                }

                is SimpleCol -> SchemaProperty(
                    marker = marker,
                    name = it.name,
                    dataRowReturnType = it.type.type(),
                    columnContainerReturnType = it.type.type().toFirResolvedTypeRef().projectOverDataColumnType()
                )

                else -> TODO("shouldn't happen")
            }
        }
        scopeState[scopeId] = SchemaContext(properties)
        tokenState[tokenId] = SchemaContext(properties)
        types += ConeClassLikeLookupTagImpl(scopeId).constructClassType(emptyArray(), isNullable = false)
        return marker
    }

    dataFrameSchema.materialize(rootMarker)
    associatedScopes[rootMarker.classId!!] = types
    return types
}

fun String.titleCase() = replaceFirstChar { it.uppercaseChar() }

data class CallResult(val rootMarker: ConeClassLikeType, val dataFrameSchema: PluginDataFrameSchema)

fun KotlinTypeFacade.analyzeRefinedCallShape(call: FirFunctionCall, reporter: InterpretationErrorReporter): CallResult? {
    val callReturnType = call.coneTypeSafe<ConeClassLikeType>() ?: return null
    if (callReturnType.classId != DF_CLASS_ID) return null
    val rootMarker = callReturnType.typeArguments[0]
    // rootMarker is expected to be a token generated by the plugin.
    // it's implied by "refined call"
    // thus ConeClassLikeType
    if (rootMarker !is ConeClassLikeType) {
        return null
    }

    val origin = rootMarker.toSymbol(session)?.origin
    val notFromPlugin = origin !is FirDeclarationOrigin.Plugin || origin.key != DataFramePlugin
    // temporary hack to workaround Token classes not existing when toSymbol is called
    val notToken = rootMarker.classId?.shortClassName?.asString()?.startsWith("Token") != true
    if (notFromPlugin && notToken) {
        return null
    }

    val processor = call.loadInterpreter(session) ?: return null

    val dataFrameSchema = interpret(call, processor, reporter = reporter)
        .let {
            val value = it?.value
            if (value !is PluginDataFrameSchema) {
                if (!reporter.errorReported) {
                    reporter.reportInterpretationError(call, "${processor::class} must return ${PluginDataFrameSchema::class}, but was ${value}")
                }
                return null
            }
            value
        }


    return CallResult(rootMarker, dataFrameSchema)
}

fun FirFunctionCall.functionSymbol(): FirNamedFunctionSymbol {
    val firResolvedNamedReference = calleeReference as FirResolvedNamedReference
    return firResolvedNamedReference.resolvedSymbol as FirNamedFunctionSymbol
}

class Arguments(val refinedArguments: List<RefinedArgument>) : List<RefinedArgument> by refinedArguments

data class RefinedArgument(val name: Name, val expression: FirExpression) {

    override fun toString(): String {
        return "RefinedArgument(name=$name, expression=${expression})"
    }
}

data class SchemaProperty(
    val marker: ConeTypeProjection,
    val name: String,
    val dataRowReturnType: ConeKotlinType,
    val columnContainerReturnType: ConeKotlinType,
    val override: Boolean = false
)
