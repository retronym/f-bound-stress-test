import java.lang.reflect.*;
import java.util.*;

/**
 * Prints the generic signatures of the stress types using core reflection
 * (java.lang.reflect.Type), which decodes the Signature class-file attribute —
 * the same data a Zinc-style API extractor walks. Run: java -cp out SigDump
 */
public class SigDump {

    public static void main(String[] args) throws Exception {
        for (Class<?> c : new Class<?>[]{
                L0.class, Mirror.class, L8.class, Leaf.class,
                Carrier.class, Yin.class, Yang.class,
                ConcYin.class, WildStress.class }) {
            dumpType(c);
        }
        System.out.println();
        dumpMethod(Utilities.class, "mutualCompare");
        dumpMethod(Utilities.class, "roundTrip");
    }

    static void dumpType(Class<?> c) {
        System.out.println("== " + c.getSimpleName() + " ==");

        TypeVariable<?>[] tps = c.getTypeParameters();
        if (tps.length > 0) {
            StringJoiner sj = new StringJoiner(", ", "<", ">");
            for (TypeVariable<?> tp : tps) {
                StringJoiner bounds = new StringJoiner(" & ");
                for (Type b : tp.getBounds()) bounds.add(name(b));
                sj.add(tp.getName() + " extends " + bounds);
            }
            System.out.println("  type params : " + sj);
        }

        Type sup = c.getGenericSuperclass();
        if (sup != null && sup != Object.class)
            System.out.println("  superclass  : " + name(sup));
        for (Type i : c.getGenericInterfaces())
            System.out.println("  implements  : " + name(i));

        for (Method m : c.getDeclaredMethods()) {
            if (m.isSynthetic() || m.isBridge()) continue;   // skip the bridges for clarity
            System.out.println("  method      : " + signature(m));
        }
        // Show bridges separately so the proliferation is visible.
        for (Method m : c.getDeclaredMethods())
            if (m.isBridge())
                System.out.println("  bridge      : " + signature(m));
        System.out.println();
    }

    static void dumpMethod(Class<?> c, String name) {
        for (Method m : c.getDeclaredMethods())
            if (m.getName().equals(name))
                System.out.println("== " + c.getSimpleName() + "." + name + " ==\n  " + signature(m) + "\n");
    }

    static String signature(Method m) {
        StringJoiner tp = new StringJoiner(", ", "<", "> ");
        for (TypeVariable<?> v : m.getTypeParameters()) {
            StringJoiner bounds = new StringJoiner(" & ");
            for (Type b : v.getBounds()) bounds.add(name(b));
            tp.add(v.getName() + " extends " + bounds);
        }
        String tps = m.getTypeParameters().length == 0 ? "" : tp.toString();

        StringJoiner ps = new StringJoiner(", ", "(", ")");
        for (Type p : m.getGenericParameterTypes()) ps.add(name(p));
        return tps + name(m.getGenericReturnType()) + " " + m.getName() + ps;
    }

    /** Render a reflected Type the way it reads in source. */
    static String name(Type t) {
        if (t instanceof Class<?> c) return c.getSimpleName();
        if (t instanceof ParameterizedType pt) {
            StringJoiner args = new StringJoiner(", ", "<", ">");
            for (Type a : pt.getActualTypeArguments()) args.add(name(a));
            return name(pt.getRawType()) + args;
        }
        if (t instanceof WildcardType wt) {
            if (wt.getLowerBounds().length > 0) return "? super " + name(wt.getLowerBounds()[0]);
            Type up = wt.getUpperBounds()[0];
            return up == Object.class ? "?" : "? extends " + name(up);
        }
        if (t instanceof GenericArrayType ga) return name(ga.getGenericComponentType()) + "[]";
        if (t instanceof TypeVariable<?> tv) return tv.getName();   // don't recurse into bounds -> would loop
        return t.getTypeName();
    }
}
