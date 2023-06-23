package dev.luna5ama.trollhack

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

class CoreCodeGenProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        genMetadata(resolver)

        return emptyList()
    }

    private fun genMetadata(resolver: Resolver) {
        if (resolver.getAllFiles().any { it.fileName == "Metadata.kt" }) return

        FileSpec.builder("dev.luna5ama.trollhack", "Metadata")
            .addType(
                TypeSpec.objectBuilder("Metadata")
                    .addAttribute("GROUP")
                    .addAttribute("ID")
                    .addAttribute("NAME")
                    .addAttribute("VERSION")
                    .build()
            )
            .build()
            .writeTo(environment.codeGenerator, Dependencies(false))
    }

    private fun TypeSpec.Builder.addAttribute(k: String) =
        addProperty(
            PropertySpec.builder(k, String::class)
                .addModifiers(KModifier.CONST)
                .initializer("\"${environment.options[k]!!}\"")
                .build()
        )
}