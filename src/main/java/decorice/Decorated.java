package decorice;

import java.lang.annotation.Annotation;

public class Decorated implements DecoratedBy {
    private final Class<?> value;

    private Decorated(final Class<?> value) {
        this.value = value;
    }

    public static DecoratedBy by(final Class<?> class__) {
        return new Decorated(class__);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return DecoratedBy.class;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof DecoratedBy)) {
            return false;
        }
        final DecoratedBy rhs = (DecoratedBy) obj;
        return value().equals(rhs.value());
    }

    @Override
    public int hashCode() {
        return 127 * "value".hashCode() ^ value.hashCode();
    }

    @Override
    public Class<?> value() {
        return value;
    }
}
