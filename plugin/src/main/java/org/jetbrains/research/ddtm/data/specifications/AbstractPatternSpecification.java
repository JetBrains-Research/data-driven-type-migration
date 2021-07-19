package org.jetbrains.research.ddtm.data.specifications;

import java.util.function.Predicate;

public abstract class AbstractPatternSpecification<T> implements Predicate<T> {
    public AbstractPatternSpecification<T> and(AbstractPatternSpecification<T> other) {
        return new AndSpecification<>(this, other);
    }
}
