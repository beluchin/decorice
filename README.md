# decorice
helper library to bind chain of decorators in guice

You have a chain of decorators (D2 -> D1 -> D0) to bind with `Guice`. 

    interface Foo {
        String bar();
    }

    class D2 implements Foo {
        D2(Foo decorated /* ... */) {/* ... */}
        /* ... */
    }

    class D1 implements Foo {
        D1(Foo decorated /* ... */) {/* ... */}
        /* ... */
    }

    class D0 implements Foo {/* ... */}

`Guice` does not offer much syntactic sugar to help with binding such chain. (inspired by [an answer](http://stackoverflow.com/a/6197660/614800) posted on stackoverflow) `decorice` complements `Guice` by helping you bind the chain with less boilerplate.

First, on each decorator, annotate the constructor parameter associated with the decorated instance with the `@DecorateBy` annotation:

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
        bind(Foo.class).to(
                D2.class,
                D1.class,
                D0.class);
    }});
    
The order of the classes on the call to the `to` method defines the order of decorations.

Binding annotations and scopes are supported:
    
    install(new DecoratorModule() {{
        bind(Foo.class)
                .annotatedWith(SomeAnnotation.class)
                .to(
                    D2.class,
                    D1.class,
                    D0.class)
                .in(Singleton.class);
    }});
    
Compatible with `Guice` 3.0
