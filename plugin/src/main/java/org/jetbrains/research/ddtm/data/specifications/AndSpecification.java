package org.jetbrains.research.ddtm.data.specifications;

import java.util.List;

public class AndSpecification<T> extends AbstractPatternSpecification<T> {
    private final List<AbstractPatternSpecification<T>> leafComponents;

    @SafeVarargs
    AndSpecification(AbstractPatternSpecification<T>... selectors) {
        this.leafComponents = List.of(selectors);
    }

    @Override
    public boolean test(T t) {
        return leafComponents.stream().allMatch(comp -> (comp.test(t)));
    }
}
