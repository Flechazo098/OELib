package cc.sighs.oelib.util;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Predicate;

@Deprecated(since = "0.3.0")
public final class AnnotationScanUtil {
    @Deprecated(since = "0.3.0")
    public AnnotationScanUtil() {}
    @Deprecated(since = "0.3.0")
    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation, Set<String> packages) { return cc.sighs.oelib.misc.util.AnnotationScanUtil.findAnnotatedClasses(annotation, packages); }
    @Deprecated(since = "0.3.0")
    public static Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation, Set<String> packages, Predicate<Class<?>> filter) { return cc.sighs.oelib.misc.util.AnnotationScanUtil.findAnnotatedClasses(annotation, packages, filter); }
    @Deprecated(since = "0.3.0")
    public static Predicate<Class<?>> nonAbstractNonInterface() { return cc.sighs.oelib.misc.util.AnnotationScanUtil.nonAbstractNonInterface(); }
}
