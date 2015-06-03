# decorice
helper library to bind chain of decorators in guice

You have a chain of decorators (D2 -> D1 -> D0) to bind with `Guice`. 

    interface Foo { /* .... * }

    class D2 implements Foo {
        D2(Foo decorated /* ... */) {/* ... */}
        /* ... */
    }

    class D1 implements Foo {
        D1(Foo decorated /* ... */) {/* ... */}
        /* ... */
    }

    class D0 implements Foo { /* ... */ }

`Guice` does not offer much syntactic sugar to help with binding such chain. (inspired by [an answer](http://stackoverflow.com/a/6197660/614800) posted on stackoverflow and [a blog] (http://www.mikevalenty.com/configuring-decorators-with-google-guice/) post) `decorice` complements `Guice` by helping you bind the chain with less boilerplate.

First, on each decorator, annotate the constructor parameter associated with the decorated instance with the `@DecorateBy` annotation passing the decorator class name as an attribute to the annotation (the decorator is oblivious to the concrete class it decorates at runtime):

    class D2 implements Foo {
        D2(@DecoratedBy(D2.class) Foo decorated /* ... */) {/* ... */}
        /* ... */
    }

    class D1 implements Foo {
        D1(@DecoratedBy(D1.class) Foo decorated /* ... */) {/* ... */}
        /* ... */
    }

Later, you bind the chain in a `Module` with:

    install(new DecoratorModule() {{
        bind(Foo.class)
                .to(D0.class)
                .decoratedBy(
                        D2.class,
                        D1.class);
    }});
    
The order of the classes on the call to the `decoratedBy` method defines the order of decorations.

Binding annotations, scopes, and keys are supported:
    
    bind(Foo.class)
            .annotatedWith(SomeAnnotation.class)
            .to(D0.class);
    install(new DecoratorModule() {{
        bind(Foo.class)
                .annotatedWith(SomeOtherAnnotation.class)
                .to(Key.get(Foo.class, SomeAnnotation.class))
                .decoratedBy(
                        D2.class,
                        D1.class)
                .in(Singleton.class);
    }});

**Limitations**: does not support generics (yet)

Compatible with `Guice` 3.0
