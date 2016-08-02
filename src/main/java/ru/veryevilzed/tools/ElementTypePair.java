package ru.veryevilzed.tools;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * Created by zed on 02.08.16.
 */
public class ElementTypePair {
    public ElementTypePair(TypeElement element, DeclaredType type) {
        this.element = element;
        this.type = type;
    }

    final TypeElement element;
    final DeclaredType type;
}
