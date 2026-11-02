package suchi.morphe.patches.duchinese

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FRAGMENT = "Lorg/sinamon/duchinese/ui/views/marquee/MarqueeActivityFragment;"
private const val BUILDER = "Landroidx/appcompat/app/b\$a;"
private const val DIAGNOSTICS = "Lsuchi/morphe/extension/duchinese/DownloadDiagnostics;"

private val readingErrorFingerprint = Fingerprint(
    definingClass = FRAGMENT.dropLast(1) + "\$d;",
    name = "a",
    returnType = "V",
    parameters = listOf("Lf5/u;"),
)

private val downloadDialogFingerprint = Fingerprint(
    definingClass = FRAGMENT,
    name = "d4",
    returnType = "V",
    parameters = listOf("Z"),
)

@Suppress("unused")
val downloadDiagnosticsPatch = bytecodePatch(
    name = "Show reading download errors",
    description = "Adds the Volley error class, HTTP status and safe response metadata to " +
        "Du Chinese's reading download dialog. Does not change requests or account entitlement.",
    default = false,
) {
    compatibleWith(duChineseCompatibility)
    extendWith("extensions/duchinese.mpe")

    execute {
        val fields = mutableMapOf<String, Map<String, String>>()
        classDefForEach { classDef ->
            if (classDef.type == "Lf5/u;" || classDef.type == "Lf5/k;") {
                fields[classDef.type] = classDef.fields.associate { it.name to it.type }
            }
        }
        if (fields["Lf5/u;"]?.get("x") != "Lf5/k;" ||
            fields["Lf5/k;"]?.get("a") != "I" ||
            fields["Lf5/k;"]?.get("b") != "[B" ||
            fields["Lf5/k;"]?.get("c") != "Ljava/util/Map;"
        ) {
            throw PatchException("Du Chinese's reading response metadata has changed.")
        }

        val callback = readingErrorFingerprint.method
        val callbackInstructions = callback.implementation!!.instructions.toList()
        val returnIndex = callbackInstructions.withIndex()
            .singleOrNull { it.value.opcode == Opcode.RETURN_VOID }?.index
            ?: throw PatchException("Du Chinese's reading callback return has changed.")
        if (callback.implementation!!.registerCount != 2 ||
            callbackInstructions.firstOrNull()?.opcode != Opcode.IGET_OBJECT ||
            returnIndex != callbackInstructions.lastIndex
        ) {
            throw PatchException("Du Chinese's reading error callback has changed.")
        }

        val dialog = downloadDialogFingerprint.method
        val instructions = dialog.implementation!!.instructions.toList()
        fun MethodReference.matches(name: String, parameters: List<String>, returnType: String) =
            definingClass == BUILDER && this.name == name &&
                parameterTypes == parameters && this.returnType == returnType

        val messageCall = instructions.withIndex().singleOrNull { (_, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.matches("g", listOf("I"), BUILDER) == true
        } ?: throw PatchException("Du Chinese's download message call has changed.")
        val message = messageCall.value as? FiveRegisterInstruction
            ?: throw PatchException("Du Chinese's download message registers have changed.")
        val constructor = instructions.withIndex().singleOrNull { (_, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.matches("<init>", listOf("Landroid/content/Context;", "I"), "V") == true
        } ?: throw PatchException("Du Chinese's download dialog constructor has changed.")
        val context = constructor.value as? FiveRegisterInstruction
            ?: throw PatchException("Du Chinese's download context registers have changed.")
        val titleCall = instructions.getOrNull(constructor.index + 1)
        val title = titleCall as? FiveRegisterInstruction
        val titleReference = (titleCall as? ReferenceInstruction)?.reference as? MethodReference
        val titleResult = instructions.getOrNull(constructor.index + 2)
        val messageResult = instructions.getOrNull(messageCall.index + 1)
        if (messageCall.value.opcode != Opcode.INVOKE_VIRTUAL ||
            constructor.value.opcode != Opcode.INVOKE_DIRECT ||
            message.registerCount != 2 || context.registerCount != 3 ||
            context.registerD != 2 || context.registerE != 3 ||
            message.registerC != 0 || message.registerD != 5 ||
            titleCall?.opcode != Opcode.INVOKE_VIRTUAL ||
            titleReference?.matches("s", listOf("I"), BUILDER) != true ||
            title?.registerCount != 2 || title.registerC != context.registerC ||
            titleResult?.opcode != Opcode.MOVE_RESULT_OBJECT ||
            (titleResult as? OneRegisterInstruction)?.registerA != message.registerC ||
            constructor.index + 3 != messageCall.index ||
            messageResult?.opcode != Opcode.MOVE_RESULT_OBJECT ||
            (messageResult as? OneRegisterInstruction)?.registerA != message.registerD
        ) {
            throw PatchException("Du Chinese's download dialog layout has changed.")
        }

        val messageRegister = message.registerD
        dialog.replaceInstruction(
            messageCall.index,
            "move v${context.registerE}, v$messageRegister",
        )
        dialog.addInstructions(
            messageCall.index + 1,
            """
                invoke-virtual { v${context.registerD}, v$messageRegister }, Landroid/content/Context;->getText(I)Ljava/lang/CharSequence;
                move-result-object v$messageRegister
                invoke-static { v$messageRegister, v${context.registerE} }, $DIAGNOSTICS->appendToMessage(Ljava/lang/CharSequence;I)Ljava/lang/CharSequence;
                move-result-object v$messageRegister
                invoke-virtual { v${message.registerC}, v$messageRegister }, $BUILDER->h(Ljava/lang/CharSequence;)$BUILDER
            """.trimIndent(),
        )
        callback.replaceInstruction(returnIndex, "invoke-static { }, $DIAGNOSTICS->clear()V")
        callback.addInstruction(returnIndex + 1, "return-void")
        callback.addInstruction(
            0,
            "invoke-static/range { p1 .. p1 }, $DIAGNOSTICS->capture(Ljava/lang/Object;)V",
        )
    }
}
