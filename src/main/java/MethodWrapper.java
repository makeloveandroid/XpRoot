import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.util.List;
import java.util.Set;

public class MethodWrapper implements Method {


    @NotNull
    @Override
    public String getDefiningClass() {
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return null;
    }

    @NotNull
    @Override
    public List<? extends CharSequence> getParameterTypes() {
        return null;
    }

    @NotNull
    @Override
    public List<? extends MethodParameter> getParameters() {
        return null;
    }

    @NotNull
    @Override
    public String getReturnType() {
        return null;
    }

    @Override
    public int compareTo(@NotNull MethodReference o) {
        return 0;
    }

    @Override
    public int getAccessFlags() {
        return 0;
    }

    @NotNull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return null;
    }

    @NotNull
    @Override
    public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
        return null;
    }

    @Nullable
    @Override
    public MethodImplementation getImplementation() {
        return null;
    }

    @Override
    public void validateReference() throws InvalidReferenceException {

    }
}
