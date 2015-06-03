package decorice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Scope;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

@SuppressWarnings("unchecked")
public class DecoratorModule implements com.google.inject.Module {

    public static interface ScopedBindingBuilder {
        void in(Class<? extends Annotation> var1);
        void asEagerSingleton();
        void in(Scope scope);
    }

    public static interface LinkedBindingBuilder<T> {
        DecorationBindingBuilder<T> to(Key<? extends T> key);
        DecorationBindingBuilder<T> to(Class<? extends T> clazz);
    }

    public static interface DecorationBindingBuilder<T> {
        ScopedBindingBuilder decoratedBy(
                Class<? extends T> first,
                Class<? extends T>... rest);
    }

    public static interface AnnotatedBindingBuilder<T>
            extends LinkedBindingBuilder<T> {
        LinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType);
    }

    private class BindingBuilder<T> implements
            AnnotatedBindingBuilder<T>,
            DecorationBindingBuilder<T>,
            ScopedBindingBuilder
    {
        @Override
        public LinkedBindingBuilder<T> annotatedWith(
                final Class<? extends Annotation> annotationType) {
            annotation = Optional.of(b -> b.annotatedWith(annotationType));
            return this;
        }

        @Override
        public ScopedBindingBuilder decoratedBy(
                final Class<? extends T> first,
                final Class<? extends T>... rest) {
            decorators = new ArrayList<>();
            decorators.add(first);
            decorators.addAll(Arrays.asList(rest));
            return this;
        }

        @Override
        public DecorationBindingBuilder<T> to(final Key<? extends T> key) {
            link = b -> b.to(key);
            return this;
        }

        @Override
        public DecorationBindingBuilder<T> to(final Class<? extends T> clazz) {
            link = b -> b.to(clazz);
            return this;
        }

        @Override
        public void in(final Class<? extends Annotation> annotationType) {
            scope = Optional.of(b -> b.in(annotationType));
        }

        @Override
        public void asEagerSingleton() {
            scope = Optional.of(com.google.inject.binder.ScopedBindingBuilder::asEagerSingleton);
        }

        @Override
        public void in(final Scope scope) {
            DecoratorModule.this.scope = Optional.of(b -> b.in(scope));
        }

    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static class Decorated implements DecoratedBy {
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

    private Class targetClass;
    private Function<
            com.google.inject.binder.LinkedBindingBuilder,
            com.google.inject.binder.ScopedBindingBuilder> link;
    private List<Class> decorators;
    private Optional<Function<
            com.google.inject.binder.AnnotatedBindingBuilder,
            com.google.inject.binder.LinkedBindingBuilder>> annotation =
            Optional.empty();
    private Optional<
            Consumer<com.google.inject.binder.ScopedBindingBuilder>> scope =
            Optional.empty();

    public <T> AnnotatedBindingBuilder<T> bind(Class<T> clazz) {
        targetClass = clazz;
        return new BindingBuilder<>();
    }

    @Override
    public void configure(final Binder binder) {
        new AbstractModule() {
            @Override
            protected void configure() {
                applyScope(applyAnnotation(bind(targetClass))
                        .to(decorators.get(0)));

                IntStream.range(1, decorators.size()).forEach(i ->
                        bind(targetClass)
                                .annotatedWith(Decorated.by(decorators.get(i - 1)))
                                .to(decorators.get(i)));

                applyLink(bind(targetClass)
                        .annotatedWith(Decorated.by(last(decorators))));

            }
        }.configure(binder);
    }

    private void applyLink(
            final com.google.inject.binder.LinkedBindingBuilder b) {
        link.apply(b);
    }

    private com.google.inject.binder.LinkedBindingBuilder applyAnnotation(
            final com.google.inject.binder.AnnotatedBindingBuilder b) {
        if (!annotation.isPresent()) {
            return b;
        }
        return annotation.get().apply(b);
    }

    private void applyScope(
            final com.google.inject.binder.ScopedBindingBuilder b) {
        if (scope.isPresent()) {
            scope.get().accept(b);
        }
    }

    private static Class<?> last(final List<Class> classes) {
        return classes.get(classes.size() - 1);
    }
}

