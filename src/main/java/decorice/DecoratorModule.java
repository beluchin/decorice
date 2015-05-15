package decorice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Scope;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

@SuppressWarnings("unchecked")
public class DecoratorModule implements com.google.inject.Module {

    public static interface ScopedBindingBuilder {
        void in(Class<? extends Annotation> var1);
        void asEagerSingleton();
        void in(Scope scope);
    }

    public static interface LinkedBindingBuilder<T>
            extends ScopedBindingBuilder {
        ScopedBindingBuilder to(
                Class<? extends T> first,
                Class<? extends T> second,
                Class<? extends T>... rest);
    }

    public static interface AnnotatedBindingBuilder<T>
            extends LinkedBindingBuilder<T> {
        LinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotationType);
    }

    private class BindingBuilder<T> implements AnnotatedBindingBuilder<T> {
        @Override
        public LinkedBindingBuilder<T> annotatedWith(
                final Class<? extends Annotation> annotationType) {
            annotation = Optional.of(b -> b.annotatedWith(annotationType));
            return this;
        }

        @Override
        public ScopedBindingBuilder to(
                final Class<? extends T> first,
                final Class<? extends T> second,
                final Class<? extends T>... rest) {
            classes = concat(Stream.of(first, second), stream(rest))
                    .collect(toList());
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

    private Class targetClass;
    private List<Class> classes;
    private Optional<Function<
            com.google.inject.binder.AnnotatedBindingBuilder,
            com.google.inject.binder.LinkedBindingBuilder>> annotation =
            Optional.empty();
    private Optional<
            Consumer<com.google.inject.binder.ScopedBindingBuilder>> scope =
            Optional.empty();
    private static ThreadLocal<DecoratorModule> decoratorModule = new ThreadLocal<>();

    public <T> AnnotatedBindingBuilder<T> bind(Class<T> clazz) {
        final DecoratorModule module = new DecoratorModule();
        module.targetClass = clazz;
        decoratorModule.set(module);
        return module.newBindingBuilder();
    }

    @Override
    public void configure(final Binder binder) {
        new AbstractModule() {
            @Override
            protected void configure() {
                final DecoratorModule m = decoratorModule.get();

                // TODO generalize on the number of classes

                m.applyScope(m.applyAnnotation(bind(m.targetClass))
                        .to(m.classes.get(0)));

                IntStream.range(0, m.classes.size() - 1).forEach(i ->
                        bind(m.targetClass)
                                .annotatedWith(Decorated.by(m.classes.get(i)))
                                .to(m.classes.get(i + 1)));

            }
        }.configure(binder);
    }

    private <T> AnnotatedBindingBuilder<T> newBindingBuilder() {
        return new BindingBuilder<>();
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
}

