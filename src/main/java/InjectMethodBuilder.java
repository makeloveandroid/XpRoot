import com.google.common.collect.Lists;
import jdk.nashorn.internal.ir.CallNode;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction;
import org.jf.dexlib2.immutable.instruction.ImmutableInstructionFactory;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * Created by ljh102 on 2017/1/30.
 * 创建需要注入的方法
 */
public class InjectMethodBuilder {

    public static MethodReference getStaticContextMethodRef() {
        return new ImmutableMethodReference("Lcom/ks/xproot/XpRoot;", "start", null, "V");
    }

    public static Method buildStaticContextMethod(String className) {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList(
                ImmutableInstructionFactory.INSTANCE.makeInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, getStaticContextMethodRef()),
                ImmutableInstructionFactory.INSTANCE.makeInstruction10x(Opcode.RETURN_VOID)
        );
        ImmutableMethodImplementation methodImpl = new ImmutableMethodImplementation(0, instructions, null, null);
        return new ImmutableMethod(className, "<clinit>", new ArrayList<>(), "V", AccessFlags.STATIC.getValue() | AccessFlags.CONSTRUCTOR.getValue(), null, null, methodImpl);
    }

    public static Method buildStaticContextMethod(String className, Method mehtod) {
        ArrayList<Instruction> instructions = Lists.newArrayList(
                ImmutableInstructionFactory.INSTANCE.makeInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, getStaticContextMethodRef())
        );
        MethodImplementation implementation = mehtod.getImplementation();
        MethodImplementation newImplementation = null;
        if (implementation != null) {
            int registerCount = implementation.getRegisterCount();
            for (Instruction instruction : mehtod.getImplementation().getInstructions()) {
                instructions.add(instruction);
            }
            newImplementation = new ImmutableMethodImplementation(registerCount, instructions, implementation.getTryBlocks(), implementation.getDebugItems());
        }

        return new ImmutableMethod(className, mehtod.getName(), mehtod.getParameters(), mehtod.getReturnType(), mehtod.getAccessFlags(), mehtod.getAnnotations(),
                mehtod.getHiddenApiRestrictions(), newImplementation);
    }


    public static MethodReference getSuperRf(String className) {
        return new ImmutableMethodReference(className, "<init>", null, "V");
    }


    public static Method changeInitSuperMethod(String className, Method mehtod, String callClassName) {
        ArrayList<Instruction> instructions = Lists.newArrayList(
                ImmutableInstructionFactory.INSTANCE.makeInstruction35c(Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0, getSuperRf(callClassName))
        );
        MethodImplementation implementation = mehtod.getImplementation();
        MethodImplementation newImplementation = null;
        if (implementation != null) {
            int registerCount = implementation.getRegisterCount();
            boolean isFirst = false;
            for (Instruction instruction : mehtod.getImplementation().getInstructions()) {
                // 删除第一条指令 就是super方法
                if (isFirst) {
                    instructions.add(instruction);
                }
                isFirst = true;
            }
            newImplementation = new ImmutableMethodImplementation(registerCount, instructions, implementation.getTryBlocks(), implementation.getDebugItems());
        }

        return new ImmutableMethod(className, mehtod.getName(), mehtod.getParameters(), mehtod.getReturnType(), mehtod.getAccessFlags(), mehtod.getAnnotations(),
                mehtod.getHiddenApiRestrictions(), newImplementation);
    }
}